package com.anshify.myapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.webkit.ValueCallback;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.view.WindowManager;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URLDecoder;



public class MainActivity extends Activity {
    WebView w;
    ValueCallback<Uri[]> fc;
    ImageView sp;
    long t0;
    String lastUrl = "";
    long lastBack = 0;
    static final String POLY = "(function(){if(window.__anshN)return;window.__anshN=1;function N(t,o){o=o||{};try{AnshifyApp.notify(String(t),String(o.body||''))}catch(e){}}N.permission='granted';N.requestPermission=function(cb){if(cb)cb('granted');return Promise.resolve('granted')};window.Notification=N})();";
    static final String OFFLINE = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head><body style='margin:0;background:#10151c;color:#e9eef5;font-family:sans-serif;display:flex;min-height:100vh;align-items:center;justify-content:center;text-align:center'><div style='padding:24px'><div style='font-size:56px'>&#128268;</div><h2>No connection</h2><p style='color:#8d9bb0'>This page could not load. Check your internet and try again.</p><button onclick='AnshifyApp.retry()' style='background:#35d07f;color:#06251a;border:0;border-radius:10px;padding:14px 28px;font-size:16px;font-weight:bold'>Retry</button></div></body></html>";
    

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        t0 = SystemClock.uptimeMillis();
        FrameLayout root = new FrameLayout(this);
        w = new WebView(this);
        root.addView(w, new FrameLayout.LayoutParams(-1, -1));
        sp = new ImageView(this);
        sp.setBackgroundColor(Color.parseColor("#10151c"));
        sp.setImageResource(R.drawable.splash);
        sp.setScaleType(ImageView.ScaleType.CENTER);
        root.addView(sp, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        w.addJavascriptInterface(new Bridge(), "AnshifyApp");
        w.setDownloadListener(new DownloadListener() { public void onDownloadStart(String url, String ua, String cd, String mime, long len) { handleDownload(url, ua, cd, mime); } });

        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        
        s.setSupportZoom(false); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false);
        w.setWebViewClient(new WebViewClient() {
            
            @Override
            public void onPageFinished(WebView v, String u) {  v.evaluateJavascript(POLY, null); hideSplash(); }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) { if (r.isForMainFrame()) { lastUrl = r.getUrl().toString(); v.loadDataWithBaseURL("https://offline.local/", OFFLINE, "text/html", "utf-8", null); } }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fc != null) fc.onReceiveValue(null);
                fc = cb;
                try { startActivityForResult(p.createIntent(), 77); }
                catch (Exception e) { fc = null; return false; }
                return true;
            }
            @Override
            public void onPermissionRequest(final PermissionRequest r) {
                runOnUiThread(new Runnable() { public void run() { r.grant(r.getResources()); } });
            }
            @Override
            public void onGeolocationPermissionsShowPrompt(String o, GeolocationPermissions.Callback cb) {
                cb.invoke(o, true, false);
            }
        });
        String[] need = new String[]{"android.permission.RECORD_AUDIO","android.permission.POST_NOTIFICATIONS","android.permission.WRITE_EXTERNAL_STORAGE"};
        if (need.length > 0 && Build.VERSION.SDK_INT >= 23) requestPermissions(need, 1);
        
        w.loadUrl("file:///android_asset/index.html");
    }

    void hideSplash() {
        long d = Math.max(0, 1200 - (SystemClock.uptimeMillis() - t0));
        sp.postDelayed(new Runnable() {
            public void run() {
                sp.animate().alpha(0f).setDuration(300).withEndAction(new Runnable() {
                    public void run() { sp.setVisibility(View.GONE); }
                });
            }
        }, d);
    }

    @Override
    public void onWindowFocusChanged(boolean f) {
        super.onWindowFocusChanged(f);
        if (f) w.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    protected void onActivityResult(int rq, int rs, Intent d) {
        super.onActivityResult(rq, rs, d);
        if (rq == 77 && fc != null) { fc.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(rs, d)); fc = null; }
    }

    @Override
    public void onBackPressed() {
        if (w.canGoBack()) { w.goBack(); return; }
        long now = SystemClock.uptimeMillis(); if (now - lastBack < 2000) super.onBackPressed(); else { lastBack = now; Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show(); }
    }

    void toast(final String m) { runOnUiThread(new Runnable() { public void run() { Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show(); } }); }

    void showNote(String title, String body) {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel("anshify", "Notifications", NotificationManager.IMPORTANCE_HIGH));
            Intent in = new Intent(this, MainActivity.class);
            in.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(this, 0, in, Build.VERSION.SDK_INT >= 23 ? (PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT) : PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder nb = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "anshify") : new Notification.Builder(this);
            nb.setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle(title).setContentText(body).setAutoCancel(true).setContentIntent(pi);
            try { nb.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher)); } catch (Exception e) {}
            if (Build.VERSION.SDK_INT < 26) nb.setPriority(Notification.PRIORITY_HIGH);
            nm.notify((int) (System.currentTimeMillis() % 100000000), nb.build());
        } catch (Exception e) {}
    }

    void saveBytes(String name, String mime, byte[] data) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
                cv.put(MediaStore.Downloads.MIME_TYPE, mime);
                cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                OutputStream o = getContentResolver().openOutputStream(u);
                o.write(data); o.close();
            } else {
                File d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                d.mkdirs();
                FileOutputStream o = new FileOutputStream(new File(d, name));
                o.write(data); o.close();
            }
            toast("Saved to Downloads: " + name);
        } catch (Exception e) { toast("Could not save file"); }
    }

    void saveDataUrl(String name, String url) {
        try {
            int c = url.indexOf(',');
            String head = url.substring(5, c);
            String mime = head.split(";")[0];
            if (mime.length() == 0) mime = "application/octet-stream";
            byte[] data = head.contains("base64") ? Base64.decode(url.substring(c + 1), Base64.DEFAULT) : URLDecoder.decode(url.substring(c + 1), "UTF-8").getBytes("UTF-8");
            saveBytes(name, mime, data);
        } catch (Exception e) { toast("Could not save file"); }
    }

    void handleDownload(String url, String ua, String cd, String mime) {
        String name = URLUtil.guessFileName(url, cd, mime).replaceAll("[^A-Za-z0-9._-]", "_");
        try {
            if (url.startsWith("blob:")) {
                final String n = name;
                w.evaluateJavascript("(function(){var x=new XMLHttpRequest();x.open('GET','" + url + "',true);x.responseType='blob';x.onload=function(){var r=new FileReader();r.onloadend=function(){AnshifyApp.saveData('" + n + "',r.result)};r.readAsDataURL(x.response)};x.send()})()", null);
                return;
            }
            if (url.startsWith("data:")) { saveDataUrl(name, url); return; }
            DownloadManager.Request q = new DownloadManager.Request(Uri.parse(url));
            q.setMimeType(mime);
            q.addRequestHeader("User-Agent", ua);
            String ck = CookieManager.getInstance().getCookie(url);
            if (ck != null) q.addRequestHeader("cookie", ck);
            q.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            q.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(q);
            toast("Downloading " + name);
        } catch (Exception e) { toast("Download failed"); }
    }

    class Bridge {
        @JavascriptInterface public void notify(final String t, final String b) { runOnUiThread(new Runnable() { public void run() { showNote(t, b); } }); }
        @JavascriptInterface public void notifyIn(final String t, final String b, int sec) { new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { public void run() { showNote(t, b); } }, sec * 1000L); }
        @JavascriptInterface public void toast(String m) { MainActivity.this.toast(m); }
        @JavascriptInterface public void share(final String s) { runOnUiThread(new Runnable() { public void run() { Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT, s); startActivity(Intent.createChooser(i, "Share")); } }); }
        @JavascriptInterface public void vibrate(long ms) { try { ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(ms); } catch (Exception e) {} }
        @JavascriptInterface public void keepAwake(final boolean on) { runOnUiThread(new Runnable() { public void run() { if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); } }); }
        @JavascriptInterface public void saveData(String name, String dataUrl) { saveDataUrl(name, dataUrl); }
        @JavascriptInterface public void retry() { runOnUiThread(new Runnable() { public void run() { if (lastUrl.length() > 0) w.loadUrl(lastUrl); else w.reload(); } }); }
        @JavascriptInterface public void exit() { runOnUiThread(new Runnable() { public void run() { finish(); } }); }
    }

    @Override protected void onPause() { super.onPause(); w.onPause(); }
    @Override protected void onResume() { super.onResume(); w.onResume(); }
}

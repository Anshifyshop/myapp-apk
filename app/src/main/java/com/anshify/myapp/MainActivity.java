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
    static final String ERRJS = "(function(){if(window.__anshE)return;window.__anshE=1;function r(m){try{AnshifyApp.jsError(String(m).slice(0,600))}catch(e){}}window.addEventListener('error',function(e){r((e.message||'error')+' @ '+String(e.filename||'').split('/').pop()+':'+(e.lineno||0))});window.addEventListener('unhandledrejection',function(e){r('Promise: '+((e.reason&&e.reason.stack)||e.reason))});var ce=console.error;console.error=function(){try{r('console.error: '+[].slice.call(arguments).join(' '))}catch(x){}ce.apply(console,arguments)}})();";
    static final String HAPJS = "(function(){if(window.__anshH)return;window.__anshH=1;navigator.vibrate=function(p){try{AnshifyApp.vibratePattern(Array.isArray(p)?p.join(','):String(p))}catch(e){}return true};window.anshifyHaptic=function(t){try{AnshifyApp.haptic(String(t||'tap'))}catch(e){}};window.anshifyRumble=function(ms,s){try{AnshifyApp.rumble(ms||200,s==null?0.7:s)}catch(e){}};var g0=navigator.getGamepads&&navigator.getGamepads.bind(navigator);if(g0)navigator.getGamepads=function(){var a=g0();for(var i=0;i<a.length;i++){var p=a[i];if(p&&!p.vibrationActuator){try{p.vibrationActuator={playEffect:function(t,o){o=o||{};window.anshifyRumble(o.duration||200,o.strongMagnitude);return Promise.resolve('complete')},reset:function(){return Promise.resolve('complete')}}}catch(e){}}}return a}})();";
    android.widget.TextView errBtn;
    java.util.ArrayList<String> jsErrs = new java.util.ArrayList<String>();
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
        sp.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(sp, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        w.addJavascriptInterface(new Bridge(), "AnshifyApp");
        installCrash(root);initFcm(); w.setFocusable(true); w.requestFocus();
        
        w.setDownloadListener(new DownloadListener() { public void onDownloadStart(String url, String ua, String cd, String mime, long len) { handleDownload(url, ua, cd, mime); } });

        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        
        s.setSupportZoom(false); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false);
        w.setWebViewClient(new WebViewClient() {
            
            @Override
            public void onPageFinished(WebView v, String u) {  v.evaluateJavascript(POLY, null); v.evaluateJavascript(ERRJS, null); v.evaluateJavascript(HAPJS, null); pushToken(null); hideSplash(); }
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

    void checkUpdate(final String u) {
        new Thread(new Runnable() { public void run() { try {
            java.net.HttpURLConnection hc = (java.net.HttpURLConnection) new java.net.URL(u).openConnection();
            hc.setConnectTimeout(8000); hc.setReadTimeout(8000);
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(hc.getInputStream()));
            StringBuilder sb = new StringBuilder(); String ln; while ((ln = br.readLine()) != null) sb.append(ln); br.close();
            final org.json.JSONObject jo = new org.json.JSONObject(sb.toString());
            int cur = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            if (jo.optInt("version", 0) > cur) runOnUiThread(new Runnable() { public void run() { try {
                new android.app.AlertDialog.Builder(MainActivity.this).setTitle("Update available").setMessage(jo.optString("msg", "A new version is ready."))
                    .setPositiveButton("Update", new android.content.DialogInterface.OnClickListener() { public void onClick(android.content.DialogInterface dlg, int which) { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(jo.optString("url")))); } })
                    .setNegativeButton("Later", null).show();
            } catch (Exception e) {} } });
        } catch (Exception e) {} } }).start();
    }

    void hideSplash() {
        long d = Math.max(0, 0 - (SystemClock.uptimeMillis() - t0));
        sp.postDelayed(new Runnable() {
            public void run() {
                sp.animate().alpha(0f).setDuration(0).withEndAction(new Runnable() {
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

    Vibrator vib() { return (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); }

    void buzz(long[] pat) {
        try {
            if (Build.VERSION.SDK_INT >= 26) vib().vibrate(android.os.VibrationEffect.createWaveform(pat, -1));
            else vib().vibrate(pat, -1);
        } catch (Exception e) {}
    }

    void doHaptic(String t) {
        long[] p = t.equals("heavy") ? new long[]{0, 60} : t.equals("success") ? new long[]{0, 20, 60, 20} : t.equals("error") ? new long[]{0, 50, 40, 50, 40, 50} : t.equals("select") ? new long[]{0, 8} : new long[]{0, 18};
        buzz(p);
    }

    void doRumble(long ms, double strength) {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                for (int id : android.view.InputDevice.getDeviceIds()) {
                    android.view.InputDevice d = android.view.InputDevice.getDevice(id);
                    if (d != null && (d.getSources() & android.view.InputDevice.SOURCE_GAMEPAD) == android.view.InputDevice.SOURCE_GAMEPAD) {
                        Vibrator v = d.getVibratorManager().getDefaultVibrator();
                        if (v.hasVibrator()) { v.vibrate(android.os.VibrationEffect.createOneShot(ms, Math.max(1, Math.min(255, (int) (strength * 255))))); return; }
                    }
                }
            }
        } catch (Exception e) {}
        buzz(new long[]{0, ms});
    }

    void initFcm() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<String>() {
                public void onComplete(com.google.android.gms.tasks.Task<String> t) { if (t.isSuccessful() && t.getResult() != null) pushToken(t.getResult()); }
            });
        } catch (Exception e) {}
    }

    void pushToken(String t0) {
        final android.content.SharedPreferences sp1 = getSharedPreferences("anshify_fcm", 0);
        if (t0 != null) sp1.edit().putString("token", t0).apply();
        final String t = sp1.getString("token", "");
        if (t.length() == 0) return;
        runOnUiThread(new Runnable() { public void run() { try { w.evaluateJavascript("window.anshifyFcmToken='" + t + "';try{window.onAnshifyToken&&window.onAnshifyToken('" + t + "')}catch(e){}", null); } catch (Exception e) {} } });
    }

    void showReport(String title, final String text) {
        try {
            new android.app.AlertDialog.Builder(MainActivity.this).setTitle(title).setMessage(text)
                .setPositiveButton("Copy", new android.content.DialogInterface.OnClickListener() { public void onClick(android.content.DialogInterface d, int i) {
                    try { ((android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("report", text)); toast("Copied"); } catch (Exception e) {}
                } })
                .setNegativeButton("Close", null).show();
        } catch (Exception e) {}
    }

    void showErrors() {
        StringBuilder sb = new StringBuilder();
        for (String s : jsErrs) sb.append(s).append("\n\n");
        showReport("Errors in your app (" + jsErrs.size() + ")", sb.toString());
        jsErrs.clear();
        if (errBtn != null) errBtn.setVisibility(android.view.View.GONE);
    }

    void installCrash(FrameLayout root) {
        final android.content.SharedPreferences sp0 = getSharedPreferences("anshify_err", 0);
        final Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    e.printStackTrace(new java.io.PrintWriter(sw));
                    sp0.edit().putString("crash", new java.util.Date() + "\n" + Build.MODEL + " / Android " + Build.VERSION.RELEASE + "\n\n" + sw).commit();
                } catch (Throwable x) {}
                if (old != null) old.uncaughtException(t, e); else System.exit(1);
            }
        });
        int s = (int) (44 * getResources().getDisplayMetrics().density);
        errBtn = new android.widget.TextView(this);
        errBtn.setText("!");
        errBtn.setTextColor(Color.WHITE);
        errBtn.setTextSize(20);
        errBtn.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        gd.setColor(Color.parseColor("#D32F2F"));
        errBtn.setBackground(gd);
        errBtn.setVisibility(android.view.View.GONE);
        errBtn.setOnClickListener(new android.view.View.OnClickListener() { public void onClick(android.view.View v) { showErrors(); } });
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(s, s, android.view.Gravity.BOTTOM | android.view.Gravity.END);
        lp.setMargins(0, 0, s / 3, s * 2);
        root.addView(errBtn, lp);
        String cr = sp0.getString("crash", null);
        if (cr != null) { sp0.edit().remove("crash").commit(); showReport("The app crashed last time", cr); }
    }

    class Bridge {
        @JavascriptInterface public void jsError(final String m) { runOnUiThread(new Runnable() { public void run() {
            if (jsErrs.size() > 0 && jsErrs.get(jsErrs.size() - 1).equals(m)) return;
            if (jsErrs.size() >= 20) jsErrs.remove(0);
            jsErrs.add(m);
            if (errBtn != null) errBtn.setVisibility(android.view.View.VISIBLE);
        } }); }
        @JavascriptInterface public void showErrors() { runOnUiThread(new Runnable() { public void run() { MainActivity.this.showErrors(); } }); }
        @JavascriptInterface public void haptic(String t) { doHaptic(t); }
        @JavascriptInterface public String getFcmToken() { return getSharedPreferences("anshify_fcm", 0).getString("token", ""); }
        @JavascriptInterface public void vibratePattern(String s) { try { String[] a = s.split(","); long[] p = new long[a.length + 1]; for (int i = 0; i < a.length; i++) p[i + 1] = Math.max(0, Long.parseLong(a[i].trim())); buzz(p); } catch (Exception e) {} }
        @JavascriptInterface public void rumble(int ms, double strength) { doRumble(ms, strength); }
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

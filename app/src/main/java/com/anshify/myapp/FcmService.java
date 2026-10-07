package com.anshify.myapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class FcmService extends com.google.firebase.messaging.FirebaseMessagingService {
    @Override
    public void onNewToken(String t) {
        getSharedPreferences("anshify_fcm", 0).edit().putString("token", t).apply();
    }

    @Override
    public void onMessageReceived(com.google.firebase.messaging.RemoteMessage m) {
        String title = m.getData().get("title");
        String body = m.getData().get("body");
        if (m.getNotification() != null) {
            if (m.getNotification().getTitle() != null) title = m.getNotification().getTitle();
            if (m.getNotification().getBody() != null) body = m.getNotification().getBody();
        }
        if (title == null && body == null) return;
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel("anshify", "Notifications", NotificationManager.IMPORTANCE_HIGH));
            Intent in = new Intent(this, MainActivity.class);
            in.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(this, 0, in, Build.VERSION.SDK_INT >= 23 ? (PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT) : PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder nb = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "anshify") : new Notification.Builder(this);
            nb.setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle(title == null ? "" : title).setContentText(body == null ? "" : body).setAutoCancel(true).setContentIntent(pi);
            if (Build.VERSION.SDK_INT < 26) nb.setPriority(Notification.PRIORITY_HIGH);
            nm.notify((int) (System.currentTimeMillis() % 100000000), nb.build());
        } catch (Exception e) {}
    }
}

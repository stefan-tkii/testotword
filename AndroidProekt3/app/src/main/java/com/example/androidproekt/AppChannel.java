package com.example.androidproekt;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Build;

public class AppChannel extends Application {
    public static final String CHANNEL_ID = "com.example.androidproekt.Channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "MyChannel", NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setLightColor(Color.BLUE);
            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
            manager.createNotificationChannel(channel);
        }
    }

}

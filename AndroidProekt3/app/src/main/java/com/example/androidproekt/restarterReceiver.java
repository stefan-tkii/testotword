package com.example.androidproekt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class restarterReceiver extends BroadcastReceiver {
    public static final String START_SERVICE = "startService";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("Broadcast received: ", "Service stopped, will restart!");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {  context.startForegroundService(new Intent(context, mainService.class).setAction(START_SERVICE)); }
        else
        { context.startService(new Intent(context, mainService.class).setAction(START_SERVICE)); }
    }
}

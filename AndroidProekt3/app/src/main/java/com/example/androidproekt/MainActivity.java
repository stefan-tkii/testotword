package com.example.androidproekt;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class MainActivity extends Activity {
    Intent myIntent;
    private mainService myService;
    public static final String START_SERVICE = "startService";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("Redirected", "Intitiated on create of Main activity.");
        myService = new mainService();
        myIntent = new Intent(this, mainService.class);
        myIntent.setAction(START_SERVICE);
        if (!isMainServiceRunning(myService.getClass()))
        {
            Log.e("Starting", "Starting main service via Main activity");
            startService(myIntent);
        }
        finish();
    }

    private boolean isMainServiceRunning(Class<?> serviceClass)
    {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.e("Service status:", "Running");
                return true;
            }
        }
        Log.e("Service status:", "Not running");
        return false;
    }

    @Override
    protected void onDestroy() {
        Log.e("onDestroy mainactivity", "Inside on destroy of main activity");
        super.onDestroy();
    }

}





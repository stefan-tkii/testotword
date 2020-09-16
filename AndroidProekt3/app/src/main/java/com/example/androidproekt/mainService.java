package com.example.androidproekt;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Presentation;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Chronometer;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.androidproekt.AppChannel.CHANNEL_ID;

public class mainService extends Service {

    private static Timer timer;
    private static final long interval = 120000;
    private long startTime;
    private long difference;
    private long pause;
    public static final int JOB_ID = 123;
    public static final int NOTF_ID = 202;
    public static final String ACTION_STOP_SERVICE = "stopService";

    public mainService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("Oncreate of mainservice", "Inside onCreate of mainService");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        { startMyForeground(); }
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            Log.e("StartForeground lower", "Will start foreground");
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            Notification notification = builder.setOngoing(true).setContentTitle("Foreground service running.")
                    .setCategory(Notification.CATEGORY_SERVICE).setContentText("Some jobs are done in background").setSmallIcon(R.drawable.ic_icon).build();
            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    Log.e("onstartcommand", "Inside onStartCommand of mainService");
        try {
            startProcess();
        } catch (InterruptedException e) {
            Log.e("Pause error: ", "Error: " + e.getMessage());
        }
        return START_STICKY;
    }

    public void startProcess() throws InterruptedException {
        Log.e("StartProcess", "Inside startProcess");
        stopProcess();
        checkPause();
        if(timer == null)
        {
            Log.e("TimerCheck", "Timer is null");
        }
        else
        {
            Log.e("Timercheck_also", "Timer is NOT NULL");
        }
        startTime = System.currentTimeMillis();
        Log.e("StartTimer", String.valueOf(startTime));
        timer = new Timer();
        timer.scheduleAtFixedRate(new Task(), 0, interval);
    }

    public void checkPause() throws InterruptedException {
        /*
        Zabeleska: Bidejki otkako ke ja isklucev aplikacijata, servisot odnovo se restartira vednas zapocnuva so Timer taskot koj treba da e periodicen
        odnosno da se izvrsuva ednas na 2 min i sega ako se izvrsil pa sum ja isklucil aplikacijata se izvrsuva povtorno pri restartiranje na servisot i toa vednas
        bez da pominat 2 minuti bidejki toa aplikacijata ne go pamti, pa odma se izvrsuva, zatoa postoi ovoj metod koj proveruva prethodno koga bila uklucena
        kolku vreme bila aktivna i vrz osnova na toa pamtime uste kolku vreme treba da cekame za da pomine interval od 2 minuti i da pocne da se izvrsuva procesot
        so klucot 'pause' vo shared preferences ja cuvam taa informacija i ja zemam vo ovoj metod koj ke go pauzira procesot kolku sto treba.
        Ovaa vrednosta za toa kolku vreme bil aktiven servisot se popolnuva vo OnDestroy na servisot i/ili vo onTaskRemoved, on destroy vidov soodvestuva povekje
        na reboot na telefonot, a onTaskRemoved na toa koga ke ja swajpnev od telefonot
         */
        SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long val = spref.getLong("pause", 0L);
        Log.e("Value is: ", "Value: " + String.valueOf(val));
        Log.e("CheckPause", "Inside method checkPause");
        if(val != 0L)
        {
            if(val<interval)
            {
                pause = interval - val;
            }
            else if(val == interval)
            {
                pause = 0;
            }
            else
            {
                double whole = (double)(val/interval);
                double whole_lesser = Math.floor(whole);
                double diff = whole - whole_lesser;
                long val1 = (long)(diff*interval);
                pause = interval - val1;
            }
        }
        else
        {
            pause = 0;
        }
        Log.e("PauseVal", "Must pause: " + String.valueOf(pause));
        if(pause != 0)
        {
            Thread.sleep(pause); //iako mozebi e rizicno vaka da se pauzira sepak ne crashnuva taka da mi zavrsi rabota
        }
    }

    class Task extends TimerTask
    {
        @Override
        public void run() {
            Log.e("Timer task", "Inside the timer task");
            ConnectivityManager connManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            boolean isWifi = info.isConnected();
            if(isWifi)
            {
                Log.e("Connection succsess", "Device is connected to WIFI");
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int ssid = pref.getInt("ssid", 0);
                String url = "http://192.168.1.102:8080/PHP_API/api/user/read_single_ssid.php/?ssid=" + String.valueOf(ssid);
                try {
                    URL apiUrl = new URL(url);
                    HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.connect();
                    int code = conn.getResponseCode();
                    Log.e("Code", String.valueOf(code));
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String text="";
                    String line="";
                    while ((line = bufferedReader.readLine()) != null) {
                        text = text + line;
                    }
                    JSONArray array = new JSONArray(text);
                    JSONObject response = (JSONObject) array.get(0);
                    String id = response.getString("Id");
                    Log.e("Result ID:", "The id is " + id);
                    conn.getInputStream().close(); //go osloboduvam soketot za drugo baranje do drug link od Api-to
                    int count = (int)(Math.random() * (10-5+1) + 5); //slucaen broj koj e  5<=x<=10
                    url = "http://192.168.1.102:8080/PHP_API/api/variable/create_variable.php";
                    URL apiUrlnew = new URL(url);
                    HttpURLConnection connce = (HttpURLConnection) apiUrlnew.openConnection();
                    connce.setDoOutput(true);
                    connce.setDoInput(true);
                    connce.setRequestMethod("POST");
                    connce.setRequestProperty("Accept", "application/json");
                    connce.setRequestProperty("Content-Type", "application/json");
                    connce.connect();
                    JSONObject data = new JSONObject();
                    data.put("testfieldId", "1");
                    data.put("count", String.valueOf(count));
                    data.put("userId", id);
                    Log.e("JSON", "Json object is made!");
                    DataOutputStream outputStream = new DataOutputStream(connce.getOutputStream());
                    outputStream.write(data.toString().getBytes("UTF-8"));
                    Log.e("writer", "JSON DATA HAS BEEN WRITTEN!");
                    int cod = connce.getResponseCode();
                    Log.e("Code", String.valueOf(cod));
                    if(cod == 200)
                    {
                        Log.e("Succsessful input", "Job done");
                        PersistableBundle bundle = new PersistableBundle();
                        bundle.putString("id", id);
                        bundle.putInt("count", count);
                        sendDownloadOptionNotification();
                        ScheduleJob(bundle);
                    }
                } catch (MalformedURLException e) {
                    Log.e("MalformedURLException", "Error: " + e.getMessage());
                } catch (IOException e) {
                    Log.e("IOException", "Error: " + e.getMessage());
                } catch (JSONException e) {
                    Log.e("JSONException", "Error: " + e.getMessage());
                } catch(Exception e) {
                    Log.e("DefaultException", "Error: " + e.getMessage());
                }
            }
            else
            {
                Log.e("NETWORK ERROR", "No internet connection failed to execute task");
            }

        }
    }

    public void ScheduleJob(PersistableBundle extras)
    {
        Log.e("JobScheduler", "Scheduling job");
        ComponentName comp = new ComponentName(this, JobTask.class);
        JobInfo info = new JobInfo.Builder(JOB_ID, comp)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPersisted(true)//da bide ziva i posle reboot
                .setExtras(extras)
                .setOverrideDeadline(2000)
                .build();
        JobScheduler scheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
        assert scheduler != null;
        scheduler.schedule(info);
    }

 public void sendDownloadOptionNotification()
    {
        Log.e("startOption", "Sending the start download notification");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            Intent actionIntent = new Intent(this,  downloadActivity.class);
            PendingIntent pend = PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
            NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;
            manager.cancel(NOTF_ID);
            Notification notif = builder.setContentTitle("Service running").setContentText("Click here to download the pdf!")
                    .setSmallIcon(R.drawable.ic_canceler)
                    .setAutoCancel(true)
                    .setContentIntent(pend)
                    .build();
            manager.notify(NOTF_ID, notif);
        }
        else
        {
            Intent actionIntent = new Intent(this,  downloadActivity.class);
            PendingIntent pend = PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;
            manager.cancel(NOTF_ID);
            Notification notif = builder.setContentTitle("Service running").setContentText("Click here to download the pdf!")
                    .setSmallIcon(R.drawable.ic_canceler)
                    .setAutoCancel(true)
                    .setContentIntent(pend)
                    .build();
            manager.notify(NOTF_ID, notif);
        }
    }


    public void stopProcess()
    {
        Log.e("stopProcess", "Inside stopProcess");
        if(timer != null)
        {
            Log.e("TimerCheck", "Timer is not null, reinitializing.");
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e("Ontasktemoved", "Inside on taskRemoved");
        difference = System.currentTimeMillis() - startTime;
        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        manager.cancel(NOTF_ID);
        SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor edit = spref.edit();
        edit.putLong("pause", difference);
        edit.apply();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, restarterReceiver.class);
        this.sendBroadcast(broadcastIntent);
    }

    @Override
    public void onDestroy() {
        Log.e("Ondestroy", "Inside on destroy");
        stopProcess();
        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        manager.cancel(NOTF_ID);
        difference = System.currentTimeMillis() - startTime;
        SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor edit = spref.edit();
        edit.putLong("pause", difference);
        edit.apply();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, restarterReceiver.class);
        this.sendBroadcast(broadcastIntent);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyForeground()
    {
        Log.e("Myforeground", "Inside startMyForeground");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        Notification notification = builder.setOngoing(true).setContentTitle("Foreground service running.").setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .setCategory(Notification.CATEGORY_SERVICE).setContentText("Some jobs are done in background").setSmallIcon(R.drawable.ic_icon).build();
        startForeground(2, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

package com.example.androidproekt;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

public class JobTask extends JobService {

    private static final String apiURL = "http://192.168.1.102:8080/PHP_API/api/makentity/create_entity.php";
    private CountDownLatch latch;
    private String id;
    private int created;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.e("jobTask start", "Inside on StartJob of JobTask");
        try {
            job_work(params);
        } catch (InterruptedException e) {
            Log.e("Err Interrupted excep", "Error: " + e.getMessage());
        }
        return true;
    }

    private void job_work(final JobParameters params) throws InterruptedException {
        created = 0;
        Log.e("jobTask job_work", "Inside job_work of JobTask");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int leftover = prefs.getInt("remaining", 0);
        String prev_ID = prefs.getString("previous_id", "");
        if(leftover != 0)
        {
            Log.e("Leftovers", "There are certain leftovers from previous iterations");
            CountDownLatch latcher = new CountDownLatch(leftover);
            for(int i=1;i<=leftover;i++)
            {
                worker work = new worker(latcher, prev_ID);
                work.setName("PREVIOUS-WORKER-" + String.valueOf(i));
                work.start();
            }
            latcher.await();
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("remaining", 0);
            edit.apply(); //ako sme gi dovrsile resetiraj go brojacot
            created = created + leftover;
            Log.e("Finish_previous", "Leftovers have been finished, proceeding to current");
        }
        id = params.getExtras().getString("id");
        int count = params.getExtras().getInt("count");
        latch = new CountDownLatch(count); //count
        for(int i=1;i<=count;i++) //count
        {
            worker work = new worker(latch, id);
            work.setName("WORKER-" + String.valueOf(i));
            work.start();
            Log.e("WorkerStart", "Worker thread num=" + String.valueOf(i) + " has started.");
        }
        latch.await();
        Log.e("Finish", "All worker threads have finished their job");
        created = created + count;

        jobFinished(params, false);
    }

    class worker extends Thread
    {
        private CountDownLatch latch;
        private String theId;

        public worker(CountDownLatch latche, String theId)
        {
            this.latch = latche;
            this.theId = theId;
        }

        @Override
        public void run() {
           try {
               ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
               NetworkInfo activeNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
               boolean isWifi = activeNetwork.isConnected();
               if(isWifi)
               {
                   URL api = new URL(apiURL);
                   HttpURLConnection conn = (HttpURLConnection) api.openConnection();
                   conn.setDoOutput(true);
                   conn.setDoInput(true);
                   conn.setRequestMethod("POST");
                   conn.setRequestProperty("Content-Type", "application/json");
                   conn.setRequestProperty("Accept", "application/json");
                   conn.connect();
                   JSONObject data = new JSONObject();
                   data.put("field", "automated assignment");
                   data.put("userId", theId);
                   DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
                   outputStream.write(data.toString().getBytes("UTF-8"));
                   int code = conn.getResponseCode();
                   if(code == 200) {
                       BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                       String line="";
                       while ((line = bufferedReader.readLine()) != null) {
                           Log.e("Response: ", line);
                       }
                       Log.e("Worker run method", Thread.currentThread().getName() + " has finished");
                       latch.countDown();
                   }
                   else{
                       BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                       String line="";
                       while ((line = bufferedReader.readLine()) != null) {
                           Log.e("Response: ", line);
                       }
                       Log.e("Fail task", "Task execution failed");
                   }
               }
               else{
                   Log.e("Errce", "Worker execute failed no internet");
               }
           }
           catch (Exception e)
           { Log.e("worker thread default", "Error in worker thread: " + e.getMessage()); }

        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.e("jobTask stop", "Inside onStopJob of JobTask");
        int current = (int)latch.getCount(); //dokolku ne sme dovrsile zacuvaj uste kolku ostanale od ovaa pa duri i od prethodni iteracii ako podolgo vreme nemozelo,
        // ovoj metod ke se povika ako poradi nekoja pricina nemoze jobfinished da se povika, primer mobilniot ne e na WIFI uklucen ili se isklucil naglo i slicno
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor edit = prefs.edit();
        int previous = prefs.getInt("remaining", 0);
        if(previous != 0)
        {
            edit.putInt("remaining", current + previous);
            edit.putString("previous_id", id);
            edit.apply();
        }
        else {
            edit.putInt("remaining", current);
            edit.putString("previous_id", id);
            edit.apply();
        }
        return true;
    }

}

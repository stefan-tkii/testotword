package com.example.androidproekt;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class downloadActivity extends Activity {
    private static final String requestURL = "https://api.gdc.cancer.gov/data/556e5e3f-0ab9-4b6c-aa62-c42f6a6cf20c";
    private downloadProcess process;
    private NotificationManager myManager;
    private NotificationChannel myChannel;
    private static final String CHANNEL_ID = "com.example.downloadapp.notification_channel";
    private static final int NOTIFICATION_ID = 122;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWritePermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setVisible(true);
    }

    private void createNotificationChannel()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            myChannel = new NotificationChannel(CHANNEL_ID, "Channel_1", NotificationManager.IMPORTANCE_DEFAULT);
            myChannel.setDescription("This is Channel_1");
            myManager.createNotificationChannel(myChannel);
        }
    }

    private void getWritePermission()
    {
        int REQUEST_WRITE_STORAGE = 112;
        boolean hasPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if(!hasPermission)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else
        {
            myManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            createNotificationChannel();
            process = new downloadProcess(this, CHANNEL_ID, NOTIFICATION_ID);
            process.execute(requestURL);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode)
        {
            case 112: {
                if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED))
                {
                    myManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                    createNotificationChannel();
                    process = new downloadProcess(this, CHANNEL_ID, NOTIFICATION_ID);
                    process.execute(requestURL);
                }
                else
                {
                    Toast.makeText(this, "App will fail because no permission is granted.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    private class downloadProcess extends AsyncTask<String, String, String>
    {
        private Context context;
        private String id;
        private int notf_id;
        private NotificationCompat.Builder builder;

        public downloadProcess(Context context, final String id, final int notf_id)
        {
            this.context = context;
            this.id = id;
            this.notf_id = notf_id;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                builder = new NotificationCompat.Builder(this.context, this.id)
                        .setSmallIcon(R.drawable.ic_download)
                        .setContentTitle("Download process")
                        .setContentText("Download is in progress...")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setProgress(100, 0, false);
            }
            else
            {
                builder = new NotificationCompat.Builder(this.context)
                        .setSmallIcon(R.drawable.ic_download)
                        .setContentTitle("Download process")
                        .setContentText("Download is in progress...")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setProgress(100, 0, false);
            }
            myManager.notify(notf_id, builder.build());
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s!=null) {
                File file = new File(s);
                Log.e("test", "Path is: " + s);
                Uri uri = FileProvider.getUriForFile(this.context, context.getApplicationContext().getPackageName() + ".provider", file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                builder.setContentText("Download is complete!");
                builder.setContentIntent(pi);
                myManager.notify(notf_id, builder.build());
            }
            else
            {
                Log.e("dasw", "String is null can't perform operation.");
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            builder.setProgress(100, Integer.parseInt(values[0]), false);
            myManager.notify(notf_id, builder.build());
        }

        @Override
        protected String doInBackground(String... strings) {
            String storageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
            String state = Environment.getExternalStorageState();
            if(Environment.MEDIA_MOUNTED.equals(state))
            {
                Log.e("fdas", "All is good");
            }
            else if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
            {
                Log.e("das", "Can only read");
            }
            File folder = new File(storageDirectory + "/documents1/test1pdf");
            boolean succsess = folder.mkdirs();
            if(!succsess)
            {
                Log.e("err", "FOLDER NOT CREATED!");
            }
            ConnectivityManager mng = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info = mng.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            boolean wifi = info.isConnected();
            if(wifi) {
                String fileName = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase() + "downloaded.pdf";
                File pdfFile = new File(folder, fileName);
                try {
                    boolean succsessful = pdfFile.createNewFile();
                    if (pdfFile.exists()) {
                        Log.e("tt", "succsess make file");
                    }
                    if (!succsessful) {
                        Log.e("eawr", "FAILED TO MAKE FILE");
                    }
                    int buffer = 0;
                    URL apiurl = new URL(strings[0]);
                    HttpURLConnection conn = (HttpURLConnection) apiurl.openConnection();
                    conn.connect();
                    int totalSize = conn.getContentLength();
                    InputStream input = conn.getInputStream();
                    FileOutputStream output = new FileOutputStream(pdfFile);
                    byte[] data = new byte[1024];
                    long current = 0;
                    while ((buffer = input.read(data)) > 0) {
                        current = current + buffer;
                        if ((int) current % (totalSize / 20) == 0) {
                            publishProgress("" + (int) ((current * 100) / totalSize));
                        }
                        output.write(data, 0, buffer);
                    }
                    output.flush();
                    output.close();
                    input.close();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.e("dqwda", pdfFile.getPath());
                return pdfFile.getPath();
            }
            else
            {
                Log.e("das", "Failed no network info");
                return null;
            }
        }
    }

}

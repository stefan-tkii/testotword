package com.example.androidproekt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.prefs.PreferenceChangeEvent;

public class firstStart extends AppCompatActivity {
    private EditText firstName;
    private EditText lastName;
    private EditText SSID;
    private Button insert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_start);
        Log.e("ONCREATE", "On create method of firstStart initiated!");
        this.firstName = findViewById(R.id.firstname_field);
        this.lastName = findViewById(R.id.lastname_field);
        this.SSID = findViewById(R.id.ssid_field);
        insert = findViewById(R.id.insert_button);
        firstName.addTextChangedListener(inputControl);
        lastName.addTextChangedListener(inputControl);
        SSID.addTextChangedListener(inputControl);
        insert.setOnClickListener(new MyOnClickListener());
        boolean result = firstTime(this);
        if(!result)
        {
            Log.e("NotFirsttime", "This is not the first time for the app.");
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Log.e("Redirect", "Redirecting to Main activity.");
            startActivity(intent);
            Log.e("Finish", "Exiting firstStart activity.");
            finish();
        }
        else
        {
            Log.e("Firsttime", "This is the first time for the app.");
        }
    }

    public class MyOnClickListener implements View.OnClickListener
    {
        public MyOnClickListener()
        {
        }

        @Override
        public void onClick(View v) {
            Log.e("Onclick", "On click method initiated!");
            String fname = firstName.getText().toString().trim();
            String lname = lastName.getText().toString().trim();
            int ssid = Integer.parseInt(SSID.getText().toString().trim());
            SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor preferencesEditor = spref.edit();
            preferencesEditor.putString("firstName", fname);
            preferencesEditor.putString("lastName", lname);
            preferencesEditor.putInt("ssid", ssid);
            preferencesEditor.apply();
            new Thread(new MyThread(fname, lname, ssid)).start();
        }
    }

    private class MyThread implements Runnable {
        String firstname;
        String lastname;
        int ssid;

        public MyThread(String firstname, String lastname, int ssid)
        {
            this.firstname = firstname;
            this.lastname = lastname;
            this.ssid = ssid;
        }

        @Override
        public void run() {
            Log.e("ThreadStart", "Thread has been opened!");
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if(activeNetwork != null && activeNetwork.isConnected())
            {
                Log.e("Network", "There is network!");
                try {
                    URL apiUrl = new URL("http://192.168.1.102:8080/PHP_API/api/user/create.php");
                    Log.e("URL", "URL HAS BEEN PARSED!");
                    HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                    Log.e("Connection", "Connection has been made!");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.connect();
                    JSONObject data = new JSONObject();
                    data.put("firstname", this.firstname);
                    data.put("lastname", this.lastname);
                    data.put("ssid", this.ssid);
                    Log.e("JSON", "Json object is made!");
                    DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
                    outputStream.write(data.toString().getBytes("UTF-8"));
                    Log.e("writer", "JSON DATA HAS BEEN WRITTEN!");
                    int code = conn.getResponseCode();
                    Log.e("Code", String.valueOf(code));
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line="";
                    while ((line = bufferedReader.readLine()) != null) {
                        Log.e("Response: ", line);
                    }
                    if(code == 200)
                    {
                        Log.e("Redirect_new", "Register is done, redirecting to Main activity.");
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        Log.e("Finish", "Exiting firstStart activity.");
                        finish();
                    }
                    else
                    {
                        Log.e("Failed", "Failed to register.");
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
            else {
                Log.e("Network", "NO NETWORK!");
            }
        }
    }

    private boolean firstTime(Context context)
    {
        SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(context);
        String fname = spref.getString("firstName", "Empty");
        String lname = spref.getString("lastName", "Empty");
        int ss = spref.getInt("ssid", 0);
        if((fname == "Empty") || (lname == "Empty") || (ss == 0))
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    private TextWatcher inputControl = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String firstname = firstName.getText().toString().trim();
            String lastname = lastName.getText().toString().trim();
            String ssid = SSID.getText().toString().trim();
            insert.setEnabled((!firstname.isEmpty()) && (!lastname.isEmpty()) && (!ssid.isEmpty()));
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
}

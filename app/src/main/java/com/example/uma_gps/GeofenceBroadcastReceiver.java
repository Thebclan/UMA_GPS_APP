package com.example.uma_gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "What's wrong?";
    static int geofenceTransition;
    String sendTimeURL = "https://provost.uma.edu/api/record_data.php";
    String currentTimeIn;
    String currentTimeOut;

    public GeofenceBroadcastReceiver(){
    }

    // ...
    public void onReceive(final Context context, Intent intent) {

        Log.i(TAG, "Event happened");
        Toast.makeText(context.getApplicationContext(), "Intent received!", Toast.LENGTH_LONG).show();
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = "I messed up!";
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        geofenceTransition = geofencingEvent.getGeofenceTransition();

        SharedPreferences prefs = context.getSharedPreferences("prefs", MODE_PRIVATE);
        int isZero = prefs.getInt("timeIn", 0);
        //SharedPreferences.Editor editor = prefs.edit();
        //editor.putInt("timeIn", 0);
       // editor.commit();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER)
            {
                Log.i(TAG, "Enter Event happened");
                Toast.makeText(context.getApplicationContext(), "Geofence entered!", Toast.LENGTH_LONG).show();
                //MapsActivity.timeEntered();

                if (isZero == 0)
                {
                    //SharedPreferences prefs = context.getSharedPreferences("prefs", MODE_PRIVATE);
                    int currentTimeEntered = (int) (new Date().getTime() / 1000);
                    currentTimeIn = String.valueOf(currentTimeEntered);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("timeIn", currentTimeEntered);
                    editor.commit();
                }
            }

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
            {
                Log.i(TAG, "Exit Event happened");
                Toast.makeText(context.getApplicationContext(), "Geofence exited!", Toast.LENGTH_LONG).show();

                if (isZero != 0)
                {
                    int currentTimeExited = (int) (new Date().getTime() / 1000);
                    currentTimeOut = String.valueOf(currentTimeExited);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("timeOut", currentTimeExited);
                    editor.commit();
                    MapsActivity.received++;
                    // Send times to api.

                    class SendPostReqAsyncTask extends AsyncTask<String, Void, String>
                    {
                        @Override
                        protected String doInBackground(String... params)
                        {
                            String timeEntered = currentTimeIn;
                            String timeExited = currentTimeOut;
                            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                            nameValuePairs.add(new BasicNameValuePair("timeIn", timeEntered));
                            nameValuePairs.add(new BasicNameValuePair("timeOut", timeExited)); // Will use this when sending code

                            try {
                                HttpClient httpClient = new DefaultHttpClient();

                                HttpPost httpPost = new HttpPost(sendTimeURL);
                                //httpPost.addHeader("Authorization", "Bearer" + token);

                                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                                HttpResponse httpResponse;
                                httpResponse = httpClient.execute(httpPost);


                            } catch (ClientProtocolException e) {

                            } catch (IOException e) {

                            }
                            return "Data Inserted Successfully";
                        } // End doInBackground
                        @Override
                        protected void onPostExecute(String result) {
                            super.onPostExecute(result);

                            Toast.makeText(context, "Time Data Sent Successfully", Toast.LENGTH_LONG).show();

                        } // End onPostExecute
                    } // End class

                    SendPostReqAsyncTask sendPostReqAsyncTask = new SendPostReqAsyncTask();

                    sendPostReqAsyncTask.execute(currentTimeIn, currentTimeOut);


                } // End inner if
            } // End outer if


        } else
            {
            // Log the error.
            Log.e(TAG, "Event didn't happen");
            }
    }
}

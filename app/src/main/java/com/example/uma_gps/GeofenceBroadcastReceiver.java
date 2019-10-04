package com.example.uma_gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "What's wrong?";
    static int geofenceTransition;

    public GeofenceBroadcastReceiver(){
        //SharedPreferences myEvent = context.getApplicationContext().getSharedPreferences("event", MODE_PRIVATE);
        //SharedPreferences.Editor editor = myEvent.edit();
        //editor.putInt("event", 0);
        //editor.apply();
    }

    // ...
    public void onReceive(Context context, Intent intent) {


        //int event = prefs.getInt("firstStart", 0);

        Toast.makeText(context.getApplicationContext(), "Intent received!", Toast.LENGTH_LONG).show();
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = "I messed up!";
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {



            //editor.putInt("event", 1);
            //editor.apply();

            //if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

                // Get the geofences that were triggered. A single event can trigger
                // multiple geofences.
                //List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

                // Get the transition details as a String.
            //String geofenceTransitionDetails = this.toString();
            MapsActivity.received++;

                Log.i(TAG, "Event happened");
            //}
        } else {
            // Log the error.
            Log.e(TAG, "Event didn't happen");
        }
    }
}

package com.example.geofencealertapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null");
            return;
        }

        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, "Geofence Error: " + errorMessage);
            NotificationHelper.sendNotification(context, "Geofence Error", errorMessage, 101);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            String message = "You have entered the geofence area.";
            Log.d(TAG, message);
            NotificationHelper.sendNotification(context, "Geofence Entered", message, 102);

        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            String message = "You have exited the geofence area.";
            Log.d(TAG, message);
            NotificationHelper.sendNotification(context, "Geofence Exited", message, 103);

        } else {
            Log.e(TAG, "Invalid Geofence Transition Type: " + geofenceTransition);
        }
    }
}
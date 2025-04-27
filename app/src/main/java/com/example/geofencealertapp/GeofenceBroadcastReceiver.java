package com.example.geofencealertapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "GeofenceBroadcastReceiver triggered with action: " + intent.getAction());

        // Get the GeofencingEvent
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null");
            NotificationHelper.sendNotification(context, "Geofence Error", "Null geofencing event", 100);
            return;
        }

        // Check if there was an error
        if (geofencingEvent.hasError()) {
            String errorMessage = "Geofence error code: " + geofencingEvent.getErrorCode();
            Log.e(TAG, errorMessage);
            NotificationHelper.sendNotification(context, "Geofence Error", errorMessage, 101);
            return;
        }

        // Get transition type (enter or exit)
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Log.d(TAG, "Geofence transition type: " + geofenceTransition);

        // List of geofences that were triggered
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            Log.w(TAG, "No triggering geofences found");
            return;
        }

        // Get the location where the transition occurred
        Location triggeringLocation = geofencingEvent.getTriggeringLocation();
        String locationInfo = "unknown location";
        if (triggeringLocation != null) {
            locationInfo = "location (" + triggeringLocation.getLatitude() +
                    ", " + triggeringLocation.getLongitude() + ")";
        }

        // Generate IDs of triggered geofences for logging
        StringBuilder geofenceIds = new StringBuilder();
        for (Geofence geofence : triggeringGeofences) {
            geofenceIds.append(geofence.getRequestId()).append(", ");
        }
        if (geofenceIds.length() > 0) {
            geofenceIds.setLength(geofenceIds.length() - 2); // Remove last ", "
        }

        Log.d(TAG, "Triggered geofences: " + geofenceIds.toString());

        // Handle different transitions
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.i(TAG, "ENTER geofence at " + locationInfo);
            String message = "You have entered the monitored area at " + locationInfo;

            // Use unique notification ID
            int notificationId = (int) (System.currentTimeMillis() % 10000);
            NotificationHelper.sendNotification(
                    context,
                    "Geofence Entered",
                    message,
                    notificationId);

        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.i(TAG, "EXIT geofence at " + locationInfo);
            String message = "You have exited the monitored area at " + locationInfo;

            // Use unique notification ID
            int notificationId = (int) (System.currentTimeMillis() % 10000);
            NotificationHelper.sendNotification(
                    context,
                    "Geofence Exited",
                    message,
                    notificationId);

        } else {
            Log.w(TAG, "Unknown geofence transition: " + geofenceTransition);
        }
    }
}
package com.example.geofencealertapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle; // Import Bundle
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive triggered");

        // *** ADD DETAILED INTENT LOGGING ***
        if (intent == null) {
            Log.e(TAG, "Received intent is NULL!");
            return;
        }
        Log.d(TAG, "Received Intent Action: " + intent.getAction()); // Log the action
        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.d(TAG, "Received Intent has NO extras."); // Check for extras bundle
        } else {
            Log.d(TAG, "Received Intent Extras:"); // Log all extras
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                // Be careful logging potentially large objects, just log key and type for now
                Log.d(TAG, "  Key: " + key + ", Value Type: " + (value != null ? value.getClass().getName() : "NULL"));
                // You could try logging the value too if it seems safe:
                // Log.d(TAG, "  Key: " + key + ", Value: " + (value != null ? value.toString() : "NULL"));
            }
            // Specifically check for a known geofence key (internal detail, might change)
            if (extras.containsKey("gms_error_code")) {
                Log.d(TAG, "Intent contains 'gms_error_code' extra.");
            }
            if (extras.containsKey("geofence_list")) {
                Log.d(TAG, "Intent contains 'geofence_list' extra.");
            }
        }
        // *** END DETAILED INTENT LOGGING ***

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent); // Now try to parse

        if (geofencingEvent == null) {
            // Make this log more specific about the failure point
            Log.e(TAG, "GeofencingEvent.fromIntent(intent) returned null. Intent might be malformed or not a geofence event.");
            return; // Exit since parsing failed
        }

        // If geofencingEvent is NOT null, proceed as before...
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, "Geofence Error: " + errorMessage);
            NotificationHelper.sendNotification(context, "Geofence Error", "Error code: " + errorMessage, 2);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            if (triggeringGeofences != null && !triggeringGeofences.isEmpty()) {
                String geofenceId = triggeringGeofences.get(0).getRequestId();
                String message = "ALERT: Left the designated area (" + geofenceId + ")";
                Log.i(TAG, message);
                NotificationHelper.sendNotification(context, "Geofence Alert", message, 1);
            } else {
                Log.e(TAG, "No triggering geofences found, but received exit event?");
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.i(TAG, "Entered the designated area.");
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            Log.i(TAG, "Dwelling inside the designated area.");
        } else {
            Log.e(TAG, "Invalid Geofence Transition Type: " + geofenceTransition);
            NotificationHelper.sendNotification(context, "Geofence Warning", "Unknown transition: " + geofenceTransition, 5);
        }
    }
}
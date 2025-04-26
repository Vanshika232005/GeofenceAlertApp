package com.example.geofencealertapp;

// Import necessary classes
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
// Add Maps imports
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color; // Import Color
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

// Implement OnMapReadyCallback
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002;
    private static final String GEOFENCE_ID = "CHILD_SAFE_AREA";
    private static final double DEFAULT_LAT = 37.4219999; // Example: Googleplex Lat
    private static final double DEFAULT_LON = -122.0840575; // Example: Googleplex Lon
    private static final float DEFAULT_RADIUS_METERS = 150.0f; // Example: 150 meters

    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;

    private EditText editTextLatitude;
    private EditText editTextLongitude;
    private EditText editTextRadius;
    private Button buttonStartMonitoring;
    private Button buttonStopMonitoring;
    private TextView textViewStatus;

    // Map related variables
    private GoogleMap googleMap;
    private SupportMapFragment mapFragment;
    private Circle currentGeofenceCircle; // To keep track of the drawn circle

    private double currentLatitude = DEFAULT_LAT;
    private double currentLongitude = DEFAULT_LON;
    private float currentRadius = DEFAULT_RADIUS_METERS;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextLatitude = findViewById(R.id.editTextLatitude);
        editTextLongitude = findViewById(R.id.editTextLongitude);
        editTextRadius = findViewById(R.id.editTextRadius);
        buttonStartMonitoring = findViewById(R.id.buttonStartMonitoring);
        buttonStopMonitoring = findViewById(R.id.buttonStopMonitoring);
        textViewStatus = findViewById(R.id.textViewStatus);

        // Initialize Map Fragment
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this); // Trigger map loading
        } else {
            Log.e(TAG, "SupportMapFragment not found!");
            Toast.makeText(this, "Error initializing map", Toast.LENGTH_SHORT).show();
        }


        // Pre-fill with defaults or saved values
        editTextLatitude.setText(String.valueOf(currentLatitude));
        editTextLongitude.setText(String.valueOf(currentLongitude));
        editTextRadius.setText(String.valueOf(currentRadius));

        geofencingClient = LocationServices.getGeofencingClient(this);

        buttonStartMonitoring.setOnClickListener(v -> {
            // Parse values before checking permissions, so map can be drawn even if perms denied initially
            Log.d(TAG, "--- Start Button Click ---");
            Log.d(TAG, "Parsed Lat: " + currentLatitude);
            Log.d(TAG, "Parsed Lon: " + currentLongitude);
            Log.d(TAG, "Parsed Radius: " + currentRadius);
            if(parseGeofenceParameters()) {
                if (checkPermissions()) {
                    startGeofencing();
                } else {
                    requestPermissions();
                }
            }
        });

        buttonStopMonitoring.setOnClickListener(v -> stopGeofencing());

        updateStatus("Not Monitoring");
        NotificationHelper.createNotificationChannel(this);
    }

    // --- Map Methods ---

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        Log.d(TAG, "Map is ready.");
        googleMap = map;

        // Check for fine location permission *before* enabling My Location layer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"Fine location permission granted, enabling My Location layer.");
            googleMap.setMyLocationEnabled(true); // Show blue dot for current location
        } else {
            Log.w(TAG,"Fine location permission not granted. My Location layer disabled.");
            // Optionally, you could re-request permission here if needed for the map feature
        }

        googleMap.getUiSettings().setZoomControlsEnabled(true); // Add zoom buttons

        // Optional: Move camera to default location initially or last known location
        LatLng initialLocation = new LatLng(currentLatitude, currentLongitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 15f)); // Zoom level 15

        // If monitoring was potentially active (e.g., loaded from preferences), draw the circle
        // For now, we draw it when "Start" is clicked
        // drawGeofenceCircle(); // Uncomment if you load state and want to draw immediately
    }

    private void drawGeofenceCircle() {
        if (googleMap == null) {
            Log.w(TAG, "Map not ready yet, cannot draw circle.");
            return;
        }
        // *** ADD THIS LOGGING HERE ***
        Log.d(TAG, "--- drawGeofenceCircle ---");
        Log.d(TAG, "Using Lat for circle: " + currentLatitude);
        Log.d(TAG, "Using Lon for circle: " + currentLongitude);
        Log.d(TAG, "Using Radius for circle: " + currentRadius);
        // *** END LOGGING ***

        Log.d(TAG, "Drawing geofence circle at: " + currentLatitude + ", " + currentLongitude + " R: " + currentRadius);

        // Remove the previous circle if it exists
        if (currentGeofenceCircle != null) {
            currentGeofenceCircle.remove();
        }

        LatLng center = new LatLng(currentLatitude, currentLongitude);

        // Create circle options
        CircleOptions circleOptions = new CircleOptions()
                .center(center)
                .radius(currentRadius) // Radius in meters
                .strokeColor(Color.argb(255, 255, 0, 0)) // Red outline
                .fillColor(Color.argb(64, 255, 0, 0))   // Semi-transparent red fill
                .strokeWidth(4f); // Outline width

        // Add the circle to the map and store reference
        currentGeofenceCircle = googleMap.addCircle(circleOptions);

        // Move camera to show the geofence
        zoomToGeofence(center, currentRadius);
    }

    private void removeGeofenceCircle() {
        if (googleMap == null) return;
        if (currentGeofenceCircle != null) {
            Log.d(TAG, "Removing geofence circle.");
            currentGeofenceCircle.remove();
            currentGeofenceCircle = null;
        }
    }

    private void zoomToGeofence(LatLng center, float radius) {
        if (googleMap == null) return;

        // Calculate bounds to fit the circle
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(center);
        // Simple approximation for bounds based on radius (doesn't perfectly account for map projection)
        double radiusInDegreesLatitude = radius / 111000f; // Approx meters per degree latitude
        double radiusInDegreesLongitude = radius / (111000f * Math.cos(Math.toRadians(center.latitude)));
        builder.include(new LatLng(center.latitude + radiusInDegreesLatitude, center.longitude + radiusInDegreesLongitude));
        builder.include(new LatLng(center.latitude - radiusInDegreesLatitude, center.longitude - radiusInDegreesLongitude));

        LatLngBounds bounds = builder.build();
        int padding = 100; // Padding in pixels around the bounds
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }


    // --- Geofencing Methods (Modified) ---

    private boolean parseGeofenceParameters() {
        try {
            currentLatitude = Double.parseDouble(editTextLatitude.getText().toString());
            currentLongitude = Double.parseDouble(editTextLongitude.getText().toString());
            currentRadius = Float.parseFloat(editTextRadius.getText().toString());

            if (currentRadius <= 0) {
                Toast.makeText(this, "Radius must be positive", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true; // Parameters parsed successfully
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid latitude, longitude, or radius", Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    private void startGeofencing() {
        // Permission checks are now done before calling this method (in the button listener)
        // Parameter parsing is also done before calling

        Log.d(TAG, "startGeofencing called");
        // *** ADD THIS LOGGING HERE ***
        Log.d(TAG, "--- startGeofencing ---");
        Log.d(TAG, "Using Lat for Geofence: " + currentLatitude);
        Log.d(TAG, "Using Lon for Geofence: " + currentLongitude);
        Log.d(TAG, "Using Radius for Geofence: " + currentRadius);
        // *** END LOGGING ***

        // *** Draw the circle on the map ***
        drawGeofenceCircle();

        Geofence geofence = new Geofence.Builder()
                .setRequestId(GEOFENCE_ID)
                .setCircularRegion(currentLatitude, currentLongitude, currentRadius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        PendingIntent pendingIntent = getGeofencePendingIntent();

        // Need to re-check permission here because it's required by addGeofences
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Fine location permission check failed just before adding geofences.");
            Toast.makeText(this, "Fine location permission missing.", Toast.LENGTH_SHORT).show();
            // Consider requesting permissions again or informing the user clearly
            return;
        }

        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(this, aVoid -> {
                    Log.d(TAG, "Geofence Added Successfully");
                    Toast.makeText(MainActivity.this, "Monitoring Started", Toast.LENGTH_SHORT).show();
                    updateStatus("Monitoring Active");
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Failed to add Geofence: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Failed to start monitoring: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updateStatus("Error Adding Geofence");
                    removeGeofenceCircle(); // Remove circle if adding failed
                });
    }

    private void stopGeofencing() {
        Log.d(TAG, "stopGeofencing called");
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(this, aVoid -> {
                    Log.d(TAG, "Geofence Removed Successfully");
                    Toast.makeText(MainActivity.this, "Monitoring Stopped", Toast.LENGTH_SHORT).show();
                    updateStatus("Not Monitoring");
                    removeGeofenceCircle(); // *** Remove circle from map ***
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Failed to remove Geofence: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Failed to stop monitoring: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updateStatus("Error Removing Geofence");
                    // Keep the circle drawn in case of removal failure? Or remove anyway? User decision.
                    // removeGeofenceCircle();
                });
        geofencePendingIntent = null;
    }

    // --- Permission Methods (Ensure Fine Location check includes map usage) ---

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // ... (Keep existing permission handling logic) ...

        // *** ADD THIS: If fine location granted, enable map layer ***
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Fine location granted
                if (googleMap != null) {
                    // Need to check permission again because this callback might happen before onMapReady
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Permission granted in callback, enabling My Location layer.");
                        googleMap.setMyLocationEnabled(true);
                    }
                }
                // Now handle background permission or start geofencing as before...
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestBackgroundLocationPermission();
                } else {
                    startGeofencing();
                }
            } else {
                // Fine location denied... (existing logic)
                Log.d(TAG, "Fine location permission denied.");
                Toast.makeText(this, "Fine Location permission denied. Geofencing and Map require this permission.", Toast.LENGTH_LONG).show();
                updateStatus("Permission Denied");
            }
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE) {
            // ... (Keep existing background permission handling logic) ...
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Background location permission granted.");
                startGeofencing(); // Proceed if fine location was also granted previously
            } else {
                Log.d(TAG, "Background location permission denied.");
                Toast.makeText(this, "Background Location permission denied. Geofencing might not work reliably.", Toast.LENGTH_LONG).show();
                startGeofencing(); // Still try to start, but warn
                updateStatus("Monitoring (Background Limited)");
            }
        }
    }


    // --- Utility Methods (Keep existing) ---
    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags);
        return geofencePendingIntent;
    }

    private void updateStatus(String status) {
        textViewStatus.setText("Status: " + status);
    }

    private boolean checkPermissions() {
        int fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int backgroundLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            return fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                    backgroundLocationPermission == PackageManager.PERMISSION_GRANTED;
        } else {
            return fineLocationPermission == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        // ... (Keep existing requestPermissions logic) ...
        boolean shouldProvideRationaleFine = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (shouldProvideRationaleFine) {
            Log.i(TAG, "Displaying fine location permission rationale.");
            Toast.makeText(this, "Fine Location permission is needed for map display and geofencing", Toast.LENGTH_LONG).show();
        }
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void requestBackgroundLocationPermission() {
        // ... (Keep existing requestBackgroundLocationPermission logic) ...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Please grant 'Allow all the time' location access for reliable geofencing.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                startGeofencing();
            }
        }
    }

} // End of MainActivity
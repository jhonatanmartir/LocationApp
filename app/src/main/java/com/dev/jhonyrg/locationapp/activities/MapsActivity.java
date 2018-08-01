package com.dev.jhonyrg.locationapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.dev.jhonyrg.locationapp.R;
import com.dev.jhonyrg.locationapp.utils.CurrentLocation;
import com.dev.jhonyrg.locationapp.utils.CustomInfoWindows;
import com.dev.jhonyrg.locationapp.utils.LocationData;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback, CurrentLocation.OnCurrentLocation {
    private static final int REQUEST_CHECK_SETTINGS = 100;
    private static final String TAG = "MainActivity";

    private GoogleMap mMap;
    private Marker marker;
    LatLng location;
    private CurrentLocation currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        this.currentLocation = new CurrentLocation(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentLocation.startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        currentLocation.stopLocationUpdates();
    }

    private void addMarker(LocationData data){
        if(data != null){
//            if(location != null && location.latitude == data.getLatitude() && location.longitude == data.getLongitude()){
//                return;
//            }

            location = new LatLng(data.getLatitude(), data.getLongitude());

            if(marker == null){
                MarkerOptions options = new MarkerOptions().position(location)
                        .title("Current Position")
                        .snippet(new Gson().toJson(data));
                marker = mMap.addMarker(options);
            } else {
                marker.setPosition(location);
                marker.setTitle("Current Position");
                marker.setSnippet(new Gson().toJson(data));
            }

            // Construct a CameraPosition focusing on CurrentLocation View and animate the camera to that position.
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(location)           // Sets the center of the map to CurrentLocation View
                    .zoom(15.0f)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        // Set a preference for minimum and maximum zoom.
        mMap.setMinZoomPreference(10.0f);
        mMap.setMaxZoomPreference(20.0f);

        //Set custom adapter
        CustomInfoWindows infoWindows = new CustomInfoWindows(this);
        mMap.setInfoWindowAdapter(infoWindows);
    }

    @Override
    public void onSuccessLocation(LocationData data) {
        addMarker(data);
    }

    @Override
    public void onFailureSettingLocation(Exception e) {
        int statusCode = ((ApiException) e).getStatusCode();

        switch (statusCode) {
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                        "location settings ");

                try {
                    // Show the dialog by calling startResolutionForResult(), and check the
                    // result in onActivityResult().
                    ResolvableApiException rae = (ResolvableApiException) e;
                    rae.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sie) {
                    Log.i(TAG, "PendingIntent unable to execute request.");
                }
                break;

            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                String errorMessage = "Location settings are inadequate, and cannot be " +
                        "fixed here. Fix in Settings.";

                Log.e(TAG, errorMessage);
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCompletedLocation(Task result) {
        Toast.makeText(this, "Request update stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        currentLocation.startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        currentLocation.stopLocationUpdates();
                        break;
                }
                break;
        }
        currentLocation.updateLocationData();
    }
}

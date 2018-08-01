package com.dev.jhonyrg.locationapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.dev.jhonyrg.locationapp.R;
import com.dev.jhonyrg.locationapp.utils.CurrentLocation;
import com.dev.jhonyrg.locationapp.utils.LocationData;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements CurrentLocation.OnCurrentLocation{
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 111;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CHECK_SETTINGS = 100;

    @BindView(R.id.txtvLatitude) TextView latitude;
    @BindView(R.id.txtvLongitude) TextView longitude;
    @BindView(R.id.txtvAddress) TextView address;
    @BindView(R.id.txtvTime) TextView time;
    @BindView(R.id.txtvCSpeed) TextView speed;
    @BindView(R.id.tgBtnTracking) ToggleButton tracking;
    @BindView(R.id.btnMap) Button map;

    CurrentLocation myLocation;
    private Boolean permission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        this.getLocationPermission();
        this.myLocation = new CurrentLocation(this, this.permission);
        this.myLocation.updateLocationData();

        tracking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    myLocation.startLocationUpdates();

                }else {
                    myLocation.stopLocationUpdates();
                }
            }
        });

        map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setUpdateLocationUI(LocationData data){
        this.latitude.setText(String.valueOf(data.getLatitude()));
        this.longitude.setText(String.valueOf(data.getLongitude()));
        this.address.setText(data.getAddress());
        this.time.setText(data.getTimestamp());
        this.speed.setText(data.getSpeed());
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            permission = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permission = true;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        myLocation.updateLocationData();
    }

    @Override
    public void onSuccessLocation(LocationData data) {
        this.setUpdateLocationUI(data);
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
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        myLocation.stopLocationUpdates();
                        break;
                }
                break;
        }
        myLocation.updateLocationData();
    }
}

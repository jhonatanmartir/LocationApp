package com.dev.jhonyrg.locationapp.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LocationFetch implements OnSuccessListener<LocationSettingsResponse>,
        OnFailureListener, OnCompleteListener<Void>{
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final String TAG = "LocationFetch";

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private SettingsClient mSettingsClient;
    private LocationSettingsRequest mLocationSettingsRequest;
    private Location mCurrentLocation;

    private boolean mLocationPermissionGranted;
    private Boolean mRequestingLocationUpdates;
    private String mLastUpdateTime;
    private Context context;
    private OnLocationFetch locationListener;

    public LocationFetch(Context context) {
        this.context = context;
        this.locationListener = (OnLocationFetch) context;

        mLocationPermissionGranted = false;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mSettingsClient = LocationServices.getSettingsClient(context);

        mRequestingLocationUpdates = true;
        mLastUpdateTime = "";

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    public LocationFetch(Context context, OnLocationFetch listener) {
        this.context = context;
        this.locationListener = listener;

        mLocationPermissionGranted = false;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mSettingsClient = LocationServices.getSettingsClient(context);

        mRequestingLocationUpdates = true;
        mLastUpdateTime = "";

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateLocationData();
            }
        };
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void startLocationUpdates() {
        if(mRequestingLocationUpdates)
        {
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest).addOnSuccessListener(this);
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest).addOnFailureListener(this);
        }
    }

    public void updateLocationData() {
        locationListener.onSuccessLocation(getLocationData(mCurrentLocation));
    }

    public void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        mFusedLocationClient.removeLocationUpdates(mLocationCallback).addOnCompleteListener(this);
    }

    public Boolean getmRequestingLocationUpdates() {
        return mRequestingLocationUpdates;
    }

    public void setmRequestingLocationUpdates(Boolean mRequestingLocationUpdates) {
        this.mRequestingLocationUpdates = mRequestingLocationUpdates;
    }

    public boolean ismLocationPermissionGranted() {
        return mLocationPermissionGranted;
    }

    public void setmLocationPermissionGranted(boolean mLocationPermissionGranted) {
        this.mLocationPermissionGranted = mLocationPermissionGranted;
    }

    private LocationData getLocationData(Location location){
        LocationData data = new LocationData();
        try {
            if (location != null) {                                 //Alternatively evaluate state of permission
                Geocoder geocoder = new Geocoder(context);
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                data.setAddress(getStringAddress(addresses.get(0)));
                data.setLatitude(location.getLatitude());
                data.setLongitude(location.getLongitude());
                data.setTimestamp(mLastUpdateTime);
            } else {
                data.setAddress("Unknown");
                data.setLatitude(0.0);
                data.setLongitude(0.0);
                data.setTimestamp("Unknown");
            }
        } catch (SecurityException e)  {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return data;
    }

    private String getStringAddress(Address address) {
        String resultAddress;
        if(address != null) {
            ArrayList<String> addressParts = new ArrayList<>();

            // Fetch the address lines using getAddressLine
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressParts.add(address.getAddressLine(i));
            }
            resultAddress = TextUtils.join("\n", addressParts);
        } else {
            resultAddress = "Address not found";
        }
        return resultAddress;
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        locationListener.onFailureSetting(e);
        //updateLocationData();
    }

    @Override
    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
        Log.i(TAG, "All location settings are satisfied.");
        Toast.makeText(context, "Started location updates!", Toast.LENGTH_SHORT).show();

        try{
            //if (mLocationPermissionGranted) {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                updateLocationData();
            //}
        }
        catch (SecurityException e){
            Log.e(TAG, e.getMessage());
        }

    }

    @Override
    public void onComplete(@NonNull Task<Void> task) {
        mRequestingLocationUpdates = false;
        locationListener.onCompletedLocation(false);
    }

    public interface OnLocationFetch {
        void onSuccessLocation(LocationData data);
        void onFailureSetting(Exception e);
        void onCompletedLocation(Boolean requestingUpdates);
    }
}

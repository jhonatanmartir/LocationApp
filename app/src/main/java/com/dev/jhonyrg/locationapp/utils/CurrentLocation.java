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

public class CurrentLocation implements OnSuccessListener<Object>,
        OnFailureListener, OnCompleteListener<Void> {
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final String TAG = "CurrentLocation";

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private SettingsClient mSettingsClient;
    private LocationSettingsRequest mLocationSettingsRequest;
    private Location mCurrentLocation;

    private Boolean mLocationPermissionGranted;
    private Boolean mRequestingLocationUpdates;
    private String mLastUpdateTime;
    private Context context;
    private OnCurrentLocation locationListener;
    private String mSpeed;

    public CurrentLocation(Context context, Boolean permission) {
        this.context = context;
        this.locationListener = (OnCurrentLocation) context;
        this.mRequestingLocationUpdates = false;

        mLocationPermissionGranted = permission;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mSettingsClient = LocationServices.getSettingsClient(context);

        mLastUpdateTime = "Waiting...";
        mSpeed = "Getting...";

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    private void createLocationCallback() {
        if (mLocationPermissionGranted) {
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);

                    mCurrentLocation = locationResult.getLastLocation();
                    mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

                    double a = 3.6 * (mCurrentLocation.getSpeed());     //speed * (18/5)
                    int kmhSpeed = (int) (Math.round(a));
                    mSpeed = String.valueOf(kmhSpeed) + "Km/h";

                    updateLocationData();
                }
            };
        }
    }

    private void buildLocationSettingsRequest() {
        if (mLocationPermissionGranted) {
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
            builder.addLocationRequest(mLocationRequest);
            mLocationSettingsRequest = builder.build();
        }

    }

    private void createLocationRequest() {
        if(mLocationPermissionGranted) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
            mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }

    public void startLocationUpdates() {
        if(mLocationPermissionGranted){
            mRequestingLocationUpdates = true;
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest).addOnSuccessListener(this);
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest).addOnFailureListener(this);
        }
    }

    public void updateLocationData() {
        if(!mLocationPermissionGranted){
            return;
        }else if(mCurrentLocation == null)
        {
            try {
                Task locationResult = mFusedLocationClient.getLastLocation();
                locationResult.addOnSuccessListener(this);
                locationResult.addOnFailureListener(this);
            } catch(SecurityException e)  {
                Log.e(TAG, e.getMessage());
            }
        }else {
            locationListener.onSuccessLocation(getLocationData(mCurrentLocation));
        }
    }

    public void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        mFusedLocationClient.removeLocationUpdates(mLocationCallback).addOnCompleteListener(this);
        mRequestingLocationUpdates = false;
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

                double a = 3.6 * (location.getSpeed());
                int kmhSpeed = (int) (Math.round(a));
                data.setSpeed(String.valueOf(kmhSpeed) + "Km/h");
            } else {
                data.setAddress("Unknown");
                data.setLatitude(0.0);
                data.setLongitude(0.0);
                data.setTimestamp(mLastUpdateTime);
                data.setSpeed(mSpeed);
            }
        } catch (SecurityException e)  {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return data;
    }

    private String getStringAddress(Address address) {
        String resultAddress = "No available";
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
    public void onComplete(@NonNull Task<Void> task) {
        locationListener.onCompletedLocation(task);
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        locationListener.onFailureSettingLocation(e);
        //updateLocationData();
    }

    @Override
    public void onSuccess(Object result) {
        if(result != null && mRequestingLocationUpdates  && result.getClass() == LocationSettingsResponse.class){
            Log.i(TAG, "All location settings are satisfied.");
            Toast.makeText(context, "Started location updates!", Toast.LENGTH_SHORT).show();
            try{
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                updateLocationData();
            }
            catch (SecurityException e){
                Log.e(TAG, e.getMessage());
            }
        } else if(result != null && result.getClass() == Location.class){
            mCurrentLocation = (Location) result;
            updateLocationData();
        }
    }

    public interface OnCurrentLocation {
        void onSuccessLocation(LocationData data);
        void onFailureSettingLocation(Exception e);
        void onCompletedLocation(Task result);
    }
}
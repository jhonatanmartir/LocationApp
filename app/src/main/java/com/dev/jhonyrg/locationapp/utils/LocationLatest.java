package com.dev.jhonyrg.locationapp.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationLatest implements OnSuccessListener<Location>, OnFailureListener{
    private static final String TAG = "LocationLatest";

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Geocoder geocoder;

    private boolean mLocationPermissionGranted;
    private OnLocation dataListener;
    private LocationData data;
    private Context context;

    public LocationLatest(Context context) {
        this.context = context;
        this.mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        this.geocoder = new Geocoder(context, Locale.getDefault());
        this.dataListener = (OnLocation) context;
        this.data = new LocationData();
    }

    public LocationLatest(Context context, OnLocation listener) {
        this.context = context;
        this.mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        this.geocoder = new Geocoder(context, Locale.getDefault());
        this.dataListener = listener;
        this.data = new LocationData();
    }

    public boolean ismLocationPermissionGranted() {
        return mLocationPermissionGranted;
    }

    public void setmLocationPermissionGranted(boolean mLocationPermissionGranted) {
        this.mLocationPermissionGranted = mLocationPermissionGranted;
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

    public void lastDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnSuccessListener(this);
                locationResult.addOnFailureListener(this);
            }
        } catch(SecurityException e)  {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onSuccess(Location location) {
        try {
            if (location != null) {                                 //Alternatively evaluate state of permission
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                data.setAddress(getStringAddress(addresses.get(0)));
                data.setLatitude(location.getLatitude());
                data.setLongitude(location.getLongitude());
            } else {
                data.setAddress("Unknown");
                data.setLatitude(0.0);
                data.setLongitude(0.0);
            }
        } catch (SecurityException e)  {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        dataListener.onLocation(data);
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Log.d(TAG, "Error trying to get last GPS location");
        e.printStackTrace();
    }

    public interface OnLocation{
        void onLocation(LocationData data);
    }
}

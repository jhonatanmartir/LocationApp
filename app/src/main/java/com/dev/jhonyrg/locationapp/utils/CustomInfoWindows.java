package com.dev.jhonyrg.locationapp.utils;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dev.jhonyrg.locationapp.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;


import butterknife.BindView;
import butterknife.ButterKnife;

public class CustomInfoWindows implements GoogleMap.InfoWindowAdapter{
    private Activity context;

    @BindView(R.id.txtvTitle) TextView title;
    @BindView(R.id.txtvLat) TextView latitude;
    @BindView(R.id.txtvLong) TextView longitude;
    @BindView(R.id.txtvAddr) TextView address;
    @BindView(R.id.txtvTimes) TextView timestamp;
    @BindView(R.id.txtvSpeed) TextView speed;
    @BindView(R.id.imgvLocation) ImageView img;

    public CustomInfoWindows(Activity context) {
        this.context = context;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        View view = context.getLayoutInflater().inflate(R.layout.custom_infowindow, null);
        ButterKnife.bind(this, view);

        LocationData data = new Gson().fromJson(marker.getSnippet(), LocationData.class);
        if(data != null){
            this.title.setText(marker.getTitle());
            this.latitude.setText(String.valueOf(data.getLatitude()));
            this.longitude.setText(String.valueOf(data.getLongitude()));
            this.address.setText(data.getAddress());
            this.timestamp.setText(data.getTimestamp());
            this.speed.setText(data.getSpeed());
            Picasso.get().load(R.drawable.ic_location).into(img);
        }

        return view;
    }
}

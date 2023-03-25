package com.lucario.reminder;

import com.google.android.gms.maps.model.LatLng;

public class Reminder {
    String name;
    String desc;
    String dateCreated;
    String expiry;
    LatLng latLng;
    Geofence geofence;

    public Reminder(String name, String desc, String dateCreated, String expiry, LatLng latLng, Geofence geofence) {
        this.name = name;
        this.desc = desc;
        this.dateCreated = dateCreated;
        this.expiry = expiry;
        this.latLng = latLng;
        this.geofence = geofence;
    }

}

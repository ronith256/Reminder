package com.lucario.reminder;

import org.osmdroid.util.GeoPoint;

public class Geofence {
    private String id;
    private GeoPoint center;
    private float radius;

    public Geofence(String id, GeoPoint center, float radius) {
        this.id = id;
        this.center = center;
        this.radius = radius;
    }

    public String getId() {
        return id;
    }

    public GeoPoint getCenter() {
        return center;
    }

    public float getRadius() {
        return radius;
    }
}


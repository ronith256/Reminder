package com.lucario.reminder;

import org.osmdroid.util.GeoPoint;

import java.io.Serializable;

public class Geofence implements Serializable {
    private static final long serialVersionUID = 2L;
    private String id;
    private GeoPoint center;
    private float radius;

    public Geofence(String id, GeoPoint center, float radius)  {
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


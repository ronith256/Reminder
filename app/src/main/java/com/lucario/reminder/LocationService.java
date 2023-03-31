package com.lucario.reminder;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;

public class LocationService extends Service implements LocationListener {

    private LocationManager locationManager;
    private ArrayList<Reminder> reminders;
    float radius;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start requesting location updates
        Bundle extras = intent.getExtras();
        if (extras != null) {
            // Get the ArrayList and float from the extras
            reminders = (ArrayList<Reminder>) intent.getSerializableExtra("reminders");
            radius = extras.getFloat("radius");
            System.out.println(radius);
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

        ArrayList<Float> distanceArr = getDistances(currentLocation.getLatitude(), currentLocation.getLongitude());
        for(int i = 0; i < distanceArr.size(); i++){
            if(distanceArr.get(i) <= radius){
                sendNearbyNotification(reminders.get(i).name);
            }
        }
    }

    public void sendNearbyNotification(String name) {
        String channelId = "reminder";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Reminder")
                .setContentText("You are near " + name)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Create a notification channel
        CharSequence channelName = "Reminders";
        String description = "This is used to send you reminders";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);

        // notificationId is a unique int for each notification that you must define
        int notificationId = name.hashCode();
        notificationManager.notify(notificationId, builder.build());
    }

    private ArrayList<Float> getDistances(double latitude, double longitude){
        ArrayList<Float> distanceArray = new ArrayList<>();
        for(int i = 0; i < reminders.size(); i++){
            Geofence g = reminders.get(i).geofence;
            float[] results = new float[1];
            Location.distanceBetween(latitude, longitude, g.getCenter().getLatitude(), g.getCenter().getLongitude(), results);
            distanceArray.add(results[0]);
        }
        return distanceArray;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Handle status changes here
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Handle provider enabled here
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Handle provider disabled here
    }
}


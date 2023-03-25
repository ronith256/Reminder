package com.lucario.reminder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private MapView mapView;
    private Geofence[] geofences;
    private LocationManager locationManager;

    private AutoCompleteTextView searchEditText;
    private GeocoderNominatim geocoder;

    private GeocodeTask geocodeTask;

    static ArrayList<Address> addresses = new ArrayList<>();

    private MapView itemViewMap;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        org.osmdroid.config.IConfigurationProvider osmConf = org.osmdroid.config.Configuration.getInstance();
        File basePath = new File(getCacheDir().getAbsolutePath(), "osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(osmConf.getOsmdroidBasePath().getAbsolutePath(), "tile");
        osmConf.setOsmdroidTileCache(tileCache);
        osmConf.setUserAgentValue("chrome");
        setContentView(R.layout.activity_main);
        geofences = new Geofence[10];
        geocoder = new GeocoderNominatim(BuildConfig.APPLICATION_ID);
        geocoder.setService("https://nominatim.openstreetmap.org/");

        ImageButton addButton = (ImageButton) findViewById(R.id.imageButton);
        addButton.setOnClickListener(e->{
            showPopup();
        });

        mapView = findViewById(R.id.mapview);
        GeoPoint center = new GeoPoint( 10.884319, 76.908697); // New York City
        float radius = 1000.0f; // 1 kilometer
        geofences[0] = new Geofence("my_geofence", center, radius);
        // Set the map center and zoom level
        GeoPoint startPoint = new GeoPoint(40.712776, -74.005974); // New York City
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mapView.getController().setZoom(10.0);
        mapView.getController().setCenter(startPoint);
        start();
    }

    private void start(){
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                checkLocationPermission();
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, this);
        }
    }

    private void showPopup() {
        // Inflate the popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.activity_add_item, null);

        // Create the popup window
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // Show the popup window
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);
        searchEditText = popupView.findViewById(R.id.searchEditText);
        geocodeTask = new GeocodeTask();
        itemViewMap = popupView.findViewById(R.id.newItemMap);
        itemViewMap.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        itemViewMap.getController().setZoom(17.0);

        searchEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Address result = addresses.get(0);
                GeoPoint point = new GeoPoint(result.getLatitude(), result.getLongitude());
                itemViewMap.getController().animateTo(point);
                searchEditText.dismissDropDown();
                searchEditText.clearListSelection();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel any previous GeocodeTask
                if (geocodeTask != null) {
                    geocodeTask.cancel(true);
                }

                // Start a new GeocodeTask with the entered address
                String address = s.toString();
                if (!TextUtils.isEmpty(address)) {
                    geocodeTask = new GeocodeTask();
                    geocodeTask.execute(address);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // Add the blur
        View container = (View) popupWindow.getContentView().getParent();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.7f;
        wm.updateViewLayout(container, p);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private class GeocodeTask extends AsyncTask<String, Void, List<Address>>{

        private Geocoder geocoder;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Initialize the geocoder
            geocoder = new Geocoder(MainActivity.this);
        }

        @Override
        protected List<Address> doInBackground(String... params) {
            String address = params[0];
            try {
                return geocoder.getFromLocationName(address, 10);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {
            super.onPostExecute(addresses);
            if (addresses != null && !addresses.isEmpty()) {
                // Create an array of suggestion strings from the list of addresses
                String[] suggestions = new String[addresses.size()];
                for (int i = 0; i < addresses.size(); i++) {
                    suggestions[i] = addresses.get(i).getAddressLine(0);
                }

                MainActivity.addresses.addAll(addresses);
                // Create an adapter for the suggestion dropdown list
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_dropdown_item_1line, suggestions);
                searchEditText.setAdapter(adapter);
                searchEditText.showDropDown();
            }
        }
    }
    private void checkLocationPermission() {
        // Check if the location permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the location permission
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Check if the user granted the location permission
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                start();
            } else {
               checkLocationPermission();
            }
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

        // Check if the current location is inside the geofence
        float[] results = new float[1];
        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                geofences[0].getCenter().getLatitude(), geofences[0].getCenter().getLongitude(), results);
        float distanceInMeters = results[0];
        if (distanceInMeters <= geofences[0].getRadius()) {
//            Toast.makeText(this, "You're in location", Toast.LENGTH_LONG).show();
            // Do something when the user is inside the geofence
        } else {
//            Log.d("Geofence", "Current location is outside the geofence.");
            // Do something when the user is outside the geofence
        }
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}


}


package com.lucario.reminder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.icu.text.SimpleDateFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements RemindersViewAdapter.click {

    private MapView mapView;

    float radius = 1000.0f;

    private AutoCompleteTextView searchEditText;

    private GeocodeTask geocodeTask;

    static ArrayList<Address> addresses = new ArrayList<>();

    public static ArrayList<Reminder> reminders = new ArrayList<>();

    private MapView itemViewMap;

    private EditText topicText;

    private Address address;

    RecyclerView remindersView;

    RemindersViewAdapter remindersViewAdapter;

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

        reminders = loadRemindersFromFile(getApplicationContext());
        GeocoderNominatim geocoder = new GeocoderNominatim(BuildConfig.APPLICATION_ID);
        geocoder.setService("https://nominatim.openstreetmap.org/");

        remindersView = findViewById(R.id.remindersView);
        remindersViewAdapter = new RemindersViewAdapter(getApplicationContext(), reminders, this);
        remindersView.setAdapter(remindersViewAdapter);
        remindersView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        ImageButton addButton = (ImageButton) findViewById(R.id.imageButton);
        addButton.setOnClickListener(e-> showPopup());
        ImageView hamburgerMenu = findViewById(R.id.hamburger);
        hamburgerMenu.setOnClickListener(e->onHamburgerClick(hamburgerMenu.getX(), hamburgerMenu.getY()));



        mapView = findViewById(R.id.mapview);
        // Set the map center and zoom level
        start();
    }

    private void start(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                checkLocationPermission();
                return;
            }
            Location curLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
           if(curLoc != null){
               GeoPoint startPoint = new GeoPoint(curLoc.getLatitude(), curLoc.getLongitude());
               mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
               mapView.getController().setZoom(17.0);
               setNightMode(mapView);
               mapView.getController().setCenter(startPoint);
               mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
               mapView.setMultiTouchControls(true);
//               locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, this);
               Intent intent = new Intent(this, LocationService.class);
               intent.putExtra("reminders", reminders);
               intent.putExtra("radius", radius);
               if (Build.VERSION.SDK_INT >= 33) {
                   if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                       ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS},101);
                   }
                   else {
                       startService(intent);
                   }
               }

           } else {
               Toast.makeText(getApplicationContext(), "GPS cannot be acquired", Toast.LENGTH_SHORT).show();
           }
        }
    }



    private void setNightMode(MapView mapView){
        int nightModeFlags =
                getApplicationContext().getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            mapView.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
        }
    }
    private void showPopup() {
        // Inflate the popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.activity_add_item, new CardView(MainActivity.this));

        // Create the popup window
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // Show the popup window
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);
        searchEditText = popupView.findViewById(R.id.searchEditText);
        geocodeTask = new GeocodeTask(new WeakReference<>(MainActivity.this), new WeakReference<>(searchEditText));
        itemViewMap = popupView.findViewById(R.id.newItemMap);
        itemViewMap.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        setNightMode(itemViewMap);
        itemViewMap.getController().setZoom(17.0);
        itemViewMap.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        itemViewMap.setMultiTouchControls(true);


        Button addButton = popupView.findViewById(R.id.addButton);
        Button cancelButton = popupView.findViewById(R.id.cancelButton);

        addButton.setOnClickListener(e->{
            onAddButtonClick();
            popupWindow.dismiss();
        });
        cancelButton.setOnClickListener(e->popupWindow.dismiss());
        topicText = popupView.findViewById(R.id.topicEditText);

        searchEditText.setOnItemClickListener((adapterView, view, i, l) -> {
            Address result = addresses.get(0);
            GeoPoint point = new GeoPoint(result.getLatitude(), result.getLongitude());
            itemViewMap.getController().animateTo(point);
            searchEditText.dismissDropDown();
            searchEditText.clearListSelection();
            address = result;
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
                    geocodeTask = new GeocodeTask(new WeakReference<>(MainActivity.this), new WeakReference<>(searchEditText));
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



    private void onAddButtonClick(){
        String topic = topicText.getText().toString();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String desc = "TODO";
        String expiry = "TODO";
        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
        Geofence geofence = new Geofence(topic, new GeoPoint(address.getLatitude(), address.getLongitude()), 1);
        reminders.add(new Reminder(topic, desc, date, expiry, latLng, geofence));
        writeToStorage();
        refreshRemindersView();
    }

    private void refreshRemindersView(){
        remindersView = findViewById(R.id.remindersView);
        remindersViewAdapter = new RemindersViewAdapter(MainActivity.this, reminders, this);
        remindersView.setAdapter(remindersViewAdapter);
        remindersView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    private void writeToStorage(){
        try {
            FileOutputStream fileOutputStream = openFileOutput("myReminders", MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(reminders);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @SuppressWarnings("unchecked")
    public ArrayList<Reminder> loadRemindersFromFile(Context context) {
        ArrayList<Reminder> reminders = new ArrayList<>();
        try {
            FileInputStream fileInputStream = context.openFileInput("myReminders");
            System.out.println(fileInputStream.available());
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            reminders = (ArrayList<Reminder>) objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return reminders;
    }

    @Override
    public void onLongPress(int position, int color) {
        // Inflate the popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.reminder_single_view, new CardView(MainActivity.this));

        // Create the popup window
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        Reminder r = reminders.get(position);
        GeoPoint point = new GeoPoint(r.geofence.getCenter().getLatitude(), r.geofence.getCenter().getLongitude());

        Marker marker = new Marker(mapView);
        Drawable mark = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_location_on_24);
        if(mark!=null){
            mark.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        }
        marker.setIcon(mark);
        marker.setImage(mark);
        marker.setTitle(r.name);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);


        // Show the popup window
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);
        MapView itemViewMap = popupView.findViewById(R.id.newItemMap);
        itemViewMap.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        itemViewMap.getController().setZoom(17.0);
        itemViewMap.getOverlays().add(marker);
        itemViewMap.getController().animateTo(point);
        TextView t = popupView.findViewById(R.id.nameTextView);
        t.setText(r.name);
        itemViewMap.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        itemViewMap.setMultiTouchControls(true);
        setNightMode(itemViewMap);



        Button deleteButton = popupView.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(e->{
            reminders.remove(position);
            popupWindow.dismiss();
            refreshRemindersView();
            writeToStorage();
        });

        // Add the blur
        View container = (View) popupWindow.getContentView().getParent();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.7f;
        wm.updateViewLayout(container, p);
    }


    private static class GeocodeTask extends AsyncTask<String, Void, List<Address>>{

        private Geocoder geocoder;
        private final WeakReference<Context> contextRef;

        private final WeakReference<AutoCompleteTextView> searchEditText;

        private GeocodeTask(WeakReference<Context> contextRef, WeakReference<AutoCompleteTextView> searchEditText) {
            super();
            this.contextRef = contextRef;
            this.searchEditText = searchEditText;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Initialize the geocoder
            geocoder = new Geocoder(contextRef.get());
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
                ArrayAdapter<String> adapter = new ArrayAdapter<>(contextRef.get(),
                        android.R.layout.simple_dropdown_item_1line, suggestions);
                searchEditText.get().setAdapter(adapter);
                searchEditText.get().showDropDown();
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

        if(requestCode == 101){
            if(grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intent = new Intent(this, LocationService.class);
                intent.putExtra("reminders", reminders);
                intent.putExtra("radius", radius);
                startService(intent);
            } else {
                Toast.makeText(MainActivity.this, "Please enable notification permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onHamburgerClick(float x, float y){
        View popupView = LayoutInflater.from(this).inflate(R.layout.activity_settings, new CardView(MainActivity.this));

        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        EditText customRadius = popupView.findViewById(R.id.customRadiusEdit);

        customRadius.setOnKeyListener((view, i, keyEvent) -> {
            if ((keyEvent.getAction() == EditorInfo.IME_ACTION_DONE)){
                try{
                    radius = Float.parseFloat(customRadius.getText().toString()) * 1000f;
                    getSharedPreferences("reminder", Context.MODE_PRIVATE).edit().putFloat("radius", radius).apply();
                } catch (Exception ignored){
                    Toast.makeText(getApplicationContext(), "Invalid input", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        popupWindow.showAtLocation(popupView, Gravity.NO_GRAVITY, (int)x, (int)y+200);
        View container = (View) popupWindow.getContentView().getParent();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.5f;
        wm.updateViewLayout(container, p);
    }
}


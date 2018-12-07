package four_k.coinz;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.DecimalFormat;
import android.location.Location;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;

import java.util.List;

public class MapboxActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener, PermissionsListener, DownloadFileTask.AsyncResponse{

    private static final String TAG = "MapboxActivity";
    private MapView mapView;
    private MapboxMap map;

    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location originLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this,getString(R.string.access_token));
        setContentView(R.layout.activity_mapbox);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    // OnMapReadyCallback
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        if (mapboxMap == null){
            Log.d(TAG, "[onMapReady] mapBox is null");
        } else {
            map = mapboxMap;
            // Set user interface options
            map.getUiSettings().setCompassEnabled(true);
            map.getUiSettings().setZoomControlsEnabled(true);
            // Add locate user option and hide it on click
            FloatingActionButton fab = findViewById(R.id.floatingActionButton);
            fab.setOnClickListener((View view) -> {
                if (originLocation != null) {
                    setCameraPosition(originLocation);
                    fab.hide();
                }
            });
            // Show locate used button if user moves camera
            map.addOnCameraMoveStartedListener(reason -> {
                if (reason == 1){
                    fab.show();
                }
            });

            // Add current markers
            addMarkers();
            // Make location info available
            enableLocation();
        }
    }

    private void addMarkers() {
        DownloadFileTask task = new DownloadFileTask();
        task.delegate = this;
        task.execute("http://homepages.inf.ed.ac.uk/stg/coinz/2018/11/29/coinzmap.geojson");
    }

    // Async DownloadFileTask callback
    @Override
    @SuppressWarnings("ConstantConditions")
    public void processFinish(String s){
        // Create icons of different color for different currencies
        IconFactory iconFactory = IconFactory.getInstance(this);
        Bitmap dolrBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dolr_marker);
        Bitmap penyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.peny_marker);
        Bitmap quidBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.quid_marker);
        Bitmap shilBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.shil_marker);
        Icon dolrIcon = iconFactory.fromBitmap(dolrBitmap);
        Icon penyIcon = iconFactory.fromBitmap(penyBitmap);
        Icon quidIcon = iconFactory.fromBitmap(quidBitmap);
        Icon shilIcon = iconFactory.fromBitmap(shilBitmap);
        DecimalFormat df = new DecimalFormat("#.##");
        // Extract Feature collection from today's geoJson
        FeatureCollection fc = FeatureCollection.fromJson(s);
        if (fc.features() != null) {
            for (Feature f : fc.features()) {
                Point p = (Point) f.geometry();
                // Check what currency a coin is
                switch (f.properties().get("currency").getAsString()) {
                    case "DOLR":
                        map.addMarker(new MarkerOptions()
                                // Draw icon based on coin's currency
                                .icon(dolrIcon)
                                // Format title into (Value 2 decimal places + currency)
                                .title(df.format(Float.parseFloat(f.properties().get("value").getAsString())) + " " + f.properties().get("currency").getAsString())
                                // Draw point based on the latitude and longitude
                                .position(new LatLng(p.latitude(), p.longitude())));
                        break;
                    case "PENY":
                        map.addMarker(new MarkerOptions()
                                .icon(penyIcon)
                                .title(df.format(Float.parseFloat(f.properties().get("value").getAsString())) + " " + f.properties().get("currency").getAsString())
                                .position(new LatLng(p.latitude(), p.longitude())));
                        break;
                    case "QUID":
                        map.addMarker(new MarkerOptions()
                                .icon(quidIcon)
                                .title(df.format(Float.parseFloat(f.properties().get("value").getAsString())) + " " + f.properties().get("currency").getAsString())
                                .position(new LatLng(p.latitude(), p.longitude())));
                        break;
                    case "SHIL":
                        map.addMarker(new MarkerOptions()
                                .icon(shilIcon)
                                .title(df.format(Float.parseFloat(f.properties().get("value").getAsString())) + " " + f.properties().get("currency").getAsString())
                                .position(new LatLng(p.latitude(), p.longitude())));
                        break;
                    default: break;
                }
            }
        }
    }

    private void enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(TAG, "Permissions are granted");
            initializeLocationEngine();
            initializeLocationLayer();
        } else {
            Log.d(TAG, "Permissions are not granted");
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine() {
        locationEngine = new LocationEngineProvider(this)
                .obtainBestLocationEngineAvailable();
        locationEngine.setInterval(5000);
        locationEngine.setFastestInterval(1000);
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationLayer() {
        if (mapView == null) {
            Log.d(TAG, "[initializeLocationLayer] mapView is null");
        } else {
            if (map == null) {
                Log.d(TAG, "[initializeLocationLayer] map is null");
            } else {
                locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
                locationLayerPlugin.setLocationLayerEnabled(true);
                locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
                locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
            }
        }
    }

    private void setCameraPosition(Location location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                location.getLongitude()), 16));

    }
    // LocationEngineListener
    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        Log.d(TAG, "[onConnected] requesting location updates");
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        FloatingActionButton fab = findViewById(R.id.floatingActionButton);
        if (location == null) {
            Log.d(TAG, "[onLocationChanged] location is null");
            // Camera follows user only if user centered camera on the user
        } else if (fab.isOrWillBeHidden()){
            originLocation = location;
            setCameraPosition(location);
            // Camera doesn't follow if not centered on the user
        } else {
            originLocation = location;
        }
    }

    // PermissionListener
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        // present toast or dialog
        Log.d(TAG, "[onExplanationNeeded] Permissions" + permissionsToExplain.toString());
    }

    @Override
    public void onPermissionResult(boolean granted) {
        Log.d(TAG, "[onPermissionResult] granted = "+granted);
        if (granted) {
            enableLocation();
        } else {
            Toast.makeText(getApplicationContext(), "Location is necessary to collect coins", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    @SuppressWarnings("MissingPermission")
    protected void onStart() {
        super.onStart();
        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationEngine != null){
            locationEngine.deactivate();
        }
        mapView.onDestroy();
    }

}

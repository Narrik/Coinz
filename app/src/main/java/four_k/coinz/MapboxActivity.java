package four_k.coinz;

import android.location.Location;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;

import org.json.JSONObject;

import java.util.List;

public class MapboxActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener, PermissionsListener, DownloadFileTask.AsyncResponse{

    private String tag = "MapboxActivity";
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
            Log.d(tag, "[onMapReady] mapBox is null");
        } else {
            map = mapboxMap;
            // Set user interface options
            map.getUiSettings().setCompassEnabled(true);
            map.getUiSettings().setZoomControlsEnabled(true);
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

    @Override
    public void processFinish(String s){
        FeatureCollection fc = FeatureCollection.fromJson(s);
        if (fc.features() != null) {
            for (Feature f : fc.features()) {
                Point p = (Point) f.geometry();
                map.addMarker(new MarkerOptions()
                        .title(f.properties().get("currency").getAsString())
                        .snippet("okk")
                        .position(new LatLng(p.latitude(), p.longitude()))
                );
            }
        }
    }

    private void enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "Permissions are granted");
            initializeLocationEngine();
            initializeLocationLayer();
        } else {
            Log.d(tag, "Permissions are not granted");
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
            Log.d(tag, "[initializeLocationLayer] mapView is null");
        } else {
            if (map == null) {
                Log.d(tag, "[initializeLocationLayer] map is null");
            } else {
                locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
                locationLayerPlugin.setLocationLayerEnabled(true);
                locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
                locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
            }
        }
    }

    private void setCameraPosition(Location location) {
        map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(),
                location.getLongitude())));

    }
    // LocationEngineListener
    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        Log.d(tag, "[onConnected] requesting location updates");
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            Log.d(tag, "[onLocationChanged] location is null");
        } else {
            Log.d(tag, "[onLocationChanged] location is not null");
            originLocation = location;
            setCameraPosition(location);
        }
    }

    // PermissionListener
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        // present toast or dialog
        Log.d(tag, "[onExplanationNeeded] Permissions" + permissionsToExplain.toString());
    }

    @Override
    public void onPermissionResult(boolean granted) {
        Log.d(tag, "[onPermissionResult] granted = "+granted);
        if (granted) {
            enableLocation();
        } else {
            Toast.makeText(getApplicationContext(), "Location is necessary to collect coins", Toast.LENGTH_SHORT).show();
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

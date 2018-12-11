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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
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
import java.util.Map;


public class MapboxActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener, PermissionsListener{

    private static final String TAG = "MapboxActivity";
    private MapView mapView;
    private MapboxMap map;

    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location currentLocation;
    private DocumentReference userData;
    private Icon dolrIcon;
    private Icon quidIcon;
    private Icon penyIcon;
    private Icon shilIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get mapbox instance
        Mapbox.getInstance(this,getString(R.string.access_token));
        setContentView(R.layout.activity_mapbox);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    // OnMapReadyCallback
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        // Display activity name and back arrow on toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Map");
        // Set up the mapbox
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
                if (currentLocation != null) {
                    setCameraPosition(currentLocation);
                    fab.hide();
                }
            });
            // Show locate user button if user moves camera
            map.addOnCameraMoveStartedListener(reason -> {
                if (reason == 1){
                    fab.show();
                }
            });
            // Create icons of different color for different currencies
            IconFactory iconFactory = IconFactory.getInstance(this);
            Bitmap dolrBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dolr_marker);
            Bitmap penyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.peny_marker);
            Bitmap quidBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.quid_marker);
            Bitmap shilBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.shil_marker);
            dolrIcon = iconFactory.fromBitmap(dolrBitmap);
            penyIcon = iconFactory.fromBitmap(penyBitmap);
            quidIcon = iconFactory.fromBitmap(quidBitmap);
            shilIcon = iconFactory.fromBitmap(shilBitmap);
            // Get current user
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            // If there is no user, don't continue
            if (currentUser == null){
                Log.d(TAG,"Cannot load maps if user is not logged in");
                finish();
            }
            // Access our database
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setTimestampsInSnapshotsEnabled(true)
                    .build();
            database.setFirestoreSettings(settings);
            // Get current user information
            userData = database.collection("Users").document(currentUser.getUid());
            // Draw coins which have not yet been collected
            drawCoins();
            // Make location info available
            enableLocation();
        }
    }


    public void drawCoins(){
        // Remove previous markers as this method is called to update when a coin is collected
        // Download information about which coins have not yet been collected
        userData.collection("Map")
                .whereGreaterThanOrEqualTo("value",0)
                .get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                map.removeAnnotations();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    drawCoin(document.toObject(Coin.class));
                }
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    private void drawCoin(Coin coin){
        DecimalFormat df = new DecimalFormat("0.00");
        // Check what currency a coin is
        switch (coin.getCurrency()) {
            case "DOLR":
                map.addMarker(new MarkerOptions()
                    // Draw icon based on coin's currency
                    .icon(dolrIcon)
                    // Format title into (Value 2 decimal places + currency)
                    .title(df.format(coin.getValue()) + " " + coin.getCurrency())
                    // Draw point based on the latitude and longitude
                    .position(new LatLng(coin.getLatitude(),coin.getLongitude())));
                break;
            case "QUID":
                map.addMarker(new MarkerOptions()
                        .icon(quidIcon)
                        .title(df.format(coin.getValue()) + " " + coin.getCurrency())
                        .position(new LatLng(coin.getLatitude(),coin.getLongitude())));
                break;
            case "PENY":
                map.addMarker(new MarkerOptions()
                        .icon(penyIcon)
                        .title(df.format(coin.getValue()) + " " + coin.getCurrency())
                        .position(new LatLng(coin.getLatitude(),coin.getLongitude())));
                break;
            case "SHIL":
                map.addMarker(new MarkerOptions()
                        .icon(shilIcon)
                        .title(df.format(coin.getValue()) + " " + coin.getCurrency())
                        .position(new LatLng(coin.getLatitude(),coin.getLongitude())));
                break;
            default: break;
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
            currentLocation = lastLocation;
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
        } else {
            // Camera follows user only if user centered camera on the user
            if (fab.isOrWillBeHidden()) {
                currentLocation = location;
                setCameraPosition(location);
            } else {
                // Camera doesn't follow if not centered on the user
                currentLocation = location;
            }
            collectCoins();
        }
    }

    public void collectCoins(){
        userData.collection("Map")
                .whereGreaterThanOrEqualTo("value",0)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (collectCoin(document.toObject(Coin.class))){
                                document.getReference().delete();
                                drawCoins();
                            }
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });
    }

    public boolean collectCoin(Coin coin){
        DecimalFormat df = new DecimalFormat("0.00");
        Location coinLocation = new Location("");
        coinLocation.setLatitude(coin.getLatitude());
        coinLocation.setLongitude(coin.getLongitude());
        float distanceInMetres = coinLocation.distanceTo(currentLocation);
        // If our distance to coin is less than 25 metres, user can press button to collect coin
        if (distanceInMetres <= 25) {
            Toast.makeText(getApplicationContext(), "Collected a "+df.format(coin.getValue())+" "+coin.getCurrency()+" coin!", Toast.LENGTH_SHORT).show();
            userData.collection("Wallet").document(coin.getId()).set(coin);
            return true;
        }
        return false;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        DecimalFormat df = new DecimalFormat("0.00");
        int id = item.getItemId();
        // Show today's exchange rates
        if (id == R.id.rates) {
            userData.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().getData() != null){
                    Map exchangeRates = task.getResult().getData();
                    item.getSubMenu().findItem(R.id.dolrRate).setTitle("DOLR = "+ df.format(Double.parseDouble(exchangeRates.get("DOLR").toString()))+" GOLD");
                    item.getSubMenu().findItem(R.id.quidRate).setTitle("QUID = "+ df.format(Double.parseDouble(exchangeRates.get("QUID").toString()))+" GOLD");
                    item.getSubMenu().findItem(R.id.penyRate).setTitle("PENY = "+ df.format(Double.parseDouble(exchangeRates.get("PENY").toString()))+" GOLD");
                    item.getSubMenu().findItem(R.id.shilRate).setTitle("SHIL = "+ df.format(Double.parseDouble(exchangeRates.get("SHIL").toString()))+" GOLD");
                } else {
                    Log.d(TAG, "Get failed with "+task.getException());
                }
            });
            return true;
        }
        // Show user's gold and bank in allowance
        if (id == R.id.goldBag){
            userData.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().getData() != null) {
                    Map userInfo = task.getResult().getData();
                    item.getSubMenu().findItem(R.id.gold).setTitle(userInfo.get("GOLD").toString() + " GOLD");
                    item.getSubMenu().findItem(R.id.bankAllowance).setTitle(userInfo.get("bankLimit").toString() + "/25 remaining");
                } else {
                    Log.d(TAG, "Get failed with " + task.getException());
                }
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

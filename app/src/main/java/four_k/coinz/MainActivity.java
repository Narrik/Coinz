package four_k.coinz;

import android.content.Intent;
import android.icu.text.DecimalFormat;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements DownloadFileTask.AsyncResponse {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore database;
    private DocumentReference userData;
    private String today;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Include toolbar actions
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Open login screen (doesn't open if user is logged in)
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        // Button for turning on Map and playing
        Button playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener((View view) -> {
            // Prevents users from clicking more than once until action is executed
            if (MisclickPreventer.cantClickAgain()) {
                return;
            }
            Intent mapIntent = new Intent(MainActivity.this, MapboxActivity.class);
            startActivity(mapIntent);
        });
        // Button for turning on chat
        Button chatButton = findViewById(R.id.chatButton);
        chatButton.setOnClickListener((View view) -> {
            // Prevents users from clicking more than once until action is executed
            if (MisclickPreventer.cantClickAgain()) {
                return;
            }
            Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(chatIntent);
        });
        // Button for accessing bank
        Button bankButton = findViewById(R.id.bankButton);
        bankButton.setOnClickListener((View view) -> {
            // Prevents users from clicking more than once until action is executed
            if (MisclickPreventer.cantClickAgain()) {
                return;
            }
            Intent bankIntent = new Intent(MainActivity.this, BankActivity.class);
            startActivity(bankIntent);
        });
        // Button for accessing leaderboard
        Button leaderboardButton = findViewById(R.id.leaderboardButton);
        leaderboardButton.setOnClickListener((View view) -> {
            // Prevents users from clicking more than once until action is executed
            if (MisclickPreventer.cantClickAgain()) {
                return;
            }
            Intent leaderboardIntent = new Intent(MainActivity.this, LeaderboardActivity.class);
            startActivity(leaderboardIntent);
        });
        // Button for signing out
        Button signOutButton = findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener((View view) -> {
            // Prevents users from clicking more than once until action is executed
            if (MisclickPreventer.cantClickAgain()) {
                return;
            }
            FirebaseAuth mAuth;
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                mAuth.signOut();
                Toast.makeText(this, user.getEmail() + " Signed out!", Toast.LENGTH_SHORT).show();
                startActivity(loginIntent);
            } else {
                Toast.makeText(this, "You aren't logged in!", Toast.LENGTH_SHORT).show();
            }
        });
        createUsername();
    }

    @Override
    public void onStart() {
        super.onStart();
        createUsername();
    }


    public void createUsername() {
        // Get current user
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // If there is no user, don't continue
        if (currentUser == null) {
            return;
        }
        // Access our database
        database = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        database.setFirestoreSettings(settings);
        // Get current user information
        userData = database.collection("Users").document(currentUser.getUid());
        userData.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getData() != null) {
                // If user has not set a name yet, ask them to create one with an uncancellable alert dialog
                if (task.getResult().getData().get("username").equals("")) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                    final ViewGroup nullParent = null;
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_username, nullParent);
                    EditText etUsername = dialogView.findViewById(R.id.etUsername);
                    Button btnAccept = dialogView.findViewById(R.id.btnAccept);
                    alertBuilder.setView(dialogView);
                    AlertDialog dialog = alertBuilder.create();
                    dialog.show();
                    dialog.setCancelable(false);
                    dialog.setCanceledOnTouchOutside(false);
                    btnAccept.setOnClickListener(v -> {
                        if (etUsername.getText().toString().isEmpty()) {
                            etUsername.setError("Required");
                        } else {
                            // Try to create an entry in Usernames collection with username as the document and uid as key and value
                            Map<String, String> uid = new HashMap<>();
                            uid.put("uid", currentUser.getUid());
                            database.collection("Usernames").document(etUsername.getText().toString()).set(uid)
                                    // If username is unique, hide the dialog and greet user
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Created username");
                                        dialog.hide();
                                        userData.update("username", etUsername.getText().toString())
                                                .addOnSuccessListener(aVoid2 -> Log.d(TAG, "Updated username"))
                                                .addOnFailureListener(e -> Log.d(TAG, "Error updating Users document", e));
                                        Toast.makeText(getApplicationContext(), "Welcome " + etUsername.getText().toString(), Toast.LENGTH_SHORT).show();
                                    })
                                    // If username is in use, warn the user
                                    .addOnFailureListener(e -> etUsername.setError("Username already in use"));
                            Log.d(TAG, "Calling getInformation");
                            getInformation();
                        }
                    });
                } else {
                    // If user has a username, check if they have today's map
                    Log.d(TAG, "Calling getInformation");
                    getInformation();
                }
            } else {
                Log.d(TAG, "Get failed with " + task.getException());
            }
        });
    }

    private void getInformation() {
        // Create string with today's date for comparing and downloading maps
        SimpleDateFormat dtf = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
        Date todayDate = Calendar.getInstance().getTime();
        today = dtf.format(todayDate);
        // Check user's Map collection for information on when map was last downloaded (on firebase)
        userData.collection("Map").document("LastDownload").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getData() != null) {
                // If a map was never downloaded, or wasn't downloaded today,
                // get today's map and remove old coins, otherwise do nothing
                if (!task.getResult().getData().containsKey("date") || !(task.getResult().getData().get("date").equals(today))) {
                    Log.d(TAG, "Updating map");
                    // Reset today's bank in allowance to 25 coins
                    userData.update("bankLimit", 25);
                    // Remove yesterday's coins
                    removeOldCoinsFromWallet();
                    // DownloadTodayMap() called after old coins are removed
                    removeOldCoinsFromMap();
                }
                // If user doesn't have a Map collection, create one and add today's map
            } else {
                Log.d(TAG, "Couldn't check LastDownload date");
                Map<String, String> lastDownload = new HashMap<>();
                lastDownload.put("date", today);
                userData.collection("Map").document("LastDownload").set(lastDownload);
                downloadTodayMap();
            }
        });
    }

    private void removeOldCoinsFromMap() {
        // Get user's Map collection of documents and remove them all and put today's date
        userData.collection("Map")
                .whereGreaterThanOrEqualTo("value", 0)
                .get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Log.d(TAG, "Removed old coins from map");
                for (QueryDocumentSnapshot document : task.getResult()) {
                    document.getReference().delete();
                }
                Map<String, String> lastDownload = new HashMap<>();
                lastDownload.put("date", today);
                userData.collection("Map").document("LastDownload").set(lastDownload);
                downloadTodayMap();
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    private void removeOldCoinsFromWallet() {
        // Get user's Wallet collection of documents and remove them all
        userData.collection("Wallet")
                .whereGreaterThanOrEqualTo("value", 0)
                .get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Log.d(TAG, "Removed old coins from wallet");
                for (QueryDocumentSnapshot document : task.getResult()) {
                    document.getReference().delete();
                }
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    private void downloadTodayMap() {
        // Start an Async download task
        DownloadFileTask task = new DownloadFileTask();
        task.delegate = this;
        task.execute("http://homepages.inf.ed.ac.uk/stg/coinz/" + today + "/coinzmap.geojson");
    }

    // Async DownloadFileTask callback
    @Override
    // File is guaranteed to have all the necessary fields
    @SuppressWarnings("ConstantConditions")
    public void processFinish(String s) {
        // Extract Feature collection from today's geoJson
        FeatureCollection fc = FeatureCollection.fromJson(s);
        if (fc.features() != null) {
            // For each coin (feature), add a document with the coin id as document name and Coin object
            for (Feature f : fc.features()) {
                Coin coin = new Coin(f.properties().get("id").getAsString(),
                        f.properties().get("value").getAsDouble(),
                        f.properties().get("currency").getAsString(),
                        ((Point) f.geometry()).longitude(),
                        ((Point) f.geometry()).latitude());
                userData.collection("Map").document(coin.getId()).set(coin);
            }
        }
        try {
            // Set exchange rates in user's information
            JSONObject geoJson = new JSONObject(s);
            JSONObject rates = geoJson.getJSONObject("rates");
            Map<String, String> exchangeRates = new HashMap<>();
            exchangeRates.put("DOLR", rates.get("DOLR").toString());
            exchangeRates.put("QUID", rates.get("QUID").toString());
            exchangeRates.put("PENY", rates.get("PENY").toString());
            exchangeRates.put("SHIL", rates.get("SHIL").toString());
            userData.set(exchangeRates, SetOptions.merge());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if user is still logged in
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

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
                if (task.isSuccessful() && task.getResult() != null && task.getResult().getData() != null) {
                    Map userInfo = task.getResult().getData();
                    item.getSubMenu().findItem(R.id.dolrRate).setTitle("DOLR = " + df.format(Double.parseDouble(userInfo.get("DOLR").toString())) + " GOLD");
                    item.getSubMenu().findItem(R.id.quidRate).setTitle("QUID = " + df.format(Double.parseDouble(userInfo.get("QUID").toString())) + " GOLD");
                    item.getSubMenu().findItem(R.id.penyRate).setTitle("PENY = " + df.format(Double.parseDouble(userInfo.get("PENY").toString())) + " GOLD");
                    item.getSubMenu().findItem(R.id.shilRate).setTitle("SHIL = " + df.format(Double.parseDouble(userInfo.get("SHIL").toString())) + " GOLD");
                } else {
                    Log.d(TAG, "Get failed with " + task.getException());
                }
            });
            return true;
        }
        // Show user's gold and bank in allowance
        if (id == R.id.goldBag) {
            userData.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().getData() != null) {
                    Map userInfo = task.getResult().getData();
                    item.getSubMenu().findItem(R.id.gold).setTitle(userInfo.get("GOLD").toString() + " GOLD");
                    item.getSubMenu().findItem(R.id.bankAllowance).setTitle(userInfo.get("bankLimit").toString() + "/25 bank-in limit");
                } else {
                    Log.d(TAG, "Get failed with " + task.getException());
                }
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

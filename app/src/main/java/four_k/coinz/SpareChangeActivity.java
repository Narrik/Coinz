package four_k.coinz;

import android.icu.text.DecimalFormat;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpareChangeActivity extends AppCompatActivity {

    private static final String TAG = "SpareChangeActivity";
    private FirebaseFirestore database;
    private DocumentReference userData;
    private CoinAdapter adapter;
    private Coin sentCoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spare_change);
        // Display activity name and back arrow on toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Bank");
        // Get current user
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // If there is no user, don't continue
        if (currentUser == null) {
            Log.d(TAG, "Cannot load spare change if user is not logged in");
            finish();
        }
        // Access our database
        database = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        database.setFirestoreSettings(settings);
        // Get current user information
        userData = database.collection("Users").document(currentUser.getUid());
        // Construct the data source for listView
        ArrayList<Coin> coinsInWallet = new ArrayList<>();
        userData.collection("Wallet")
                .orderBy("value", Query.Direction.DESCENDING)
                .whereGreaterThanOrEqualTo("value", 0)
                .get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().isEmpty()) {
                    // If user has no coins, ask him to collect some first
                    Toast.makeText(getApplicationContext(), "Try collecting some coins on the map first!", Toast.LENGTH_LONG).show();
                }
                for (QueryDocumentSnapshot document : task.getResult()) {
                    coinsInWallet.add(document.toObject(Coin.class));
                    Log.d(TAG, "Adding a coin");
                }
                // Create the adapter to convert the array to views
                adapter = new CoinAdapter(this, coinsInWallet);
                // Attach the adapter to a ListView
                ListView listView = findViewById(R.id.list_view);
                listView.setAdapter(adapter);
                listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
            }
        });
        EditText receiver = findViewById(R.id.etReceiver);
        FloatingActionButton sendSpareChange = findViewById(R.id.sendSpareChange);
        sendSpareChange.setOnClickListener(v -> {
            // Check user wants to send at least 1 coin
            if (adapter.getSelectedCoins().isEmpty()) {
                Toast.makeText(this, "Select at least 1 Coin", Toast.LENGTH_SHORT).show();
            } else {
                // Send coin to user whose username is written in the editText field
                for (String coinId : adapter.getSelectedCoins()) {
                    sendCoin(receiver.getText().toString(), coinId);
                }
            }
        });
    }

    private void sendCoin(String receiver, String coinId) {
        // Get all information about the coin we are sending
        database.collection("Users")
                .whereEqualTo("username", receiver)
                .get().addOnCompleteListener(task -> {
            // Check if user exists
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                // Get information about the coin we are sending
                userData.collection("Wallet").document(coinId).get().addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful() && task2.getResult() != null) {
                                Log.d(TAG, "Retrieving coin information");
                                sentCoin = task2.getResult().toObject(Coin.class);
                                // Remove the coin from user's wallet
                                userData.collection("Wallet").document(coinId).delete();
                                // Send coin to receiver
                                Log.d(TAG, "Sending coins to " + receiver);
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    document.getReference().collection("Wallet").document(coinId).set(sentCoin);
                                }
                                // After sending coin return to chat
                                Toast.makeText(this, "Spare change sent!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                );
            } else {
                Toast.makeText(this, "No user with such username exists", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        DecimalFormat df = new DecimalFormat("0.00");
        int id = item.getItemId();
        if (id == R.id.rates) {
            userData.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Map exchangeRates = task.getResult().getData();
                    item.getSubMenu().findItem(R.id.dolrRate).setTitle("DOLR = " + df.format(Double.parseDouble(exchangeRates.get("DOLR").toString())) + " GOLD");
                    item.getSubMenu().findItem(R.id.quidRate).setTitle("QUID = " + df.format(Double.parseDouble(exchangeRates.get("QUID").toString())) + " GOLD");
                    item.getSubMenu().findItem(R.id.penyRate).setTitle("PENY = " + df.format(Double.parseDouble(exchangeRates.get("PENY").toString())) + " GOLD");
                    item.getSubMenu().findItem(R.id.shilRate).setTitle("SHIL = " + df.format(Double.parseDouble(exchangeRates.get("SHIL").toString())) + " GOLD");
                } else {
                    Log.d(TAG, "Get failed with " + task.getException());
                }
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

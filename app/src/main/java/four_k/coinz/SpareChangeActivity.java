package four_k.coinz;

import android.icu.text.DecimalFormat;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Map;

public class SpareChangeActivity extends AppCompatActivity {

    private static final String TAG = "SpareChangeActivity";
    private FirebaseFirestore database;
    private DocumentReference userData;
    private CoinAdapter adapter;
    private EditText etReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spare_change);
        // Display activity name and back arrow on toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Send Spare Change");
        }
        // Get current user
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // If there is no user, don't continue
        if (currentUser == null) {
            Log.d(TAG, "Cannot load spare change if user is not logged in");
            finish();
        } else {
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
            etReceiver = findViewById(R.id.etReceiver);
            FloatingActionButton sendSpareChange = findViewById(R.id.sendSpareChange);
            sendSpareChange.setOnClickListener(v -> {
                if (MisclickPreventer.cantClickAgain()) {
                    return;
                }
                // Attempt to send gold to another user
                sendSpareChangeGold(etReceiver.getText().toString());
            });

        }
    }

    private void sendSpareChangeGold(String receiver) {
        if (etReceiver.getText().toString().equals("")) {
            etReceiver.setError("Username cannot be empty");
            return;
        }
        // Check user wants to send at least 1 coin
        if (adapter.getSelectedCoins().isEmpty()) {
            Toast.makeText(this, "Select at least 1 Coin", Toast.LENGTH_SHORT).show();
            return;
        }
        userData.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getData() != null) {
                    // Check user isn't sending gold to himself
                if (task.getResult().getData().get("username").toString().equals(receiver)) {
                    Toast.makeText(this, "Cannot send spare change to yourself!", Toast.LENGTH_SHORT).show();
                    // Check user has already banked-in all of his coin allowance
                } else if (Integer.parseInt(task.getResult().getData().get("bankLimit").toString()) > 0){
                    Toast.makeText(this, "Cannot send spare change before you've used up your daily bank-in limit," +
                            " it's SPARE change after all!", Toast.LENGTH_LONG).show();
                }else {
                    // Remove every coin from user's wallet
                    for (String coinId : adapter.getSelectedCoins()) {
                        userData.collection("Wallet").document(coinId).delete();
                    }
                    // Round up the gold value
                    int roundedUpGold = new Double(adapter.getSelectedCoinsGoldValue() + 0.5d).intValue();
                    // Get receiver's current gold and increase it
                    database.collection("Users")
                            .whereEqualTo("username", receiver)
                            .get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful() && task1.getResult() != null && !task1.getResult().isEmpty()) {
                            Log.d(TAG, "Sending gold to " + receiver);
                            // Because usernames are unique there is only 1 receiver
                            for (QueryDocumentSnapshot document : task1.getResult()) {
                                document.getReference().get().addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful() && task2.getResult() != null && task2.getResult().getData() != null) {
                                        Log.d(TAG, "Increasing " + receiver + " gold");
                                        int currentGold = Integer.parseInt(task2.getResult().getData().get("GOLD").toString());
                                        document.getReference().update("GOLD", currentGold + roundedUpGold);
                                        // After sending coins return to main menu
                                        Toast.makeText(this, "Gold sent!", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                });
                            }
                        } else {
                            // No user found
                            Toast.makeText(this, "No user with such username exists", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
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

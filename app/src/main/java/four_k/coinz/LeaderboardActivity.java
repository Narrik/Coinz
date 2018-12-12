package four_k.coinz;

import android.icu.text.DecimalFormat;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

public class LeaderboardActivity extends AppCompatActivity {

    private static final String TAG = "LeaderboardActivity";
    private DocumentReference userData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        // Display activity name and back arrow on toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Leaderboard");
        }
        // Get current user
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // If there is no user, don't continue
        if (currentUser == null) {
            Log.d(TAG, "Cannot load leaderboard if user is not logged in");
            finish();
        } else {
            // Access our database
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setTimestampsInSnapshotsEnabled(true)
                    .build();
            database.setFirestoreSettings(settings);
            // Get current user information
            userData = database.collection("Users").document(currentUser.getUid());
            // Construct the data source for listView
            // We are using Message object here as it contains 2 fields for strings, perfect for username and GOLD
            ArrayList<Message> userGolds = new ArrayList<>();
            // Create the adapter to convert the array to views
            LeaderboardMessageAdapter adapter = new LeaderboardMessageAdapter(this, userGolds);
            // Attach the adapter to a ListView
            ListView listView = findViewById(R.id.list_view);
            listView.setAdapter(adapter);
            // Show leaderboard placing
            database.collection("Users")
                    .whereGreaterThan("GOLD", 0)
                    .orderBy("GOLD", Query.Direction.DESCENDING)
                    .get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    for (DocumentSnapshot document : task.getResult().getDocuments()) {
                        Map userData = document.getData();
                        if (userData != null) {
                            adapter.add(new Message(userData.get("username").toString(), userData.get("GOLD").toString() + " GOLD"));
                            Log.d(TAG, "Added a user to the leaderboard");
                        }
                    }
                }
            });
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

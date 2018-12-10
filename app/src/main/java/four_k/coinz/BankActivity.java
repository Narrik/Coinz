package four_k.coinz;

import android.app.ActionBar;
import android.icu.text.DecimalFormat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
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

public class BankActivity extends AppCompatActivity {

    private static final String TAG = "BankActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore database;
    private DocumentReference userData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank);
        // Display activity name and back arrow on toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Bank");
        // Construct the data source
        ArrayList<Coin> coinsInWallet = new ArrayList<>();
        // Get current user
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // If there is no user, don't continue
        if (currentUser == null){
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
        userData.collection("Wallet")
                .orderBy("value", Query.Direction.DESCENDING)
                .whereGreaterThanOrEqualTo("value",0)
                .get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().isEmpty()){
                    Toast.makeText(getApplicationContext(), "Try collecting some coins on the map first!", Toast.LENGTH_LONG).show();
                }
                for (QueryDocumentSnapshot document : task.getResult()) {
                    coinsInWallet.add(document.toObject(Coin.class));
                    Log.d(TAG,"Adding a coin");
                }
                // Create the adapter to convert the array to views
                CoinAdapter adapter = new CoinAdapter(this, coinsInWallet);
                // Attach the adapter to a ListView
                ListView listView = findViewById(R.id.list_view);
                listView.setAdapter(adapter);
                listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
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
        if (id == R.id.rates) {
            userData.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null){
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

        return super.onOptionsItemSelected(item);
    }
}

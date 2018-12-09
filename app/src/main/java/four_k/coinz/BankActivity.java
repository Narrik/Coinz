package four_k.coinz;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class BankActivity extends AppCompatActivity {

    private static final String TAG = "BankActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank);
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
        DocumentReference userData = database.collection("Users").document(currentUser.getUid());
        userData.collection("Wallet")
                .orderBy("value", Query.Direction.DESCENDING)
                .whereGreaterThanOrEqualTo("value",0)
                .get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
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
}

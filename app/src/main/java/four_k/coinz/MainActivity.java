package four_k.coinz;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Open login screen (doesn't open if user is logged in)
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        // Button for turning on Map
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((View view) -> {
            Intent mapIntent = new Intent(MainActivity.this, MapboxActivity.class);
            startActivity(mapIntent);
        });
        // Button for turning on chat
        Button chatButton = findViewById(R.id.chatButton);
        chatButton.setOnClickListener((View view) -> {
            Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(chatIntent);
        });
        // Button for accessing bank
        Button bankButton = findViewById(R.id.bankButton);
        bankButton.setOnClickListener((View view) -> {
            Intent bankIntent = new Intent(MainActivity.this, BankActivity.class);
            startActivity(bankIntent);
        });
        // Button for signing out
        Button signOutButton = findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener((View view) -> {
            FirebaseAuth mAuth;
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null){
                mAuth.signOut();
                Toast.makeText(this, user.getEmail()+ " Signed out!", Toast.LENGTH_SHORT).show();
                startActivity(loginIntent);
            } else {
                Toast.makeText(this, "You aren't logged in!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onStart(){
        super.onStart();
        createUsername();
    }


    public void createUsername(){
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
        userData.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()){
                // If user has not set a name yet, ask them to create one with an uncancellable alert dialog
                if (task.getResult().getData().get("username") == null){
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_username,null);
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
                            Map<String,String> uid = new HashMap<>();
                            uid.put("uid",currentUser.getUid());
                            database.collection("Usernames").document(etUsername.getText().toString()).set(uid)
                                    // If username is unique, hide the dialog and greet user
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Created username");
                                        userData.update("username", etUsername.getText().toString())
                                                .addOnSuccessListener(aVoid2 -> Log.d(TAG, "Updated username"))
                                                .addOnFailureListener(e -> Log.d(TAG, "Error updating Users document", e));
                                        dialog.hide();
                                        Toast.makeText(getApplicationContext(), "Welcome "+etUsername.getText().toString(), Toast.LENGTH_SHORT).show();
                                    })
                                    // If username is in use, warn the user
                                    .addOnFailureListener(e -> etUsername.setError("Username already in use"));
                        }
                    });
                }
            } else {
                Log.d(TAG, "Get failed with "+task.getException());
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if user is still logged in
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }
    }
}

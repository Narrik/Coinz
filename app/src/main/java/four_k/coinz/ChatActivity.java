package four_k.coinz;

import android.icu.text.DecimalFormat;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final String COLLECTION_KEY= "Chat";
    private static final String DOCUMENT_KEY = "Message";
    private static final String NAME_FIELD = "Name";
    private static final String TEXT_FIELD = "Text";
    private FirebaseAuth mAuth;
    private FirebaseFirestore database;
    private DocumentReference firestoreChat;
    private DocumentReference userData;
    private EditText nameText;
    private EditText messageText;
    private TextView messageLast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        // Display activity name and back arrow on toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Chat");
        // Get the user
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
        // Message
        nameText = findViewById(R.id.nameText);
        messageText = findViewById(R.id.messageText);
        messageLast = findViewById(R.id.messageLast);
        FloatingActionButton fab = findViewById(R.id.floatingActionButton2);
        fab.setOnClickListener(view -> sendMessage());
        firestoreChat = database.collection(COLLECTION_KEY).document(DOCUMENT_KEY);
        realtimeUpdateListener();
    }

    private void sendMessage(){
        Map<String,Object> newMessage = new HashMap<>();
        newMessage.put(NAME_FIELD, nameText.getText().toString());
        newMessage.put(TEXT_FIELD, messageText.getText().toString());
        newMessage.put("created", FieldValue.serverTimestamp());
        database.collection(COLLECTION_KEY).add(newMessage);
        /*firestoreChat.set(newMessage)
                .addOnSuccessListener(v ->
                        Toast.makeText(getApplicationContext(),"Message sent",Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {

                    Log.d(TAG,e.getMessage());
                    Toast.makeText(getApplicationContext(),"Message not sent",Toast.LENGTH_SHORT).show();
                });*/
    }

    private void realtimeUpdateListener() {
        database.collection(COLLECTION_KEY)
                .orderBy("created", Query.Direction.ASCENDING)
                .addSnapshotListener(((queryDocumentSnapshots, e) -> {
                    if (e != null ) {
                        Log.e(TAG,e.getMessage());
                    } else if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot documentSnapshot: queryDocumentSnapshots.getDocuments()) {
                            String incoming = (documentSnapshot.getData().get(NAME_FIELD))
                                    +": "+ (documentSnapshot.getData().get(TEXT_FIELD));
                            messageLast.setText(incoming);
                        }
                    }
                }));
        /*firestoreChat.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.e(TAG,e.getMessage());
            } else if (documentSnapshot != null && documentSnapshot.exists()) {
                String incoming = (documentSnapshot.getData().get(NAME_FIELD))
                        +": "+ (documentSnapshot.getData().get(TEXT_FIELD));
                messageLast.setText(incoming);
            }
        });*/
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

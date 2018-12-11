package four_k.coinz;

import android.content.Intent;
import android.icu.text.DecimalFormat;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore database;
    private DocumentReference userData;
    private EditText messageText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        // Display activity name and back arrow on toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Chat");
        // Get current user
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        // If there is no user, don't continue
        if (currentUser == null){
            Log.d(TAG,"Cannot load chat if user is not logged in");
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
        ArrayList<Message> previousMessages = new ArrayList<>();
        // Create the adapter to convert the array to views
        MessageAdapter adapter = new MessageAdapter(this, previousMessages);
        // Attach the adapter to a ListView
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(adapter);
        // Show previous messages
        database.collection("Chat")
                .orderBy("created", Query.Direction.ASCENDING)
                .addSnapshotListener(((queryDocumentSnapshots, e) -> {
                    if (e != null ) {
                        Log.e(TAG,e.getMessage());
                    } else if (queryDocumentSnapshots != null) {
                        for (DocumentChange dc: queryDocumentSnapshots.getDocumentChanges()) {
                            if (dc.getType().equals(DocumentChange.Type.ADDED)) {
                                Map messageData = dc.getDocument().getData();
                                adapter.add(new Message(messageData.get("sender").toString(), messageData.get("messageText").toString()));
                                Log.d(TAG, "Added a message");
                            }
                        }
                    }
                }));
        // Attach editText so user can type message text
        messageText = findViewById(R.id.messageText);
        // Create button for users to send messages
        FloatingActionButton fabSendMessage = findViewById(R.id.sendMessage);
        fabSendMessage.setOnClickListener(v -> sendMessage());
        Button btnSpareChange = findViewById(R.id.btnSpareChange);
        btnSpareChange.setOnClickListener(v -> startActivity(new Intent(ChatActivity.this,SpareChangeActivity.class)));
    }

    private void sendMessage(){
        // Only allow sending of message if text is not empty
        if (messageText.getText().toString().equals("")) {
            messageText.setError("Cannot send an empty message");
            return;
        }
        Map<String,Object> newMessage = new HashMap<>();
        // Add the message and empty the text field
        newMessage.put("messageText", messageText.getText().toString());
        messageText.setText("");
        userData.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null){
                Map userInfo = task.getResult().getData();
                // Fill in message information as sender, messageText and time of creation
                newMessage.put("sender",userInfo.get("username").toString());
                newMessage.put("created", FieldValue.serverTimestamp());
                database.collection("Chat").add(newMessage)
                        .addOnSuccessListener(documentReference ->{
                                Toast.makeText(getApplicationContext(),"Message sent!",Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.d(TAG,e.getMessage());
                            Toast.makeText(getApplicationContext(),"Message not sent!",Toast.LENGTH_SHORT).show();
                        });
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

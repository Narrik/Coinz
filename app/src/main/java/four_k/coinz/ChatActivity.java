package four_k.coinz;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
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
    private DocumentReference firestoreChat;
    private FirebaseFirestore database;
    private EditText nameText;
    private EditText messageText;
    private TextView messageLast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        database = FirebaseFirestore.getInstance();
        nameText = findViewById(R.id.nameText);
        messageText = findViewById(R.id.messageText);
        messageLast = findViewById(R.id.messageLast);
        FloatingActionButton fab = findViewById(R.id.floatingActionButton2);
        fab.setOnClickListener(view -> sendMessage());
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        database.setFirestoreSettings(settings);
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
}

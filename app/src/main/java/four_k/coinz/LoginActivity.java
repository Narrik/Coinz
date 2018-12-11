package four_k.coinz;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private EditText emailField;
    private EditText passwordField;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        database = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        database.setFirestoreSettings(settings);
        mAuth = FirebaseAuth.getInstance();
        // Text fields
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        // Buttons
        findViewById(R.id.loginButton).setOnClickListener(this);
        findViewById(R.id.registerButton).setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // If user is signed in, return to main menu
        if (currentUser != null) {
            finish();
        }
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.registerButton) {
            createAccount(emailField.getText().toString(), passwordField.getText().toString());
        } else if (i == R.id.loginButton) {
            signIn(emailField.getText().toString(), passwordField.getText().toString());
        }
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, "createAccount:" + email);
        if (invalidForm()) {
            return;
        }
        // Create a user with valid email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        Log.d(TAG, "createUserWithEmail:success");
                        Map<String,Object> userInfo = new HashMap<>();
                        userInfo.put("email",currentUser.getEmail());
                        userInfo.put("username","");
                        userInfo.put("GOLD",0);
                        userInfo.put("bankLimit",25);
                        database.collection("Users").document(currentUser.getUid()).set(userInfo);
                        finish();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.d(TAG, "createUserWithEmail:failure", task.getException());
                        if (password.length() < 6) {
                            Toast.makeText(this, "Password must be at least 6 characters.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Email address not valid or already in use.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);
        if (invalidForm()) {
            return;
        }
        // Sign in a user with existing email/password combination
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success");
                        finish();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.d(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(this, "Unknown email/password combination.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private boolean invalidForm() {
        boolean invalid = false;
        // Check if email field is not empty
        String email = emailField.getText().toString();
        if (TextUtils.isEmpty(email)) {
            emailField.setError("Required.");
            invalid = true;
        } else {
            emailField.setError(null);
        }
        // Check if password field is not empty
        String password = passwordField.getText().toString();
        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Required.");
            invalid = true;
        } else {
            passwordField.setError(null);
        }
        return invalid;
    }
}

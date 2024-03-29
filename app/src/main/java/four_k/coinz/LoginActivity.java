package four_k.coinz;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

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
        Button btnLogin = findViewById(R.id.loginButton);
        btnLogin.setOnClickListener(v -> {
            // Prevents user from spam clicking
            if (MisclickPreventer.cantClickAgain()) {
                return;
            }
            signIn(emailField.getText().toString(), passwordField.getText().toString());
        });
        Button btnRegister = findViewById(R.id.registerButton);
        btnRegister.setOnClickListener(v -> {
            // Prevents user from spam clicking
            if (MisclickPreventer.cantClickAgain()) {
                return;
            }
            createAccount(emailField.getText().toString(), passwordField.getText().toString());
        });
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
                        if (currentUser != null) {
                            Log.d(TAG, "createUserWithEmail:success");
                            Map<String, Object> userInfo = new HashMap<>();
                            userInfo.put("email", currentUser.getEmail());
                            userInfo.put("username", "");
                            userInfo.put("GOLD", 0);
                            userInfo.put("bankLimit", 25);
                            database.collection("Users").document(currentUser.getUid()).set(userInfo);
                        }
                        finish();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.d(TAG, "createUserWithEmail:failure", task.getException());
                        if (task.getException() != null) {
                            Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
                        if (task.getException() != null) {
                            Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
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

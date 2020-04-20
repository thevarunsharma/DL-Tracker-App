package com.example.dltracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SignupActivity extends AppCompatActivity {

    EditText firstNameInput, lastNameInput, newUsernameInput, passInput, confPassInput;
    ProgressBar signupProgressBar;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference usersRef = db.collection("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        firstNameInput = (EditText) findViewById(R.id.firstNameInput);
        lastNameInput = (EditText) findViewById(R.id.lastNameInput);
        newUsernameInput = (EditText) findViewById(R.id.newUsernameInput);
        passInput = (EditText) findViewById(R.id.passInput);
        confPassInput = (EditText) findViewById(R.id.confPassInput);
        signupProgressBar = (ProgressBar) findViewById(R.id.signupProgressBar);
    }

    public void signupUser(View v) {
        final String firstName = firstNameInput.getText().toString();
        final String lastName = lastNameInput.getText().toString();
        final String username = newUsernameInput.getText().toString();
        final String password = passInput.getText().toString();
        String confPass = confPassInput.getText().toString();

        if (! Pattern.matches("[a-z][a-z0-9_]*", username) || username.length() < 5){
            newUsernameInput.setError(
                    "Username should start with a lowercase letter or underscore "+
                    "and must contain only lowercase letters, digits or an underscore with minimum length of 5");
            newUsernameInput.requestFocus();
            return;
        }

        if (!password.equals(confPass)) {
            confPassInput.setError("Confirm password doesn't match");
            confPassInput.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passInput.setError("Weak Password");
            passInput.requestFocus();
            return;
        }

        final Map<String, String> info = new HashMap<String, String>();
        info.put("firstName", firstName);
        info.put("lastName", lastName);
        info.put("password", password);

        final DocumentReference docRef = usersRef.document(username);

        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot.exists()){
                    newUsernameInput.setError("Username not available");
                    newUsernameInput.requestFocus();
                    return;
                }
                signupProgressBar.setVisibility(View.VISIBLE);
                docRef.set(info).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        SharedPreferences sharedPref = getSharedPreferences("userinfo", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("username", username);
                        editor.putString("password", password);
                        editor.putString("firstName", firstName);
                        editor.putString("lastName", lastName);
                        editor.apply();
                        SignupActivity.this.finish();
                    }
                });
            }
        });
    }
}

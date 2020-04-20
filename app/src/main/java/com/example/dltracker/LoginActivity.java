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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class LoginActivity extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference usersRef = db.collection("users");
    EditText usernameInput, passwordInput;
    ProgressBar loginProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        usernameInput = (EditText) findViewById(R.id.usernameInput);
        passwordInput = (EditText) findViewById(R.id.passwordInput);
        loginProgressBar = (ProgressBar) findViewById(R.id.loginProgressBar);
    }

    public void loginValidate(View v) {
        final String username = usernameInput.getText().toString().trim();
        final String password = passwordInput.getText().toString();

        if (username.length() == 0){
            usernameInput.setError("No username provided");
            usernameInput.requestFocus();
            return;
        }
        if (password.length() == 0){
            passwordInput.setError("No password provided");
            passwordInput.requestFocus();
            return;
        }

        usersRef.document(username).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (! documentSnapshot.exists()){
                    usernameInput.setError("Invalid Username");
                    usernameInput.requestFocus();
                }
                else if (password.equals(documentSnapshot.getString("password"))){
                    loginProgressBar.setVisibility(View.VISIBLE);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    String firstname = documentSnapshot.getString("firstName");
                    String lastname = documentSnapshot.getString("lastName");
                    SharedPreferences sharedPref = getSharedPreferences("userinfo", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("username", username);
                    editor.putString("password", password);
                    editor.putString("firstName", firstname);
                    editor.putString("lastName", lastname);
                    editor.apply();
                    LoginActivity.this.finish();
                }else{
                    passwordInput.setError("Invalid Password");
                    passwordInput.requestFocus();
                }
            }
        });

    }

    public void openSignup(View v) {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }
}

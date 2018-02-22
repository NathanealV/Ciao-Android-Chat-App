package com.labs.morb.ciao;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegister;
    private TextView link_login;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();



        etName = findViewById(R.id.input_name);
        etUsername = findViewById(R.id.input_username);
        etEmail = findViewById(R.id.input_email);
        etPassword = findViewById(R.id.input_password);

        btnRegister = findViewById(R.id.btn_signup);
        link_login = findViewById(R.id.link_login);


        link_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                RegisterActivity.this.startActivity(intent);
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSignUp();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // mAuth.addAuthStateListener(mAuthListener);
    }

    private void startSignUp() {
        final ProgressDialog mProgressDialog = new ProgressDialog(RegisterActivity.this, R.style.AppTheme_Dark_Dialog);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage("Registering...");
        mProgressDialog.show();

        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();
        final String display_name = etName.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            mProgressDialog.dismiss();
            Toast.makeText(RegisterActivity.this, "One or more fields are empty.", Toast.LENGTH_LONG).show();
        } else {
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    mProgressDialog.dismiss();
                    if (!task.isSuccessful()) {
                        FirebaseAuthException e = (FirebaseAuthException)task.getException();
                        Log.e("LoginActivity", "Failed Registration", e);
                        Toast.makeText(RegisterActivity.this, "Sign Up Failed. Please try again soon.", Toast.LENGTH_LONG).show();
                    } else {

                        FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();
                        String uid = current_user.getUid();
                        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

                        HashMap<String, String> userMap = new HashMap<>();
                        userMap.put("name", display_name);
                        userMap.put("status", "Hey there, I'm using Ciao!");
                        userMap.put("image", "default");
                        userMap.put("thumb_image", "default");
                        userMap.put("device_token", FirebaseInstanceId.getInstance().getToken());

                        mDatabase.setValue(userMap);

                        mProgressDialog.dismiss();
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });
        }
    }

}

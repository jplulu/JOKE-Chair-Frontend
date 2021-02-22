package com.example.jokechair;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "LoginActivity";

    private Button bLogin;
    private EditText etUsername, etPassword;
    private TextView tvRegisterLink, tvWarnUsername, tvWarnPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = (EditText) findViewById(R.id.etUsername);
        etPassword = (EditText) findViewById(R.id.etPassword);
        bLogin = (Button) findViewById(R.id.bLogin);
        tvRegisterLink = (TextView) findViewById(R.id.tvRegisterLink);
        tvWarnUsername = (TextView) findViewById(R.id.tvWarnUsername);
        tvWarnPassword = (TextView) findViewById(R.id.tvWarnPassword);

        bLogin.setOnClickListener(this);
        tvRegisterLink.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.bLogin:
                processLogin();
                break;

            case R.id.tvRegisterLink:
                startActivity(new Intent(this, RegisterActivity.class));
                break;
        }
    }

    private void processLogin() {
        if (validateData()) {
            Log.d(TAG, etUsername.getText().toString() + " " + etPassword.getText().toString());
        }
    }

    private boolean validateData() {
        boolean valid = true;

        tvWarnUsername.setVisibility(View.GONE);
        tvWarnPassword.setVisibility(View.GONE);

        if (etUsername.getText().toString().equals("")) {
            tvWarnUsername.setVisibility(View.VISIBLE);
            tvWarnUsername.setText("Field must not be empty");
            valid = false;
        }

        if (etPassword.getText().toString().equals("")) {
            tvWarnPassword.setVisibility(View.VISIBLE);
            tvWarnPassword.setText("Field must not be empty");
            valid = false;
        }

        return valid;
    }

}
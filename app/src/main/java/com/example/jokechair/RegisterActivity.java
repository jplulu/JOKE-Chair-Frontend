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

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "RegisterActivity";

    UserLocalStore userLocalStore;
    private Button bRegister;
    private EditText etName, etUsername, etPassword, etPasswordRepeat;
    private TextView tvLoginLink, tvWarnName, tvWarnUsername, tvWarnPassword, tvWarnPassRepeat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = (EditText) findViewById(R.id.etName);
        etUsername = (EditText) findViewById(R.id.etUsername);
        etPassword = (EditText) findViewById(R.id.etPassword);
        etPasswordRepeat = (EditText) findViewById(R.id.etPasswordRepeat);
        bRegister = (Button) findViewById(R.id.bRegister);
        tvLoginLink = (TextView) findViewById(R.id.tvLoginLink);
        tvWarnName = (TextView) findViewById(R.id.tvWarnName);
        tvWarnUsername = (TextView) findViewById(R.id.tvWarnUsername);
        tvWarnPassword = (TextView) findViewById(R.id.tvWarnPassword);
        tvWarnPassRepeat = (TextView) findViewById(R.id.tvWarnPassRepeat);

        bRegister.setOnClickListener(this);
        tvLoginLink.setOnClickListener(this);

        userLocalStore = new UserLocalStore(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.bRegister:
                processRegister();
                break;

            case R.id.tvLoginLink:
                startActivity(new Intent(this, LoginActivity.class));
                break;
        }
    }

    // TODO: Added new user to database in the server
    private void processRegister() {
        if (validateData()) {
            User registeredUser = new User(etName.getText().toString(), etUsername.getText().toString(), etPassword.getText().toString());
            userLocalStore.storeUserData(registeredUser);
            userLocalStore.setUserLoggedIn(true);

            Log.d(TAG, etName.getText().toString() + " " + etUsername.getText().toString() + " " + etPassword.getText().toString());

            startActivity(new Intent(this, MainActivity.class));
        }
    }

    private boolean validateData() {
        boolean valid = true;

        tvWarnName.setVisibility(View.GONE);
        tvWarnUsername.setVisibility(View.GONE);
        tvWarnPassword.setVisibility(View.GONE);
        tvWarnPassRepeat.setVisibility(View.GONE);

        if (etName.getText().toString().equals("")) {
            tvWarnName.setVisibility(View.VISIBLE);
            tvWarnName.setText("Field must not be empty");
            valid = false;
        }

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

        if (!etPassword.getText().toString().equals(etPasswordRepeat.getText().toString())) {
            tvWarnPassRepeat.setVisibility(View.VISIBLE);
            tvWarnPassRepeat.setText("Passwords does not match");
            valid = false;
        }

        return valid;
    }
}
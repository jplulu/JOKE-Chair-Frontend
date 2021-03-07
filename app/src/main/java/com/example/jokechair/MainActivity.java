package com.example.jokechair;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private UserLocalStore userLocalStore;
    private Button bLogout, bPosturePage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bPosturePage = (Button) findViewById(R.id.bPosturePage);
        bLogout = (Button) findViewById(R.id.bLogout);

        bLogout.setOnClickListener(this);
        bPosturePage.setOnClickListener(this);

        userLocalStore = new UserLocalStore(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        User loggedInUser = userLocalStore.getLoggedInUser();
        if (loggedInUser != null) {
            Toast.makeText(this, "Logged in as " + loggedInUser.getName(), Toast.LENGTH_LONG).show();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.bLogout:
                userLocalStore.clearUserData();
                userLocalStore.setUserLoggedIn(false);
                startActivity(new Intent(this, LoginActivity.class));
                break;

            case R.id.bPosturePage:
                Log.e(TAG, "PRESSED POSTURE");
                startActivity(new Intent(this, PostureActivity.class));
                break;
        }
    }
}
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
    private Button bLogout, bPosturePage, bCalibrationPage, bHistoryPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bPosturePage = (Button) findViewById(R.id.bPosturePage);
        bCalibrationPage = (Button) findViewById(R.id.bCalibrationPage);
        bLogout = (Button) findViewById(R.id.bLogout);
        bHistoryPage = (Button) findViewById(R.id.bHistoryPage);

        bLogout.setOnClickListener(this);
        bPosturePage.setOnClickListener(this);
        bCalibrationPage.setOnClickListener(this);
        bHistoryPage.setOnClickListener(this);

        userLocalStore = new UserLocalStore(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        User loggedInUser = userLocalStore.getLoggedInUser();
        if (loggedInUser != null) {
            Toast.makeText(this, "Logged in as " + loggedInUser.getEmail(), Toast.LENGTH_LONG).show();
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
                startActivity(new Intent(this, PostureActivity.class));
                break;

            case R.id.bCalibrationPage:
                startActivity(new Intent(this, CalibrationActivity.class));
                break;
            case R.id.bHistoryPage:
                startActivity(new Intent(this, HistoryActivity.class));
                break;
        }
    }
}
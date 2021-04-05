package com.example.jokechair;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

// TODO: Add bluetooth connection stuff
public class CalibrationActivity extends AppCompatActivity {
    private static final String TAG = "CalibrationActivity";

    private TextView tvCalibPosture;
    private Button bStartCalibration, bStartCollection;
    private ProgressBar pbCountdown;

    private int counter = 0;
    private final String[] postures = {"Proper", "Lean Forward", "Lean Left", "Lean Right", "Left Leg Cross", "Right Leg Cross", "Slouch"};

    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        tvCalibPosture = (TextView) findViewById(R.id.tvCalibPosture);

        pbCountdown = (ProgressBar) findViewById(R.id.pbCountdown);

        bStartCalibration = (Button) findViewById(R.id.bStartCalibration);
        bStartCollection = (Button) findViewById(R.id.bStartCollection);

        countDownTimer = null;

        bStartCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bStartCalibration.setVisibility(View.GONE);
                bStartCollection.setVisibility(View.VISIBLE);
                tvCalibPosture.setText(postures[counter]);
            }
        });

        // TODO: Actual data collection and sending to server
        bStartCollection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bStartCollection.setVisibility(View.GONE);
                pbCountdown.setVisibility(View.VISIBLE);
                countDownTimer = getCountDownTimer();
                countDownTimer.start();
            }
        });
    }

    private CountDownTimer getCountDownTimer() {
        return new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) (millisUntilFinished / 1000);
                pbCountdown.setProgress(pbCountdown.getMax() - progress);
            }

            @Override
            public void onFinish() {
                counter = counter + 1;
                if (counter < postures.length) {
                    tvCalibPosture.setText(postures[counter]);
                    pbCountdown.setVisibility(View.GONE);
                    bStartCollection.setVisibility(View.VISIBLE);
                }
            }
        };
    }
}
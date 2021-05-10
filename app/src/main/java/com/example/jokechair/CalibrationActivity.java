package com.example.jokechair;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.sql.Timestamp;


public class CalibrationActivity extends AppCompatActivity {
    private static final String TAG = "CalibrationActivity";
    private static final UUID MY_UUID = UUID.fromString("0001101-0000-1000-8000-00805F9B34FB");
    private static final String TARGET_DEVICE_NAME = "ESP32test";
    private static final int REQUEST_ENABLE_BT = 1;

    private final String[] postures = {"proper", "lean_forward", "lean_left", "lean_right", "left_leg_cross", "right_leg_cross", "slouch"};
//    private final String[] postures = {"proper"};
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVE = 5;

    private TextView tvCalibPosture;
    private Button bStartCalibration, bStartCollection;
    private ProgressBar pbCountdown;

    private PmmlUtil pmmlUtil;
    private CountDownTimer countDownTimer;

    BluetoothAdapter bluetoothAdapter;
    String targetDeviceAddress;

    ConnectedThread connectedThread;

    private int counter = 0;
    private boolean collect = false;
    private int[] baseline = new int[8];
    private int baseline_rows = 0;
    private List<Integer> uids = new ArrayList<>();
    private List<List<Integer>> collected_sensor_data = new ArrayList<List<Integer>>();
    private List<Timestamp> timestamps = new ArrayList<>();
    private JSONObject calib_data = new JSONObject();
    private List<String> posture_list = new ArrayList<>();
    private RequestQueue mQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        tvCalibPosture = (TextView) findViewById(R.id.tvCalibPosture);

        pbCountdown = (ProgressBar) findViewById(R.id.pbCountdown);

        bStartCalibration = (Button) findViewById(R.id.bStartCalibration);
        bStartCollection = (Button) findViewById(R.id.bStartCollection);

        countDownTimer = null;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        targetDeviceAddress = null;

        pmmlUtil = new PmmlUtil();


        bStartCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Message message = Message.obtain();
//                message.what = STATE_CONNECTED;
//                btHandler.sendMessage(message);
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                    findDevice();
                    if (targetDeviceAddress != null) {
                        Message message = Message.obtain();
                        message.what = STATE_CONNECTING;
                        btHandler.sendMessage(message);

                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(targetDeviceAddress);
                        connect(device);
                    } else {
                        Toast.makeText(getApplicationContext(), "Could not find device to connect to", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // TODO: Actual data collection and sending to server
        bStartCollection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bStartCollection.setVisibility(View.GONE);
                pbCountdown.setVisibility(View.VISIBLE);
                countDownTimer = getCountDownTimer();
                collect = true;
                countDownTimer.start();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(btScanReceiver);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_LONG).show();
            }
        }
    }

    private CountDownTimer getCountDownTimer() {
        return new CountDownTimer(20000, 2000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) (millisUntilFinished / 2000);
                pbCountdown.setProgress(pbCountdown.getMax() - progress);
            }

            @Override
            public void onFinish() {
                collect = false;
                counter = counter + 1;
                if (counter < postures.length) {
                    tvCalibPosture.setText(postures[counter]);
                    pbCountdown.setVisibility(View.GONE);
                    bStartCollection.setVisibility(View.VISIBLE);
                }
                if (counter == postures.length) {
                    // TODO: send all collected data to the backend
                    connectedThread.cancel();


                    String url = "http://localhost:3333/user/add_traindata";
                    JSONObject jsonBody = new JSONObject();
//                    timestamps.add(Timestamp.valueOf("1999-01-01 00:00:00"));
//                    List<List<Integer>> sensor_row = new ArrayList<List<Integer>>();
//                    sensor_row.add(new ArrayList<Integer>());
//                    for(int i=0; i<10; i++) {
//                        sensor_row.get(0).add(i);
//                    }
//                    posture_list.add("test");

                    for(int i=0; i<8; i++) {
                        baseline[i] = baseline[i] / baseline_rows;
                        System.out.println(baseline[i]);
                    }

                    for(int i=0; i<collected_sensor_data.size(); i++) {
                        for(int j=0; j<collected_sensor_data.get(i).size(); j++) {
                            collected_sensor_data.get(i).set(j, collected_sensor_data.get(i).get(j) - baseline[j]);
                        }
                    }

                    try {
                        jsonBody.put("uid", uids);
                        jsonBody.put("timestamp", timestamps);
                        jsonBody.put("sensors", collected_sensor_data);
                        jsonBody.put("classification", posture_list);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mQueue = Volley.newRequestQueue(getApplicationContext());
                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    System.out.println("SUCCESS");
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, String.valueOf(error));
                        }
                    });
                    request.setRetryPolicy(new DefaultRetryPolicy(
                            10000,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                    mQueue.add(request);

//                    url = "http://localhost:3333/usermodel/generate";
//                    JSONObject modelJsonBody = new JSONObject();
//                    try {
//                        modelJsonBody.put("uid", 2);
//                        modelJsonBody.put("gen", true);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    request = new JsonObjectRequest(Request.Method.POST, url, modelJsonBody,
//                            new Response.Listener<JSONObject>() {
//                                @Override
//                                public void onResponse(JSONObject response) {
//                                    System.out.println("SUCCESS");
//                                }
//                            }, new Response.ErrorListener() {
//                        @Override
//                        public void onErrorResponse(VolleyError error) {
//                            Log.e(TAG, String.valueOf(error));
//                        }
//                    });
//                    request.setRetryPolicy(new DefaultRetryPolicy(
//                            10000,
//                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//                    mQueue.add(request);
                }
            }
        };
    }

    private final BroadcastReceiver btScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
                Log.d(TAG, "Starting discovery");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Found: " + device.getName());
                if (TARGET_DEVICE_NAME.equals(device.getName())) {
                    targetDeviceAddress = device.getAddress();
                }
            }
        }
    };

    private void findDevice() {
        Log.d(TAG, "Finding device");
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (TARGET_DEVICE_NAME.equals(device.getName())) {
                    this.targetDeviceAddress = device.getAddress();
                    break;
                }
            }
        }
        if (targetDeviceAddress == null) {
            if (this.bluetoothAdapter.isDiscovering()) {
                // cancel the discovery if it has already started
                this.bluetoothAdapter.cancelDiscovery();
            }
            this.bluetoothAdapter.startDiscovery();
            while(this.targetDeviceAddress == null) {
                if (!this.bluetoothAdapter.isDiscovering()) {
                    break;
                }
                if (this.targetDeviceAddress != null) {
                    break;
                }
            }
            this.bluetoothAdapter.cancelDiscovery();
        }
    }

    private synchronized void connect(BluetoothDevice device) {
        ConnectThread connectThread = new ConnectThread(device);
        connectThread.start();
    }

    private final Handler btHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case STATE_LISTENING:
                    //tvPostureStatusMessage.setText("Listening for device");
                    break;
                case STATE_CONNECTING:
                    //tvPostureStatusMessage.setText("Connecting to device");
                    break;
                case STATE_CONNECTED:
                    //tvPostureStatusMessage.setText("Device connected");
                    bStartCalibration.setVisibility(View.GONE);
                    bStartCollection.setVisibility(View.VISIBLE);
                    tvCalibPosture.setText(postures[counter]);
                    break;
                case STATE_CONNECTION_FAILED:
                    //tvPostureStatusMessage.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVE:
                    //tvPostureStatusMessage.setText("Receiving data");
                    // TODO: Add proper formatting for collected data
                    if (collect) {
                        byte[] readBuff = (byte[]) msg.obj;
                        List<Integer> sensorVals = new ArrayList<>();
                        boolean err = false;
                        for(int i = 0; i < 8; i++){
                            sensorVals.add((readBuff[2*i]&0xFF) + ((readBuff[2*i+1]&0xFF)*256));
                        }
                        if(err){
                            break;
                        }



                        if(counter == 0) {
                            for(int i=0; i<sensorVals.size(); i++) {
                                baseline[i] += sensorVals.get(i);
                                baseline_rows += 1;
                            }
                        }

                        String label = postures[counter];
                        collected_sensor_data.add(sensorVals);
                        timestamps.add(new Timestamp(System.currentTimeMillis()));
                        posture_list.add(label);
                        uids.add(2);
                    }
                    break;
            }
            return true;
        }
    });

    private class ConnectThread extends Thread {
        private final BluetoothSocket btSocket;
        private final BluetoothDevice btDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            btDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            btSocket = tmp;
        }

        public void run() {
            try {
                btSocket.connect();

                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                btHandler.sendMessage(message);
            } catch (IOException connectException) {
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                btHandler.sendMessage(message);

                try {
                    btSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            connectedThread = new ConnectedThread(btSocket);
            connectedThread.start();
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket btSocket;
        private final InputStream btInputStream;
        private byte[] btBuffer;

        public ConnectedThread(BluetoothSocket socket) {
            btSocket = socket;
            InputStream tmpIn = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }

            btInputStream = tmpIn;
        }

        public void run() {
            btBuffer = new byte[1024];
            int numBytes;
            int totalBytesRead;

            while (true) {
                try {
                    totalBytesRead = 0;
                    while(totalBytesRead < 21) {
                        numBytes = btInputStream.read(btBuffer);
                        totalBytesRead += numBytes;
                    }
                    btHandler.obtainMessage(STATE_MESSAGE_RECEIVE, totalBytesRead, -1, btBuffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Input stream was disconnected", e);
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
        }
    }
}
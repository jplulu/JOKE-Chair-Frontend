package com.example.jokechair;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.UUID;
import org.jpmml.evaluator.Evaluator;

public class PostureActivity extends AppCompatActivity {

    private static final String TAG = "PostureActivity";
    private static final UUID MY_UUID = UUID.fromString("0001101-0000-1000-8000-00805F9B34FB");
    private static final String TARGET_DEVICE_NAME = "ESP32test";
    private static final int REQUEST_ENABLE_BT = 1;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVE = 5;

    private final String[] postures = {"lean forward", "lean left", "lean right", "left leg cross", "proper", "right leg cross", "slouch"};

    private PmmlUtil pmmlUtil;
    private RequestQueue mQueue;
    private UserLocalStore userLocalStore;
    private Evaluator evaluator;

    TextView tvPostureStatusMessage, tvCurrentPosture;
    Button bStart, bHome;

    BluetoothAdapter bluetoothAdapter;
    String targetDeviceAddress;

    ConnectedThread connectedThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posture);

        tvPostureStatusMessage = (TextView) findViewById(R.id.tvPostureStatusMessage);
        tvCurrentPosture = (TextView) findViewById(R.id.tvCurrentPosture);
        bStart = (Button) findViewById(R.id.bStart);
        bHome = (Button) findViewById(R.id.bHome);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        targetDeviceAddress = null;

        pmmlUtil = new PmmlUtil();
        userLocalStore = new UserLocalStore(this);

        bStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
//                        Message message = Message.obtain();
//                        message.what = STATE_CONNECTION_FAILED;
//                        btHandler.sendMessage(message);
                        tvPostureStatusMessage.setText("Connection Failed");
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_SHORT).show();
                }
            }
        });

        bHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(btScanReceiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
        } else {
            if(!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        mQueue = Volley.newRequestQueue(getApplicationContext());

        //TODO use stored UID
        String url = String.format("http://10.0.2.2:5000/usermodel/generate?uid=%s&gen=%s",
                userLocalStore.getLoggedInUser().getUid(),
                false);

        String filename = "predictmodel" + userLocalStore.getLoggedInUser().getUid() + ".json";
        if (pmmlUtil.isModelPresent(getApplicationContext(), filename)) {
            mQueue = Volley.newRequestQueue(getApplicationContext());
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            pmmlUtil.createModelFile(getApplicationContext(), filename, response.toString());
                            System.out.println(pmmlUtil.isModelPresent(getApplicationContext(), filename));
                            InputStream inputStream = pmmlUtil.readModelFile(getApplicationContext(), filename);
                            try {
                                evaluator = pmmlUtil.createEvaluator(inputStream);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
//                    System.out.println("error");
                    error.printStackTrace();
                }
            });
            mQueue.add(request);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(btScanReceiver);
        super.onDestroy();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void findDevice() {
        Log.d(TAG, "Finding device");
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

//        Message message = Message.obtain();
//        message.what = STATE_LISTENING;
//        btHandler.sendMessage(message);
        this.tvPostureStatusMessage.setText("Listening for device");

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
            int counter = 0;
            while(this.targetDeviceAddress == null) {
                if (!this.bluetoothAdapter.isDiscovering()) {
                    break;
                }
                if (this.targetDeviceAddress != null) {
                    break;
                }
                counter++;
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
            this.bluetoothAdapter.cancelDiscovery();
        }
    }

    private synchronized void connect(BluetoothDevice device) {
        ConnectThread connectThread = new ConnectThread(device);
        connectThread.start();
    }

    // TODO: Create statuses and implement handler
    private final Handler btHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case STATE_LISTENING:
                    tvPostureStatusMessage.setText("Listening for device");
                    break;
                case STATE_CONNECTING:
                    tvPostureStatusMessage.setText("Connecting to device");
                    break;
                case STATE_CONNECTED:
                    tvPostureStatusMessage.setText("Device connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    tvPostureStatusMessage.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVE:
                    tvPostureStatusMessage.setText("Receiving data");
                    byte[] readBuff = (byte[]) msg.obj;
                    int[] sensorVals = new int[8];
                    for(int i = 0; i < 8; i++){
                        sensorVals[i] = (readBuff[2*i]&0xFF) + (readBuff[2*i+1]&0xFF)*256;
                        System.out.println(i + "th number: " + sensorVals[i]);
                    }
                    int prediction = pmmlUtil.predict(evaluator, sensorVals);
                    Log.d(TAG, "Prediction: " + postures[prediction]);
                    tvCurrentPosture.setText(postures[prediction]);
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

            while (true) {
                try {
                    numBytes = btInputStream.read(btBuffer);
                    System.out.println(numBytes);
                    // TODO: Process received bytes in message handler
                    btHandler.obtainMessage(STATE_MESSAGE_RECEIVE, numBytes, -1, btBuffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Input stream was disconnected", e);
                    break;
                }
                try {
                    Thread.sleep(1000);
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
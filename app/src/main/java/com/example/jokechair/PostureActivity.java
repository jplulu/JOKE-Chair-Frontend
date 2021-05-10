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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.concurrent.TimeUnit;

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

    static final int NUM_SENSORS = 8;

    private final String[] postures = {"lean_forward", "lean_left", "lean_right", "left_leg_cross", "proper", "right_leg_cross", "slouch"};

    private PmmlUtil pmmlUtil;
    private RequestQueue mQueue;
    private UserLocalStore userLocalStore;
    private Evaluator evaluator;

    TextView tvPostureStatusMessage, tvCurrentPosture;
    ImageView postureImage;
    Button bStart, bHome, bStop;

    BluetoothAdapter bluetoothAdapter;
    String targetDeviceAddress;

    ConnectedThread connectedThread;
    public int[] baseline = {-1, -1, -1, -1,  -1, -1, -1, -1};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posture);

        tvPostureStatusMessage = (TextView) findViewById(R.id.tvPostureStatusMessage);
        tvCurrentPosture = (TextView) findViewById(R.id.tvCurrentPosture);
        bStart = (Button) findViewById(R.id.bStart);
        bHome = (Button) findViewById(R.id.bHome);
        postureImage = (ImageView) findViewById(R.id.postureImage);
        bStop = (Button) findViewById(R.id.bStop);
      
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
                        bStart.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(getApplicationContext(), "Could not find device to connect to", Toast.LENGTH_LONG).show();
//                        Message message = Message.obtain();
//                        message.what = STATE_CONNECTION_FAILED;
//                        btHandler.sendMessage(message);
                        tvPostureStatusMessage.setText("Connection Failed");
                        bStart.setVisibility(View.VISIBLE);
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

        bStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectedThread.cancel();
                bStop.setVisibility(View.GONE);
                bStart.setVisibility(View.VISIBLE);
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(btScanReceiver, filter);
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
        
        mQueue = Volley.newRequestQueue(getApplicationContext());

        //TODO use stored UID
        String url = String.format("http://localhost:3333/usermodel/get_model?uid=%d",
                2);

        mQueue = Volley.newRequestQueue(getApplicationContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String filename = "predictmodel2" + ".json";
                        pmmlUtil.createModelFile(getApplicationContext(), filename, response.toString());
                        System.out.println(response.toString());
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

//        String jsonString = "{\"PMML\": {\"version\": \"4.4\", \"x-baseVersion\": \"4.4\", \"Header\": {\"Application\": {\"name\": \"JPMML-SkLearn\", \"version\": \"1.6.17\"}, \"Timestamp\": {\"content\": [\"2021-05-06T23:59:39Z\"]}}, \"DataDictionary\": {\"DataField\": [{\"name\": \"y\", \"optype\": \"categorical\", \"dataType\": \"string\", \"Value\": [{\"value\": \" lean_forward\"}, {\"value\": \" lean_left\"}, {\"value\": \" lean_right\"}, {\"value\": \" left_leg_cross\"}, {\"value\": \" proper\"}, {\"value\": \" right_leg_cross\"}, {\"value\": \" slouch\"}, {\"value\": \"proper\"}]}, {\"name\": \"x1\", \"optype\": \"continuous\", \"dataType\": \"double\"}, {\"name\": \"x2\", \"optype\": \"continuous\", \"dataType\": \"double\"}, {\"name\": \"x3\", \"optype\": \"continuous\", \"dataType\": \"double\"}, {\"name\": \"x4\", \"optype\": \"continuous\", \"dataType\": \"double\"}, {\"name\": \"x5\", \"optype\": \"continuous\", \"dataType\": \"double\"}, {\"name\": \"x6\", \"optype\": \"continuous\", \"dataType\": \"double\"}, {\"name\": \"x7\", \"optype\": \"continuous\", \"dataType\": \"double\"}, {\"name\": \"x8\", \"optype\": \"continuous\", \"dataType\": \"double\"}]}, \"Model\": [{\"RegressionModel\": {\"functionName\": \"classification\", \"algorithmName\": \"sklearn.linear_model._logistic.LogisticRegression\", \"normalizationMethod\": \"softmax\", \"MiningSchema\": {\"MiningField\": [{\"name\": \"y\", \"usageType\": \"target\"}, {\"name\": \"x1\"}, {\"name\": \"x2\"}, {\"name\": \"x3\"}, {\"name\": \"x4\"}, {\"name\": \"x5\"}, {\"name\": \"x6\"}, {\"name\": \"x7\"}, {\"name\": \"x8\"}]}, \"Output\": {\"OutputField\": [{\"name\": \"probability( lean_forward)\", \"optype\": \"continuous\", \"dataType\": \"double\", \"feature\": \"probability\", \"value\": \" lean_forward\"}, {\"name\": \"probability( lean_left)\", \"optype\": \"continuous\", \"dataType\": \"double\", \"feature\": \"probability\", \"value\": \" lean_left\"}, {\"name\": \"probability( lean_right)\", \"optype\": \"continuous\", \"dataType\": \"double\", \"feature\": \"probability\", \"value\": \" lean_right\"}, {\"name\": \"probability( left_leg_cross)\", \"optype\": \"continuous\", \"dataType\": \"double\", \"feature\": \"probability\", \"value\": \" left_leg_cross\"}, {\"name\": \"probability( proper)\", \"optype\": \"continuous\", \"dataType\": \"double\", \"feature\": \"probability\", \"value\": \" proper\"}, {\"name\": \"probability( right_leg_cross)\", \"optype\": \"continuous\", \"dataType\": \"double\", \"feature\": \"probability\", \"value\": \" right_leg_cross\"}, {\"name\": \"probability( slouch)\", \"optype\": \"continuous\", \"dataType\": \"double\", \"feature\": \"probability\", \"value\": \" slouch\"}, {\"name\": \"probability(proper)\", \"optype\": \"continuous\", \"dataType\": \"double\", \"feature\": \"probability\", \"value\": \"proper\"}]}, \"RegressionTable\": [{\"intercept\": 0.00026692887911318854, \"targetCategory\": \" lean_forward\", \"NumericPredictor\": [{\"name\": \"x1\", \"coefficient\": -0.036218698229005176}, {\"name\": \"x2\", \"coefficient\": 0.018065209255064555}, {\"name\": \"x3\", \"coefficient\": 0.02088237968223849}, {\"name\": \"x4\", \"coefficient\": -0.04847957786931876}, {\"name\": \"x5\", \"coefficient\": 0.003761780349587777}, {\"name\": \"x6\", \"coefficient\": 0.050528433776462975}, {\"name\": \"x7\", \"coefficient\": 0.0033397290190661645}, {\"name\": \"x8\", \"coefficient\": 0.003000452897308707}]}, {\"intercept\": 1.1491276824677876e-05, \"targetCategory\": \" lean_left\", \"NumericPredictor\": [{\"name\": \"x1\", \"coefficient\": -0.018467189064091755}, {\"name\": \"x2\", \"coefficient\": -0.020016041691413168}, {\"name\": \"x3\", \"coefficient\": -0.003680282442319881}, {\"name\": \"x4\", \"coefficient\": 0.0030695657270080326}, {\"name\": \"x5\", \"coefficient\": 0.017790122809103925}, {\"name\": \"x6\", \"coefficient\": 0.016885521787581154}, {\"name\": \"x7\", \"coefficient\": 0.004670988864314089}, {\"name\": \"x8\", \"coefficient\": 0.004628975112170103}]}, {\"intercept\": 2.2469336829066647e-05, \"targetCategory\": \" lean_right\", \"NumericPredictor\": [{\"name\": \"x1\", \"coefficient\": 0.012657708927225323}, {\"name\": \"x2\", \"coefficient\": 0.010328675065207947}, {\"name\": \"x3\", \"coefficient\": 0.009413931571178973}, {\"name\": \"x4\", \"coefficient\": -0.00787250899096964}, {\"name\": \"x5\", \"coefficient\": -0.015284047370692009}, {\"name\": \"x6\", \"coefficient\": -0.00893605621209626}, {\"name\": \"x7\", \"coefficient\": 0.002928603523879022}, {\"name\": \"x8\", \"coefficient\": 0.003320719134513516}]}, {\"intercept\": -0.00015729325182735297, \"targetCategory\": \" left_leg_cross\", \"NumericPredictor\": [{\"name\": \"x1\", \"coefficient\": -0.011796027981286972}, {\"name\": \"x2\", \"coefficient\": 0.0005350739787607811}, {\"name\": \"x3\", \"coefficient\": 0.03986224796105178}, {\"name\": \"x4\", \"coefficient\": 0.03656267357923088}, {\"name\": \"x5\", \"coefficient\": 0.03253491847847173}, {\"name\": \"x6\", \"coefficient\": -0.10273660307929637}, {\"name\": \"x7\", \"coefficient\": -0.0007666609479697294}, {\"name\": \"x8\", \"coefficient\": 0.0012612277929958016}]}, {\"intercept\": -0.00016085956322575435, \"targetCategory\": \" proper\", \"NumericPredictor\": [{\"name\": \"x1\", \"coefficient\": 0.016528547324492736}, {\"name\": \"x2\", \"coefficient\": 0.06590720822819741}, {\"name\": \"x3\", \"coefficient\": -0.1343017140461306}, {\"name\": \"x4\", \"coefficient\": 0.04583944779348483}, {\"name\": \"x5\", \"coefficient\": 0.05290642270625988}, {\"name\": \"x6\", \"coefficient\": -0.12323053931119883}, {\"name\": \"x7\", \"coefficient\": -0.025383193755577054}, {\"name\": \"x8\", \"coefficient\": 0.060649680376539936}]}, {\"intercept\": -0.0002626021997453449, \"targetCategory\": \" right_leg_cross\", \"NumericPredictor\": [{\"name\": \"x1\", \"coefficient\": 0.04421038283159219}, {\"name\": \"x2\", \"coefficient\": -0.044382188587877916}, {\"name\": \"x3\", \"coefficient\": -0.0006297028328804224}, {\"name\": \"x4\", \"coefficient\": -0.043171213374488066}, {\"name\": \"x5\", \"coefficient\": -0.012711569097950017}, {\"name\": \"x6\", \"coefficient\": 0.06276587344939966}, {\"name\": \"x7\", \"coefficient\": -0.0003456405168491971}, {\"name\": \"x8\", \"coefficient\": 0.010385559961793505}]}, {\"intercept\": -5.946026808879973e-06, \"targetCategory\": \" slouch\", \"NumericPredictor\": [{\"name\": \"x1\", \"coefficient\": -0.015834629486140304}, {\"name\": \"x2\", \"coefficient\": -0.013274175156294057}, {\"name\": \"x3\", \"coefficient\": 0.05129894093218073}, {\"name\": \"x4\", \"coefficient\": -0.008945359082930343}, {\"name\": \"x5\", \"coefficient\": -0.01962610776235168}, {\"name\": \"x6\", \"coefficient\": 0.018434735199169442}, {\"name\": \"x7\", \"coefficient\": 0.005881134317874611}, {\"name\": \"x8\", \"coefficient\": 0.008632464907546699}]}, {\"intercept\": 0.00028581154880387595, \"targetCategory\": \"proper\", \"NumericPredictor\": [{\"name\": \"x1\", \"coefficient\": 0.008919905668478424}, {\"name\": \"x2\", \"coefficient\": -0.017163761098980927}, {\"name\": \"x3\", \"coefficient\": 0.017154199167886828}, {\"name\": \"x4\", \"coefficient\": 0.02299697221150386}, {\"name\": \"x5\", \"coefficient\": -0.05937152012087373}, {\"name\": \"x6\", \"coefficient\": 0.08628863438309618}, {\"name\": \"x7\", \"coefficient\": 0.009675039489923284}, {\"name\": \"x8\", \"coefficient\": -0.0918790801876485}]}]}}]}}";
//        String filename = "predictmodel2.json";
//        pmmlUtil.createModelFile(getApplicationContext(), filename, jsonString);
//        System.out.println(pmmlUtil.isModelPresent(getApplicationContext(), filename));
//        InputStream inputStream = pmmlUtil.readModelFile(getApplicationContext(), filename);
//        try {
//            evaluator = pmmlUtil.createEvaluator(inputStream);
//        } catch (IOException e) {
//            e.printStackTrace();
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
                    bStart.setVisibility(View.VISIBLE);
                    break;
                case STATE_MESSAGE_RECEIVE:
                    tvPostureStatusMessage.setText("Receiving data");
                    tvPostureStatusMessage.setText("Setting baseline. Leave the seat empty while we calibrate the sensors...");
                    byte[] readBuff = (byte[]) msg.obj; // Initialize buffer to read bytes from HC-06

                    bStop.setVisibility(View.VISIBLE);
                    if(baseline[0] == -1) {
                        try {
                            TimeUnit.SECONDS.sleep(2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // Determine baseline for this session
                        for (int j = 0; j < NUM_SENSORS; j++) {
                            for (int i = 0; i < NUM_SENSORS; i++) {
                                baseline[i] += (readBuff[2 * i] & 0xFF) + (readBuff[2 * i + 1] & 0xFF) * 256;
                            }
                        }
                        for (int k = 0; k < NUM_SENSORS; k++) {
                            baseline[k] = baseline[k] / NUM_SENSORS; // Take the mean baseline value for each sensor
                            System.out.println(baseline[k]);
                        }
                    }
                    // Collect the sensor values and subtract their respective baseline values before classifying.
                    tvPostureStatusMessage.setText("Detecting Posture...");
                    int[] sensorVals = new int[NUM_SENSORS];
                    String log = "";
                    for(int i = 0; i < NUM_SENSORS; i++){
                        sensorVals[i] = ((readBuff[2*i]&0xFF) + (readBuff[2*i+1]&0xFF)*256 - baseline[i])/100;
                        System.out.println(sensorVals[i]);
//                        log = Integer.toString(sensorVals[i]);
//                        Log.d(TAG, i + ": " + log);
                    }
                    if(sensorVals[0] > 1023 || sensorVals[5] > 1023) {
                        break;
                    }
                    // Classify the posture using the user's LR model.
                    String prediction = pmmlUtil.predict(evaluator, sensorVals);
                    System.out.println(prediction);
//                    Log.d(TAG, "Prediction: " + postures[prediction]);
                    if(prediction.equals("proper")) {
                        for(int i=0; i<NUM_SENSORS; i++) {
                            baseline[i] = sensorVals[i];
                        }
                    }

                    int prediction = pmmlUtil.predict(evaluator, sensorVals);
                    Log.d(TAG, "Prediction: " + postures[prediction]);
                    tvCurrentPosture.setText(postures[prediction]);
                    switch (prediction) {
                        case 0:
                            postureImage.setImageResource(R.drawable.placeholder1);
                        case 1:
                            postureImage.setImageResource(R.drawable.lean_forward);
                            break;
                        case 2:
                            postureImage.setImageResource(R.drawable.lean_left);
                            break;
                        case 3:
                            postureImage.setImageResource(R.drawable.lean_right);
                            break;
                        case 4:
                            postureImage.setImageResource(R.drawable.left_leg_cross);
                            break;
                        case 5:
                            postureImage.setImageResource(R.drawable.right_leg_cross);
                            break;
                        case 6:
                            postureImage.setImageResource(R.drawable.slouch);
                            break;
                    }
                    tvCurrentPosture.setText(prediction);
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
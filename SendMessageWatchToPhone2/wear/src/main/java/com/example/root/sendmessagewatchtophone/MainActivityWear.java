package com.example.root.sendmessagewatchtophone;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivityWear extends Activity implements GoogleApiClient.ConnectionCallbacks, SensorEventListener {

    private TextView mTextView;
    private static final String START_ACTIVITY = "/start_activity";
    private static final String WEAR_MESSAGE_PATH = "/message";
    private GoogleApiClient mApiClient;
    private JSONObject json;
    private JSONArray jsonAccel;
    private JSONArray jsonGyro;
    private JSONArray jsonStep;
    private SensorManager sensorManager;
    private List<Sensor> listOfSensors = new ArrayList<>();
    private Button btnStart;
    private Button btnStop;
    private Map<String,JSONArray> sensorValues=new HashMap<>();
    private long startingTime;
    private static String ACCEL = "accelerometer";
    private static String GYRO = "gyroscope";
    private static String UNKNOWN = "unknwown";
    private static String PEDO = "pedometer";
    private static String TAG="sensorValues";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity_wear);
        init();
        initGoogleApiClient();
        initSensors();
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        fillListOfSensors();

    }
    private void fillListOfSensors() {
        listOfSensors.add(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        listOfSensors.add(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        listOfSensors.add(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER));

    }
    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .build();
        mApiClient.connect();
    }
    /**
     * deregister / deactivate sensor from list of sensors
     */
    private void unRegisterSensors() {
        for (Sensor currentSensor : listOfSensors) {
            sensorManager.unregisterListener(this, currentSensor);
            //     Log.i(TAG, "sensor " + currentSensor.getName() + " unregistered");
        }
    }

    /**
     * register/activate sensors
     */
    private void registerSensors() {
        for (Sensor currentSensor : listOfSensors) {
            sensorManager.registerListener(this, currentSensor, SensorManager.SENSOR_DELAY_UI);
            // Log.i(TAG, "sensor " + currentSensor.getName() + " registered");
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mApiClient.disconnect();
    }
    private void init() {
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerSensors();
                registerJsons();
                startingTime = new Date().getTime();

                mApiClient.connect();
                Log.i(TAG, "started");
            }
        });

        btnStop = (Button) findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unRegisterSensors();
                Log.i(TAG, "stopped");
                try {
                    json.put(ACCEL, jsonAccel);
                    json.put(GYRO, jsonGyro);
                    json.put(PEDO,jsonStep);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.i(TAG,"could not create json");
                }
                sendMessage(WEAR_MESSAGE_PATH, json.toString());
                saveAsJson();
                long runtime = new Date().getTime() - startingTime;
                Log.i(TAG, "runtime (ms) " + runtime);


            }
        });
    }

    private void saveAsJson() {

        File prepath = Environment.getExternalStorageDirectory();


        String filename = "/excercises.json";
        String path = prepath + filename;
        try {
            Log.i(TAG,"result: "+json.toString());
            PrintWriter out = new PrintWriter(new File(prepath, filename));
            out.println(json.toString());
            out.close();
            Log.i(TAG, "saved to " + path);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "not saved " + e);

        }
    }

    private void registerJsons() {
        json = new JSONObject();
        jsonAccel = new JSONArray();
        jsonGyro = new JSONArray();
        jsonStep = new JSONArray();
        sensorValues.put(ACCEL,jsonAccel);
        sensorValues.put(GYRO,jsonGyro);
        sensorValues.put(PEDO,jsonStep);
    }

    private void sendMessage( final String path, final String text ) {



        new Thread( new Runnable() {
            @Override
            public void run() {

                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }
                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                      //TODO
                    }
                });
            }
        }).start();
    }
    @Override
    public void onConnected(Bundle bundle) {
        sendMessage(START_ACTIVITY, "start");
    }
    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG,"sensor changed");
        String sensorName = UNKNOWN;
        for (float i : event.values) {
            Log.i(TAG, event.timestamp + event.sensor.getName() + ": " + i);

        }
        JSONArray obj=null;
        if (event.sensor.getType() == listOfSensors.get(0).getType()) { //Accelerometer
            obj=sensorValues.get(ACCEL);

            Log.i(TAG, sensorName + " : " + event.sensor.getName());
            try {

                JSONObject jo = new JSONObject();
                Log.i(TAG, event.values.length+"");
                for(int i=0; i<event.values.length;i++) {
                    Log.i(TAG+"!!!", event.values[i]+"");
                }
                jo.put("x", event.values[0]);
                jo.put("y", event.values[1]);
                jo.put("z", event.values[2]);
                JSONArray ja = new JSONArray();
                ja.put(jo);
                JSONObject mainObj = new JSONObject();
                mainObj.put("values", ja);
                mainObj.put("Time", event.timestamp);
                jsonAccel.put(mainObj);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else if (event.sensor.getType() == listOfSensors.get(1).getType()) { //Gyroscope
            try {

                JSONObject jo = new JSONObject();
                Log.i(TAG, event.values.length+"");
                for(int i=0; i<event.values.length;i++) {
                    Log.i(TAG+"!!!", event.values[i]+"");
                }
                jo.put("x", event.values[0]);
                jo.put("y", event.values[1]);
                jo.put("z", event.values[2]);
                JSONArray ja = new JSONArray();
                ja.put(jo);
                JSONObject mainObj = new JSONObject();
                mainObj.put("values", ja);
                mainObj.put("Time", event.timestamp);
                jsonGyro.put(mainObj);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (event.sensor.getType() == listOfSensors.get(2).getType()) { //pedo
            try {

                JSONObject jo = new JSONObject();
                Log.i(TAG, event.values.length+"");
                for(int i=0; i<event.values.length;i++) {
                    Log.i(TAG, event.values[i]+"");
                }
                jo.put("x", event.values[0]);
                JSONArray ja = new JSONArray();
                ja.put(jo);
                JSONObject mainObj = new JSONObject();
                mainObj.put("values", ja);
                mainObj.put("Time", event.timestamp);

                jsonStep.put(mainObj);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG,"unknown sensor");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

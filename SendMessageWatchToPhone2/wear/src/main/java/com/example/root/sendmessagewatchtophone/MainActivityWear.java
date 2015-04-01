package com.example.root.sendmessagewatchtophone;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivityWear extends Activity implements GoogleApiClient.ConnectionCallbacks, SensorEventListener {

    private static final String START_ACTIVITY = "/start_activity";
    private static final String WEAR_MESSAGE_PATH = "/message";
    private static String ACCEL = "accelerometer";
    private static String GYRO = "gyroscope";
    private static String UNKNOWN = "unknwown";
    private static String PEDO = "pedometer";
    private static String TAG="sensorValues";
    private TextView mTextView;
    private GoogleApiClient mApiClient;
    private JSONObject json;
    private JSONArray jsonAccel;
    private JSONArray jsonGyro;
    private JSONArray jsonStep;
    private SensorManager sensorManager;
    private List<Sensor> listOfSensors = new ArrayList<>();
    private Button btnStart;
    private Button btnStop;
    private TextView resultText;
    private TextView AccelText;
    private TextView StepText;
    private TextView GyroText;
    private long startingTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity_wear);
        AccelText =(TextView) findViewById(R.id.AccelText);
        StepText =(TextView) findViewById(R.id.StepText);
        resultText=(TextView) findViewById(R.id.resultText);
        GyroText =(TextView) findViewById(R.id.GyroText);

        initButtons();
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
    private void initButtons() {
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerSensors();
                createJsons();
                startingTime = new Date().getTime();

                mApiClient.connect();
                resultText.setText("running");
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


                long runtime = new Date().getTime() - startingTime;
                Log.i(TAG,Long.toString(runtime));
                resultText.setText("stopped. runtime: "+String.valueOf(runtime * 0.001));
                Log.i(TAG, "runtime (ms) " + runtime);
                displaySensorDetails();



            }
        });
    }

    /**
     * Log details of the sensor to console
     */
    private void displaySensorDetails() {
    Log.i(TAG,"sensor details:");

        for(Sensor currentSensor:listOfSensors) {
            //supported only Api>=21
           // Log.i(TAG,currentSensor.getName()+" reporting mode: "+currentSensor.getReportingMode());
            Log.i(TAG,currentSensor.getName()+" FifoMaxEventCount: "+currentSensor.getFifoMaxEventCount());
            Log.i(TAG,currentSensor.getName()+" FifoReservedEventCount: "+currentSensor.getFifoReservedEventCount());
            Log.i(TAG,currentSensor.getName()+" max range: "+currentSensor.getMaximumRange());
            Log.i(TAG,currentSensor.getName()+" min delay: "+currentSensor.getMinDelay());

            //supported only Api>=21
       //   Log.i(TAG,currentSensor.getName()+" max delay: "+currentSensor.getMaxDelay());


        }
    }


    private void createJsons() {
        json = new JSONObject();
        jsonAccel = new JSONArray();
        jsonGyro = new JSONArray();
        jsonStep = new JSONArray();

    }

    /**
     * sending a message to the phone
     * @param path string to identify the wearable-phone pair
     * @param text message to be send
     */
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
                        CharSequence oldText = resultText.getText()+"\n";

                        resultText.setText(oldText+"message send.");

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
        Log.i(TAG,"sensor "+event.sensor.getName() +" changed");

        Long timeStamp = new Long ((new Date().getTime()- startingTime));
        double timeStampLong =  timeStamp*0.001;
        String timeStampStr = String.valueOf(timeStamp*0.001);
        String sensorName = UNKNOWN;
        for (float i : event.values) {
            Log.i(TAG, timeStampStr +" :" +event.sensor.getName()+": " + i);

        }

        Log.i(TAG, timeStampStr+"--");
        JSONObject valueObj = new JSONObject();
        JSONObject mainObj = new JSONObject();

        if (event.sensor == sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) { //Accelerometer

            try {
                AccelText.setText(ACCEL+": x: "+event.values[0]+" y: "+event.values[1]+" z: "+event.values[2]);
                valueObj.put("x", event.values[0]);
                valueObj.put("y", event.values[1]);
                valueObj.put("z", event.values[2]);

                jsonAccel.put(mainObj);

            } catch (JSONException e) {
                Log.i(TAG,"error creating accel json");

                e.printStackTrace();
            }
        }
        else if (event.sensor == sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)) { //Gyroscope

            try {
                GyroText.setText(GYRO+": x: "+event.values[0]+" y: "+event.values[1]+" z: "+event.values[2]);

                valueObj.put("x", event.values[0]);
                valueObj.put("y", event.values[1]);
                valueObj.put("z", event.values[2]);

                jsonGyro.put(mainObj);

            } catch (JSONException e) {
                Log.i(TAG,"error creating gyro json");

                e.printStackTrace();
            }
        } else if (event.sensor == sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)) { //pedo
            try {
                StepText.setText(PEDO+": "+event.values[0]);
                valueObj.put("x", event.values[0]);

                jsonStep.put(mainObj);
            } catch (JSONException e) {
                Log.i(TAG,"error creating pedo json");

                e.printStackTrace();
            }
        } else {
            Log.i(TAG,"unknown sensor");
        }
        try {
            mainObj.put("value", valueObj);
            mainObj.put("time", timeStampLong);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.i(TAG,"error creating main json");
        }

     //   sendMessage(WEAR_MESSAGE_PATH, "");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    Log.i(TAG,"sensor accuracy changed for "+sensor.getName()+" to "+i);
    }
}

package com.example.root.sensordatacollecting;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * collecting sensor data and storing the sensors and its values in a string array
 */
public class MainActivity extends Activity implements SensorEventListener {

    private TextView valueTxt;
    private SensorManager sensorManager;
    private String TAG="sensorValues";
    private List<String> foundSensors =new ArrayList<>();
    private List<String> sensorValues =new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        valueTxt = (TextView) findViewById(R.id.valueTxt);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();


        for(Sensor currentSensor:sensorManager.getSensorList(Sensor.TYPE_ALL)) {

            sensorManager.registerListener(this, currentSensor, SensorManager.SENSOR_DELAY_UI);
            Log.i(TAG,currentSensor.getName()+" sensor found ");
            foundSensors.add(currentSensor.getName());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    Log.i(TAG,event.sensor.getName());
    for(int i=0; i<event.values.length;i++) {
        String msg = event.sensor.getName()+": "+String.valueOf(event.values[i]);
        Log.i(TAG,msg);
        sensorValues.add(msg);
    }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    }


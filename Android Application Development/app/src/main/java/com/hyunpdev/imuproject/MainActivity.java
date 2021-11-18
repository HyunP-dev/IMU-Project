package com.hyunpdev.imuproject;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.slider.Slider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager = null;

    private final String DISPLAY_TEMPLATE = "Accelerometer:\n    X: %f\n    Y: %f\n    Z: %f\n\n" +
                                            "Gyroscope:\n    X: %f\n    Y: %f\n    Z: %f\n\n" +
                                            "Rotation:\n    X: %f\n    Y: %f\n    Z: %f";

    private TextView textView = null;
    private int freq = 0;

    BufferedWriter bufferedWriter = null;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.displayBox);
        TextView logoTxt = findViewById(R.id.logoTextView);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor grvSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Sensor pedoSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        if(stepSensor == null){
            Toast.makeText(this, "Step Sensor Not Found", Toast.LENGTH_LONG).show();
            Log.d("Sensor Searcher", "Step Sensor Nout Found");
        }

        if(pedoSensor == null){
            Toast.makeText(this, "Pedo Sensor Not Found", Toast.LENGTH_LONG).show();
            Log.d("Sensor Searcher", "Pedo Sensor Nout Found");
        }

        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, grvSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, pedoSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);

        Slider valueSlider = findViewById(R.id.slider);
        valueSlider.addOnChangeListener((slider, value, fromUser) -> {
            Log.e("value", String.valueOf(value));
            freq = (int) value;
        });

        Button startButton = findViewById(R.id.button);
        Button stopButton = findViewById(R.id.button2);

        stopButton.setOnClickListener(v -> {
            stopLogging();
        });

        startButton.setOnClickListener(v -> {
            @SuppressLint("DefaultLocale") Thread sensorViewThread = new Thread(() -> {
                while(true){
                    runOnUiThread(() -> {
                        String out = String.format(DISPLAY_TEMPLATE,
                                acc_x, acc_y, acc_z,
                                gyro_x, gyro_y, gyro_z,
                                rot_x, rot_y, rot_z);
                        Log.d("sensorViewThread", out);
                        textView.setText(out);
                    });
                    try {
                        Thread.sleep(1000/freq);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            sensorViewThread.setDaemon(true);
            sensorViewThread.start();

            Thread sensorSaveThread = new Thread(() -> {
                try {
                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/IMU Project");
                    if(!file.exists()) {
                        file.mkdir();
                    }
                    bufferedWriter = new BufferedWriter(new FileWriter(file+"/out.csv", false));
                    bufferedWriter.append("timestamp,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,gravity_x,gravity_y,gravity_z,pedo");
                    bufferedWriter.newLine();
                    while (true) {
                        String out = System.currentTimeMillis()+","+String.join(",", Arrays.toString(new double[]{
                                acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z, gravity_x, gravity_y, gravity_z
                        }).replace("[", "").replace("]", ""))+","+pedo;
                        bufferedWriter.append(out);
                        bufferedWriter.newLine();
                        try {
                            Thread.sleep(1000/freq);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            });
            sensorSaveThread.start();

        });

    }


    private void stopLogging(){
        sensorManager.unregisterListener(this);

        try {
            if (bufferedWriter != null)
                bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d("life cycle", "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("life cycle", "onDestroy");
        stopLogging();
    }

    private double acc_x = 0;
    private double acc_y = 0;
    private double acc_z = 0;

    private double gyro_x = 0;
    private double gyro_y = 0;
    private double gyro_z = 0;

    private double rot_x = 0;
    private double rot_y = 0;
    private double rot_z = 0;

    private double gravity_x = 0;
    private double gravity_y = 0;
    private double gravity_z = 0;

    private int pedo = 0;

    private long beforeTime = System.currentTimeMillis();

    @SuppressLint("DefaultLocale")
    @Override
    public void onSensorChanged(SensorEvent event) {
        double before_gyro_x = gyro_x;
        double before_gyro_y = gyro_y;
        double before_gyro_z = gyro_z;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                acc_x = event.values[SensorManager.DATA_X];
                acc_y = event.values[SensorManager.DATA_Y];
                acc_z = event.values[SensorManager.DATA_Z];
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyro_x = event.values[SensorManager.DATA_X];
                gyro_y = event.values[SensorManager.DATA_Y];
                gyro_z = event.values[SensorManager.DATA_Z];
                break;
            case Sensor.TYPE_GRAVITY:
                gravity_x = event.values[0];
                gravity_y = event.values[1];
                gravity_z = event.values[2];
            case Sensor.TYPE_STEP_DETECTOR:
                if (event.values[0] == 1.0f)
                    pedo++;
        }

        long now = System.currentTimeMillis();
        long dt = now - beforeTime;

        rot_x += dt * (before_gyro_x + gyro_x) / 2;
        rot_y += dt * (before_gyro_y + gyro_y) / 2;
        rot_z += dt * (before_gyro_z + gyro_z) / 2;
        beforeTime = now;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
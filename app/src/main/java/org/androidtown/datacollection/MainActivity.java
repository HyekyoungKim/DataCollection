package org.androidtown.datacollection;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private SensorManager manager = null;

    private Sensor magnetometer = null;
    private SensorEventListener magListener;
    private double magX, magY, magZ;

    private Sensor accelerometer = null;
    private SensorEventListener accListener;
    private double accX, accY, accZ;

    private Sensor gyroscope = null;
    private SensorEventListener gyroListener;
    private double gyroX, gyroY, gyroZ;

    private Sensor gravity = null;
    private SensorEventListener gravListener;
    private double gravX, gravY, gravZ;

    private Sensor linearAccelerometer = null;
    private SensorEventListener linListener;
    private double linX, linY, linZ;

    private Sensor rotationVector = null;
    private SensorEventListener rotListener;
    private double rotX, rotY, rotZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = (SensorManager) getSystemService(SENSOR_SERVICE);

        magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        magListener = new MagnetometerListener();
        manager.registerListener(magListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accListener = new AccelerometerListener();
        manager.registerListener(accListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        gyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gyroListener = new GyroscopeListener();
        manager.registerListener(gyroListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        gravity = manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        gravListener = new GravityListener();
        manager.registerListener(gravListener, gravity, SensorManager.SENSOR_DELAY_NORMAL);

        linearAccelerometer = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        linListener = new LinearAccelerometerListener();
        manager.registerListener(linListener, linearAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        rotationVector = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        rotListener = new RotationVectorListener();
        manager.registerListener(rotListener, rotationVector, SensorManager.SENSOR_DELAY_NORMAL);

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSensorData();
            }
        });
    }

    private class MagnetometerListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // All values are in micro-Tesla (uT)
            magX = event.values[0];
            magY = event.values[1];
            magZ = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private class AccelerometerListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Acceleration force along the x,y and z axis (including gravity)
            // All values are in m/s^2
            accX = event.values[0];
            accY = event.values[1];
            accZ = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private class GyroscopeListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Rate of rotation around the x,y and z axis
            // All values are in rad/s
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private class GravityListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Force of gravity along the x,y and z axis
            // All values are in m/s^2
            gravX = event.values[0];
            gravY = event.values[1];
            gravZ = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private class LinearAccelerometerListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Acceleration force along the x,y and z axis (excluding gravity)
            // All values are in m/s^2
            linX = event.values[0];
            linY = event.values[1];
            linZ = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private class RotationVectorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Rotation vector component along the x,y and z axis (unitless)
            rotX = event.values[0];
            rotY = event.values[1];
            rotZ = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }


    public void saveSensorData() {
        Log.v("Mag", "[X]:" + String.format("%.4f", magX)
        + " [Y]:" + String.format("%.4f", magY)
        + " [Z]:" + String.format("%.4f", magZ));
        Log.v("Acc", "[X]:" + String.format("%.4f", accX)
                + " [Y]:" + String.format("%.4f", accY)
                + " [Z]:" + String.format("%.4f", accZ));
        Log.v("Gyro", "[X]:" + String.format("%.4f", gyroX)
                + " [Y]:" + String.format("%.4f", gyroY)
                + " [Z]:" + String.format("%.4f", gyroZ));
        Log.v("Grav", "[X]:" + String.format("%.4f", gravX)
                + " [Y]:" + String.format("%.4f", gravY)
                + " [Z]:" + String.format("%.4f", gravZ));
        Log.v("Lin", "[X]:" + String.format("%.4f", linX)
                + " [Y]:" + String.format("%.4f", linY)
                + " [Z]:" + String.format("%.4f", linZ));
        Log.v("Rot", "[X]:" + String.format("%.4f", rotX)
                + " [Y]:" + String.format("%.4f", rotY)
                + " [Z]:" + String.format("%.4f", rotZ));
    }
}

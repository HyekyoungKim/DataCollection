package org.androidtown.datacollection;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
    /* For Database */
    String databaseName = "DATA_COLLECTION";
    String tableName = "SENSOR_DATA";
    boolean databaseCreated = false;
    boolean tableCreated = false;

    SQLiteDatabase db;

    /* For Sensors */
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

        createDatabase(databaseName);
        createTable(tableName);

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

        Button button = findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSensorData(tableName);
                Cursor c = db.rawQuery("select * from " + tableName, null);
                int recordCount = c.getCount();
                Log.d("Log", "cursor count : " + recordCount + "\n");
                for (int i = 0; i< recordCount; i++) {
                    c.moveToNext();
                    int _id = c.getInt(0);
                    double _magX = c.getDouble(1);
                    double _magY = c.getDouble(2);
                    double _magZ = c.getDouble(3);
                    double _accX = c.getDouble(4);
                    double _accY = c.getDouble(5);
                    double _accZ = c.getDouble(6);

                    Log.d("Log", "Record #" + i + ": "
                            + "id " + _id + "  "
                            + _magX +", " + _magY + ", " + _magZ + ", "
                            + _accX +", " + _accY + ", " + _accZ);
                }
                c.close();
            }
        });
    }

    private void createDatabase(String name) {
        Log.d("Log","creating database ["+name+"]");
        try {
            db = openOrCreateDatabase(
                    name,
                    Activity.MODE_PRIVATE,
                    null);

            databaseCreated = true;
            Log.d("Log","database has been created.");
        } catch(Exception e) {
            e.printStackTrace();
            Log.d("Log","database has not been created.");
        }
    }

    private void createTable(String name) {
        Log.d("Log", "creating table ["+name+"]");
        db.execSQL("create table if not exists " + name + "(" +
                "_id integer PRIMARY KEY autoincrement, " +
                "mag_x real, mag_y real, mag_z real," +
                "acc_x real, acc_y real, acc_z real," +
                "gyro_x real, gyro_y real, gyro_z real," +
                "grav_x real, grav_y real, grav_z real," +
                "lin_x real, lin_y real, lin_z real," +
                "rot_x real, rot_y real, rot_z real);");

        tableCreated = true;
        Log.d("Log", "table has been created.");
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


    private void saveSensorData(String name) {
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

        Log.d("Log", "inserting records using parameters.");
        ContentValues recordValues = new ContentValues();

        recordValues.put("mag_x", magX);
        recordValues.put("mag_y", magY);
        recordValues.put("mag_z", magZ);
        recordValues.put("acc_x", accX);
        recordValues.put("acc_y", accY);
        recordValues.put("acc_z", accZ);
        recordValues.put("gyro_x", gyroX);
        recordValues.put("gyro_y", gyroY);
        recordValues.put("gyro_z", gyroZ);
        recordValues.put("grav_x", gravX);
        recordValues.put("grav_y", gravY);
        recordValues.put("grav_z", gravZ);
        recordValues.put("lin_x", linX);
        recordValues.put("lin_y", linY);
        recordValues.put("lin_z", linZ);
        recordValues.put("rot_x", rotX);
        recordValues.put("rot_y", rotY);
        recordValues.put("rot_z", rotZ);

        db.insert(name, null, recordValues);
        Log.d("saveSensorData", "insertion complete");
    }
}
package org.androidtown.datacollection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

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

        final EditText locationId = findViewById(R.id.location);
        Button button1 = findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String locId = locationId.getText().toString().trim();
                if(locId.getBytes().length <= 0) {
                    Toast.makeText(getApplicationContext(),
                            "Enter the location ID", Toast.LENGTH_SHORT).show();
                } else {
                    saveSensorData(tableName, locId);
                    locationId.setText(null);
                }
            }
        });

        final TextView contents = findViewById(R.id.contents);
        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor c = db.rawQuery("select * from " + tableName, null);
                int recordCount = c.getCount();
                Log.d("Log", "cursor count : " + recordCount + "\n");
                contents.setText("");
                for (int i = 0; i< recordCount; i++) {
                    c.moveToNext();
                    String _location = c.getString(1);
                    double _magX = c.getDouble(2);
                    double _magY = c.getDouble(3);
                    double _magZ = c.getDouble(4);
                    double _accX = c.getDouble(5);
                    double _accY = c.getDouble(6);
                    double _accZ = c.getDouble(7);
                    double _gyroX = c.getDouble(8);
                    double _gyroY = c.getDouble(9);
                    double _gyroZ = c.getDouble(10);
                    double _gravX = c.getDouble(11);
                    double _gravY = c.getDouble(12);
                    double _gravZ = c.getDouble(13);
                    double _linX = c.getDouble(14);
                    double _linY = c.getDouble(15);
                    double _linZ = c.getDouble(16);
                    double _rotX = c.getDouble(17);
                    double _rotY = c.getDouble(18);
                    double _rotZ = c.getDouble(19);

                    contents.append("\nRecord #" + i + "\n"
                            + "Location ID: " + _location + "\n"
                            + "< Magnetometer >\n"
                            + "X: " + String.format(Locale.KOREA,"%.2f", _magX) + ", "
                            + "Y: " + String.format(Locale.KOREA,"%.2f", _magY) + ", "
                            + "Z: " + String.format(Locale.KOREA,"%.2f", _magZ) + "\n"
                            + "< Accelerometer >\n"
                            + "X: " + String.format(Locale.KOREA,"%.2f", _accX) + ", "
                            + "Y: " + String.format(Locale.KOREA,"%.2f", _accY) + ", "
                            + "Z: " + String.format(Locale.KOREA,"%.2f", _accZ) + "\n"
                            + "< Gyroscope >\n"
                            + "X: " + String.format(Locale.KOREA,"%.2f", _gyroX) + ", "
                            + "Y: " + String.format(Locale.KOREA,"%.2f", _gyroY) + ", "
                            + "Z: " + String.format(Locale.KOREA,"%.2f", _gyroZ) + "\n"
                            + "< Gravity >\n"
                            + "X: " + String.format(Locale.KOREA,"%.2f", _gravX) + ", "
                            + "Y: " + String.format(Locale.KOREA,"%.2f", _gravY) + ", "
                            + "Z: " + String.format(Locale.KOREA,"%.2f", _gravZ) + "\n"
                            + "< Linear Accelerometer >\n"
                            + "X: " + String.format(Locale.KOREA,"%.2f", _linX) + ", "
                            + "Y: " + String.format(Locale.KOREA,"%.2f", _linY) + ", "
                            + "Z: " + String.format(Locale.KOREA,"%.2f", _linZ) + "\n"
                            + "< Rotation Vector >\n"
                            + "X: " + String.format(Locale.KOREA,"%.2f", _rotX) + ", "
                            + "Y: " + String.format(Locale.KOREA,"%.2f", _rotY) + ", "
                            + "Z: " + String.format(Locale.KOREA,"%.2f", _rotZ) + "\n");
                }
                c.close();
            }
        });

        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.delete(tableName, null, null);
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
        // db.execSQL("drop table if exists " + name);
        Log.d("Log", "creating table ["+name+"]");
        db.execSQL("create table if not exists " + name + "(" +
                "_id integer PRIMARY KEY autoincrement, " +
                "location text," +
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


    private void saveSensorData(final String tbName, final String locId) {
        Log.d("Log", "inserting records using parameters.");
        final ContentValues recordValues = new ContentValues();

        recordValues.put("location", locId);
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

        String SQL = "select * from " + tbName + " where location=" + locId;
        Cursor c = db.rawQuery(SQL, null);
        if (c.moveToFirst()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Duplicate Location");
            builder.setMessage("Do you want to replace the existing data?");
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    db.update(tbName,
                            recordValues,
                            "location = ?",
                            new String[] {locId});
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
            c.close();
        } else {
            db.insert(tbName, null, recordValues);
            Log.d("saveSensorData", "insertion complete");
        }
    }
}

package org.androidtown.datacollection;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
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

public class CollectionActivity extends AppCompatActivity {
    public static final String KEY = "reset";
    boolean resetFlag;

    /* For Database */
    String databaseName = "DATA_COLLECTION";
    String rawTableName = "SENSOR_DATA";
    String convertedTableName = "CONVERTED_DATA";
    String finalTableName = "FINAL_DATA";
    boolean databaseCreated = false;
    boolean tableCreated = false;

    SQLiteDatabase db;

    /* For Sensors */
    private SensorManager manager = null;

    private Sensor magnetometer = null;
    private SensorEventListener magListener;
    private double magX, magY, magZ;

    private Sensor gravity = null;
    private SensorEventListener gravListener;
    private double gravX, gravY, gravZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        Intent intent = getIntent();
        processIntent(intent);

        createDatabase();
        createTable();

        manager = (SensorManager) getSystemService(SENSOR_SERVICE);

        magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        magListener = new MagnetometerListener();
        manager.registerListener(magListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        gravity = manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        gravListener = new GravityListener();
        manager.registerListener(gravListener, gravity, SensorManager.SENSOR_DELAY_NORMAL);

        /* Collect sensor data at user's current location */
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
                    saveSensorData(locId);
                    locationId.setText(null);
                }
            }
        });

        /* Show data collected so far */
        final TextView contents = findViewById(R.id.contents);
        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor c = db.rawQuery(
                        "select location, mag_x, mag_y, mag_z, " +
                                "grav_x, grav_y, grav_z, vertical, horizontal, magnitude " +
                                "from " + rawTableName + " join " + convertedTableName +
                                " using(_id, location)", null);
                int recordCount = c.getCount();
                Log.d("Log", "cursor count : " + recordCount + "\n");
                contents.setText("");
                for (int i = 0; i< recordCount; i++) {
                    c.moveToNext();
                    String _location = c.getString(0);
                    double _magX = c.getDouble(1);
                    double _magY = c.getDouble(2);
                    double _magZ = c.getDouble(3);
                    double _gravX = c.getDouble(4);
                    double _gravY = c.getDouble(5);
                    double _gravZ = c.getDouble(6);
                    double _vertical = c.getDouble(7);
                    double _horizontal = c.getDouble(8);
                    double _magnitude = c.getDouble(9);

                    contents.append("\nRecord #" + i + "\n"
                            + "Location ID: " + _location + "\n"
                            + "< Magnetometer >\n"
                            + "X: " + String.format(Locale.KOREA,"%.2f", _magX) + ", "
                            + "Y: " + String.format(Locale.KOREA,"%.2f", _magY) + ", "
                            + "Z: " + String.format(Locale.KOREA,"%.2f", _magZ) + "\n"
                            + "< Gravity >\n"
                            + "X: " + String.format(Locale.KOREA,"%.2f", _gravX) + ", "
                            + "Y: " + String.format(Locale.KOREA,"%.2f", _gravY) + ", "
                            + "Z: " + String.format(Locale.KOREA,"%.2f", _gravZ) + "\n"
                            + "< Converted Magnetic Field >\n"
                            + "Vertical: " + String.format(Locale.KOREA,"%.2f", _vertical) + ", "
                            + "Horizontal: " + String.format(Locale.KOREA,"%.2f", _horizontal) + ", "
                            + "Magnitude: " + String.format(Locale.KOREA,"%.2f", _magnitude) + "\n");
                }
                c.close();
            }
        });

        /* Delete all data in the tables */
        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.delete(rawTableName, null, null);
                db.delete(convertedTableName, null, null);
            }
        });

        /* Finish data collection and save the final result */
        Button button5 = findViewById(R.id.button5);
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ContentValues finalValues = new ContentValues();
                Cursor c2 = db.rawQuery("SELECT location, " +
                        "(sum(vertical)-max(vertical)-min(vertical))/(count(_id)-2), " +
                        "(sum(horizontal)-max(horizontal)-min(horizontal))/(count(_id)-2), " +
                        "(sum(magnitude)-max(magnitude)-min(magnitude))/(count(_id)-2) " +
                        "FROM " + convertedTableName +
                        " GROUP BY location", null);
                int recordCount2 = c2.getCount();
                for (int i = 0; i < recordCount2; i++) {
                    c2.moveToNext();
                    String _location = c2.getString(0);
                    double _vertical = c2.getDouble(1);
                    double _horizontal = c2.getDouble(2);
                    double _magnitude = c2.getDouble(3);

                    finalValues.put("location", _location);
                    finalValues.put("vertical", _vertical);
                    finalValues.put("horizontal", _horizontal);
                    finalValues.put("magnitude", _magnitude);

                    db.insert(finalTableName, null, finalValues);
                }
                c2.close();
            }
        });
    }

    /* Decide whether to reset or keep DB (<- user's choice) */
    private void processIntent (Intent intent) {
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            ResetOrKeep reset = bundle.getParcelable(KEY);
            if (reset.getReset() == 1)
                resetFlag = true;
            else
                resetFlag = false;
        }
    }

    /* Create DB if there is none. It there is one, open it. */
    private void createDatabase() {
        Log.d("Log","creating database ["+databaseName+"]");
        try {
            db = openOrCreateDatabase(
                    databaseName,
                    Activity.MODE_PRIVATE,
                    null);

            databaseCreated = true;
            Log.d("Log","database has been created.");
        } catch(Exception e) {
            e.printStackTrace();
            Log.d("Log","database has not been created.");
        }
    }

    /* Create two tables (one for raw sensor data, the other one for converted data) */
    private void createTable() {
        if (resetFlag) {
            db.execSQL("drop table if exists " + rawTableName);
            db.execSQL("drop table if exists " + convertedTableName);
            db.execSQL("drop table if exists " + finalTableName);
        }
        Log.d("Log", "creating table ["+rawTableName+"]");
        db.execSQL("create table if not exists " + rawTableName + "(" +
                "_id integer PRIMARY KEY autoincrement, " +
                "location text," +
                "mag_x real, mag_y real, mag_z real," +
                "grav_x real, grav_y real, grav_z real);");
        Log.d("Log", "creating table ["+convertedTableName+"]");
        db.execSQL("create table if not exists " + convertedTableName + "(" +
                "_id integer PRIMARY KEY autoincrement, " +
                "location text," +
                "vertical real, horizontal real, magnitude real);");
        Log.d("Log", "creating table ["+finalTableName+"]");
        db.execSQL("create table if not exists " + finalTableName + "(" +
                "_id integer PRIMARY KEY autoincrement, " +
                "location text," +
                "vertical real, horizontal real, magnitude real);");

        tableCreated = true;
        Log.d("Log", "tables have been created.");
    }

    /* Listen to the magnetometer */
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

    /* Listen to the gravity sensor */
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

    /* Save raw sensor data and corresponding converted data to two separate tables */
    private void saveSensorData(final String locId) {
        Log.d("Log", "inserting records using parameters.");
        final ContentValues recordValues = new ContentValues();
        final ContentValues convertedRecordValues = new ContentValues();

        recordValues.put("location", locId);
        recordValues.put("mag_x", magX);
        recordValues.put("mag_y", magY);
        recordValues.put("mag_z", magZ);
        recordValues.put("grav_x", gravX);
        recordValues.put("grav_y", gravY);
        recordValues.put("grav_z", gravZ);

        /* Magnetic Data Elements Conversion */
        /* Reference:
           Lee, N.; Ahn S.; Han D. AMID: Accurate Magnetic Indoor Localization Using Deep Learning.
           Sensors 2018, 18, 1598. */
        double G = Math.sqrt(Math.pow(gravX,2) + Math.pow(gravY,2) + Math.pow(gravZ,2));
        double cosA = gravZ/G;
        double magXY = Math.sqrt(Math.pow(magX,2) + Math.pow(magY,2));
        double magVer = magZ * cosA + magXY * Math.sqrt(1 - Math.pow(cosA,2));
        double magMag = Math.sqrt(Math.pow(magX,2) + Math.pow(magY,2) + Math.pow(magZ,2));
        double magHor = Math.sqrt(Math.pow(magMag,2) - Math.pow(magVer,2));

        convertedRecordValues.put("location", locId);
        convertedRecordValues.put("vertical", magVer);
        convertedRecordValues.put("horizontal", magHor);
        convertedRecordValues.put("magnitude", magMag);
        Log.d("Log", "Magnitude of gravity: " + G);
        Log.d("Log", "Cosine of A: " + cosA);
        Log.d("Log", "Vertical magnetic field: " + magVer);
        Log.d("Log", "Horizontal magnetic field: " + magHor);
        Log.d("Log", "Magnitude of magnetic field: " + Math.sqrt(Math.pow(magX,2) + Math.pow(magY,2) + Math.pow(magZ,2)));

        db.insert(rawTableName, null, recordValues);
        db.insert(convertedTableName, null, convertedRecordValues);
        Log.d("saveSensorData", "insertion complete");
    }
}

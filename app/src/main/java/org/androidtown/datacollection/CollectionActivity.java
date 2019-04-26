package org.androidtown.datacollection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

public class CollectionActivity extends AppCompatActivity {
    boolean resetFlag;
    Button collectButton, showDataButton, clearButton, progressButton, finishButton;
    TextView contents, status;

    /* For Database */
    String databaseName = "DATA_COLLECTION";
    public static String tableName = "CONVERTED_DATA";
    boolean databaseCreated = false;
    boolean tableCreated = false;

    public static SQLiteDatabase db;

    /* For Sensors */
    private SensorManager manager = null;
    private HandlerThread mSensorThread;
    private Handler mSensorHandler;

    private Sensor magnetometer = null;
    private SensorEventListener magListener;
    private double magX, magY, magZ;
    private boolean magSensorChanged = false;

    private Sensor gravity = null;
    private SensorEventListener gravListener;
    private double gravX, gravY, gravZ;
    private boolean gravSensorChanged = false;
    private double [] sensorData = new double [6];

    /* For UWB localization */
    UWBLocalizer uwbLocalizer;
    Handler handler;
    public static boolean LOCATION_READY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        handler = new MyHandler(this);
        uwbLocalizer = new UWBLocalizer(this, handler);
        uwbLocalizer.registerReceiver();
        uwbLocalizer.begin();

        Intent intent = getIntent();
        processIntent(intent);

        createDatabase();
        createTable();

        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
        mSensorThread.start();
        mSensorHandler = new Handler(mSensorThread.getLooper());

        magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        magListener = new MagnetometerListener();
        manager.registerListener(magListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL,mSensorHandler);

        gravity = manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        gravListener = new GravityListener();
        manager.registerListener(gravListener, gravity, SensorManager.SENSOR_DELAY_NORMAL, mSensorHandler);

        collectButton = findViewById(R.id.buttonCollect);
        showDataButton = findViewById(R.id.buttonShowData);
        clearButton = findViewById(R.id.buttonClear);
        progressButton = findViewById(R.id.buttonProgress);
        finishButton = findViewById(R.id.buttonFinish);
        contents = findViewById(R.id.contents);
        status = findViewById(R.id.status);
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
        manager.unregisterListener(magListener);
        manager.unregisterListener(gravListener);
        uwbLocalizer.end();
        uwbLocalizer.unregisterReceiver();
    }

    /** Decide whether to reset or keep DB (<- user's choice) */
    private void processIntent (Intent intent) {
        resetFlag = intent.getExtras().getBoolean("reset");
        uwbLocalizer.setID1(intent.getExtras().getShort("id1"));
        uwbLocalizer.setID2(intent.getExtras().getShort("id2"));
        uwbLocalizer.setID3(intent.getExtras().getShort("id3"));
        uwbLocalizer.setU(intent.getExtras().getFloat("u"));
        uwbLocalizer.setVx(intent.getExtras().getFloat("vx"));
        uwbLocalizer.setVy(intent.getExtras().getFloat("vy"));
        Log.d("intent", "reset: "+resetFlag+", id1: "+intent.getExtras().getShort("id1")
                +", U: "+intent.getExtras().getFloat("u"));
    }

    /** Collect sensor data at user's current location */
    public void onClickCollect(View view) {
        status.setText("");
        LOCATION_READY =false;
        uwbLocalizer.localize();
        ArrayList<double[]> sensorDataList = new ArrayList<>();
        int sensorDataCount = 0;
        while (!LOCATION_READY || sensorDataCount < 5) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (magSensorChanged && gravSensorChanged) {
                sensorData[0] = magX;
                sensorData[1] = magY;
                sensorData[2] = magZ;
                sensorData[3] = gravX;
                sensorData[4] = gravY;
                sensorData[5] = gravZ;
                sensorDataList.add(sensorData);
                sensorDataCount++;
                magSensorChanged = false;
                gravSensorChanged = false;
            }
        }
        Toast.makeText(getApplicationContext(),
                "Sensor Data Count: "+sensorDataCount, Toast.LENGTH_SHORT).show();
        float x = uwbLocalizer.getX();
        float y = uwbLocalizer.getY();

        saveSensorData(x, y, sensorDataList, sensorDataCount);
    }

    /** Show data collected so far */
    public void onClickShowData(View view) {
        Cursor c = db.rawQuery(
                "select pos_x, pos_y, vertical, horizontal, magnitude " +
                        "from " + tableName, null);
        int recordCount = c.getCount();
        Log.d("Log", "cursor count : " + recordCount + "\n");
        contents.setText("");
        for (int i = 0; i < recordCount; i++) {
            c.moveToNext();
            double _posX = c.getDouble(0);
            double _posY = c.getDouble(1);
            double _vertical = c.getDouble(2);
            double _horizontal = c.getDouble(3);
            double _magnitude = c.getDouble(4);

            contents.append("\nRecord #" + i + "\n"
                    + "Location (x,y): (" + String.format(Locale.KOREA,"%.2f", _posX)
                    + ", " + String.format(Locale.KOREA,"%.2f", _posY) + ")" + "\n"
                    + "< Converted Magnetic Field >\n"
                    + "Vertical: " + String.format(Locale.KOREA,"%.2f", _vertical) + ", "
                    + "Horizontal: " + String.format(Locale.KOREA,"%.2f", _horizontal) + ", "
                    + "Magnitude: " + String.format(Locale.KOREA,"%.2f", _magnitude) + "\n");
        }
        c.close();
    }

    /** Delete all data in the tables */
    public void onClickClear(View view) {
        clearTable();
    }

    /** Show the progress of data collection at each location */
    public void onClickProgress(View view) {
        Cursor c = db.rawQuery(
                "select pos_x, pos_y " +
                        "from " + tableName, null);
        int recordCount = c.getCount();
        contents.setText("");
        contents.append("Data collected at " + recordCount + " positions.\nPosition (x,y):\n");
        for (int i = 0; i < recordCount; i++) {
            c.moveToNext();
            double _posX = c.getDouble(0);
            double _posY = c.getDouble(1);
            contents.append("(" + String.format(Locale.KOREA,"%.2f", _posX)
                    + ", " + String.format(Locale.KOREA,"%.2f", _posY) + ")\n");
        }
        c.close();
    }

    /** Finish data collection and save the final result */
    public void onClickFinish(View view) {
        Intent intent = new Intent(getApplicationContext(), FinalActivity.class);
        startActivity(intent);
    }

    /** Create DB if there is none. It there is one, open it. */
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

    /** Create a table */
    private void createTable() {
        if (resetFlag) {
            db.execSQL("drop table if exists " + tableName);
        }
        db.execSQL("create table if not exists " + tableName + "(" +
                "_id integer PRIMARY KEY autoincrement, " +
                "pos_x real, pos_y real, " +
                "vertical real, horizontal real, magnitude real);");

        tableCreated = true;
    }

    /** Listen to the magnetometer */
    private class MagnetometerListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // All values are in micro-Tesla (uT)
            magX = event.values[0];
            magY = event.values[1];
            magZ = event.values[2];
            magSensorChanged = true;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    /** Listen to the gravity sensor */
    private class GravityListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Force of gravity along the x,y and z axis
            // All values are in m/s^2
            gravX = event.values[0];
            gravY = event.values[1];
            gravZ = event.values[2];
            gravSensorChanged = true;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    /** Process raw sensor data and save corresponding converted data to the table */
    private void saveSensorData(final float x, final float y, final ArrayList<double[]> dataList, int dataCount) {
        double[] magVer = new double[dataCount];
        double[] magHor = new double[dataCount];
        double[] magMag = new double[dataCount];
        for (int i = 0; i < dataCount; i++) {
            double[] data = dataList.get(i);
            double _magX = data[0];
            double _magY = data[1];
            double _magZ = data[2];
            double _gravX = data[3];
            double _gravY = data[4];
            double _gravZ = data[5];

            /** Magnetic Data Elements Conversion
             * Reference:
             * Lee, N.; Ahn S.; Han D. AMID: Accurate Magnetic Indoor Localization Using Deep Learning.
             * Sensors 2018, 18, 1598. */
            double G = Math.sqrt(Math.pow(_gravX,2) + Math.pow(_gravY,2) + Math.pow(_gravZ,2));
            double cosA = _gravZ/G;
            double _magXY = Math.sqrt(Math.pow(_magX,2) + Math.pow(_magY,2));
            double _magVer = _magZ * cosA + _magXY * Math.sqrt(1 - Math.pow(cosA,2));
            double _magMag = Math.sqrt(Math.pow(_magX,2) + Math.pow(_magY,2) + Math.pow(_magZ,2));
            double _magHor = Math.sqrt(Math.pow(_magMag,2) - Math.pow(_magVer,2));

            magVer[i] = _magVer;
            magHor[i] = _magHor;
            magMag[i] = _magMag;
        }

        int verticalMin = 0, verticalMax = 0;
        int horizontalMin = 0, horizontalMax = 0;
        int magnitudeMin = 0, magnitudeMax = 0;
        for (int i = 1; i < dataCount; i++) {
            if (magVer[i] < magVer[verticalMin])
                verticalMin = i;
            else if (magVer[i] > magVer[verticalMax])
                verticalMax = i;

            if (magHor[i] < magHor[horizontalMin])
                horizontalMin = i;
            else if (magHor[i] > magHor[horizontalMax])
                horizontalMax = i;

            if (magMag[i] < magMag[magnitudeMin])
                magnitudeMin = i;
            else if (magMag[i] > magMag[magnitudeMax])
                magnitudeMax = i;
        }

        double verticalSum = 0, horizontalSum = 0, magnitudeSum = 0;
        for (int i = 0; i < dataCount; i++) {
            if (i != verticalMin && i != verticalMax)
                verticalSum += magVer[i];
            if (i != horizontalMin && i != horizontalMax)
                horizontalSum += magHor[i];
            if (i != magnitudeMin && i != magnitudeMax)
                magnitudeSum += magMag[i];
        }
        double magVerAvg = verticalSum / (dataCount - 2);
        double magHorAvg = horizontalSum / (dataCount - 2);
        double magMagAvg = magnitudeSum / (dataCount - 2);

        final ContentValues recordValues = new ContentValues();

        recordValues.put("pos_x", x);
        recordValues.put("pos_y", y);
        recordValues.put("vertical", magVerAvg);
        recordValues.put("horizontal", magHorAvg);
        recordValues.put("magnitude", magMagAvg);

        db.insert(tableName, null, recordValues);
    }

    /** Delete all records in the tables */
    private void clearTable() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear the tables");
        builder.setMessage("All the records in the tables will be deleted. " +
                "Are you sure to continue?");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db.delete(tableName, null, null);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

    public static class MyHandler extends Handler {
        private final WeakReference<CollectionActivity> mActivity;

        public MyHandler(CollectionActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UWBLocalizer.TRIGER_SCAN:
                    mActivity.get().tvAppend(mActivity.get().status, "trigger scan\n");
                    break;
                case UWBLocalizer.SCAN_FAILED:
                    mActivity.get().tvAppend(mActivity.get().status, "scan failed\n");
                    break;
                case UWBLocalizer.DATA_READY_SEND:
                    mActivity.get().tvAppend(mActivity.get().status, "send data ready reason: " + String.valueOf(msg.arg1) +"\n");
                    break;
                case UWBLocalizer.NO_DATA:
                    mActivity.get().tvAppend(mActivity.get().status, "no data\n");
                    break;
                case UWBLocalizer.DATA_READY:
                    mActivity.get().tvAppend(mActivity.get().status, "data ready\n");
                    break;
                case UWBLocalizer.DATA_READY_FAILED:
                    mActivity.get().tvAppend(mActivity.get().status, "data ready failed\n");
                    break;
                case UWBLocalizer.ANCHOR_ID_SEND:
                    mActivity.get().tvAppend(mActivity.get().status, "anchor id send\n");
                    break;
                case UWBLocalizer.ANCHOR_ID_RECEIVE:
                    short id = (Short)msg.obj;
                    mActivity.get().tvAppend(mActivity.get().status, "anchor id received id: " + String.valueOf(id) + "\n");
                    break;
                case UWBLocalizer.ANCHOR_ID_FAILED:
                    mActivity.get().tvAppend(mActivity.get().status, "anchor id failed\n");
                    break;
                case UWBLocalizer.ANCHOR_DIST_SEND:
                    mActivity.get().tvAppend(mActivity.get().status, "anchor dist send\n");
                    break;
                case UWBLocalizer.ANCHOR_DIST_RECEIVE:
                    float dist = (Float)msg.obj;
                    mActivity.get().tvAppend(mActivity.get().status, "anchor dist received distance: " + dist + "\n");
                    break;
                case UWBLocalizer.ANCHOR_DIST_FAIL:
                    mActivity.get().tvAppend(mActivity.get().status, "anchor dist failed\n");
                    break;
                case UWBLocalizer.TASK_DONE:
                    mActivity.get().tvAppend(mActivity.get().status, "task finished\n");
                    break;
            }
        }

    }
}

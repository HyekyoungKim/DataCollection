package org.androidtown.datacollection;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by dongdokee on 19/04/2019.
 */

public class UWBLocalizer {
    /* constants */
    private final byte CMD_NONE = 0;
    private final byte CMD_SCAN = 1;
    private final byte CMD_DATA_READY = 2;
    private final byte CMD_TYPE_ID = 3;
    private final byte CMD_TYPE_DIST = 4;

    private final byte TYPE_NONE = 0;
    private final byte TYPE_DATA_READY = 1;
    private final byte TYPE_ID = 2;
    private final byte TYPE_DIST = 3;

    private final byte SERIAL_NODATA = (byte)0b01111011;
    private final byte SERIAL_DATARD = (byte)0b10000011;

    private final short ID_NONE = 0;

    private final int NUM_ANCHORS = 5;

    private final int NUM_ANCHORS_USED = 4;
    private final int NUM_REPEAT = 5;
    private float x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4;
    private short ID1;    // At (x1, y1, z1)
    private short ID2;    // At (x2, y2, z2)
    private short ID3;    // At (x3, y3, z3)
    private short ID4;    // At (x4, y4, z4)
    private float x, y;

    public static final int TRIGER_SCAN = 0;
    public static final int SCAN_FAILED = 1;
    public static final int DATA_READY_SEND = 2;
    public static final int NO_DATA = 3;
    public static final int DATA_READY = 4;
    public static final int DATA_READY_FAILED = 5;
    public static final int ANCHOR_ID_SEND = 6;
    public static final int ANCHOR_ID_RECEIVE = 7;
    public static final int ANCHOR_ID_FAILED = 8;
    public static final int ANCHOR_DIST_SEND = 9;
    public static final int ANCHOR_DIST_RECEIVE = 10;
    public static final int ANCHOR_DIST_FAIL = 11;

    public static final int TASK_DONE = 100;

    private short[] validAnchors;
    private float[] validDistance;

    private Map anchorDistMap;


    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    public byte[] received_data;
    public boolean is_received = false;

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";

    Activity activity;
    Handler handler;

    public UWBLocalizer(Activity activity, Handler handler ) {
        this.activity = activity;
        usbManager = (UsbManager) activity.getSystemService(activity.USB_SERVICE);
        this.handler = handler;
    }

    /*
    public byte[] fetch() {
        is_received = false;
        return received_data;
    }*/

    // callback function for READ
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            /*
            //String data = null;
            try {
                //data = new String(arg0, "UTF-8");
                //data.concat("/n");

                is_received = true;
                received_data = arg0;

                handler.sendEmptyMessage(MainActivity.RECV_DATA);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "(AFTER)I Send Message", Toast.LENGTH_LONG).show();
                    }
                });

            }  catch (Exception e) {
                e.printStackTrace();
            }
            */

        }
    };

    // broadcast receiver for serial connection
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.syncOpen()) { //Set Serial Connection Parameters.
                            serialPort.setBaudRate(115200);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                begin();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                end();
            }
        }
    };

    public void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        activity.registerReceiver(broadcastReceiver, filter);
    }

    public void unregisterReceiver() {
        activity.unregisterReceiver(broadcastReceiver);
    }

    public void begin() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x1A86)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(activity, "Connection is made", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }

    public int send(byte[] bytes) {
        return serialPort.syncWrite(bytes, 100);
    }
    public int send(byte byte_) {
        return send(new byte[]{byte_});
    }
    public byte[] recv() {
        byte[] buffer = new byte[100];
        int n = serialPort.syncRead(buffer, 100);
        if (n > 0) {
            byte[] received = new byte[n];
            System.arraycopy(buffer, 0, received, 0, n);
            return received;
        }

        return null;
    }

    public void end() {
        serialPort.syncClose();
    }

    public void triggerScan() {
        if (send(CMD_SCAN) <= 0) {
            handler.obtainMessage(SCAN_FAILED);
            return;
        }
        handler.obtainMessage(TRIGER_SCAN).sendToTarget();
    }

    public boolean isReady() {
        byte[] received;
        handler.obtainMessage(DATA_READY_SEND).sendToTarget();
        if (send(CMD_DATA_READY) <= 0) {
            handler.obtainMessage(DATA_READY_FAILED, 0, 0).sendToTarget();
            return false;
        }

        received = recv();
        if (received == null) {
            return false;
        }

        if (received[0] == SERIAL_NODATA) {
            handler.obtainMessage(NO_DATA).sendToTarget();
            return false;
        }

        if (received[0] != SERIAL_DATARD) {
            handler.obtainMessage(DATA_READY_FAILED, 1, 1).sendToTarget();
            return false;
        }

        handler.obtainMessage(DATA_READY).sendToTarget();
        return true;
    }

    public short[] getAnchorIds () {
        byte[] data;
        short[] anchorId = new short[NUM_ANCHORS];

        handler.obtainMessage(ANCHOR_ID_SEND).sendToTarget();
        if (send(CMD_TYPE_ID) <= 0) {
            // fail
            handler.obtainMessage(ANCHOR_ID_FAILED).sendToTarget();
            return null;
        }
        data = recv();
        if (data == null) {
            // fail
            handler.obtainMessage(ANCHOR_ID_FAILED).sendToTarget();
            return null;
        }

        // convert a 2-byte long binary to integer
        // Arduino uses little endian
        for (int i = 0 ; i < NUM_ANCHORS ; i++) {
            anchorId[i] = ID_NONE;
            /* Arduino uses little endian */
            anchorId[i] = (short)((data[2 * i + 1] << 8) | data[2 * i + 0]);
            handler.obtainMessage(ANCHOR_ID_RECEIVE, new Short(anchorId[i])).sendToTarget();
        }

        return anchorId;
    }

    public float[] getDists() {
        byte[] data;
        float[] distance = new float[NUM_ANCHORS];

        handler.obtainMessage(ANCHOR_DIST_SEND).sendToTarget();
        if (send(CMD_TYPE_DIST) <= 0) {
            handler.obtainMessage(ANCHOR_DIST_FAIL).sendToTarget();
            return null;
        }
        data = recv();
        if (data == null) {
            handler.obtainMessage(ANCHOR_DIST_FAIL).sendToTarget();
            return null;
        }

        // little endian
        for (int i = 0 ; i < NUM_ANCHORS ; i++) {
            int float_binary = ((data[4 * i + 3] & 0x000000FF) << 24) | ((data[4 * i + 2] & 0x000000FF) << 16) | ((data[4 * i + 1] & 0x000000FF) << 8) | ((data[4 * i + 0] & 0x000000FF));
            distance[i] = Float.intBitsToFloat(float_binary);
            handler.obtainMessage(ANCHOR_DIST_RECEIVE, distance[i]).sendToTarget();
        }
        return distance;
    }

    public int readMeasurement(short[] anchorId, float[] distance) {
        int ret;

        if (!isReady()) {
            return -1;
        }

        short[] anchorId_ = getAnchorIds();
        if (anchorId_ == null) {
            return -1;
        }

        float[] distance_ = getDists();
        if (distance_ == null) {
            return -1;
        }

        int valid_num = 0;
        for (int i = 0 ; i < NUM_ANCHORS ; i++) {
            anchorId[i] = anchorId_[i];
            distance[i] = distance_[i];

            if ((anchorId[i] != 0) && (distance[i] != 0))
                valid_num++;
        }

        return valid_num;
    }


    public void getValidMeasurement(int valid_num, short[] anchorId, float[] distance) {
        validAnchors = new short[valid_num];
        validDistance = new float[valid_num];
        int idx = 0;
        for (int i = 0 ; i < NUM_ANCHORS ; i++) {
            if ((anchorId[i] != 0) && (distance[i] != 0)) {
                validAnchors[idx] = anchorId[i];
                validDistance[idx] = distance[i];
                idx++;
            }
        }
    }

    public void collectOne(Map<Short, List<Float>> map) {
        triggerScan();
        try {
            Thread.sleep(330);
        } catch (Exception e) {
            e.printStackTrace();
        }

        short[] anchorId = new short[NUM_ANCHORS];
        float[] distance = new float[NUM_ANCHORS];
        int valid_num = readMeasurement(anchorId, distance);

        if (valid_num < 0) {
            return;
        }

        getValidMeasurement(valid_num, anchorId, distance);

        // insert them to the map
        for (int i = 0 ; i < valid_num ; i++) {
            if (!map.containsKey(validAnchors[i]))
                //map.put(anchorId[i], new ArrayList<Float>());
                map.put(validAnchors[i], new ArrayList<Float>());

            //List list = map.get(anchorId[i]);
            List list = map.get(validAnchors[i]);
            list.add(validDistance[i]);
        }
    }

    public void localize_() {
        anchorDistMap = new HashMap<Short, List<Float>>();

        // collect data N times
        for (int i = 0 ; i < NUM_REPEAT ; i++) {
            collectOne(anchorDistMap);
        }

        short [] anchor_id = {ID1, ID2, ID3, ID4};
        float [] distance = new float[NUM_ANCHORS_USED];
        for (int i = 0 ; i < NUM_ANCHORS_USED; i++) {
            short id = anchor_id[i];
            List list = (List) anchorDistMap.get(id);
            float[] dist = new float[NUM_REPEAT];
            if (list != null) {
                int min = 0, max = 0;
                int num_records = list.size();
                if (num_records > 2) {
                    for (int j = 0; j < num_records; j++) {
                        Object object = list.get(j);
                        float d = ((Float) object).floatValue();
                        dist[j] = d;
                        if (j > 0) {
                            if (d < dist[min])
                                min = j;
                            else if (d > dist[max])
                                max = j;
                        }
                    }
                    float sum = 0;
                    for (int j = 0; j < num_records; j++) {
                        if (j != min && j != max)
                            sum += dist[j];
                    }
                    distance[i] = sum / (num_records - 2);
                    CollectionActivity.ENOUGH_DATA = true;
                } else {
                    CollectionActivity.ENOUGH_DATA = false;
                    break;
                }
            } else {
                CollectionActivity.ENOUGH_DATA = false;
                break;
            }
        }

        if (CollectionActivity.ENOUGH_DATA) {
            double[][] positions = new double[][] {{x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}, {x4, y4, z4}};
            double[] distances = new double[] {distance[0], distance[1], distance[2], distance[3]};
            Log.d("test", "distance[0]: "+distance[0]+"distance[1]: "+distance[1]+"distance[2]: "+distance[2]+"distance[3]: "+distance[3]);

            NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(
                    new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
            LeastSquaresOptimizer.Optimum optimum = solver.solve();

            double[] centroid = optimum.getPoint().toArray();
            x = (float) centroid[0];
            y = (float) centroid[1];
            Log.d("test", "centroid.length: "+centroid.length+", centroid[0]: "+centroid[0]+
                    ", centroid[1]: "+centroid[1]+", centroid[2]: "+centroid[2]);
        }
        CollectionActivity.LOCATION_READY = true;
    }

    public void localize() {
        new LocalizeTask(handler).execute();
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setID1(short ID1) {
        this.ID1 = ID1;
    }

    public void setID2(short ID2) {
        this.ID2 = ID2;
    }

    public void setID3(short ID3) {
        this.ID3 = ID3;
    }

    public void setID4(short ID4) {
        this.ID4 = ID4;
    }

    public void setPosition1(float x1, float y1, float z1) {
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
    }

    public void setPosition2(float x2, float y2, float z2) {
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
    }

    public void setPosition3(float x3, float y3, float z3) {
        this.x3 = x3;
        this.y3 = y3;
        this.z3 = z3;
    }

    public void setPosition4(float x4, float y4, float z4) {
        this.x4 = x4;
        this.y4 = y4;
        this.z4 = z4;
    }

    public class LocalizeTask extends AsyncTask {
        Handler handler;
        public LocalizeTask(Handler handler) {
            this.handler = handler;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            localize_();
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Object o) {
            // transmit distances to the server
            handler.obtainMessage(TASK_DONE).sendToTarget();
            super.onPostExecute(o);
        }
    }
}

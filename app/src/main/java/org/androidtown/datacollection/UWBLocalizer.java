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

    private final int NUM_ANCHORS_USED = 3;
    private final float U = 10;
    private final float Vx = 5;
    private final float Vy = 7;
    private final float [] ANCHOR_POSITION_X = {0, U, Vx};
    private final float [] ANCHOR_POSITION_Y = {0, 0, Vy};
    private final short ID1 = 1;    // At (0,0)
    private final short ID2 = 2;    // At (U,0)
    private final short ID3 = 3;    // At (Vx,Vy)
    private final short [] ANCHOR_ID = {ID1, ID2, ID3};
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
                validAnchors[idx] = validAnchors[i];
                validDistance[idx] = validDistance[i];
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
                map.put(anchorId[i], new ArrayList<Float>());

            List list = map.get(anchorId[i]);
            list.add(validDistance[i]);
        }
    }

    public void localize_() {
        anchorDistMap = new HashMap<Short, List<Float>>();

        // collect data N times
        for (int i = 0 ; i < 5 ; i++) {
            collectOne(anchorDistMap);
        }

        float [] distance = new float[NUM_ANCHORS_USED];
        for (int i = 0 ; i < NUM_ANCHORS_USED; i++) {
            short id = ANCHOR_ID[i];
            List list = (List) anchorDistMap.get(id);
            if (list != null) {
                int count = 0;
                float sum = 0;
                for (int j = 0; j < 5; j++) {
                    float d = ((Float) list.get(i)).floatValue();
                    if (d != 0) {
                        count++;
                        sum += d;
                    }
                }
                distance[i] = sum / count;
                Log.d("test", "distance: " + distance[i]);
            }
        }

        Random random = new Random();
        float r1 = random.nextFloat() * random.nextInt(20);
        float r2 = random.nextFloat() * random.nextInt(20);
        float r3 = random.nextFloat() * random.nextInt(20);

//        float r1 = distance[0];
//        float r2 = distance[1];
//        float r3 = distance[2];
        /** True Range Multilateration
         *  Reference:
         *  https://en.wikipedia.org/wiki/True_range_multilateration#Three_Cartesian_dimensions,_three_measured_slant_ranges
         *  Assume that all anchors and tag are at the same height
         */
        x = (float) (Math.pow(r1,2) - Math.pow(r2,2) + Math.pow(U,2))
                / (2 * U);
        y = (float) (Math.pow(r1,2) - Math.pow(r3,2) + (Math.pow(Vx,2) + Math.pow(Vy,2)) - 2 * Vx * x)
                / (2 * Vy);
        CollectionActivity.LOCATION_READY = true;
        Log.d("test", "x: " + x + " y: " + y);
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

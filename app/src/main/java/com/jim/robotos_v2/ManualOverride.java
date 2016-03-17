package com.jim.robotos_v2;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.jim.robotos_v2.Utilities.Bluetooth;
import com.jim.robotos_v2.Utilities.Utilities;

public class ManualOverride extends AppCompatActivity {

    String command = "STOP";
    ImageView ivForward, ivBackward, ivRight, ivLeft, ivBluetooth, ivDirection;
    static TextView tvDistance;
    Bluetooth bt;
    private static StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_override);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeGraphicComponents();

        bt = new Bluetooth(this, mHandler);
        connectService();

        ivForward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        command = "FORWARD";
                        Utilities.setDirectionImage(command, ivDirection, bt);
                        return true;
                    case MotionEvent.ACTION_UP:
                        command = "STOP";
                        Utilities.setDirectionImage(command, ivDirection, bt);
                        return true;
                }

                return false;
            }
        });

        ivBackward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        command = "BACKWARD";
                        Utilities.setDirectionImage(command, ivDirection, bt);
                        return true;
                    case MotionEvent.ACTION_UP:
                        command = "STOP";
                        Utilities.setDirectionImage(command, ivDirection, bt);
                        return true;
                }
                return false;
            }
        });

        ivLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        command = "LEFT";
                        Utilities.setDirectionImage(command, ivDirection, bt);
                        return true;
                    case MotionEvent.ACTION_UP:
                        command = "STOP";
                        Utilities.setDirectionImage(command, ivDirection, bt);
                        return true;
                }
                return false;
            }
        });

        ivRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        command = "RIGHT";
                        Utilities.setDirectionImage(command, ivDirection, bt);
                        return true;
                    case MotionEvent.ACTION_UP:
                        command = "STOP";
                        Utilities.setDirectionImage(command, ivDirection, bt);
                        return true;
                }
                return false;
            }
        });
    }

    private void initializeGraphicComponents() {
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        ivBluetooth = (ImageView) findViewById(R.id.ivBluetooth);
        ivDirection = (ImageView) findViewById(R.id.ivDirection);
        ivForward = (ImageView) findViewById(R.id.ivForward);
        ivBackward = (ImageView) findViewById(R.id.ivBackward);
        ivRight = (ImageView) findViewById(R.id.ivRight);
        ivLeft = (ImageView) findViewById(R.id.ivLeft);
    }

    public void connectService() {
        try {
            ivBluetooth.setImageResource(R.drawable.connecting);
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter.isEnabled()) {
                bt.start();
                bt.connectDevice("HC-05");///device name
                Log.d("BLUETOOTH", "Btservice started - listening");
                ivBluetooth.setImageResource(R.drawable.connected);
            } else {
                Log.w("BLUETOOTH", "Btservice started - bluetooth is not enabled");
                ivBluetooth.setImageResource(R.drawable.disabled);
            }
        } catch (Exception e) {
            Log.e("BLUETOOTH", "Unable to start bt ", e);
            ivBluetooth.setImageResource(R.drawable.disconnected);
        }
    }

    static String TAG = "HANDLER";

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case Bluetooth.MESSAGE_STATE_CHANGE:
                    Log.d(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    break;
                case Bluetooth.MESSAGE_WRITE:
                    Log.d(TAG, "MESSAGE_WRITE ");
                    break;
                case Bluetooth.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String strIncom = new String(readBuf, 0, msg.arg1);
                    sb.append(strIncom);
                    int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                    if (endOfLineIndex > 0) {                                            // if end-of-line,
                        String sbprint = sb.substring(0, endOfLineIndex);               // extract string
                        sb.delete(0, sb.length());                                      // and clear
                        Log.d("READ_FROM_ARDUINO", sbprint);
                        tvDistance.setText(sbprint + "cm");
                    }
                    break;
                case Bluetooth.MESSAGE_DEVICE_NAME:
                    Log.d(TAG, "MESSAGE_DEVICE_NAME " + msg);
                    break;
                case Bluetooth.MESSAGE_TOAST:
                    Log.d(TAG, "MESSAGE_TOAST " + msg);
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utilities.setDirectionImage("STOP", ivDirection, bt);
        bt.stop();
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Utilities.setDirectionImage("STOP", ivDirection, bt);
        bt.stop();
        finish();
    }
}

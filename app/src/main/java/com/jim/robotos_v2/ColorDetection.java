package com.jim.robotos_v2;

import android.bluetooth.BluetoothAdapter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.jim.robotos_v2.ComputerVision.ColorBlobDetector;
import com.jim.robotos_v2.Utilities.Bluetooth;
import com.jim.robotos_v2.Utilities.Preferences;
import com.jim.robotos_v2.Utilities.Utilities;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class ColorDetection extends AppCompatActivity implements View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "ColorDetection";

    private boolean mIsColorSelected = false;
    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private ColorBlobDetector mDetector;
    private Mat mSpectrum;
    private Size SPECTRUM_SIZE;
    private org.opencv.core.Point center;
    private Bluetooth bt;
    private double bottomLineHeight, cameraViewHeight = 0, cameraViewWidth = 0, leftLineWidth = 0, rightLineWidth = 0;
    private Thread directionThread;
    private static StringBuilder sb = new StringBuilder();
    private ImageView ivDirection, ivBluetooth, ivDetectionColor;
    private static TextView tvDistance;
    private Rect temp;
    private String command = "STOP";
    private org.opencv.core.Point rectTopLeft;
    private static int distanceToObject = 200000;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(ColorDetection.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_detection);

        initializeGraphicComponents();

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.jcvColorDetection);
        mOpenCvCameraView.setCvCameraViewListener(this);

        bt = new Bluetooth(this, mHandler);
        connectService();

        directionThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (!Thread.interrupted())
                    try {
                        Log.d(TAG, "run: " + Thread.interrupted());
                        Thread.sleep(Preferences.loadPrefsInt("COMMUNICATION_LOOP_REPEAT_TIME", 100, getApplicationContext()));
                        runOnUiThread(new Runnable() // start actions in UI thread
                        {

                            @Override
                            public void run() {
                                Log.d(TAG, "run: 2" + Thread.interrupted());
                                if (mIsColorSelected)
                                    command = Utilities.giveDirectionColorDetectionVersion2(center, distanceToObject, bottomLineHeight, leftLineWidth, rightLineWidth, ivDirection, bt, getApplicationContext(), command);
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e(TAG, "run: Direction Thread interrupted");
                        break;
                    }
            }
        });

        directionThread.start();

    }

    private void initializeGraphicComponents() {

        ivDetectionColor = (ImageView) findViewById(R.id.ivDetectionColor);
        ivDirection = (ImageView) findViewById(R.id.ivDirection);
        ivBluetooth = (ImageView) findViewById(R.id.ivBluetooth);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        center = null;
        bottomLineHeight = (double) Preferences.loadPrefsInt("BOTTOM_LINE_VALUE", 500, getApplicationContext());
        cameraViewHeight = (double) mOpenCvCameraView.getHeight();
        cameraViewWidth = (double) mOpenCvCameraView.getWidth();
        leftLineWidth = (double) 1 * (cameraViewWidth / 4);
        rightLineWidth = (double) 2 * (cameraViewWidth / 4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();

            try {
                if (!contours.isEmpty()) {
                    temp = Imgproc.boundingRect(contours.get(0));
                    Core.rectangle(mRgba, temp.tl(), temp.br(), new Scalar(238, 233, 60), 3);

                    rectTopLeft = temp.tl();

                    center = new org.opencv.core.Point(rectTopLeft.x + (temp.width / 2), rectTopLeft.y + (temp.height / 2));

                    Core.circle(mRgba, center, 4, new Scalar(128, 255, 0));
                }
            } catch (Exception e) {
                e.printStackTrace();
                center = new org.opencv.core.Point(-1, -1);
            }


            Core.line(mRgba, new org.opencv.core.Point(leftLineWidth, cameraViewHeight), new org.opencv.core.Point(leftLineWidth, 0), new Scalar(255, 0, 0, 255), 5);

            Core.line(mRgba, new org.opencv.core.Point(rightLineWidth, cameraViewHeight), new org.opencv.core.Point(rightLineWidth, 0), new Scalar(255, 0, 0, 255), 5);

            Core.line(mRgba, new org.opencv.core.Point(cameraViewWidth, bottomLineHeight), new org.opencv.core.Point(0, bottomLineHeight), new Scalar(154, 189, 47), 6);


        }


        return mRgba;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x > 4) ? x - 4 : 0;
        touchedRect.y = (y > 4) ? y - 4 : 0;

        touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = Utilities.convertScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        ivDetectionColor.setBackgroundColor(Color.rgb((int) mBlobColorRgba.val[0], (int) mBlobColorRgba.val[1], (int) mBlobColorRgba.val[2]));

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        Utilities.setDirectionImage("STOP", ivDirection, bt);
        bt.stop();
        directionThread.interrupt();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        Utilities.setDirectionImage("STOP", ivDirection, bt);
        bt.stop();
        directionThread.interrupt();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        Utilities.setDirectionImage("STOP", ivDirection, bt);
        bt.stop();
        directionThread.interrupt();
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
                        //  Log.d("READ_FROM_ARDUINO", sbprint);
                        tvDistance.setText(sbprint + "cm");
                        try {
                            distanceToObject = Integer.parseInt(sbprint);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("ERROR", "could not parse string to integer");
                        }
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

    public void detectionColorClicked(View view) {

        final Scalar tempBlobColorRgba = mBlobColorRgba;

        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.new_color_selection)
                .titleColor(getResources().getColor(R.color.colorAccent))
                .customView(R.layout.dialog_color_define, true)
                .positiveText(R.string.ok)
                .backgroundColorRes(R.color.background)
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        mBlobColorRgba.val[0] = tempBlobColorRgba.val[0];
                        mBlobColorRgba.val[1] = tempBlobColorRgba.val[1];
                        mBlobColorRgba.val[2] = tempBlobColorRgba.val[2];
                        mBlobColorRgba.val[3] = tempBlobColorRgba.val[3];
                        ivDetectionColor.setBackgroundColor(Color.rgb((int) mBlobColorRgba.val[0], (int) mBlobColorRgba.val[1], (int) mBlobColorRgba.val[2]));
                        mDetector.setHsvColor(Utilities.convertScalarRgba2Hsv(mBlobColorRgba));
                        mIsColorSelected = true;
                    }
                }).build();

        final EditText etRed = (EditText) dialog.getCustomView().findViewById(R.id.etRed);
        final EditText etGreen = (EditText) dialog.getCustomView().findViewById(R.id.etGreen);
        final EditText etBlue = (EditText) dialog.getCustomView().findViewById(R.id.etBlue);
        final ImageView ivColor = (ImageView) dialog.getCustomView().findViewById(R.id.ivDetectionColorDialog);
        final EditText etAlpha = (EditText) dialog.getCustomView().findViewById(R.id.etAlpha);

        ivColor.setBackgroundColor(Color.argb((int) mBlobColorRgba.val[3], (int) mBlobColorRgba.val[0], (int) mBlobColorRgba.val[1], (int) mBlobColorRgba.val[2]));

        etRed.setText("" + (int) mBlobColorRgba.val[0]);
        etGreen.setText("" + (int) mBlobColorRgba.val[1]);
        etBlue.setText("" + (int) mBlobColorRgba.val[2]);
        etAlpha.setText("" + (int) mBlobColorRgba.val[3]);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            etRed.setTextColor(getColor(R.color.red_area));
            etGreen.setTextColor(getColor(R.color.green));
            etBlue.setTextColor(getColor(R.color.blue));
            etAlpha.setTextColor(getColor(R.color.grey));
        }

        etRed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                try {
                    tempBlobColorRgba.val[0] = Double.parseDouble(etRed.getText().toString());
                    ivColor.setBackgroundColor(Color.argb((int) tempBlobColorRgba.val[3], (int) tempBlobColorRgba.val[0], (int) tempBlobColorRgba.val[1], (int) tempBlobColorRgba.val[2]));
                } catch (Exception e) {
                    e.printStackTrace();
                    ivColor.setBackgroundColor(Color.BLACK);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etGreen.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                try {
                    tempBlobColorRgba.val[1] = Double.parseDouble(etGreen.getText().toString());
                    ivColor.setBackgroundColor(Color.argb((int) tempBlobColorRgba.val[3], (int) tempBlobColorRgba.val[0], (int) tempBlobColorRgba.val[1], (int) tempBlobColorRgba.val[2]));
                } catch (Exception e) {
                    e.printStackTrace();
                    ivColor.setBackgroundColor(Color.BLACK);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etBlue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                try {
                    tempBlobColorRgba.val[2] = Double.parseDouble(etBlue.getText().toString());
                    ivColor.setBackgroundColor(Color.argb((int) tempBlobColorRgba.val[3], (int) tempBlobColorRgba.val[0], (int) tempBlobColorRgba.val[1], (int) tempBlobColorRgba.val[2]));
                } catch (Exception e) {
                    e.printStackTrace();
                    ivColor.setBackgroundColor(Color.BLACK);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etAlpha.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                try {
                    tempBlobColorRgba.val[1] = Double.parseDouble(etAlpha.getText().toString());
                    ivColor.setBackgroundColor(Color.argb((int) tempBlobColorRgba.val[3], (int) tempBlobColorRgba.val[0], (int) tempBlobColorRgba.val[1], (int) tempBlobColorRgba.val[2]));
                } catch (Exception e) {
                    e.printStackTrace();
                    ivColor.setBackgroundColor(Color.BLACK);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        View positiveAction = dialog.getActionButton(DialogAction.POSITIVE);

        dialog.show();

        positiveAction.setEnabled(true);


    }
}

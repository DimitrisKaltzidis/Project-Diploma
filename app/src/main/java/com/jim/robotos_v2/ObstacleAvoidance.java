package com.jim.robotos_v2;

import android.bluetooth.BluetoothAdapter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
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
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.jim.robotos_v2.ComputerVision.ColorBlobDetector;
import com.jim.robotos_v2.RouteLogic.Point;
import com.jim.robotos_v2.RouteLogic.Route;
import com.jim.robotos_v2.Utilities.Bluetooth;
import com.jim.robotos_v2.Utilities.MapUtilities;
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
import java.util.Locale;

public class ObstacleAvoidance extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleMap.OnMapClickListener, SensorEventListener, View.OnLongClickListener, /*View.OnTouchListener,*/ CameraBridgeViewBase.CvCameraViewListener2 {
    private GoogleMap mMap;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private SensorManager mSensorManager;
    private float compassBearingDegrees = 0f;
    private float currentDegree = 0f, currentDegreeNorth = 0f;//for the image rotation
    private ImageView ivCompass, ivDirection, ivCompassNorth, ivPlayStop, ivBluetooth, ivAddToRoute, ivDetectionColor;
    private TextView tvDistance;
    private Location robotLocation;
    private Route route;
    private boolean running = false;
    private Marker robotMarker = null;
    private String command = "STOP";
    private TextToSpeech textToSpeech;
    private Bluetooth bt;
    private Thread directionThread;
    private static StringBuilder sb = new StringBuilder();
    private int distanceToObstacle = 2000;
    private float obstacleCompassDegrees, obstacleAvoidanceDegrees;
    private Point systemDefinedPoint;
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private ColorBlobDetector mDetector;
    private Mat mSpectrum;
    private Size SPECTRUM_SIZE;
    private org.opencv.core.Point center, topLeft, topRight, bottomLeft, bottomRight, topMiddle, bottomMiddle;
    private boolean mIsColorSelected = false;
    private double cameraViewHeight;
    private double cameraViewWidth;
    private int contourColor, pointColor, smallAreaColor, bigAreaColor;
    private int areaLeft, areaRight;
    private String previousCommand = "STOP";
    private Sensor gSensor;
    private Sensor mSensor;

    private String mode = "PATH";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    // mOpenCvCameraView.setOnTouchListener(ObstacleAvoidance.this);
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
        setContentView(R.layout.activity_obstacle_avoidance);


        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.jcvColorDetection);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int cols = mRgba.cols();
                int rows = mRgba.rows();

                int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
                int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

                int x = (int) event.getX() - xOffset;
                int y = (int) event.getY() - yOffset;

                Log.i("TouchEvent", "Touch image coordinates: (" + event.getX() + ", " + event.getY() + ")" + " downTime: " + event.getDownTime() + " eventTime: " + event.getEventTime() + " action: " + event.getAction() + " xOffset: " + xOffset + " yOffset: " + yOffset + " pressure: " + event.getPressure() + " size: " + event.getSize() + " metaState: " + event.getMetaState() + " xPrecision: " + event.getXPrecision() + " yPrecision: " + event.getYPrecision() + " deviceID: " + event.getDeviceId() + " edgeFlags: " + event.getEdgeFlags());

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

                //Toast.makeText(getApplicationContext(), "Touched", Toast.LENGTH_LONG).show();
                Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                        ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

                ivDetectionColor.setBackgroundColor(Color.rgb((int) mBlobColorRgba.val[0], (int) mBlobColorRgba.val[1], (int) mBlobColorRgba.val[2]));

                mDetector.setHsvColor(mBlobColorHsv);

                Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

                mIsColorSelected = true;

                touchedRegionRgba.release();
                touchedRegionHsv.release();

                // Log.d("SCAN", "PLATOS - IPSOS: " + v.getWidth() + " " + v.getHeight());
                return true;
            }
        });
        initializeGraphicComponents();

        route = new Route();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentMap);
        mapFragment.getMapAsync(this);

        mLocationRequest = Utilities.createLocationRequest(getResources());

        // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(AppIndex.API).build();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        gSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        bt = new Bluetooth(this, mHandler);
        connectService();

        ivAddToRoute.setOnLongClickListener(this);

        ///lathos topothetisi den kerdizw kati apo to thread
        directionThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (!Thread.interrupted())
                    try {
                        Thread.sleep(Preferences.loadPrefsInt("COMMUNICATION_LOOP_REPEAT_TIME", 300, getApplicationContext()));
                        runOnUiThread(new Runnable() // start actions in UI thread
                        {

                            @Override
                            public void run() {
                                if (robotLocation != null && (!route.isEmpty()) && running && textToSpeech != null) {

                                    if (distanceToObstacle < Preferences.loadPrefsInt("OBSTACLE_DETECTION_RANGE", 100, getApplicationContext())) {
                                        mode = "OBSTACLE";
                                    }

                                    if (mode.equals("PATH")) {
                                        command = Utilities.giveDirection(compassBearingDegrees, ivDirection, ivCompass, route, robotLocation, getApplicationContext(), command, mMap, getResources(), tvDistance, textToSpeech, bt);
                                        previousCommand = command;
                                        mIsColorSelected = false;

                                    } else if (mode.equals("OBSTACLE")) {

                                        if (!mIsColorSelected) {
                                            long downTime = SystemClock.uptimeMillis();
                                            long eventTime = SystemClock.uptimeMillis() + 100;

                                            Utilities.setDirectionImage("STOP", ivDirection, bt);
                                            previousCommand = "STOP";
                                            MotionEvent motionEvent = MotionEvent.obtain(downTime, eventTime, 0,
                                                    225.0f, 225.0f, 0.5625f, 0.26666668f,
                                                    0, 1.0f, 1.0f,
                                                    4, 0);
                                            obstacleCompassDegrees = compassBearingDegrees;
                                            mOpenCvCameraView.dispatchTouchEvent(motionEvent);
                                        }
                                    }


                                    // END OF PATH REACHED - FINISH PROGRAM
                                    if (command.equals("FINISH")) {
                                        mMap.clear();
                                        tvDistance.setText("---m");
                                        if (running)
                                            running = Utilities.playStopButtonHandler(route, running, ivPlayStop, getApplicationContext());

                                        route.clearRoute();


                                        Utilities.setDirectionImage("STOP", ivDirection, bt);
                                    }
                                }
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
        ivCompass = (ImageView) findViewById(R.id.ivBearing);
        ivDirection = (ImageView) findViewById(R.id.ivDirection);
        ivCompassNorth = (ImageView) findViewById(R.id.ivCompassNorth);
        ivPlayStop = (ImageView) findViewById(R.id.ivPlayStop);
        ivBluetooth = (ImageView) findViewById(R.id.ivBluetooth);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        ivAddToRoute = (ImageView) findViewById(R.id.ivAddPointToPath);
        ivDetectionColor = (ImageView) findViewById(R.id.ivDetectionColor);
    }


    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(Utilities.getMapType(this));
        mMap.setOnMapClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected())
            startLocationUpdates();

        mSensorManager.registerListener(this, gSensor,
                SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(
                this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_UI);


        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.sendMessage(Integer.toString(3));
        bt.stop();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (mGoogleApiClient.isConnected())
            stopLocationUpdates();

        mSensorManager.unregisterListener(this);

        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
    }


    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        bt.sendMessage(Integer.toString(3));
        bt.stop();
        finish();
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        robotLocation = location;
        robotMarker = MapUtilities.placeRobotMarkerOnMap(robotMarker, mMap, Utilities.convertLocationToLatLng(robotLocation), true, getResources(), getApplicationContext());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onMapClick(LatLng latLng) {
        route.addPoint(new Point(latLng, "Point " + route.getPointsNumber(), false));
        MapUtilities.drawPathOnMap(mMap, route, getResources());
        robotMarker = MapUtilities.placeRobotMarkerOnMap(robotMarker, mMap, Utilities.convertLocationToLatLng(robotLocation), true, getResources(), getApplicationContext());
    }

    int counter = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (robotLocation != null && (!route.isEmpty()) && running && textToSpeech != null) {

            //Legacy compass sensor code
            //compassBearingDegrees = Utilities.correctCompassBearing(Math.round(event.values[0]), robotLocation);

            float azimuth = 0;

            azimuth = Utilities.landscapeModeCompassCalibration(event);
            compassBearingDegrees = Utilities.correctCompassBearing(azimuth, robotLocation);

            currentDegree = Utilities.compassAnimationHandler(ivCompass, compassBearingDegrees, currentDegree);
            currentDegreeNorth = Utilities.compassNorthIconHandler(ivCompassNorth, compassBearingDegrees, currentDegreeNorth);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //old shit not in use
    }

    public void showMyLocationClicked(View view) {
        robotMarker = MapUtilities.placeRobotMarkerOnMap(robotMarker, mMap, Utilities.convertLocationToLatLng(robotLocation), true, getResources(), getApplicationContext());

    }

    public void clearRouteClicked(View view) {
        mMap.clear();
        tvDistance.setText("---m");
        if (running)
            running = Utilities.playStopButtonHandler(route, running, ivPlayStop, this);

        route.clearRoute();

        Utilities.setDirectionImage("STOP", ivDirection, bt);
    }

    public void addMyLocationToRoute(View view) {
        if (!running) {
            route.addPoint(new Point(new LatLng(robotLocation.getLatitude(), robotLocation.getLongitude()), "Point " + route.getPointsNumber(), false));
            MapUtilities.drawPathOnMap(mMap, route, getResources());
            robotMarker = MapUtilities.placeRobotMarkerOnMap(robotMarker, mMap, Utilities.convertLocationToLatLng(robotLocation), true, getResources(), getApplicationContext());
        }
    }

    public void playButtonClicked(View view) {
        running = Utilities.playStopButtonHandler(route, running, ivPlayStop, this);
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
                        sb.delete(0, sb.length());   // and clear


                        Log.d("READ_FROM_ARDUINO", sbprint + "");
                        try {

                            distanceToObstacle = Utilities.normalizeReadingsFromDistanceSensor(Integer.parseInt(sbprint), distanceToObstacle);

                            Log.d("READ_FROM_ARDUINO_NORM", distanceToObstacle + "");
                            tvDistance.setText(distanceToObstacle + "cm");
                            //distanceToObstacle = Integer.parseInt(sbprint);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "handleMessage: CRASH CONVERSION");
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

    @Override
    public boolean onLongClick(View v) {

        final Point pointToAdd = new Point(new LatLng(-31.90, 115.86), "Point " + route.getPointsNumber(), false);

        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.new_point)
                .titleColor(getResources().getColor(R.color.colorAccent))
                .customView(R.layout.dialog_new_point_define, true)
                .positiveText(R.string.add)
                .backgroundColorRes(R.color.background)
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (pointToAdd.getPosition().latitude != -31.90 && pointToAdd.getPosition().longitude != 115.86) {
                            route.addPoint(pointToAdd);
                            MapUtilities.drawPathOnMap(mMap, route, getResources());
                            robotMarker = MapUtilities.placeRobotMarkerOnMap(robotMarker, mMap, Utilities.convertLocationToLatLng(robotLocation), true, getResources(), getApplicationContext());
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getApplicationContext(), "Please define coordinates for the new Point", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).build();

        final EditText etLatitude = (EditText) dialog.getCustomView().findViewById(R.id.etLatitude);
        final EditText etLongitude = (EditText) dialog.getCustomView().findViewById(R.id.etLongitude);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            etLatitude.setTextColor(getColor(R.color.green));
            etLongitude.setTextColor(getColor(R.color.colorPrimaryDark));
        }

        etLatitude.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                pointToAdd.setPosition(new LatLng(Double.parseDouble(s.toString()), pointToAdd.getPosition().longitude));
            }
        });

        etLongitude.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {


            }

            @Override
            public void afterTextChanged(Editable s) {
                pointToAdd.setPosition(new LatLng(pointToAdd.getPosition().latitude, Double.parseDouble(s.toString())));
            }
        });

        dialog.show();
        return false;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        cameraViewHeight = (double) mOpenCvCameraView.getHeight();
        cameraViewWidth = (double) mOpenCvCameraView.getWidth();
        center = null;
        topLeft = null;
        topRight = null;
        bottomLeft = null;
        bottomRight = null;
        bottomMiddle = null;
        topMiddle = null;
        contourColor = getResources().getColor(R.color.red_soft);
        pointColor = getResources().getColor(R.color.lime);
        bigAreaColor = getResources().getColor(R.color.red_area);
        smallAreaColor = getResources().getColor(R.color.green_area);
        systemDefinedPoint = null;
    }

    @Override
    public void onCameraViewStopped() {

    }

    private Rect temp;
    private org.opencv.core.Point rectTopLeft;
    private double leftLineWidth = 0;


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        //Log.d("SCAN", "PLATOS - IPSOS: " + mRgba.width() + " " + mRgba.height());
        if (mIsColorSelected) {
            mDetector.process(mRgba);
            final List<MatOfPoint> contours = mDetector.getContours();
            Log.e("IF", "<" + contours.isEmpty() + ">");
            try {
                if (!contours.isEmpty()) {
                    temp = Imgproc.boundingRect(contours.get(0));
                    //Core.rectangle(mRgba, temp.tl(), temp.br(), new Scalar(Color.red(contourColor), Color.green(contourColor), Color.blue(contourColor)), 3);

                    //Top Left of detected color area
                    rectTopLeft = temp.tl();

                    // Calculate detect object edges
                    center = new org.opencv.core.Point(rectTopLeft.x + (temp.width / 2), rectTopLeft.y + (temp.height / 2));
                    topLeft = new org.opencv.core.Point(rectTopLeft.x, rectTopLeft.y);
                    topRight = new org.opencv.core.Point(rectTopLeft.x + temp.width, rectTopLeft.y);
                    bottomLeft = new org.opencv.core.Point(rectTopLeft.x, rectTopLeft.y + temp.height);
                    bottomRight = new org.opencv.core.Point(rectTopLeft.x + temp.width, rectTopLeft.y + temp.height);

                    if (topLeft.x < mRgba.width() / 2 && topRight.x > mRgba.width() / 2) {
                        topMiddle = new org.opencv.core.Point(mRgba.width() / 2, rectTopLeft.y);
                        bottomMiddle = new org.opencv.core.Point(mRgba.width() / 2, rectTopLeft.y + temp.height);
                        Core.circle(mRgba, topMiddle, 7, new Scalar(Color.red(pointColor), Color.green(pointColor), Color.blue(pointColor)), -1);
                        Core.circle(mRgba, bottomMiddle, 7, new Scalar(Color.red(pointColor), Color.green(pointColor), Color.blue(pointColor)), -1);

                        areaLeft = (int) Utilities.calculateDistanceBetweenTwoPoints(topLeft, topMiddle) * temp.height;
                        areaRight = (int) Utilities.calculateDistanceBetweenTwoPoints(topMiddle, topRight) * temp.height;

                        if (areaRight > areaLeft) {
                            Core.rectangle(mRgba, topMiddle, bottomRight, new Scalar(Color.red(bigAreaColor), Color.green(bigAreaColor), Color.blue(bigAreaColor), Color.alpha(bigAreaColor)), -3);
                            Core.rectangle(mRgba, topLeft, bottomMiddle, new Scalar(Color.red(smallAreaColor), Color.green(smallAreaColor), Color.blue(smallAreaColor), Color.alpha(smallAreaColor)), -3);
                            Utilities.giveDirectionObstacleAvoidance(ivDirection, bt, "LEFT", previousCommand);
                            previousCommand = "LEFT";
                        } else if (areaRight < areaLeft) {
                            Core.rectangle(mRgba, topMiddle, bottomRight, new Scalar(Color.red(smallAreaColor), Color.green(smallAreaColor), Color.blue(smallAreaColor), Color.alpha(smallAreaColor)), -3);
                            Core.rectangle(mRgba, topLeft, bottomMiddle, new Scalar(Color.red(bigAreaColor), Color.green(bigAreaColor), Color.blue(bigAreaColor), Color.alpha(bigAreaColor)), -3);
                            Utilities.giveDirectionObstacleAvoidance(ivDirection, bt, "RIGHT", previousCommand);
                            previousCommand = "RIGHT";
                        } else if (areaRight == areaLeft) {
                            Core.rectangle(mRgba, topMiddle, bottomRight, new Scalar(Color.red(bigAreaColor), Color.green(bigAreaColor), Color.blue(bigAreaColor), Color.alpha(bigAreaColor)), -3);
                            Core.rectangle(mRgba, topLeft, bottomMiddle, new Scalar(Color.red(bigAreaColor), Color.green(bigAreaColor), Color.blue(bigAreaColor), Color.alpha(bigAreaColor)), -3);

                            if (Utilities.calculateDistanceBetweenTwoPoints(topLeft, topMiddle) == mRgba.width() / 2) {//full screen
                                Utilities.giveDirectionObstacleAvoidance(ivDirection, bt, "BACKWARD", previousCommand);
                                previousCommand = "BACKWARD";
                            } else {/// an den pianei oli tin othoni
                                Utilities.giveDirectionObstacleAvoidance(ivDirection, bt, "RIGHT", previousCommand);
                                previousCommand = "RIGHT";
                            }
                        }

                    } else {

                        topMiddle = null;
                        bottomMiddle = null;
                        areaRight = 0;
                        areaLeft = 0;
                        Core.rectangle(mRgba, temp.tl(), temp.br(), new Scalar(Color.red(contourColor), Color.green(contourColor), Color.blue(contourColor)), -3);

                        if (topLeft.x < mRgba.width() / 2 && topRight.x < mRgba.width() / 2) {
                            Utilities.giveDirectionObstacleAvoidance(ivDirection, bt, "RIGHT", previousCommand);
                            previousCommand = "RIGHT";
                        } else if (topLeft.x > mRgba.width() / 2 && topRight.x > mRgba.width() / 2) {
                            Utilities.giveDirectionObstacleAvoidance(ivDirection, bt, "LEFT", previousCommand);
                            previousCommand = "LEFT";
                        }

                    }


                    // Draw points
                    Core.circle(mRgba, center, 7, new Scalar(Color.red(pointColor), Color.green(pointColor), Color.blue(pointColor)), -1);
                    Core.circle(mRgba, topLeft, 7, new Scalar(Color.red(pointColor), Color.green(pointColor), Color.blue(pointColor)), -1);
                    Core.circle(mRgba, topRight, 7, new Scalar(Color.red(pointColor), Color.green(pointColor), Color.blue(pointColor)), -1);
                    Core.circle(mRgba, bottomLeft, 7, new Scalar(Color.red(pointColor), Color.green(pointColor), Color.blue(pointColor)), -1);
                    Core.circle(mRgba, bottomRight, 7, new Scalar(Color.red(pointColor), Color.green(pointColor), Color.blue(pointColor)), -1);


                    // Core.line(mRgba, new org.opencv.core.Point(mRgba.width() / 2, 0), new org.opencv.core.Point(mRgba.width() / 2, mRgba.height()), new Scalar(255, 0, 0, 255), 5);
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //  Utilities.giveDirectionObstacleAvoidance(ivDirection, bt, "STOP", previousCommand);
                            //  previousCommand = "STOP";
                            // Toast.makeText(getApplicationContext(), "Took place", Toast.LENGTH_SHORT).show();
                            Log.e("ELSE", "<" + contours.isEmpty() + ">>>>1>");
                            mIsColorSelected = false;
                            obstacleAvoidanceDegrees = compassBearingDegrees;
                            for (Point point : route.getRoute()) {
                                Log.d("Route", "BEFORE " + point.getName() + " " + "Position-1: " + point.getName() + " System defined " + point.isSystemDefined() + " Visited: " + point.isVisited());

                            }
                            systemDefinedPoint = Utilities.calculateObstacleAvoidingPoint(obstacleAvoidanceDegrees, obstacleCompassDegrees, distanceToObstacle, robotLocation, Preferences.loadPrefsFloat("OBSTACLE_AVOIDING_MAP_ERROR_METERS", 1f, getApplicationContext()), getApplicationContext());
                            route.addPointPosition(route.getNextPointPosition(), systemDefinedPoint);
                            for (Point point : route.getRoute()) {
                                Log.d("Route", "AFTER" + point.getName() + " " + "Position-1: " + point.getName() + " System defined " + point.isSystemDefined() + " Visited: " + point.isVisited());

                            }
                            MapUtilities.drawPathOnMap(mMap, route, getResources());
                            mode = "PATH";
                        }
                    });

                    /// ADD TO ROUTE

                }
            } catch (Exception e) {
                e.printStackTrace();
                center = new org.opencv.core.Point(-1, -1);
            }

        }


        return mRgba;
    }

}

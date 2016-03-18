package com.jim.robotos_v2;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FaceRecognition extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleMap.OnMapClickListener, SensorEventListener, View.OnLongClickListener {

    // Face detection
    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier cascadeClassifier;
    private Mat grayScaleImage;
    private int absoluteFaceSize;
    private Mat mRgba;

    //Map & UI
    private ImageView ivCompass, ivDirection, ivCompassNorth, ivPlayStop, ivBluetooth, ivAddToRoute;
    private TextView tvDistance, tvFaceRatio;
    private GoogleMap mMap;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private SensorManager mSensorManager;
    private float compassBearingDegrees = 0f;
    private float currentDegree = 0f, currentDegreeNorth = 0f;//for the image rotation
    private Location robotLocation;
    private Route route;
    private boolean running = false;
    private Marker robotMarker = null;
    private String command = "STOP";
    private TextToSpeech textToSpeech;
    private Bluetooth bt;
    private static StringBuilder sb = new StringBuilder();
    private List<Boolean> facesInASecond = Collections.synchronizedList(new ArrayList<Boolean>());
    private Sensor gSensor;
    private Sensor mSensor;
    private volatile boolean faceDetected = false;
    final Handler handler1 = new Handler();
    Runnable runnable;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {

                    // mOpenCvCameraView.enableView();
                    initializeOpenCVDependencies();

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private Thread directionThread;


    private void initializeGraphicComponents() {
        ivCompass = (ImageView) findViewById(R.id.ivBearing);
        ivDirection = (ImageView) findViewById(R.id.ivDirection);
        ivCompassNorth = (ImageView) findViewById(R.id.ivCompassNorth);
        ivPlayStop = (ImageView) findViewById(R.id.ivPlayStop);
        ivBluetooth = (ImageView) findViewById(R.id.ivBluetooth);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        ivAddToRoute = (ImageView) findViewById(R.id.ivAddPointToPath);
        tvFaceRatio = (TextView) findViewById(R.id.tvFaceRatio);
    }

    private void initializeOpenCVDependencies() {


        try {

            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();


            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }


        openCvCameraView.enableView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeGraphicComponents();

        route = new Route();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentMap);
        mapFragment.getMapAsync(this);

        mLocationRequest = Utilities.createLocationRequest(getResources());

        // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(AppIndex.API).build();
        }
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

        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.jcvFaceDetection);
        openCvCameraView.setCvCameraViewListener(this);


        /*handler1.postDelayed(*/
        runnable = new Runnable() {
            public void run() {
                try {
                    int listSize = facesInASecond.size();
                    int trues = Collections.frequency(facesInASecond, true);
                    // int falses = Collections.frequency(facesInASecond, false);


                    final double faceRatio = ((double) trues / (double) listSize) * (double) 100;

                    if (faceRatio > 65) {
                        faceDetected = true;
                        if (!textToSpeech.isSpeaking())
                            textToSpeech.speak("Please get out the way", TextToSpeech.QUEUE_FLUSH, null);

                    } else {
                        faceDetected = false;
                    }

                    runOnUiThread(new Runnable() // start actions in UI thread
                    {

                        @Override
                        public void run() {
                            if (faceDetected)

                                if (!command.equals("STOP")) {
                                    Utilities.setDirectionImage("STOP", ivDirection, bt);
                                    command = "STOP";
                                }
                            if (!(faceRatio == Double.NaN))
                                tvFaceRatio.setText("Face ratio: " + String.format("%.02f", faceRatio) + "%");
                            else
                                tvFaceRatio.setText("Face ratio: " + 0 + "%");
                        }
                    });

                    Log.d("RATIO", "" + faceRatio);
                    Log.d("face state", " " + faceDetected);
                    facesInASecond.clear();
                } catch (Exception e) {
                    //  Log.e("ERROR", e.printStackTrace() + "");
                    e.printStackTrace();
                }
                handler1.postDelayed(this, 1000); //1 second
            }
        };

        handler1.postDelayed(runnable, 1000);

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

                                    if (!faceDetected)
                                        command = Utilities.giveDirection(compassBearingDegrees, ivDirection, ivCompass, route, robotLocation, getApplicationContext(), command, mMap, getResources(), tvDistance, textToSpeech, bt, false);


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


    @Override
    public void onCameraViewStarted(int width, int height) {
        grayScaleImage = new Mat(height, width, CvType.CV_8UC4);


        // The faces will be a 20% of the height of the screen
        absoluteFaceSize = (int) (height * (double) Preferences.loadPrefsFloat("FACE_PERCENT_OF_THE_SCREEN", 0.2f, getApplicationContext()));
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {


        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba, grayScaleImage, Imgproc.COLOR_RGBA2RGB);


        MatOfRect faces = new MatOfRect();


        // detect faces
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(grayScaleImage, faces, 1.1, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }


        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++)
            Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);


        if (facesArray.length > 0) {
            facesInASecond.add(true);
        } else {
            facesInASecond.add(false);
        }

        return mRgba;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        if (mGoogleApiClient.isConnected())
            startLocationUpdates();

        //Legacy compass sensor
        /*mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);*/

        mSensorManager.registerListener(this, gSensor,
                SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(
                this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_UI);


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
        robotMarker = MapUtilities.placeRobotMarkerOnMap(robotMarker, mMap, Utilities.convertLocationToLatLng(robotLocation), false, getResources(), getApplicationContext());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

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
    public void onMapClick(LatLng latLng) {
        route.addPoint(new Point(latLng, "Point " + route.getPointsNumber(), false));
        MapUtilities.drawPathOnMap(mMap, route, getResources());
        robotMarker = MapUtilities.placeRobotMarkerOnMap(robotMarker, mMap, Utilities.convertLocationToLatLng(robotLocation), true, getResources(), getApplicationContext());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(Utilities.getMapType(this));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnMapClickListener(this);
    }

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
                        // tvDistance.setText(sbprint + "cm");
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

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.sendMessage(Integer.toString(3));
        bt.stop();

    }

    @Override
    protected void onPause() {

        directionThread.interrupt();
        super.onPause();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (mGoogleApiClient.isConnected())
            stopLocationUpdates();

        mSensorManager.unregisterListener(this);

        handler1.removeCallbacks(runnable);

        finish();
    }

    @Override
    protected void onStart() {

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {

        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        bt.sendMessage(Integer.toString(3));
        bt.stop();
        finish();
    }
}

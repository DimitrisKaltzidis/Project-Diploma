package com.jim.robotos_v2;

import android.bluetooth.BluetoothAdapter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.jim.robotos_v2.RouteLogic.Point;
import com.jim.robotos_v2.RouteLogic.Route;
import com.jim.robotos_v2.Utilities.Bluetooth;
import com.jim.robotos_v2.Utilities.MapUtilities;
import com.jim.robotos_v2.Utilities.Utilities;

import java.util.Locale;

public class ObstacleAvoidance extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleMap.OnMapClickListener, SensorEventListener,View.OnLongClickListener  {
    private GoogleMap mMap;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private SensorManager mSensorManager;
    private float compassBearingDegrees = 0f;
    private float currentDegree = 0f, currentDegreeNorth = 0f;//for the image rotation
    private ImageView ivCompass, ivDirection, ivCompassNorth, ivPlayStop, ivBluetooth,ivAddToRoute;
    private TextView tvDistance;
    private Location robotLocation;
    private Route route;
    private boolean running = false;
    private Marker robotMarker = null;
    private String command = "STOP";
    private TextToSpeech textToSpeech;
    private Bluetooth bt;
    private static StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obstacle_avoidance);

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
    }

    private void initializeGraphicComponents() {
        ivCompass = (ImageView) findViewById(R.id.ivBearing);
        ivDirection = (ImageView) findViewById(R.id.ivDirection);
        ivCompassNorth = (ImageView) findViewById(R.id.ivCompassNorth);
        ivPlayStop = (ImageView) findViewById(R.id.ivPlayStop);
        ivBluetooth = (ImageView) findViewById(R.id.ivBluetooth);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        ivAddToRoute = (ImageView) findViewById(R.id.ivAddPointToPath);
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

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
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
        robotMarker = MapUtilities.placeRobotMarkerOnMap(robotMarker, mMap, Utilities.convertLocationToLatLng(robotLocation), true, getResources(),getApplicationContext());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onMapClick(LatLng latLng) {
        route.addPoint(new Point(latLng, "Point " + route.getPointsNumber()));
        MapUtilities.drawPathOnMap(mMap, route, getResources());
        robotMarker = MapUtilities.placeRobotMarkerOnMap(robotMarker, mMap, Utilities.convertLocationToLatLng(robotLocation), true, getResources(),getApplicationContext());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (robotLocation != null && (!route.isEmpty()) && running && textToSpeech != null) {
            compassBearingDegrees = Utilities.correctCompassBearing(Math.round(event.values[0]), robotLocation);
            currentDegree = Utilities.compassAnimationHandler(ivCompass, compassBearingDegrees, currentDegree);
            currentDegreeNorth = Utilities.compassNorthIconHandler(ivCompassNorth, compassBearingDegrees, currentDegreeNorth);
            command = Utilities.giveDirection(compassBearingDegrees, ivDirection, ivCompass, route, robotLocation, this, command, mMap, getResources(), tvDistance, textToSpeech,bt);
            //  Log.e("DIRECTION", command);

            if (command.equals("FINISH")) {
                mMap.clear();
                tvDistance.setText("---m");
                if (running)
                    running = Utilities.playStopButtonHandler(route, running, ivPlayStop, this);

                route.clearRoute();


                Utilities.setDirectionImage("STOP", ivDirection,bt);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //old shit not in use
    }

    public void showMyLocationClicked(View view) {
        robotMarker = MapUtilities.placeRobotMarkerOnMap(robotMarker, mMap, Utilities.convertLocationToLatLng(robotLocation), true, getResources(),getApplicationContext());

    }

    public void clearRouteClicked(View view) {
        mMap.clear();
        tvDistance.setText("---m");
        if (running)
            running = Utilities.playStopButtonHandler(route, running, ivPlayStop, this);

        route.clearRoute();

        Utilities.setDirectionImage("STOP", ivDirection,bt);
    }

    public void addMyLocationToRoute(View view) {
        if (!running) {
            route.addPoint(new Point(new LatLng(robotLocation.getLatitude(), robotLocation.getLongitude()), "Point " + route.getPointsNumber()));
            MapUtilities.drawPathOnMap(mMap, route, getResources());
            robotMarker = MapUtilities.placeRobotMarkerOnMap(robotMarker, mMap, Utilities.convertLocationToLatLng(robotLocation), true, getResources(),getApplicationContext());
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
    public boolean onLongClick(View v) {

        final Point pointToAdd = new Point(new LatLng(-31.90, 115.86), "Point " + route.getPointsNumber());

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
                            robotMarker = MapUtilities.placeRobotMarkerOnMap(robotMarker, mMap, Utilities.convertLocationToLatLng(robotLocation), true, getResources(),getApplicationContext());
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
}
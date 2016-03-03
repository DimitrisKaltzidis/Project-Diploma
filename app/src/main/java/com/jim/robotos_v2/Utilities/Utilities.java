package com.jim.robotos_v2.Utilities;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.jim.robotos_v2.R;
import com.jim.robotos_v2.RouteLogic.Point;
import com.jim.robotos_v2.RouteLogic.Route;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

/**
 * Created by the awesome and extraordinary developer Jim on 9/12/2015.
 */
public class Utilities {

    static float[] mGravity = new float[3];
    static float[] mGeomagnetic = new float[3];

    public static float landscapeModeCompassCalibration(SensorEvent event) {

        float azimuth = 0;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mGravity = event.values.clone();
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                mGeomagnetic = event.values.clone();
                break;
        }

        float R[] = new float[9];
        float I[] = new float[9];
        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
        if (success)

        {
            float orientation[] = new float[3];
            SensorManager.getOrientation(R, orientation);
            azimuth = orientation[0]; // orientation contains: azimut, pitch and roll
            azimuth = (float) Math.toDegrees(azimuth) + 90;
            azimuth = (azimuth + 360) % 360;
           // Log.d("AZIMUTH", " " + azimuth);
        }

        return azimuth;
    }

    public static int getMapType(Context context) {
        String mapType = Preferences.loadPrefsString("MAP_TYPE", "HYBRID", context);
        int mapTypeInteger;
        switch (mapType) {

            case "HYBRID":
                mapTypeInteger = GoogleMap.MAP_TYPE_HYBRID;
                break;
            case "NORMAL":
                mapTypeInteger = GoogleMap.MAP_TYPE_NORMAL;
                break;
            case "TERRAIN":
                mapTypeInteger = GoogleMap.MAP_TYPE_TERRAIN;
                break;
            default:
                mapTypeInteger = GoogleMap.MAP_TYPE_HYBRID;
        }
        return mapTypeInteger;
    }

    public static String giveDirection(float compassBearing, ImageView ivDirection, ImageView ivCompass, Route route, Location robotLocation, Context context, String previousCommand, GoogleMap mMap, Resources resources, TextView tvDistance, TextToSpeech tts, Bluetooth bluetooth) {
        String directionToReturn;

        float bearingRange = Preferences.loadPrefsFloat("BEARING_RANGE", 20, context);

        if (route.getNextPoint() != null) {
            float desiredBearing = calculateAngleBetweenTwoPoint(robotLocation, route.getNextPoint().getPositionAsLocationobject(), true);

            desiredBearing = correctBearing(desiredBearing);

            Utilities.decideCompassImage(ivCompass, compassBearing, desiredBearing, bearingRange);

            float distance = robotLocation.distanceTo(route.getNextPoint().getPositionAsLocationobject());

            //tvDistance.setText((int) distance + "m");

            float distanceErrorRange = Preferences.loadPrefsFloat("DISTANCE_ERROR_RANGE", 3, context);

            if (distance <= distanceErrorRange) {
                tts.speak(route.getNextPoint().getName() + " REACHED", TextToSpeech.QUEUE_FLUSH, null);
                route.setCurrentPointAsVisited();
                MapUtilities.drawPathOnMap(mMap, route, resources);

                return previousCommand;
            } else {

                if (inRange(compassBearing, desiredBearing, bearingRange)) {
                    directionToReturn = "FORWARD";
                } else {
                    if (desiredBearing >= 180) {
                        double symmetricBearing = desiredBearing - 180;
                        if (compassBearing < desiredBearing && compassBearing > symmetricBearing) {
                            directionToReturn = "RIGHT";
                        } else {
                            directionToReturn = "LEFT";
                        }
                    } else {
                        double symmetricBearing = desiredBearing + 180;
                        if (compassBearing > desiredBearing && compassBearing < symmetricBearing) {
                            directionToReturn = "LEFT";
                        } else {
                            directionToReturn = "RIGHT";
                        }
                    }
                }
                if (!directionToReturn.equals(previousCommand))
                    setDirectionImage(directionToReturn, ivDirection, bluetooth);

                return directionToReturn;
            }
        } else {
            if (!tts.isSpeaking()) {
                tts.speak("CURRENT ROUTE COMPLETED", TextToSpeech.QUEUE_FLUSH, null);
            }
            return "FINISH";

        }

    }

    public static float calculateAngleBetweenTwoPoint(Location myLocation, Location destinationLocation, boolean automaticMode) {
        if (automaticMode) {
            return myLocation.bearingTo(destinationLocation);
        } else {
            return (float) angleFromCoordinate(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()), new LatLng(destinationLocation.getLatitude(), destinationLocation.getLongitude()));
        }
    }

    public static void setDirectionImage(String command, ImageView ivDirection, Bluetooth bluetooth) {

        int directionDrawable;
        int strCmndToInt = -1;
        switch (command) {
            case "FORWARD":
                directionDrawable = R.drawable.forward;
                strCmndToInt = 0;
                break;
            case "LEFT":
                directionDrawable = R.drawable.rotate_left;
                strCmndToInt = 1;
                break;
            case "RIGHT":
                directionDrawable = R.drawable.rotate_right;
                strCmndToInt = 2;
                break;
            case "BACKWARD":
                directionDrawable = R.drawable.backward;
                strCmndToInt = 4;
                break;
            case "STOP":
                directionDrawable = R.drawable.stop;
                strCmndToInt = 3;
                break;
            default:
                directionDrawable = R.drawable.stop;
                strCmndToInt = 3;

        }

        try {
            bluetooth.sendMessage(Integer.toString(strCmndToInt));
            ivDirection.setImageResource(directionDrawable);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DIRECTIONS", "COULD NOT SET DIRECTION IMAGE");
            Log.e("BLUETOOTH", "COULD NOT SEND DIRECTION TO ROBOT");
        }
    }

    public static boolean inRange(float compassBearing, float desiredBearing, float bearingRange) {

        boolean adjusted = false;
        boolean inRange = false;

        double upLimit = (double) desiredBearing + bearingRange;
        double lowLimit = (double) desiredBearing - bearingRange;
        double compassBearingFloat = (double) compassBearing;

        if (upLimit > 360 || upLimit < 0) {
            adjusted = true;
        }

        if (lowLimit > 360 || lowLimit < 0) {
            adjusted = true;
        }

        upLimit = normalizeAngles(upLimit);
        lowLimit = normalizeAngles(lowLimit);

        if (adjusted) {
            if (compassBearingFloat >= lowLimit || compassBearingFloat < upLimit) {
                inRange = true;
            }
        } else {
            if (compassBearingFloat >= lowLimit && compassBearingFloat < upLimit) {
                inRange = true;
            }
        }
        return inRange;
    }

    public static double normalizeAngles(double angle) {
        if (angle >= 0)
            angle = (angle % 360);
        else
            angle = ((angle % 360) + 360) % 360;

        return angle;
    }

    public static float correctBearing(float bearing) {
        if (bearing < 0) {
            return bearing + 360;
        }
        return bearing;
    }

    public static float correctCompassBearing(float compassBearingDegreesFromSensor, Location robotsLocation) {

        GeomagneticField geoField = new GeomagneticField(Double
                .valueOf(robotsLocation.getLatitude()).floatValue(), Double
                .valueOf(robotsLocation.getLongitude()).floatValue(),
                Double.valueOf(robotsLocation.getAltitude()).floatValue(),
                System.currentTimeMillis());
        compassBearingDegreesFromSensor += geoField.getDeclination(); // converts magnetic north into true north

        //Correct the azimuth mirror degrees
        return (float) normalizeAngles((double) compassBearingDegreesFromSensor);
        //return compassBearingDegreesFromSensor % 360;
    }

    public static void decideCompassImage(ImageView ivCompass, float compassBearing, float desiredBearing, float bearingRange) {
        if (Utilities.inRange(compassBearing, desiredBearing, bearingRange)) {
            ivCompass.setImageResource(R.drawable.correct_direction);
        } else {
            ivCompass.setImageResource(R.drawable.wrong_direction);
        }
    }


    public static float compassAnimationHandler(ImageView ivCompass, float compassBearingDegrees, float currentDegree) {

        RotateAnimation rotateAnimation = new RotateAnimation(
                -currentDegree,
                compassBearingDegrees,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        rotateAnimation.setDuration(210);
        ivCompass.startAnimation(rotateAnimation);
        currentDegree = -compassBearingDegrees;
        return currentDegree;
    }

    public static float compassNorthIconHandler(ImageView ivCompassNorth, float compassBearingDegrees, float currentDegreeNorth) {
        RotateAnimation rotateAnimation = new RotateAnimation(
                currentDegreeNorth,
                -compassBearingDegrees,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        rotateAnimation.setDuration(210);
        ivCompassNorth.startAnimation(rotateAnimation);
        currentDegreeNorth = -compassBearingDegrees;
        return currentDegreeNorth;
    }

    public static LocationRequest createLocationRequest(Resources res) {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(res.getInteger(R.integer.refresh_location_interval_milliseconds));
        // mLocationRequest.setFastestInterval(res.getInteger(R.integer.refresh_location_fastest_interval_milliseconds));
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    /**
     * Calculates the angle between two points.
     *
     * @param point1 first point for angle calculation
     * @param point2 second point for angle calculation
     * @return a double number representing an angle
     */
    public static double angleFromCoordinate(LatLng point1, LatLng point2) {

        double lat1 = point1.latitude;
        double long1 = point1.longitude;
        double lat2 = point2.latitude;
        double long2 = point2.longitude;

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        brng = 360 - brng;

        return abs(brng - 360);
    }

    public static LatLng convertLocationToLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public static Location convertLatLngToLocation(LatLng latLng) {
        Location temp = new Location("");
        temp.setLatitude(latLng.latitude);
        temp.setLongitude(latLng.longitude);
        return temp;
    }

    public static boolean playStopButtonHandler(Route route, boolean running, ImageView ivPlayStop, Context context) {
        if (!route.isEmpty()) {
            if (running) {
                running = false;
                ivPlayStop.setImageResource(R.drawable.play);
            } else {
                running = true;
                ivPlayStop.setImageResource(R.drawable.pause);
            }
        } else {
            Toast.makeText(context, "Please define a route", Toast.LENGTH_SHORT).show();
        }
        return running;
    }

    public static List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    public static Route convertPathToRoute(String result, Route route) {

        try {
            // Tranform the string into a json object
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes
                    .getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);

            for (LatLng latLng : list) {
                route.addPoint(new Point(latLng, "Point " + route.getPointsNumber()));
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("Utilities", "drawPath: ");
        }

        return route;
    }

    public static String makeURL(LatLng start, LatLng finish) {
        double sourceLat, sourceLong, destLat, destLog;
        sourceLat = start.latitude;
        sourceLong = start.longitude;
        destLat = finish.latitude;
        destLog = finish.longitude;

        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourceLat));
        urlString.append(",");
        urlString.append(Double.toString(sourceLong));
        urlString.append("&destination=");// to
        urlString.append(Double.toString(destLat));
        urlString.append(",");
        urlString.append(Double.toString(destLog));
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        return urlString.toString();
    }

    public static Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    public static Scalar convertScalarRgba2Hsv(Scalar rgbColor) {
        Mat pointMatHsv = new Mat();
        Mat pointMatRgba = new Mat(1, 1, CvType.CV_8UC3, rgbColor);
        Imgproc.cvtColor(pointMatRgba, pointMatHsv, Imgproc.COLOR_RGB2HSV_FULL, 4);

        return new Scalar(pointMatHsv.get(0, 0));
    }

    public static void giveDirectionColorDetection(org.opencv.core.Point center, int distanceToObject, double bottomLineHeight, double leftLineWidth, double rightLineWidth, ImageView ivDirection, Bluetooth bt, Context context) {
        try {
            String command;

            if (distanceToObject < Preferences.loadPrefsInt("DISTANCE_TO_STOP_FROM_OBSTACLE_CM", 50, context)) {
                if (center.y > bottomLineHeight) {
                    command = "STOP";
                } else {
                    if (center.x < leftLineWidth) {
                        command = "LEFT";
                    } else if (center.x > rightLineWidth) {
                        command = "RIGHT";
                    } else if ((center.x >= leftLineWidth) && (center.x <= rightLineWidth)) {
                        if (distanceToObject < Preferences.loadPrefsInt("DISTANCE_TO_STOP_FROM_OBSTACLE_CM", 50, context)) {
                            command = "STOP";
                        } else {
                            command = "FORWARD";
                        }
                    } else {
                        command = "RIGHT";
                    }
                }
            }else{
                command = "STOP";
            }
            Utilities.setDirectionImage(command, ivDirection, bt);

        } catch (Exception e) {
            Utilities.setDirectionImage("STOP", ivDirection, bt);
        }
    }

}

package com.jim.robotos_v2.Utilities;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.jim.robotos_v2.R;
import com.jim.robotos_v2.RouteLogic.Route;

import static java.lang.Math.abs;

/**
 * Created by the awesome and extraordinary developer Jim on 9/12/2015.
 */
public class Utilities {

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

        float bearingRange = Preferences.loadPrefsFloat("BEARING_RANGE", 10, context);

        if (route.getNextPoint() != null) {
            float desiredBearing = calculateAngleBetweenTwoPoint(robotLocation, route.getNextPoint().getPositionAsLocationobject(), true);

            desiredBearing = correctBearing(desiredBearing);

            Utilities.decideCompassImage(ivCompass, compassBearing, desiredBearing, bearingRange);

            float distance = robotLocation.distanceTo(route.getNextPoint().getPositionAsLocationobject());

            tvDistance.setText((int) distance + "m");

            float distanceErrorRange = Preferences.loadPrefsFloat("DISTANCE_ERROR_RANGE", 2, context);

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
                if(!directionToReturn.equals(previousCommand))
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

}

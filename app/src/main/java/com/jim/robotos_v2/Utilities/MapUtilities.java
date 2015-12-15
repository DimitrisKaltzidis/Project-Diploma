package com.jim.robotos_v2.Utilities;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jim.robotos_v2.R;
import com.jim.robotos_v2.RouteLogic.Point;
import com.jim.robotos_v2.RouteLogic.Route;

/**
 * Created by the awesome and extraordinary developer Jim on 10/12/2015.
 */
public class MapUtilities {

    public static void drawPathOnMap(GoogleMap mMap, Route route, Resources resources) {
        try {
            mMap.clear();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("ERROR", "CAN NOT CLEAR THE MAP");
        }

        PolylineOptions options = new PolylineOptions().width(6).color(resources.getColor(R.color.colorPrimary)).geodesic(true);

        for (Point point: route.getRoute()) {
            options.add(point.getPosition());
            placeMarkerOnMapLatLng(mMap,point.getPosition(),point.getName(),point.isVisited(),resources);
        }

        mMap.addPolyline(options);
    }

    public static void placeMarkerOnMapLatLng(GoogleMap mMap, LatLng location, String markerInfo, boolean visited,Resources res) {
        if (visited) {
            mMap.addMarker(new MarkerOptions().position(location).title(markerInfo).flat(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.grey_marker))).showInfoWindow();
        } else {
            mMap.addMarker(new MarkerOptions().position(location).title(markerInfo).flat(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.green_marker))).showInfoWindow();
        }
        animateCameraLatLng(mMap,location,res);
    }

    public static void animateCameraLatLng(GoogleMap mMap, LatLng location, Resources resources) {
       CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(location).zoom(resources.getInteger(R.integer.zoom_level)).tilt(0).bearing(0).build();
        mMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));
    }


    public static Marker placeRobotMarkerOnMap(Marker marker, GoogleMap mMap, LatLng location, boolean animateTheCamera, Resources res, Context context) {
        try {
            marker.remove();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MapUtilities", "placeMarkerOnMapUserLatLng: Could not remove Robots Marker");
        }
        MarkerOptions m = new MarkerOptions().position(location).icon(BitmapDescriptorFactory.fromResource(R.drawable.robot)).title("ROBOT");
        marker = mMap.addMarker(m);
        marker.showInfoWindow();
        if (animateTheCamera)
            animateCameraLatLng(mMap,location,res);

        Log.d("ROBOT_LOCATION", "Latitude: " +location.latitude+" Longitude: "+location.longitude);
        Toast.makeText(context,"Latitude: " +location.latitude+" Longitude: "+location.longitude,Toast.LENGTH_LONG).show();
        return marker;

    }

}

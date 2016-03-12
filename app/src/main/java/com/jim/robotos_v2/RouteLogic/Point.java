package com.jim.robotos_v2.RouteLogic;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class Point {

    private LatLng position;
    private boolean isVisited;
    private String name;
    private boolean systemDefined;

    public Point(LatLng position, String name, boolean systemDefined) {
        this.position = position;
        this.isVisited = false;
        this.name = name;
        this.systemDefined = systemDefined;
    }

    public Point() {
    }

    public LatLng getPosition() {
        return position;
    }

    public Location getPositionAsLocationobject() {
        Location temp = new Location("Point Location");
        temp.setLatitude(position.latitude);
        temp.setLongitude(position.longitude);
        return temp;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public void setVisited() {
        this.isVisited = true;
    }

    public void setNotVisited() {
        this.isVisited = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVisited() {
        return isVisited;
    }

    public void setVisited(boolean visited) {
        isVisited = visited;
    }

    public boolean isSystemDefined() {
        return systemDefined;
    }

    public void setSystemDefined(boolean systemDefined) {
        this.systemDefined = systemDefined;
    }
}

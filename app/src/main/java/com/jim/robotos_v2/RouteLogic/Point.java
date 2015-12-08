package com.jim.robotos_v2.RouteLogic;

import com.google.android.gms.maps.model.LatLng;

public class Point {

    private LatLng position;
    private boolean isVisited = false;
    private String name;

    public Point(LatLng position, boolean isVisited, String name) {
        this.position = position;
        this.isVisited = isVisited;
        this.name = name;
    }

    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public boolean isVisited() {
        return isVisited;
    }

    public void setIsVisited(boolean isVisited) {
        this.isVisited = isVisited;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

package com.jim.robotos_v2.RouteLogic;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by the awesome and extraordinary developer Jim on 9/12/2015.
 */
public class Route {

    private ArrayList<Point> route = new ArrayList<>();

    public void addPoint(Point newPoint){
        route.add(newPoint);
        Log.d("ROUTE","NEW POINT ADDED");
    }

    public ArrayList<Point> getRoute() {
        return route;
    }

    public void setRoute(ArrayList<Point> route) {
        this.route = route;
    }

    public int getPointsNumber(){
        if(route.isEmpty()){
            Log.e("ROUTE","NO POINTS IN ROUTE");
            return 01;
        }
        Log.d("ROUTE",route.size()+1+" POINTS IN ROUTE");
        return route.size();
    }

    public Point getNextPoint(){
        for (int i = 0; i < route.size(); i++) {
            if(!route.get(i).isVisited()){
       //         Log.d("ROUTE","NEXT POINT --> "+route.get(i));
                return route.get(i);
            }
        }
        Log.e("ROUTE","NO NEXT POINT");
        return null;
    }

    public void setCurrentPointAsVisited(){
        for (int i = 0; i < route.size(); i++) {
            if(!route.get(i).isVisited()){
                route.get(i).setVisited();
                Log.d("ROUTE","POINT --> "+route.get(i)+" VISITED");
                break;
            }
        }
    }

    public void clearRoute(){
        route.clear();
        Log.d("ROUTE","ROUTE CLEARED");
    }

    public boolean isEmpty(){
        if(route.isEmpty()){
            return true;
        }
        return false;
    }
}

package com.jim.robotos_v2.RouteLogic;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.abs;

/**
 * Created by Jim on 16/3/2016.
 */
public class Obstacle implements Serializable {

    private static final AtomicInteger count = new AtomicInteger(0);
    private double latitude, longitude;
    private int[] color;
    private float compassDetectionDegrees;
    private float compassAvoidanceDegrees;
    private float avoidingAngleDegrees;
    private String relativeSpecification; /// an simfwna me tin gwnia apofigis to katatasw se megalo i mikro i meseo
    private float errorRange;
    private final int obstacleID;
    private final String name;

    public Obstacle(LatLng location, int[] color, float compassDetectionDegrees, float compassAvoidanceDegrees, float errorRange) {
        this.latitude = location.latitude;
        this.longitude = location.longitude;
        this.color = color;
        this.compassDetectionDegrees = compassDetectionDegrees;
        this.compassAvoidanceDegrees = compassAvoidanceDegrees;
        this.errorRange = errorRange;
        avoidingAngleDegrees = 180 - abs(abs(compassDetectionDegrees - compassAvoidanceDegrees) - 180);
        if (avoidingAngleDegrees < 30) {
            relativeSpecification = "SMALL";
        } else if (avoidingAngleDegrees < 60) {
            relativeSpecification = "MEDIUM";
        } else {
            relativeSpecification = "BIG";
        }
        obstacleID = count.incrementAndGet();
        name = "Obstacle " + obstacleID;
    }


    public String getName() {
        return this.name;
    }


    public int[] getColor() {
        return color;
    }

    public void setColor(int[] color) {
        this.color = color;
    }

    public float getCompassAvoidanceDegrees() {
        return compassAvoidanceDegrees;
    }

    public void setCompassAvoidanceDegrees(float compassAvoidanceDegrees) {
        this.compassAvoidanceDegrees = compassAvoidanceDegrees;
    }

    public float getAvoidingAngleDegrees() {
        return avoidingAngleDegrees;
    }

    public void setAvoidingAngleDegrees(float avoidingAngleDegrees) {
        this.avoidingAngleDegrees = avoidingAngleDegrees;
    }

    public String getRelativeSpecification() {
        return relativeSpecification;
    }

    public void setRelativeSpecification(String relativeSpecification) {
        this.relativeSpecification = relativeSpecification;
    }

    public float getErrorRange() {
        return errorRange;
    }

    public void setErrorRange(float errorRange) {
        this.errorRange = errorRange;
    }

    @Override
    public String toString() {

        return "Obstacle{" +
                "location=" + latitude + " , " + longitude + "\n" +
                ", compassDetectionDegrees=" + compassDetectionDegrees + "\n" +
                ", compassAvoidanceDegrees=" + compassAvoidanceDegrees + "\n" +
                ", avoidingAngleDegrees=" + avoidingAngleDegrees + "\n" +
                ", relativeSpecification='" + relativeSpecification + '\'' + "\n" +
                ", errorRange=" + errorRange + "\n" +
                ", obstacleID=" + obstacleID + "\n" +
                ", name='" + name + '\'' + "\n" + ", color rgb='" + color[0] + ", " + color[1] + ", " + color[2] + "'" + "\n" +
                '}'
                ;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}


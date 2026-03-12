package org.example.Model;

public class Clock {

    private static Clock instance;
    private double currentTime;

    private Clock() {
        currentTime = 0.0;
    }

    public static Clock getInstance() {
        if (instance == null) {
            instance = new Clock();
        }
        return instance;
    }

    public double getTime() {
        return currentTime;
    }

    public void setTime(double time) {
        this.currentTime = time;
    }

    public void reset() {
        currentTime = 0.0;
    }
}

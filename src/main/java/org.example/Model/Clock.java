package org.example.Model;

/**
 * A singleton class representing the simulation clock.
 * The Clock provides a unified reference point for the current simulation time,
 * allowing different components of the airport system to synchronize their events.
 */
public class Clock {

    /**
     * The single instance of the Clock class.
     */
    private static Clock instance;

    /**
     * The current simulation time in double precision.
     */
    private double currentTime;

    /**
     * Private constructor to prevent direct instantiation.
     * Initializes the simulation time to 0.0.
     */
    private Clock() {
        currentTime = 0.0;
    }

    /**
     * Returns the singleton instance of the Clock.
     * If the instance does not exist, it is created.
     * * @return The singleton Clock instance.
     */
    public static Clock getInstance() {
        if (instance == null) {
            instance = new Clock();
        }
        return instance;
    }

    /**
     * Returns the current simulation time.
     * * @return Current time as a double.
     */
    public double getTime() {
        return currentTime;
    }

    /**
     * Updates the simulation clock to a specific point in time.
     * Typically called by the SimulationEngine as it moves from one event to the next.
     * * @param time The new simulation time.
     */
    public void setTime(double time) {
        this.currentTime = time;
    }

    /**
     * Resets the simulation clock back to 0.0.
     * Essential for clearing the state before starting a new simulation run.
     */
    public void reset() {
        currentTime = 0.0;
    }
}
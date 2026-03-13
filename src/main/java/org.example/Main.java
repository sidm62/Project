package org.example;

import javafx.application.Application;
import org.example.Model.Configuration;
import org.example.Model.Event;
import org.example.Model.EventType;
import org.example.Model.SimulationEngine;
import org.example.Model.Passenger;
import org.example.View.AirportView;

/**
 * Entry point of the airport simulation application.
 *
 * This class initializes the simulation configuration,
 * creates the simulation engine and launches the JavaFX UI.
 */
public class Main {
    /**
     * Main method that starts the airport simulation program.
     *
     * The method performs the following steps:
     * 1. Creates the simulation configuration.
     * 2. Initializes the simulation engine.
     * 3. Schedules the first passenger arrival event.
     * 4. Starts the simulation in a background thread.
     * 5. Launches the JavaFX graphical user interface.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        // 1. Alustetaan konfiguraatio ja moottori
        Configuration config = new Configuration();
        double simulationEndTime = 100; // Pidempi aika antaa tarkemmat tulokset

        SimulationEngine engine = new SimulationEngine(simulationEndTime, config);
        engine.setDebugMode(false); // Debug-tulostus hidastaa UI-animaatiota

        // 2. Asetetaan ensimmäinen saapuminen (siemen)
        engine.scheduleEvent(new Event(
                0.0,
                EventType.ARRIVAL_SYSTEM,
                new Passenger(engine)
        ));

        // 3. Asetetaan alkunopeus
        engine.setSpeedMultiplier(5.0); // 5x nopeus on hyvä aloitus

        // 4. Välitetään moottori näkymälle
        AirportView.setEngine(engine);

        // 5. Käynnistetään JavaFX (Tämä kutsuu AirportView.java.start() -> Controller.startSimulation())
        Application.launch(AirportView.class, args);
    }
}

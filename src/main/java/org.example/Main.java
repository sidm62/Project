package org.example;

import javafx.application.Application;
import org.example.Model.Configuration;
import org.example.Model.Event;
import org.example.Model.EventType;
import org.example.Model.SimulationEngine;
import org.example.Model.Passenger;
import org.example.View.AirportView;

public class Main {

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

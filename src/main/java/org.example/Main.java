package org.example;

import javafx.application.Application;
import org.example.View.AirportView;

/**
 * Entry point of the airport simulation application.
 *
 * The main method simply launches the JavaFX user interface.
 * All simulation logic is managed by the AirportController,
 * which creates a new SimulationEngine instance for every run.
 */
public class Main {

    /**
     * Starts the JavaFX application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Application.launch(AirportView.class, args);
    }

}

package org.example.Controller;

import javafx.application.Platform;
import org.example.Model.SimulationEngine;
import org.example.View.AirportView;

/**
 * Controller class for the airport simulation.
 * Handles communication between the simulation engine and the view.
 */
public class AirportController {

    /**
     * Reference to the AirportView to update the UI.
     */
    private AirportView view;

    /**
     * Creates a new AirportController with the given view.
     *
     * @param view the AirportView instance to be controlled
     */
    public AirportController(AirportView view) {
        this.view = view;
    }

    /**
     * Starts the simulation in a separate thread.
     *
     * The simulation engine is executed on a background thread to avoid blocking the UI.
     * After completion, the results are displayed on the UI using Platform.runLater.
     *
     * @param engine the SimulationEngine instance to run the simulation
     */
    public void startSimulation(SimulationEngine engine) {

        Thread simThread = new Thread(() -> {
            try {
                Thread.sleep(500);
                System.out.println("Controller: Starting simulation...");

                engine.run();

                double avgTime = engine.getAverageSystemTime();
                int totalCompleted = engine.getTotalPassengersCompleted();

                double validationErrorPercent = engine.getLittleLawQueueError();

                Platform.runLater(() -> {
                    view.showFinalResults(
                            avgTime,
                            totalCompleted,
                            validationErrorPercent // UI receives the percentage (e.g., 0.45)
                    );
                });

            } catch (InterruptedException e) {
                System.err.println("Controller: Simulation thread was interrupted.");
            } catch (Exception e) {
                System.err.println("Controller: Error occurred during simulation execution.");
                e.printStackTrace();
            }
        });

        // Ensures that the thread terminates if the program is closed
        simThread.setDaemon(true);
        simThread.start();
    }

}
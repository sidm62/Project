package org.example.Controller;

import javafx.application.Platform;
import org.example.Model.*;
import org.example.View.AirportView;

/**
 * Controller class for the airport simulation.
 *
 * Responsible for coordinating the interaction between the
 * user interface (AirportView) and the simulation engine.
 *
 * The controller starts simulations on a background thread to
 * prevent blocking the JavaFX UI thread. For every simulation run,
 * a fresh SimulationEngine instance is created to ensure that no
 * residual state from previous runs affects the new simulation.
 */
public class AirportController {

    /**
     * Reference to the AirportView used for updating the UI.
     */
    private AirportView view;

    /**
     * Simulation configuration used when creating new SimulationEngine instances.
     */
    private Configuration config;

    /** The active simulation engine instance
     *
     */
    private SimulationEngine engine;

    /**
     * Creates a new AirportController with the given view and configuration.
     *
     * @param view   the AirportView instance controlled by this controller
     * @param config the simulation configuration
     */
    public AirportController(AirportView view, Configuration config) {
        this.view = view;
        this.config = config;
    }

    /**
     * Injects the simulation engine instance into the controller
     * @param engine the engine to be managed
     */

    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
    }

    /**
     * Starts a new simulation in a separate background thread.
     *
     * A new SimulationEngine instance is created for each run to ensure that
     * all simulation state (event list, passengers, service points, statistics)
     * starts from a clean state.
     *
     * The simulation runs asynchronously so that the JavaFX UI thread remains responsive.
     * When the simulation finishes, the final results are sent back to the UI
     * using Platform.runLater().
     *
     * @param simulationEndTime the total simulation duration in simulation time units
     */
    public void startSimulation(double simulationEndTime) {
        if (engine == null) {
            throw new IllegalStateException("Cannot start simulation: engine is null. Did you forget to set it?");
        }

        Thread simThread = new Thread(() -> {
            try {
                Thread.sleep(500);
                System.out.println("Controller: Starting simulation...");

                view.setEngine(engine);
                engine.setSimulationEndTime(simulationEndTime);
                engine.run();

                double avgTime = engine.getAverageSystemTime();
                int totalCompleted = engine.getTotalPassengersCompleted();
                double validationErrorPercent = engine.getLittleLawQueueError();

                Platform.runLater(() -> {
                    view.showFinalResults(
                            avgTime,
                            totalCompleted,
                            validationErrorPercent
                    );
                });

            } catch (InterruptedException e) {
                System.err.println("Controller: Simulation thread was interrupted.");
            } catch (Exception e) {
                System.err.println("Controller: Error occurred during simulation execution.");
                e.printStackTrace();
            }
        });

        // Ensure thread terminates if the application closes
        simThread.setDaemon(true);
        simThread.start();
    }
}

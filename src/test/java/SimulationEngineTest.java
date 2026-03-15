import org.example.Model.Configuration;
import org.example.Model.Passenger;
import org.example.Model.SimulationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link SimulationEngine} class.
 * Focuses on system stability, parameter validation, and the correct
 * resetting of the simulation state between runs.
 */
class SimulationEngineTest {
    private SimulationEngine engine;

    /**
     * Initializes the simulation engine before each test.
     * Sets up a default simulation duration and configuration to provide
     * a consistent baseline for testing engine logic.
     */
    @BeforeEach
    void setUp() {
        Configuration config = new Configuration();
        engine = new SimulationEngine(1000, config);
    }

    /**
     * Verifies that the engine prevents unstable system states.
     * The simulation uses a "Load Ratio" (λ/μ) to ensure that the arrival rate
     * does not exceed the service capacity by an unsafe margin.
     * An exception should be thrown if the configuration leads to a
     * theoretical queue growth that would crash or stall the simulation.
     */
    @Test
    @DisplayName("Validation of coefficients: Load ratio limit")
    void testLoadRatioValidation() {
        assertThrows(Exception.class, () -> {
            // Attempting to set high arrival speed and low service speed
            engine.setServiceSpeedFactor(1.5);
            engine.setArrivalSpeedFactor(2.0);
        }, "The engine should prevent unstable states where loadRatio > 1.29.");
    }

    /**
     * Tests the simulation's reset functionality.
     * Ensures that all internal data structures, specifically the
     * passenger tracking lists, are completely cleared when a reset is
     * triggered. This is essential for running multiple consecutive simulations.
     */
    @Test
    @DisplayName("Reset function: Passenger list should be cleared")
    void testResetSimulation() {
        // Act: Populate the engine with data
        engine.getAllPassengers().add(new Passenger(engine));

        // Act: Reset the engine state
        engine.resetSimulation();

        // Verification: The state must be empty
        assertTrue(engine.getAllPassengers().isEmpty(),
                "The passenger list must be empty after a simulation reset.");
    }
}
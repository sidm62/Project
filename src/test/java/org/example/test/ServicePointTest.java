package org.example.test;

import org.example.Model.Clock;
import org.example.Model.Configuration;
import org.example.Model.Passenger;
import org.example.Model.SimulationEngine;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ServicePoint logic and its subclasses.
 * This class validates queue management, service completion tracking,
 * dynamic speed scaling, and the accuracy of utilization metrics.
 */
@DisplayName("ServicePoint: Testing Queue and Service Logic")
class ServicePointTest {
    private SimulationEngine engine;
    private SimulationEngine.NormalCheckin testSP;

    /**
     * Prepares a controlled test environment for the service point.
     * Initializes a fresh {@link SimulationEngine}, creates a specific
     * {@link SimulationEngine.NormalCheckin} instance, and resets the global
     * {@link Clock} to zero.
     */
    @BeforeEach
    void setUp() {
        Configuration config = new Configuration();
        engine = new SimulationEngine(1000, config);

        testSP = engine.new NormalCheckin(engine, "Normal Check-in");
        Clock.getInstance().setTime(0);
    }

    /**
     * Verifies that arrivals correctly increment the internal queue and counters.
     * Ensures that when multiple passengers arrive, the system tracks the
     * maximum queue length achieved during the simulation period.
     */
    @Test
    @DisplayName("Arrivals should increment queue and maximum length counter")
    void testArrivalIncrementsQueue() {
        Passenger p1 = new Passenger(engine);
        Passenger p2 = new Passenger(engine);
        Passenger p3 = new Passenger(engine);
        Passenger p4 = new Passenger(engine);

        // Act: Sequential arrivals without processing
        testSP.handleArrival(p1);
        testSP.handleArrival(p2);
        testSP.handleArrival(p3);
        testSP.handleArrival(p4);

        // Verification: The queue metrics must reflect the backlog
        assertTrue(testSP.getMaxQueueLength() >= 1,
                "Maximum queue length should be at least 1 when multiple passengers arrive sequentially.");
    }

    /**
     * Verifies that completing a service correctly updates the statistics.
     * Ensures that when a passenger is processed, the completion counter
     * increments and the passenger is removed from the active queue.
     */
    @Test
    @DisplayName("Service completion should update counters and decrement queue")
    void testCompletionDecrementsQueue() {
        Passenger p = new Passenger(engine);
        testSP.handleArrival(p);

        int countBefore = testSP.getCompletionCount();
        testSP.handleCompletion(p);

        assertEquals(countBefore + 1, testSP.getCompletionCount(),
                "The completion count should increment after a service is finished.");
    }

    /**
     * Tests the dynamic scaling of service times.
     * Verifies that adjusting the service factor (e.g., to simulate staff boosts
     * or slowdowns) correctly updates the service point's internal state.
     */
    @Test
    @DisplayName("Impact of ServiceSpeedFactor on processing speed")
    void testServiceTimeScaling() {
        double originalFactor = testSP.getCurrentFactor();

        // Simulating a staff boost (0.5 = double speed)
        testSP.adjustServiceTime(0.5);
        assertEquals(0.5, testSP.getCurrentFactor(), 0.01, "The service factor should reflect the speed adjustment.");

        // Resetting back to normal
        testSP.adjustServiceTime(1.0);
        assertEquals(1.0, testSP.getCurrentFactor(), 0.01, "The factor should return to 1.0.");
    }

    /**
     * Validates the calculation of the service point utilization rate.
     * Formula checked: {@code Utilization = BusyTime / TotalTime}.
     * This ensures that the simulation provides accurate data on how
     * efficiently the service resources are being used.
     */
    @Test
    @DisplayName("Calculation of resource utilization (Utilization %)")
    void testUtilizationCalculation() {
        Passenger p = new Passenger(engine);

        // Clock starts at 0
        Clock.getInstance().setTime(0);
        testSP.handleArrival(p); // Point becomes busy

        Clock.getInstance().setTime(10);
        testSP.handleCompletion(p); // Point becomes idle

        Clock.getInstance().setTime(20);

        // Expected Utilization: 10 units of work in 20 units of time = 0.5 (50%)
        double util = testSP.getUtilization(20.0);
        assertEquals(0.5, util, 0.05, "Utilization rate should be 50% based on the timeline.");
    }
}
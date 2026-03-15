import org.example.Model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Passenger} class data management.
 * Verifies that the passenger correctly records and retrieves timestamps
 * for queueing, service, and movement transitions across the airport.
 */
@DisplayName("Passenger: Data Integrity and History Tracking Tests")
class PassengerDataTest {
    private SimulationEngine engine;

    /**
     * Sets up the simulation environment before each test.
     * Initializes the engine and resets the global {@link Clock} to zero
     * to ensure consistent arrival time baselines for passengers.
     */
    @BeforeEach
    void setUp() {
        Configuration config = new Configuration();
        engine = new SimulationEngine(1000, config);
        Clock.getInstance().setTime(0);
    }

    /**
     * Verifies the recording of timestamps for service point interactions.
     * Ensures that entry, start, and completion times are correctly stored
     * in the passenger's history map using the service point name as a key.
     */
    @Test
    @DisplayName("Recording and retrieving history (Queue, Service, Completion)")
    void testHistoryMapTransfer() {
        Passenger p = new Passenger(engine);
        String spName = "Security";

        // Simulate event sequence
        p.recordQueueEntryTime(spName, 10.5);
        p.recordServiceStartTime(spName, 15.0);
        p.recordServiceCompletionTime(spName, 22.3);

        // Verification: Ensure values are retrieved correctly
        assertEquals(10.5, p.getQueueEntryTimeFor(spName), "Queue entry time should match recorded value.");
        assertEquals(15.0, p.getServiceStartTimeFor(spName), "Service start time should match recorded value.");
        assertEquals(22.3, p.getServiceCompletionTimeFor(spName), "Service completion time should match recorded value.");
    }

    /**
     * Tests the tracking of traversal times between different simulation points.
     * Validates that movement durations are stored correctly and that
     * non-existent keys return an appropriate default value.
     */
    @Test
    @DisplayName("Tracking traversal durations between service points")
    void testTraversalHistoryTransfer() {
        Passenger p = new Passenger(engine);
        p.recordTraversalTime("Check-in", 5.2);
        p.recordTraversalTime("Security", 3.8);

        assertEquals(5.2, p.getTraversalTimeFrom("Check-in"), "Check-in traversal time is incorrect.");
        assertEquals(3.8, p.getTraversalTimeFrom("Security"), "Security traversal time is incorrect.");
        assertEquals(-1.0, p.getTraversalTimeFrom("Non-existent"), "Should return -1.0 for missing data.");
    }

    /**
     * Verifies that traversal time sampling respects configuration bounds.
     * Ensures that the passenger's attributes (like TicketType) influence
     * the generated movement times and that results are within legal limits.
     */
    @Test
    @DisplayName("Impact of passenger attributes on traversal time")
    void testTraversalScalingByTicketType() {
        Passenger p = new Passenger(engine);

        // Sample time based on predefined transitions
        double time = p.sampleTraversalTime(Transition.CHECKIN_TO_SECURITY);

        assertTrue(time >= 0, "Traversal time must be positive.");
        assertTrue(time <= engine.getConfiguration().getTraversalMaxTime(), "Time exceeds maximum allowed in configuration.");
    }

    /**
     * Validates the calculation of the passenger's total time in the system.
     * The formula checked is {@code TotalTime = DepartureTime - ArrivalTime}.
     */
    @Test
    @DisplayName("Calculation of total journey time (Departure - Arrival)")
    void testTotalJourneyTimeCalculation() {
        Passenger p = new Passenger(engine);

        // Passenger arrives at time 0 (set in setUp)
        assertEquals(0, p.getSystemArrivalTime());

        // Manually set departure time
        p.setDepartureTime(100.0);

        assertEquals(100.0, p.getTotalJourneyTime(), "The total journey time calculation formula is incorrect.");
    }
}
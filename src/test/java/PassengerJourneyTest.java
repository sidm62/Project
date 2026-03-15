import org.example.Model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the {@link Passenger} journey logic.
 * This class verifies how a passenger moves through the simulation timeline
 * and ensures that time-dependent attributes are updated correctly as
 * the simulation clock progresses.
 */
@DisplayName("Passenger Journey: Lifecycle and Timeline Integration Tests")
class PassengerJourneyTest {

    private SimulationEngine engine;
    private Clock clock;

    /**
     * Resets the simulation environment before each test.
     * Reinitializes the singleton {@link Clock}, resets the {@link Passenger}
     * ID counter, and creates a fresh {@link SimulationEngine} to ensure
     * that tests are independent and repeatable.
     */
    @BeforeEach
    void setUp() {
        clock = Clock.getInstance();
        clock.reset(); // Custom reset method to clear the singleton state
        engine = new SimulationEngine(1000.0, new Configuration());
        Passenger.resetIdCounter();
    }

    /**
     * Verifies the full lifecycle of a passenger from arrival to departure.
     * Ensures that the total journey time is calculated correctly by
     * subtracting the system arrival time from the final departure time.
     */
    @Test
    @DisplayName("Test passenger lifecycle with EventType values")
    void testPassengerFullEventJourney() {
        clock.setTime(10.0);
        Passenger p = new Passenger(engine); // System arrival time captured as 10.0

        clock.setTime(100.0);
        p.setDepartureTime(clock.getTime());

        assertEquals(10.0, p.getSystemArrivalTime(), "Arrival time was not captured correctly.");
        assertEquals(100.0, p.getDepartureTime(), "Departure time was not recorded correctly.");
        assertEquals(90.0, p.getTotalJourneyTime(), 0.001, "The total journey time calculation failed.");
    }

    /**
     * Ensures that the simulation clock progresses logically in accordance
     * with the event chain.
     * Validates that events occur in a linear, forward-moving sequence:
     * Arrival -> Service Completion -> Removal.
     */
    @Test
    @DisplayName("Verify clock progression following the EventType chain")
    void testClockProgressionWithEvents() {
        Passenger p = new Passenger(engine);

        double t1 = 10.0; // Simulated ARRIVAL_SYSTEM
        double t2 = 40.0; // Simulated NORMAL_CHECKIN_COMPLETE
        double t3 = 80.0; // Simulated BOARDING_COMPLETE

        clock.setTime(t1);
        double arrival = clock.getTime();

        clock.setTime(t2);
        double checkin = clock.getTime();

        clock.setTime(t3);
        p.setRemovalTime(clock.getTime());

        assertTrue(arrival < checkin, "Check-in event must occur after system arrival.");
        assertTrue(checkin < p.getRemovalTime(), "Removal/Boarding must occur after check-in.");
    }

    /**
     * Tests the integrity of service history when mapped to specific event types.
     * Confirms that timestamps for multiple different service points are
     * stored accurately and can be retrieved using unique event identifiers.
     */
    @Test
    @DisplayName("Test service history with specific EventType categories")
    void testServiceHistoryWithSpecificEvents() {
        Passenger p = new Passenger(engine);

        // Using EventType names as keys for history mapping
        String checkinPoint = EventType.NORMAL_CHECKIN_COMPLETE.name();
        String boardingPoint = EventType.BOARDING_COMPLETE.name();

        clock.setTime(50.0);
        p.recordServiceCompletionTime(checkinPoint, clock.getTime());

        clock.setTime(120.0);
        p.recordServiceCompletionTime(boardingPoint, clock.getTime());

        assertEquals(50.0, p.getServiceCompletionTimeFor(checkinPoint), "Check-in completion time mismatch.");
        assertEquals(120.0, p.getServiceCompletionTimeFor(boardingPoint), "Boarding completion time mismatch.");
    }
}
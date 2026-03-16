package org.example.test;

import org.example.Model.Event;
import org.example.Model.EventType;
import org.example.Model.Passenger;
import org.example.Model.SimulationEngine;
import org.example.Model.Configuration;
import org.junit.jupiter.api.*;
import java.util.PriorityQueue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Event} class.
 * Focuses on verifying the integrity of event data and the correctness of
 * the comparison logic used for chronological sorting in the simulation.
 */
@DisplayName("Event: Testing Event Data and Chronological Comparison")
class EventTest {
    private Passenger mockPassenger;
    private SimulationEngine engine;

    /**
     * Sets up a mock environment before each test.
     * Initializes a simulation engine and a passenger to be used as
     * data for the event objects.
     */
    @BeforeEach
    void setUp() {
        Configuration config = new Configuration();
        engine = new SimulationEngine(1000, config);
        mockPassenger = new Passenger(engine);
    }

    /**
     * Verifies that an event object correctly stores and returns its properties.
     * Checks if the time, event type, and associated passenger remain
     * unchanged after instantiation.
     */
    @Test
    @DisplayName("Event data should be stored correctly")
    void testEventCreation() {
        double time = 45.5;
        EventType type = EventType.ARRIVAL_SYSTEM;

        Event e = new Event(time, type, mockPassenger);

        assertEquals(time, e.getEventTime(), "The event time should match the input value.");
        assertEquals(type, e.getType(), "The event type should match the input value.");
        assertEquals(mockPassenger, e.getPassenger(), "The associated passenger should match the input value.");
    }

    /**
     * Tests the implementation of the {@link Comparable} interface.
     * Ensures that events are compared based on their execution time,
     * which is critical for the priority-based scheduling in the simulation.
     */
    @Test
    @DisplayName("Comparison of events based on time (compareTo)")
    void testEventComparison() {
        Event early = new Event(10.0, EventType.ARRIVAL_SYSTEM, mockPassenger);
        Event late = new Event(20.0, EventType.ARRIVAL_SYSTEM, mockPassenger);
        Event sameTime = new Event(10.0, EventType.BOARDING_COMPLETE, mockPassenger);

        // Verification of chronological ordering
        assertTrue(early.compareTo(late) < 0, "Earlier event should be considered 'smaller' than a later event.");
        assertTrue(late.compareTo(early) > 0, "Later event should be considered 'larger' than an earlier event.");

        // Handling of simultaneous events
        assertEquals(0, early.compareTo(sameTime), "Events with the same time should have a comparison result of 0.");
    }

    /**
     * Validates that a PriorityQueue correctly orders events.
     * This simulates the behavior of the EventList, ensuring that regardless
     * of insertion order, events are processed from earliest to latest.
     */
    @Test
    @DisplayName("PriorityQueue ordering of events")
    void testPriorityQueueOrdering() {
        PriorityQueue<Event> eventList = new PriorityQueue<>();

        // Adding events in a non-chronological order
        eventList.add(new Event(50.0, EventType.BOARDING_COMPLETE, mockPassenger));
        eventList.add(new Event(10.0, EventType.ARRIVAL_SYSTEM, mockPassenger));
        eventList.add(new Event(30.0, EventType.NORMAL_CHECKIN_COMPLETE, mockPassenger));

        // Verification: polling should always return the earliest event
        assertEquals(10.0, eventList.poll().getEventTime(), "The first event polled should be 10.0.");
        assertEquals(30.0, eventList.poll().getEventTime(), "The second event polled should be 30.0.");
        assertEquals(50.0, eventList.poll().getEventTime(), "The third event polled should be 50.0.");
    }
}
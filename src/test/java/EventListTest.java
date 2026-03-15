package org.example.Model;

import org.example.Model.Event;
import org.example.Model.EventList;
import org.example.Model.EventType;
import org.example.Model.Passenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link EventList} class.
 * Ensures that the priority queue mechanism correctly manages the simulation
 * timeline by ordering events chronologically and handling empty states.
 */
@DisplayName("EventList: Timeline and Priority Management Tests")
public class EventListTest {
    private EventList eventList;

    /**
     * Initializes a fresh EventList before each test.
     * This ensures that event stacks from previous tests do not interfere
     * with the current simulation timeline being tested.
     */
    @BeforeEach
    void setUp() {
        eventList = new EventList();
    }

    /**
     * Verifies that events are returned in chronological order regardless of
     * the order in which they were scheduled.
     * This is the core requirement for a Discrete Event Simulation (DES)
     * to maintain a consistent flow of time.
     */
    @Test
    @DisplayName("Events should be retrieved in chronological order")
    void testEventOrdering() {
        Passenger p = null;

        // Schedule events in a non-linear order
        eventList.schedule(new Event(100.5, EventType.ARRIVAL_SYSTEM, p));
        eventList.schedule(new Event(50.0, EventType.ARRIVAL_SYSTEM, p));
        eventList.schedule(new Event(75.2, EventType.ARRIVAL_SYSTEM, p));

        // Verification: The PriorityQueue should reorder them based on time
        assertEquals(50.0, eventList.getNextEvent().getEventTime(),
                "The event at time 50.0 should be processed first.");

        assertEquals(75.2, eventList.getNextEvent().getEventTime(),
                "The event at time 75.2 should be processed second.");

        assertEquals(100.5, eventList.getNextEvent().getEventTime(),
                "The event at time 100.5 should be processed last.");
    }

    /**
     * Tests the state management of the EventList.
     * Ensures that the list correctly reports its empty/non-empty status
     * as events are added and removed.
     */
    @Test
    @DisplayName("isEmpty functionality check")
    void testIsEmpty() {
        assertTrue(eventList.isEmpty(), "List should be empty initially.");

        eventList.schedule(new Event(10.0, EventType.ARRIVAL_SYSTEM, null));
        assertFalse(eventList.isEmpty(), "List should not be empty after scheduling an event.");

        eventList.getNextEvent();
        assertTrue(eventList.isEmpty(), "List should be empty again after polling the only event.");
    }

    /**
     * Verifies the behavior when attempting to retrieve events from an empty list.
     * Ensures that the system returns null rather than crashing,
     * allowing for safe simulation termination.
     */
    @Test
    @DisplayName("Polling from an empty list")
    void testPollEmptyList() {
        assertNull(eventList.getNextEvent(), "Polling an empty list should return null.");
    }
}
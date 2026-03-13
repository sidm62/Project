package org.example.Model;

import java.util.PriorityQueue;

/**
 * Stores and manages simulation events.
 *
 * Events are stored in a priority queue so that the event with
 * the earliest event time is always processed first.
 *
 * The event list is used by the simulation engine to schedule
 * future events and retrieve the next event to execute.
 */
public class EventList {

    /**
     * Priority queue storing all scheduled events.
     * Events are automatically ordered by their event time.
     */
    private PriorityQueue<Event> events;

    /**
     * Creates an empty event list.
     */
    public EventList() {
        events = new PriorityQueue<>();
    }

    /**
     * Adds a new event to the event list.
     *
     * @param event event to be scheduled
     */
    public void schedule(Event event) {
        events.add(event);
    }

    /**
     * Returns and removes the next event from the event list.
     *
     * The event returned is the one with the smallest event time.
     *
     * @return next scheduled event, or null if the list is empty
     */
    public Event getNextEvent() {
        return events.poll();
    }

    /**
     * Checks whether the event list is empty.
     *
     * @return true if no events are scheduled
     */
    public boolean isEmpty() {
        return events.isEmpty();
    }

}

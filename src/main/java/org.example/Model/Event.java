package org.example.Model;

/**
 * Represents an event in the simulation.
 *
 * An event contains the time when it occurs, the type of event,
 * and the passenger related to the event. Events are comparable
 * so that they can be ordered by event time in the event list.
 */
public class Event implements Comparable<Event> {

    /**
     * Time when the event occurs in the simulation.
     */
    private final double eventTime;

    /**
     * Type of the event.
     */
    private final EventType type;

    /**
     * Passenger associated with the event.
     */
    private final Passenger passenger;

    /**
     * Creates a new event.
     *
     * @param eventTime time when the event occurs
     * @param type type of the event
     * @param passenger passenger related to the event
     */
    public Event(double eventTime, EventType type, Passenger passenger) {
        this.eventTime = eventTime;
        this.type = type;
        this.passenger = passenger;
    }

    /**
     * Returns the time of the event.
     *
     * @return event time
     */
    public double getEventTime() {
        return eventTime;
    }

    /**
     * Returns the type of the event.
     *
     * @return event type
     */
    public EventType getType() {
        return type;
    }

    /**
     * Returns the passenger related to the event.
     *
     * @return passenger
     */
    public Passenger getPassenger() {
        return passenger;
    }

    /**
     * Compares two events based on their event time.
     * This allows events to be ordered in a priority queue.
     *
     * @param other the event to compare with
     * @return negative if this event occurs earlier,
     *         zero if times are equal,
     *         positive if this event occurs later
     */
    @Override
    public int compareTo(Event other) {
        return Double.compare(this.eventTime, other.eventTime);
    }
}
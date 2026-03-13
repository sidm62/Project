package org.example.Model;

/**
 * Enumeration of all possible event types in the simulation.
 *
 * Each value represents a specific event that can occur during the
 * passenger flow through the airport system. These events are used
 * by the simulation engine to determine what action should happen
 * next when an event is processed.
 */
public enum EventType {

    /** Passenger arrives to the airport system. */
    ARRIVAL_SYSTEM,

    /** Passenger arrives at the normal check-in counter. */
    ARRIVAL_NORMAL_CHECKIN,

    /** Normal check-in service is completed. */
    NORMAL_CHECKIN_COMPLETE,

    /** Passenger arrives at the self check-in machine. */
    ARRIVAL_SELF_CHECKIN,

    /** Self check-in process is completed. */
    SELF_CHECKIN_COMPLETE,

    /** Passenger arrives at regular security control. */
    ARRIVAL_REGULAR_SECURITY,

    /** Regular security check is completed. */
    REGULAR_SECURITY_COMPLETE,

    /** Passenger arrives at fast-track security. */
    ARRIVAL_FASTTRACK_SECURITY,

    /** Fast-track security check is completed. */
    FASTTRACK_SECURITY_COMPLETE,

    /** Passenger arrives at customs control. */
    ARRIVAL_CUSTOMS,

    /** Customs processing is completed. */
    CUSTOMS_COMPLETE,

    /** Passenger arrives at boarding gate. */
    ARRIVAL_BOARDING,

    /** Boarding process is completed. */
    BOARDING_COMPLETE
}
package org.example.Model;

/**
 * Defines the passenger ticket classes used in the airport simulation.
 *
 * The ticket type affects several passenger properties such as
 * service speed, security access, and probability values used
 * during the simulation.
 */
public enum TicketType {

    /** Economy class ticket. */
    ECONOMY,

    /** Business class ticket. */
    BUSINESS,

    /** First class ticket. */
    FIRST
}


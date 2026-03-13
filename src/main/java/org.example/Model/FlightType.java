package org.example.Model;

/**
 * Represents the type of a flight in the airport simulation.
 *
 * The flight type determines the passenger flow in the system.
 * Domestic passengers typically go directly to boarding,
 * while international passengers may go through customs
 * before boarding.
 */
public enum FlightType {

    /**
     * Domestic flight within the same country.
     */
    DOMESTIC,

    /**
     * International flight between different countries.
     */
    INTERNATIONAL
}

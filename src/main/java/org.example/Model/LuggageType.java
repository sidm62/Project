package org.example.Model;

/**
 * Defines the luggage types used in the airport simulation.
 *
 * The luggage type can affect the processing path of a passenger,
 * for example oversized luggage may require normal check-in
 * instead of self check-in.
 */
public enum LuggageType {

    /** Standard size luggage. */
    STANDARD,

    /** Oversized luggage that may require special handling. */
    OVERSIZED
}
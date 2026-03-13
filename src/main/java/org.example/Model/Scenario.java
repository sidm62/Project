package org.example.Model;

/**
 * Defines the possible simulation scenarios.
 *
 * Each scenario represents a different airport operating condition.
 * The simulation engine may adjust service times and system behaviour
 * based on the selected scenario.
 */
public enum Scenario {

        /** Normal passenger traffic level. */
        NORMAL,

        /** High passenger traffic during peak hours. */
        PEAK_TIME,

        /** Lower than normal passenger traffic. */
        LOW_TRAFFIC,

        /** Extremely high load causing system stress. */
        SYSTEM_STRESS,

        /** System recovering after overload or disruption. */
        RECOVERY_MODE
}


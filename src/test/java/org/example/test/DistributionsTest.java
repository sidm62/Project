package org.example.test;

import org.example.Model.distributions.Negexp;
import org.example.Model.distributions.Normal;
import org.example.Model.distributions.Uniform;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the probability distribution classes.
 * These tests ensure that the random number generators produce mathematically
 * valid and physically possible values for the simulation (e.g., no negative time).
 */
@DisplayName("Distributions: Validation of Random Generators")
class DistributionsTest {

    /**
     * Verifies that the Negative Exponential distribution (Negexp)
     * never produces negative values.
     * In an airport simulation, this ensures that the time between
     * passenger arrivals is always a positive duration.
     */
    @Test
    @DisplayName("Negexp (Exponential) distribution: Non-negative values")
    void testNegexpNonNegative() {
        // Parameters: mean = 5.0, seed = 123
        Negexp dist = new Negexp(5.0, 123);

        for (int i = 0; i < 1000; i++) {
            double value = dist.sample();
            assertTrue(value >= 0, "Negexp produced a negative value: " + value);
        }
    }

    /**
     * Verifies that the Normal distribution remains positive within
     * the simulation's safety boundaries.
     * Normal distributions can mathematically return negative values;
     * this test ensures our implementation clamps or protects against
     * negative service times.
     */
    @Test
    @DisplayName("Normal distribution: Positive values only")
    void testNormalPositive() {
        // Parameters: mean = 10.0, variance = 2.0
        Normal dist = new Normal(10.0, 2.0, 123);

        for (int i = 0; i < 1000; i++) {
            double value = dist.sample();
            // Critical check for simulation stability
            assertTrue(value >= 0, "Normal distribution produced a negative value: " + value);
        }
    }

    /**
     * Verifies that the Uniform distribution strictly adheres
     * to the specified minimum and maximum bounds.
     * This is essential for parameters like luggage weights or
     * specific processing windows where boundaries are fixed.
     */
    @Test
    @DisplayName("Uniform distribution: Adherence to bounds")
    void testUniformBounds() {
        double min = 1.0;
        double max = 3.0;
        Uniform dist = new Uniform(min, max, 123);

        for (int i = 0; i < 1000; i++) {
            double value = dist.sample();
            assertTrue(value >= min && value <= max,
                    "Uniform value " + value + " is outside the bounds [" + min + "," + max + "]");
        }
    }
}
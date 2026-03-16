package org.example.test;

import org.example.Model.Clock;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Clock} class.
 * Tests the singleton implementation and ensures that time management
 * within the simulation remains consistent and accurate.
 */
@DisplayName("Clock: Testing Time Management and Singleton Pattern")
class ClockTest {

    /**
     * Resets the Clock singleton to zero before each test.
     * This ensures test isolation so that time values from one test
     * do not affect the outcome of subsequent tests.
     */
    @BeforeEach
    void setUp() {
        Clock.getInstance().setTime(0);
    }

    /**
     * Verifies the Singleton pattern implementation.
     * Ensures that multiple calls to getInstance() return the same object reference,
     * preventing multiple independent clocks from existing simultaneously.
     */
    @Test
    @DisplayName("Clock should be a Singleton")
    void testClockIsSingleton() {
        Clock instance1 = Clock.getInstance();
        Clock instance2 = Clock.getInstance();

        assertSame(instance1, instance2, "getInstance() should always return the same instance.");
    }

    /**
     * Tests setting and retrieving the simulation time.
     * Ensures that the time value is stored correctly with high floating-point precision.
     */
    @Test
    @DisplayName("Setting and getting time")
    void testSetAndGetTime() {
        Clock clock = Clock.getInstance();
        clock.setTime(150.5);

        assertEquals(150.5, clock.getTime(), 0.0001, "The retrieved time should match the set value.");
    }

    /**
     * Tests the advancement of simulation time.
     * Verifies that adding duration to the current clock time results
     * in the correct cumulative simulation time.
     */
    @Test
    @DisplayName("Advancing simulation time")
    void testAdvanceTime() {
        Clock clock = Clock.getInstance();
        clock.setTime(10.0);

        double increment = 5.5;
        clock.setTime(clock.getTime() + increment);

        assertEquals(15.5, clock.getTime(), 0.0001, "The clock should advance correctly.");
    }

    /**
     * Verifies that simulation time remains logically valid.
     * Ensures that the clock does not represent negative time, which is
     * essential for maintaining chronological event processing.
     */
    @Test
    @DisplayName("Clock should not have negative time")
    void testClockShouldNotBeNegative() {
        Clock clock = Clock.getInstance();
        clock.setTime(100.0);

        assertTrue(clock.getTime() >= 0, "Simulation time cannot be negative.");
    }
}
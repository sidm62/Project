package org.example.test;

import org.example.Model.Configuration;
import org.example.Model.Scenario;
import org.example.Model.ServicePoint;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Configuration} class.
 * Validates the management of simulation parameters, scenario-based
 * adjustments, and the integrity of probability distributions.
 */
@DisplayName("Configuration: Deep Testing of System Settings")
class ConfigurationTest {
    private Configuration config;

    /**
     * Initializes a new Configuration instance before each test.
     * Ensures a clean state for testing parameter adjustments.
     */
    @BeforeEach
    void setUp() {
        config = new Configuration();
    }

    /**
     * Verifies that service means are correctly loaded for different scenarios.
     * Ensures that the SYSTEM_STRESS scenario correctly reflects higher load
     * (higher mean service times) compared to the NORMAL scenario.
     */
    @Test
    @DisplayName("Scenario-specific values loading")
    void testScenarioServiceMeans() {
        var scenarioMap = config.getScenarioServiceMeans();

        assertTrue(scenarioMap.containsKey(Scenario.NORMAL), "Normal scenario is missing.");
        assertTrue(scenarioMap.containsKey(Scenario.SYSTEM_STRESS), "Stress scenario is missing.");

        double normalCheckin = scenarioMap.get(Scenario.NORMAL).get(ServicePoint.NORMAL_CHECKIN);
        double stressCheckin = scenarioMap.get(Scenario.SYSTEM_STRESS).get(ServicePoint.NORMAL_CHECKIN);

        assertTrue(stressCheckin > normalCheckin, "Stress scenario service times should be higher than normal.");
    }

    /**
     * Tests the influence of individual speed factors on service means.
     * Ensures that the calculation {@code result = baseMean / factor} is
     * applied correctly when adjusting the speed of specific service points.
     */
    @Test
    @DisplayName("Impact of individual speed factors")
    void testIndividualSpeedFactors() {
        // Default base is 4.0. Setting factor to 2.0 (double speed)
        config.setIndividualServiceSpeedFactors(ServicePoint.NORMAL_CHECKIN, 2.0);

        double result = config.getServiceMeanFor(ServicePoint.NORMAL_CHECKIN);

        assertEquals(2.0, result, 0.001, "Mean should be halved when speed factor is 2.0.");
    }

    /**
     * Verifies that the configuration handles invalid inputs gracefully.
     * Ensures that querying a non-existent service point triggers an
     * {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("Handling of unknown service points")
    void testUnknownServicePointThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            config.getServiceMeanFor("Hogwarts Express Gate");
        }, "Querying an unknown service point should throw an exception.");
    }

    /**
     * Ensures the mathematical integrity of passenger distributions.
     * Validates that the sum of ticket type probabilities equals exactly 100% (1.0).
     */
    @Test
    @DisplayName("Integrity of ticket type probabilities")
    void testTicketTypeProbabilities() {
        double total = config.getEconomyProbability() +
                config.getBusinessProbability() +
                config.getFirstProbability();

        assertEquals(1.0, total, 0.001, "The sum of ticket type probabilities must equal 100%.");
    }

    /**
     * Tests standard configuration setters and getters.
     * Verifies that global simulation parameters can be updated dynamically
     * during the setup phase.
     */
    @Test
    @DisplayName("Functionality of configuration setters")
    void testSetters() {
        config.setServiceMeanFor(ServicePoint.CUSTOMS, 12.5);
        assertEquals(12.5, config.getServiceMeanFor(ServicePoint.CUSTOMS), "Updating Customs value failed.");

        config.setArrivalLambda(5.5);
        assertEquals(5.5, config.getArrivalLambda(), "Updating arrival lambda failed.");
    }
}
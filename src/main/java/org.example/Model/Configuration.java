package org.example.Model;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores configuration values used by the airport simulation.
 *
 * The configuration contains random seed settings, arrival parameters,
 * service time means and standard deviations, traversal parameters,
 * scaling factors, probabilities, and scenario-specific service means.
 *
 * This class provides getter methods for reading configuration values
 * and selected setter methods for updating simulation parameters.
 */
public class Configuration {

    // =========================
    // RANDOMNESS
    // =========================
    /**
     * Random seed used for reproducible simulation runs.
     */
    private long randomSeed = 12345L;



    // =========================
    // ARRIVAL PARAMETERS
    // =========================
    /**
     * Type of arrival time distribution.
     */
    private String arrivalDistributionType = "NEGEXP";
    /**
     * Mean value used for passenger arrivals.
     */
    private double arrivalMean = 3.333;

    // =========================
    // BASE SERVICE MEANS
    // =========================
    /**
     * Base mean service time for normal check-in.
     */
    private double normalCheckinMean = 4.0;
    /**
     * Base mean service time for self check-in.
     */
    private double selfCheckinMean = 2.5;
    /**
     * Base mean service time for regular security.
     */
    private double regularSecurityMean = 3.5;
    /**
     * Base mean service time for fast track security.
     */
    private double fasttrackSecurityMean = 2.0;
    /**
     * Base mean service time for customs.
     */
    private double customsMean = 5.0;
    /**
     * Base mean service time for boarding.
     */
    private double boardingMean = 2.5;


    // =========================
    // BASE SERVICE STANDARD DEVIATIONS
    // =========================
    /**
     * Standard deviation for normal check-in service time.
     */
    private double normalCheckinStdDev = 1.0;
    /**
     * Standard deviation for self check-in service time.
     */
    private double selfCheckinStdDev = 0.7;
    /**
     * Standard deviation for regular security service time.
     */
    private double regularSecurityStdDev = 0.8;
    /**
     * Standard deviation for fast track security service time.
     */
    private double fasttrackSecurityStdDev = 0.5;
    /**
     * Standard deviation for customs service time.
     */
    private double customsStdDev = 1.2;
    /**
     * Standard deviation for boarding service time.
     */
    private double boardingStdDev = 0.6;

    // =========================
    // TRAVERSAL MEANS
    // =========================
    /**
     * Mean traversal time from check-in to security.
     */
    private double traversalCheckinToSecurityMean = 1.2;
    /**
     * Mean traversal time from security to customs.
     */
    private double traversalSecurityToCustomsMean = 1.5;
    /**
     * Mean traversal time from customs to boarding.
     */
    private double traversalCustomsToBoardingMean = 0.8;
    /**
     * Mean traversal time from security to boarding.
     */
    private double traversalSecurityToBoardingMean = 1.0;

    // =========================
    // TRAVERSAL STANDARD DEVIATIONS
    // =========================
    /**
     * Standard deviation of traversal time from check-in to security.
     */
    private double traversalCheckinToSecurityStdDev = 0.3;
    /**
     * Standard deviation of traversal time from security to customs.
     */
    private double traversalSecurityToCustomsStdDev = 0.3;
    /**
     * Standard deviation of traversal time from customs to boarding.
     */
    private double traversalCustomsToBoardingStdDev = 0.3;
    /**
     * Standard deviation of traversal time from security to boarding.
     */
    private double traversalSecurityToBoardingStdDev = 0.2;

    /**
     * Maximum allowed traversal time.
     */

    private double traversalMaxTime = 15.0;


    // =========================
    // SCALING FACTORS
    // =========================
    /**
     * Check-in scaling factor for economy passengers.
     */
    private double economyCheckinScale = 1.0;
    /**
     * Check-in scaling factor for business passengers.
     */
    private double businessCheckinScale = 0.8;
    /**
     * Check-in scaling factor for first-class passengers.
     */
    private double firstCheckinScale = 0.6;

    /**
     * Customs scaling factor for economy passengers.
     */
    private double economyCustomsScale = 1.0;
    /**
     * Customs scaling factor for business passengers.
     */
    private double businessCustomsScale = 0.85;
    /**
     * Customs scaling factor for first-class passengers.
     */
    private double firstCustomsScale = 0.7;

    /**
     * Extra weight factor used in security service time calculation.
     */
    private double securityWeightFactor = 0.15;

    // =========================
    // PROBABILITIES
    // =========================
    /**
     * Probability of oversized luggage for economy passengers.
     */
    private double economyOversizedProb = 0.2;
    /**
     * Probability of oversized luggage for business passengers.
     */
    private double businessOversizedProb = 0.15;
    /**
     * Probability of oversized luggage for first-class passengers.
     */
    private double firstOversizedProb = 0.10;

    /**
     * Probability of self check-in eligibility for economy passengers.
     */
    private double economySelfCheckinProb = 0.7;
    /**
     * Probability of self check-in eligibility for business passengers.
     */
    private double businessSelfCheckinProb = 0.4;
    /**
     * Probability of self check-in eligibility for first-class passengers.
     */
    private double firstSelfCheckinProb = 0.2;

    // =========================
    // TICKET TYPE PROBABILITIES
    // =========================
    /**
     * Probability of economy-class passengers.
     */
    private double economyProbability = 0.6;
    /**
     * Probability of business-class passengers.
     */
    private double businessProbability = 0.3;
    /**
     * Probability of first-class passengers.
     */
    private double firstProbability = 0.1;
    /**
     * Probability of domestic flights.
     */
    private double domesticProbability = 0.7;

    // =========================
    // GETTERS ONLY
    // =========================

    /**
     * Returns the probability of economy-class passengers.
     *
     * @return economy passenger probability
     */
    public double getEconomyProbability() { return economyProbability; }
    /**
     * Returns the probability of business-class passengers.
     *
     * @return business passenger probability
     */
    public double getBusinessProbability() { return businessProbability; }
    /**
     * Returns the probability of first-class passengers.
     *
     * @return first-class passenger probability
     */
    public double getFirstProbability() { return firstProbability; }
    /**
     * Returns the probability of domestic flights.
     *
     * @return domestic flight probability
     */
    public double getDomesticProbability() { return domesticProbability; }



    /**
     * Returns the arrival distribution type.
     *
     * @return arrival distribution type
     */
    public String getArrivalDistributionType() { return arrivalDistributionType; }
    /**
     * Returns the arrival mean value.
     *
     * @return arrival mean
     */
    public double getArrivalLambda() { return arrivalMean; }
    /**
     * Sets the arrival mean value.
     *
     * @param provided_mean new arrival mean
     */
    public void setArrivalLambda(double provided_mean) { arrivalMean = provided_mean; }

    /**
     * Returns the random seed used by the simulation.
     *
     * @return random seed
     */
    public long getRandomSeed() { return randomSeed; }
    /**
     * Returns the base mean service time for normal check-in.
     *
     * @return normal check-in mean
     */
    public double getNormalCheckinMean() { return normalCheckinMean; }
    /**
     * Returns the base mean service time for self check-in.
     *
     * @return self check-in mean
     */
    public double getSelfCheckinMean() { return selfCheckinMean; }
    /**
     * Returns the base mean service time for regular security.
     *
     * @return regular security mean
     */
    public double getRegularSecurityMean() { return regularSecurityMean; }
    /**
     * Returns the base mean service time for fast track security.
     *
     * @return fast track security mean
     */
    public double getFasttrackSecurityMean() { return fasttrackSecurityMean; }
    /**
     * Returns the base mean service time for customs.
     *
     * @return customs mean
     */
    public double getCustomsMean() { return customsMean; }
    /**
     * Returns the base mean service time for boarding.
     *
     * @return boarding mean
     */
    public double getBoardingMean() { return boardingMean; }

    /**
     * Returns the standard deviation for normal check-in service time.
     *
     * @return normal check-in standard deviation
     */
    public double getNormalCheckinStdDev() { return normalCheckinStdDev; }
    /**
     * Returns the standard deviation for self check-in service time.
     *
     * @return self check-in standard deviation
     */
    public double getSelfCheckinStdDev() { return selfCheckinStdDev; }
    /**
     * Returns the standard deviation for regular security service time.
     *
     * @return regular security standard deviation
     */
    public double getRegularSecurityStdDev() { return regularSecurityStdDev; }
    /**
     * Returns the standard deviation for fast track security service time.
     *
     * @return fast track security standard deviation
     */

    public double getFasttrackSecurityStdDev() { return fasttrackSecurityStdDev; }
    /**
     * Returns the standard deviation for customs service time.
     *
     * @return customs standard deviation
     */
    public double getCustomsStdDev() { return customsStdDev; }
    /**
     * Returns the standard deviation for boarding service time.
     *
     * @return boarding standard deviation
     */
    public double getBoardingStdDev() { return boardingStdDev; }


    /**
     * Returns the mean traversal time from check-in to security.
     *
     * @return traversal mean from check-in to security
     */
    public double getTraversalCheckinToSecurityMean() { return traversalCheckinToSecurityMean; }

    /**
     * Returns the mean traversal time from security to customs.
     *
     * @return traversal mean from security to customs
     */
    public double getTraversalSecurityToCustomsMean() { return traversalSecurityToCustomsMean; }

    /**
     * Returns the mean traversal time from customs to boarding.
     *
     * @return traversal mean from customs to boarding
     */
    public double getTraversalCustomsToBoardingMean() { return traversalCustomsToBoardingMean; }

    /**
     * Returns the mean traversal time from security to boarding.
     *
     * @return traversal mean from security to boarding
     */
    public double getTraversalSecurityToBoardingMean() { return traversalSecurityToBoardingMean; }

    /**
     * Returns the standard deviation of traversal time from check-in to security.
     *
     * @return traversal standard deviation from check-in to security
     */
    public double getTraversalCheckinToSecurityStdDev() { return traversalCheckinToSecurityStdDev; }

    /**
     * Returns the standard deviation of traversal time from security to customs.
     *
     * @return traversal standard deviation from security to customs
     */
    public double getTraversalSecurityToCustomsStdDev() { return traversalSecurityToCustomsStdDev; }

    /**
     * Returns the standard deviation of traversal time from customs to boarding.
     *
     * @return traversal standard deviation from customs to boarding
     */
    public double getTraversalCustomsToBoardingStdDev() { return traversalCustomsToBoardingStdDev; }

    /**
     * Returns the standard deviation of traversal time from security to boarding.
     *
     * @return traversal standard deviation from security to boarding
     */
    public double getTraversalSecurityToBoardingStdDev() { return traversalSecurityToBoardingStdDev; }

    /**
     * Returns the maximum allowed traversal time.
     *
     * @return traversal maximum time
     */
    public double getTraversalMaxTime() { return traversalMaxTime; }

    /**
     * Returns the check-in scaling factor for economy passengers.
     *
     * @return economy check-in scale
     */
    public double getEconomyCheckinScale() { return economyCheckinScale; }

    /**
     * Returns the check-in scaling factor for business passengers.
     *
     * @return business check-in scale
     */
    public double getBusinessCheckinScale() { return businessCheckinScale; }

    /**
     * Returns the check-in scaling factor for first-class passengers.
     *
     * @return first-class check-in scale
     */
    public double getFirstCheckinScale() { return firstCheckinScale; }

    /**
     * Returns the customs scaling factor for economy passengers.
     *
     * @return economy customs scale
     */
    public double getEconomyCustomsScale() { return economyCustomsScale; }

    /**
     * Returns the customs scaling factor for business passengers.
     *
     * @return business customs scale
     */
    public double getBusinessCustomsScale() { return businessCustomsScale; }

    /**
     * Returns the customs scaling factor for first-class passengers.
     *
     * @return first-class customs scale
     */
    public double getFirstCustomsScale() { return firstCustomsScale; }

    /**
     * Returns the weight factor used in security time calculation.
     *
     * @return security weight factor
     */
    public double getSecurityWeightFactor() { return securityWeightFactor; }

    /**
     * Returns the probability of oversized luggage for economy passengers.
     *
     * @return economy oversized luggage probability
     */
    public double getEconomyOversizedProb() { return economyOversizedProb; }

    /**
     * Returns the probability of oversized luggage for business passengers.
     *
     * @return business oversized luggage probability
     */
    public double getBusinessOversizedProb() { return businessOversizedProb; }

    /**
     * Returns the probability of oversized luggage for first-class passengers.
     *
     * @return first-class oversized luggage probability
     */
    public double getFirstOversizedProb() { return firstOversizedProb; }

    /**
     * Returns the probability of self check-in eligibility for economy passengers.
     *
     * @return economy self check-in probability
     */
    public double getEconomySelfCheckinProb() { return economySelfCheckinProb; }

    /**
     * Returns the probability of self check-in eligibility for business passengers.
     *
     * @return business self check-in probability
     */
    public double getBusinessSelfCheckinProb() { return businessSelfCheckinProb; }

    /**
     * Returns the probability of self check-in eligibility for first-class passengers.
     *
     * @return first-class self check-in probability
     */
    public double getFirstSelfCheckinProb() { return firstSelfCheckinProb; }

    /**
     * Scenario-specific service mean values for all service points.
     */
    private final Map<Scenario, Map<String, Double>> scenarioServiceMeans = new HashMap<>();
    /**
     * Returns scenario-specific service mean values.
     *
     * @return map of scenarios and their service mean values
     */
    public Map<Scenario, Map<String, Double>> getScenarioServiceMeans() { return scenarioServiceMeans; }

    /**
     * Creates a new configuration with default values.
     *
     * The constructor initializes scenario-specific service mean maps
     * for normal, peak, low traffic, stress, and recovery scenarios.
     */
    public Configuration() {

        // Base servie times

        /*
        private double normalCheckinMean = 4.0;
        private double selfCheckinMean = 2.5;
        private double regularSecurityMean = 3.5;
        private double fasttrackSecurityMean = 2.0;
        private double customsMean = 5.0;
        private double boardingMean = 2.5;
         */



        // Initialize default service means per scenario
        Map<String, Double> normalMeans = Map.of(
                ServicePoint.NORMAL_CHECKIN, 4.0,
                ServicePoint.SELF_CHECKIN, 2.5,
                ServicePoint.REGULAR_SECURITY, 3.5,
                ServicePoint.FASTTRACK_SECURITY, 2.0,
                ServicePoint.CUSTOMS, 5.0,
                ServicePoint.BOARDING, 2.5
        );

        Map<String, Double> peakMeans = Map.of(
                ServicePoint.NORMAL_CHECKIN, 5.0,
                ServicePoint.SELF_CHECKIN, 3.0,
                ServicePoint.REGULAR_SECURITY, 4.5,
                ServicePoint.FASTTRACK_SECURITY, 2.5,
                ServicePoint.CUSTOMS, 6.0,
                ServicePoint.BOARDING, 3.0
        );

        Map<String, Double> lowTrafficMeans = Map.of(
                ServicePoint.NORMAL_CHECKIN, 3.0,
                ServicePoint.SELF_CHECKIN, 2.0,
                ServicePoint.REGULAR_SECURITY, 2.5,
                ServicePoint.FASTTRACK_SECURITY, 1.5,
                ServicePoint.CUSTOMS, 4.0,
                ServicePoint.BOARDING, 2.0
        );

        Map<String, Double> systemStressMeans = Map.of(
                ServicePoint.NORMAL_CHECKIN, 8.0,
                ServicePoint.SELF_CHECKIN, 5.0,
                ServicePoint.REGULAR_SECURITY, 6.0,
                ServicePoint.FASTTRACK_SECURITY, 4.0,
                ServicePoint.CUSTOMS, 10.0,
                ServicePoint.BOARDING, 5.0
        );

        Map<String, Double> recoveryMeans = Map.of(
                ServicePoint.NORMAL_CHECKIN, 2.5,
                ServicePoint.SELF_CHECKIN, 1.5,
                ServicePoint.REGULAR_SECURITY, 2.0,
                ServicePoint.FASTTRACK_SECURITY, 1.2,
                ServicePoint.CUSTOMS, 3.0,
                ServicePoint.BOARDING, 1.8
        );

        // Map scenarios to their service means
        scenarioServiceMeans.put(Scenario.NORMAL, normalMeans);
        scenarioServiceMeans.put(Scenario.PEAK_TIME, peakMeans);
        scenarioServiceMeans.put(Scenario.LOW_TRAFFIC, lowTrafficMeans);
        scenarioServiceMeans.put(Scenario.SYSTEM_STRESS, systemStressMeans);
        scenarioServiceMeans.put(Scenario.RECOVERY_MODE, recoveryMeans);
    }


    /**
     * Returns the base service mean for the given service point.
     *
     * @param servicePointName name of the service point
     * @return service mean for the given service point
     * @throws IllegalArgumentException if the service point name is unknown
     */
    public double getServiceMeanFor(String servicePointName) {

        switch (servicePointName) {

            case ServicePoint.NORMAL_CHECKIN:
                return normalCheckinMean;

            case ServicePoint.SELF_CHECKIN:
                return selfCheckinMean;

            case ServicePoint.REGULAR_SECURITY:
                return regularSecurityMean;

            case ServicePoint.FASTTRACK_SECURITY:
                return fasttrackSecurityMean;

            case ServicePoint.CUSTOMS:
                return customsMean;

            case ServicePoint.BOARDING:
                return boardingMean;

            default:
                throw new IllegalArgumentException(
                        "Unknown service point: " + servicePointName
                );
        }
    }

    /**
     * Sets the base service mean for the given service point.
     *
     * @param servicePointName name of the service point
     * @param mean new service mean
     * @throws IllegalArgumentException if the service point name is unknown
     */
    public void setServiceMeanFor(String servicePointName, double mean) {
        switch (servicePointName) {
            case ServicePoint.NORMAL_CHECKIN:
                normalCheckinMean = mean;
                break;
            case ServicePoint.SELF_CHECKIN:
                selfCheckinMean = mean;
                break;
            case ServicePoint.REGULAR_SECURITY:
                regularSecurityMean = mean;
                break;
            case ServicePoint.FASTTRACK_SECURITY:
                fasttrackSecurityMean = mean;
                break;
            case ServicePoint.CUSTOMS:
                customsMean = mean;
                break;
            case ServicePoint.BOARDING:
                boardingMean = mean;
                break;
            default:
                throw new IllegalArgumentException("Unknown service point: " + servicePointName);
        }
    }





}
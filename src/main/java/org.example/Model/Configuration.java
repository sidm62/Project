import java.util.HashMap;
import java.util.Map;

public class Configuration {

    // =========================
    // RANDOMNESS
    // =========================
    private long randomSeed = 12345L;



    // =========================
    // ARRIVAL PARAMETERS
    // =========================
    private String arrivalDistributionType = "NEGEXP";
    private double arrivalMean = 3.333;

    // =========================
    // BASE SERVICE MEANS
    // =========================
    private double normalCheckinMean = 4.0;
    private double selfCheckinMean = 2.5;
    private double regularSecurityMean = 3.5;
    private double fasttrackSecurityMean = 2.0;
    private double customsMean = 5.0;
    private double boardingMean = 2.5;


    // =========================
    // BASE SERVICE STANDARD DEVIATIONS
    // =========================
    private double normalCheckinStdDev = 1.0;
    private double selfCheckinStdDev = 0.7;
    private double regularSecurityStdDev = 0.8;
    private double fasttrackSecurityStdDev = 0.5;
    private double customsStdDev = 1.2;
    private double boardingStdDev = 0.6;

    // =========================
    // TRAVERSAL MEANS
    // =========================
    private double traversalCheckinToSecurityMean = 1.2;
    private double traversalSecurityToCustomsMean = 1.5;
    private double traversalCustomsToBoardingMean = 0.8;
    private double traversalSecurityToBoardingMean = 1.0;

    // =========================
    // TRAVERSAL STANDARD DEVIATIONS
    // =========================
    private double traversalCheckinToSecurityStdDev = 0.3;
    private double traversalSecurityToCustomsStdDev = 0.3;
    private double traversalCustomsToBoardingStdDev = 0.3;
    private double traversalSecurityToBoardingStdDev = 0.2;

    private double traversalMaxTime = 15.0;


    // =========================
    // SCALING FACTORS
    // =========================
    private double economyCheckinScale = 1.0;
    private double businessCheckinScale = 0.8;
    private double firstCheckinScale = 0.6;

    private double economyCustomsScale = 1.0;
    private double businessCustomsScale = 0.85;
    private double firstCustomsScale = 0.7;

    private double securityWeightFactor = 0.15;

    // =========================
    // PROBABILITIES
    // =========================
    private double economyOversizedProb = 0.2;
    private double businessOversizedProb = 0.15;
    private double firstOversizedProb = 0.10;

    private double economySelfCheckinProb = 0.7;
    private double businessSelfCheckinProb = 0.4;
    private double firstSelfCheckinProb = 0.2;

    // =========================
    // TICKET TYPE PROBABILITIES
    // =========================
    private double economyProbability = 0.6;
    private double businessProbability = 0.3;
    private double firstProbability = 0.1;
    private double domesticProbability = 0.7;

    // =========================
    // GETTERS ONLY
    // =========================

    public double getEconomyProbability() { return economyProbability; }
    public double getBusinessProbability() { return businessProbability; }
    public double getFirstProbability() { return firstProbability; }
    public double getDomesticProbability() { return domesticProbability; }



    public String getArrivalDistributionType() { return arrivalDistributionType; }
    public double getArrivalLambda() { return arrivalMean; }
    public void setArrivalLambda(double provided_mean) { arrivalMean = provided_mean; }

    public long getRandomSeed() { return randomSeed; }
    public double getNormalCheckinMean() { return normalCheckinMean; }
    public double getSelfCheckinMean() { return selfCheckinMean; }
    public double getRegularSecurityMean() { return regularSecurityMean; }
    public double getFasttrackSecurityMean() { return fasttrackSecurityMean; }
    public double getCustomsMean() { return customsMean; }
    public double getBoardingMean() { return boardingMean; }

    public double getNormalCheckinStdDev() { return normalCheckinStdDev; }
    public double getSelfCheckinStdDev() { return selfCheckinStdDev; }
    public double getRegularSecurityStdDev() { return regularSecurityStdDev; }
    public double getFasttrackSecurityStdDev() { return fasttrackSecurityStdDev; }
    public double getCustomsStdDev() { return customsStdDev; }
    public double getBoardingStdDev() { return boardingStdDev; }


    public double getTraversalCheckinToSecurityMean() { return traversalCheckinToSecurityMean; }
    public double getTraversalSecurityToCustomsMean() { return traversalSecurityToCustomsMean; }
    public double getTraversalCustomsToBoardingMean() { return traversalCustomsToBoardingMean; }
    public double getTraversalSecurityToBoardingMean() { return traversalSecurityToBoardingMean; }

    public double getTraversalCheckinToSecurityStdDev() { return traversalCheckinToSecurityStdDev; }
    public double getTraversalSecurityToCustomsStdDev() { return traversalSecurityToCustomsStdDev; }
    public double getTraversalCustomsToBoardingStdDev() { return traversalCustomsToBoardingStdDev; }
    public double getTraversalSecurityToBoardingStdDev() { return traversalSecurityToBoardingStdDev; }

    public double getTraversalMaxTime() { return traversalMaxTime; }


    public double getEconomyCheckinScale() { return economyCheckinScale; }
    public double getBusinessCheckinScale() { return businessCheckinScale; }
    public double getFirstCheckinScale() { return firstCheckinScale; }

    public double getEconomyCustomsScale() { return economyCustomsScale; }
    public double getBusinessCustomsScale() { return businessCustomsScale; }
    public double getFirstCustomsScale() { return firstCustomsScale; }

    public double getSecurityWeightFactor() { return securityWeightFactor; }

    public double getEconomyOversizedProb() { return economyOversizedProb; }
    public double getBusinessOversizedProb() { return businessOversizedProb; }
    public double getFirstOversizedProb() { return firstOversizedProb; }

    public double getEconomySelfCheckinProb() { return economySelfCheckinProb; }
    public double getBusinessSelfCheckinProb() { return businessSelfCheckinProb; }
    public double getFirstSelfCheckinProb() { return firstSelfCheckinProb; }

    private final Map<Scenario, Map<String, Double>> scenarioServiceMeans = new HashMap<>();
    public Map<Scenario, Map<String, Double>> getScenarioServiceMeans() { return scenarioServiceMeans; }

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

package org.example.Model;

import org.example.Model.distributions.Bernoulli;
import org.example.Model.distributions.ContinuousGenerator;
import org.example.Model.distributions.DiscreteGenerator;
import org.example.Model.distributions.Normal;
import org.example.Model.distributions.Uniform;

import java.util.HashMap;

public class Passenger {

    // =========================
    // Attributes
    // =========================
    private FlightType flightType;       // Domestic / International
    private TicketType ticketType;       // Economy / Business / First
    private LuggageType checkinLuggage;   // Standard / Oversized
    private double carryOnWeight;    // 0–8 kg
    private boolean eligibleForSelfCheckin;
    private double systemArrivalTime; // Time tracking
    private double departureTime; // Time tracking

    private double queueEntryTime;
    private double serviceStartTime;
    private double serviceCompletionTime;

    // Service timestamps per service point
    private HashMap<String, Double> queueEntryHistory = new HashMap<>();
    private HashMap<String, Double> serviceStartHistory = new HashMap<>();
    private HashMap<String, Double> serviceCompletionHistory = new HashMap<>();
    private HashMap<String, Double> traversalHistory = new HashMap<>();


    private Configuration config;

    // =========================
    // Generators (reusable instances)
    // =========================

    private DiscreteGenerator oversizedGen;   // Bernoulli for luggage type
    private ContinuousGenerator economyWeightGen;
    private ContinuousGenerator businessWeightGen;
    private ContinuousGenerator firstWeightGen;
    private ContinuousGenerator uniformGen;

    private SimulationEngine engine;

    private static int nextId = 1;
    private final int id;

    // Constructor that initializes everything probabilistically
    public Passenger(SimulationEngine engine) {
        this.engine = engine;
        this.config = engine.getConfiguration();
        this.id = nextId++;

        // Initialize continuous generators
        this.economyWeightGen = new Uniform(0, 8);
        this.businessWeightGen = new Normal(5, 1.5 * 1.5); // variance = sd^2
        this.firstWeightGen = new Normal(6, 1.0);

        this.uniformGen = new Uniform(0, 1);

        // Initialize all attributes
        initializeAttributes();
    }

    public int getId() { return id; }

    public void setDepartureTime(double time) { this.departureTime = time; }
    public double getDepartureTime() { return departureTime; }

    public double getTotalJourneyTime() { return departureTime - systemArrivalTime; }
    public double setTotalJourneyTime(double time) { double totalJourneyTime = getTotalJourneyTime(); return totalJourneyTime = time; }

    public void setQueueEntryTime(double time) { this.queueEntryTime = time; }
    public double getQueueEntryTime() { return queueEntryTime; }

    public void setServiceStartTime(double time) { this.serviceStartTime = time; }
    public double getServiceStartTime() { return serviceStartTime; }

    public void setServiceCompletionTime(double time) { this.serviceCompletionTime = time; }
    public double getServiceCompletionTime() { return serviceCompletionTime; }

    // =========================
    // Historical (HashMap) getters/setters
    // =========================
    public void recordQueueEntryTime(String servicePoint, double time) {
        queueEntryHistory.put(servicePoint, time);
    }
    public double getQueueEntryTimeFor(String servicePoint) {
        return queueEntryHistory.getOrDefault(servicePoint, -1.0);
    }

    public void recordServiceStartTime(String servicePoint, double time) {
        serviceStartHistory.put(servicePoint, time);
    }
    public double getServiceStartTimeFor(String servicePoint) {
        return serviceStartHistory.getOrDefault(servicePoint, -1.0);
    }

    public void recordServiceCompletionTime(String servicePoint, double time) {
        serviceCompletionHistory.put(servicePoint, time);
    }
    public double getServiceCompletionTimeFor(String servicePoint) {
        return serviceCompletionHistory.getOrDefault(servicePoint, -1.0);
    }

    public void recordTraversalTime(String fromServicePoint, double time) {
        traversalHistory.put(fromServicePoint, time);
    }
    public double getTraversalTimeFrom(String fromServicePoint) {
        return traversalHistory.getOrDefault(fromServicePoint, -1.0);
    }

    // Optionally: full maps for CSV export
    public HashMap<String, Double> getQueueEntryHistory() { return queueEntryHistory; }
    public HashMap<String, Double> getServiceStartHistory() { return serviceStartHistory; }
    public HashMap<String, Double> getServiceCompletionHistory() { return serviceCompletionHistory; }
    public HashMap<String, Double> getTraversalHistory() { return traversalHistory; }



    // =========================
    // Getters
    // =========================
    public FlightType getFlightType() { return flightType; }
    public TicketType getTicketType() { return ticketType; }
    public LuggageType getCheckinLuggageType() { return checkinLuggage; }
    public boolean isEligibleForSelfCheckin() { return eligibleForSelfCheckin; }
    public double getCarryOnWeight() { return carryOnWeight; }
    public double getSystemArrivalTime() { return systemArrivalTime; }

    // =========================
    // Attribute Initialization
    // =========================
    public void initializeAttributes() {

        // --- Flight Type ---
        this.flightType = uniformGen.sample() < config.getDomesticProbability() ? FlightType.DOMESTIC : FlightType.INTERNATIONAL;

        // --- Ticket Type ---
        double rand = uniformGen.sample();
        if (rand < config.getEconomyProbability()) this.ticketType = TicketType.ECONOMY;
        else if (rand < config.getEconomyProbability() + config.getBusinessProbability()) this.ticketType = TicketType.BUSINESS;
        else this.ticketType = TicketType.FIRST;

        // --- Oversized Luggage ---
        double oversizedProb = switch (ticketType) {
            case TicketType.ECONOMY -> config.getEconomyOversizedProb();
            case TicketType.BUSINESS -> config.getBusinessOversizedProb();
            default -> config.getFirstOversizedProb();
        };
        this.oversizedGen = new Bernoulli(oversizedProb);
        this.checkinLuggage = (oversizedGen.sample() == 1) ? LuggageType.OVERSIZED : LuggageType.STANDARD;

        // --- Self Check-in Eligibility ---
        if (LuggageType.STANDARD.equals(checkinLuggage)) {
            double selfCheckinProb = switch (ticketType) {
                case TicketType.ECONOMY -> config.getEconomySelfCheckinProb();
                case TicketType.BUSINESS -> config.getBusinessSelfCheckinProb();
                default -> config.getFirstSelfCheckinProb();
            };
            this.eligibleForSelfCheckin = uniformGen.sample() < selfCheckinProb;
        } else {
            this.eligibleForSelfCheckin = false; // Oversized cannot self-check-in
        }

        // --- Carry-on Weight ---
        this.carryOnWeight = generateCarryOnWeight();

        // --- Arrival Time ---
        this.systemArrivalTime = Clock.getInstance().getTime();
    }

    // =========================
    // Carry-on Weight Logic
    // =========================
    private double generateCarryOnWeight() {
        switch (ticketType) {

            case TicketType.ECONOMY:
                return economyWeightGen.sample(); // Uniform(0,8)

            case TicketType.BUSINESS:
                return sampleTruncatedNormal(businessWeightGen, 0, 8);

            case TicketType.FIRST:
                return sampleTruncatedNormal(firstWeightGen, 0, 8);

            default:
                throw new IllegalStateException("Unknown ticket type");
        }
    }

    private double sampleTruncatedNormal(ContinuousGenerator dist, double min, double max) {
        double value;
        do {
            value = dist.sample();
        } while (value < min || value > max);
        return value;
    }

    public double sampleTraversalTime(Transition transition) {

        double mean = transition.getMean(config);
        double sd = transition.getStdDev(config);


        double factor = switch (ticketType) {
            case TicketType.ECONOMY -> 1.0;
            case TicketType.BUSINESS -> 0.9;
            case TicketType.FIRST -> 0.8;
        };

        mean *= factor;

        // Truncated normal distribution: [0, 8] minutes max? (or whatever upper limit)
        Normal traversalDist = new Normal(mean, sd * sd);
        double value;
        do {
            value = traversalDist.sample();
        } while (value < 0 || value > config.getTraversalMaxTime());
        return value;
    }

    public boolean usesFastTrackSecurity() {
        return TicketType.BUSINESS.equals(ticketType) ||
                TicketType.FIRST.equals(ticketType);
    }

    public boolean isInternationalFlight() {
        return FlightType.INTERNATIONAL.equals(flightType);
    }


    /*

        // Constructor with predefined attributes (for testing or fixed passengers)
    public Passenger(String flightType, String ticketType, String checkinLuggage, double carryOnWeight) {
        this.flightType = flightType;
        this.ticketType = ticketType;
        this.checkinLuggage = checkinLuggage;
        this.carryOnWeight = carryOnWeight;
        this.systemArrivalTime = Clock.getInstance().getTime();
    }


     */

}



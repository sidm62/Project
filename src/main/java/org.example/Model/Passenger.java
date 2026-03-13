package org.example.Model;

import org.example.Model.distributions.Bernoulli;
import org.example.Model.distributions.ContinuousGenerator;
import org.example.Model.distributions.DiscreteGenerator;
import org.example.Model.distributions.Normal;
import org.example.Model.distributions.Uniform;

import java.util.HashMap;

/**
 * Represents a passenger in the airport simulation.
 *
 * A passenger has properties such as flight type, ticket type,
 * luggage type, carry-on weight, and timestamps related to the
 * passenger's journey through the system.
 *
 * The class also stores service history information for different
 * service points, such as queue entry time, service start time,
 * service completion time, and traversal time.
 */
public class Passenger {

    // =========================
    // Attributes
    // =========================
    /**
     * Type of flight for the passenger.
     */
    private FlightType flightType;       // Domestic / International
    /**
     * Type of ticket owned by the passenger.
     */
    private TicketType ticketType;       // Economy / Business / First
    /**
     * Type of check-in luggage carried by the passenger.
     */
    private LuggageType checkinLuggage;   // Standard / Oversized
    /**
     * Weight of the passenger's carry-on luggage.
     */
    private double carryOnWeight;    // 0–8 kg
    /**
     * Indicates whether the passenger can use self check-in.
     */
    private boolean eligibleForSelfCheckin;
    /**
     * Simulation time when the passenger entered the system.
     */
    private double systemArrivalTime; // Time tracking
    /**
     * Simulation time when the passenger left the system.
     */
    private double departureTime; // Time tracking

    /**
     * Time when the passenger entered the queue.
     */
    private double queueEntryTime;
    /**
     * Time when service started for the passenger.
     */
    private double serviceStartTime;
    /**
     * Time when service was completed for the passenger.
     */
    private double serviceCompletionTime;

    // Service timestamps per service point
    /**
     * History of queue entry times by service point name.
     */
    private HashMap<String, Double> queueEntryHistory = new HashMap<>();
    /**
     * History of service start times by service point name.
     */
    private HashMap<String, Double> serviceStartHistory = new HashMap<>();
    /**
     * History of service completion times by service point name.
     */
    private HashMap<String, Double> serviceCompletionHistory = new HashMap<>();
    /**
     * History of traversal times from service points.
     */
    private HashMap<String, Double> traversalHistory = new HashMap<>();


    /**
     * Configuration used by the passenger generation logic.
     */
    private Configuration config;

    // =========================
    // Generators (reusable instances)
    // =========================

    /**
     * Generator for oversized luggage probability.
     */
    private DiscreteGenerator oversizedGen;   // Bernoulli for luggage type
    /**
     * Generator for economy-class carry-on weight.
     */
    private ContinuousGenerator economyWeightGen;
    /**
     * Generator for business-class carry-on weight.
     */
    private ContinuousGenerator businessWeightGen;
    /**
     * Generator for first-class carry-on weight.
     */
    private ContinuousGenerator firstWeightGen;
    /**
     * Uniform random generator used in passenger attribute generation.
     */
    private ContinuousGenerator uniformGen;

    /**
     * Simulation engine that created this passenger.
     */
    private SimulationEngine engine;

    /**
     * Next available passenger id.
     */
    private static int nextId = 1;
    /**
     * Unique id of this passenger.
     */
    private final int id;

    /**
     * Creates a new passenger and initializes its attributes.
     *
     * The constructor stores the simulation engine reference,
     * loads the configuration, assigns a unique id, initializes
     * random generators, and generates the passenger attributes.
     *
     * @param engine simulation engine that creates the passenger
     */
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

    /**
     * Returns the unique id of the passenger.
     *
     * @return passenger id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the departure time of the passenger.
     *
     * @param time simulation time when the passenger leaves the system
     */
    public void setDepartureTime(double time) {
        this.departureTime = time;
    }

    /**
     * Returns the departure time of the passenger.
     *
     * @return departure time
     */
    public double getDepartureTime() {
        return departureTime;
    }

    /**
     * Returns the total journey time of the passenger.
     *
     * @return time spent in the system
     */
    public double getTotalJourneyTime() {
        return departureTime - systemArrivalTime;
    }

    /**
     * Returns the given total journey time value.
     *
     * This method does not update the actual stored journey time.
     *
     * @param time total journey time value
     * @return given time value
     */
    public double setTotalJourneyTime(double time) {
        double totalJourneyTime = getTotalJourneyTime();
        return totalJourneyTime = time;
    }

    /**
     * Sets the queue entry time.
     *
     * @param time queue entry time
     */
    public void setQueueEntryTime(double time) {
        this.queueEntryTime = time;
    }

    /**
     * Returns the queue entry time.
     *
     * @return queue entry time
     */
    public double getQueueEntryTime() {
        return queueEntryTime;
    }

    /**
     * Sets the service start time.
     *
     * @param time service start time
     */
    public void setServiceStartTime(double time) {
        this.serviceStartTime = time;
    }

    /**
     * Returns the service start time.
     *
     * @return service start time
     */
    public double getServiceStartTime() {
        return serviceStartTime;
    }

    /**
     * Sets the service completion time.
     *
     * @param time service completion time
     */
    public void setServiceCompletionTime(double time) {
        this.serviceCompletionTime = time;
    }

    /**
     * Returns the service completion time.
     *
     * @return service completion time
     */
    public double getServiceCompletionTime() {
        return serviceCompletionTime;
    }

    // =========================
    // Historical (HashMap) getters/setters
    // =========================
    /**
     * Records the queue entry time for a specific service point.
     *
     * @param servicePoint name of the service point
     * @param time queue entry time
     */
    public void recordQueueEntryTime(String servicePoint, double time) {
        queueEntryHistory.put(servicePoint, time);
    }

    /**
     * Returns the recorded queue entry time for a service point.
     *
     * @param servicePoint name of the service point
     * @return recorded queue entry time, or -1.0 if not found
     */
    public double getQueueEntryTimeFor(String servicePoint) {
        return queueEntryHistory.getOrDefault(servicePoint, -1.0);
    }

    /**
     * Records the service start time for a specific service point.
     *
     * @param servicePoint name of the service point
     * @param time service start time
     */
    public void recordServiceStartTime(String servicePoint, double time) {
        serviceStartHistory.put(servicePoint, time);
    }

    /**
     * Returns the recorded service start time for a service point.
     *
     * @param servicePoint name of the service point
     * @return recorded service start time, or -1.0 if not found
     */
    public double getServiceStartTimeFor(String servicePoint) {
        return serviceStartHistory.getOrDefault(servicePoint, -1.0);
    }

    /**
     * Records the service completion time for a specific service point.
     *
     * @param servicePoint name of the service point
     * @param time service completion time
     */
    public void recordServiceCompletionTime(String servicePoint, double time) {
        serviceCompletionHistory.put(servicePoint, time);
    }

    /**
     * Returns the recorded service completion time for a service point.
     *
     * @param servicePoint name of the service point
     * @return recorded service completion time, or -1.0 if not found
     */
    public double getServiceCompletionTimeFor(String servicePoint) {
        return serviceCompletionHistory.getOrDefault(servicePoint, -1.0);
    }

    /**
     * Records traversal time from a specific service point.
     *
     * @param fromServicePoint name of the previous service point
     * @param time traversal time
     */
    public void recordTraversalTime(String fromServicePoint, double time) {
        traversalHistory.put(fromServicePoint, time);
    }

    /**
     * Returns the recorded traversal time from a service point.
     *
     * @param fromServicePoint name of the previous service point
     * @return recorded traversal time, or -1.0 if not found
     */
    public double getTraversalTimeFrom(String fromServicePoint) {
        return traversalHistory.getOrDefault(fromServicePoint, -1.0);
    }

    // Optionally: full maps for CSV export
    /**
     * Returns the full queue entry history.
     *
     * @return queue entry history map
     */
    public HashMap<String, Double> getQueueEntryHistory() {
        return queueEntryHistory;
    }

    /**
     * Returns the full service start history.
     *
     * @return service start history map
     */
    public HashMap<String, Double> getServiceStartHistory() {
        return serviceStartHistory;
    }

    /**
     * Returns the full service completion history.
     *
     * @return service completion history map
     */
    public HashMap<String, Double> getServiceCompletionHistory() {
        return serviceCompletionHistory;
    }

    /**
     * Returns the full traversal history.
     *
     * @return traversal history map
     */
    public HashMap<String, Double> getTraversalHistory() {
        return traversalHistory;
    }


    // =========================
    // Getters
    // =========================
    /**
     * Returns the flight type of the passenger.
     *
     * @return flight type
     */
    public FlightType getFlightType() {
        return flightType;
    }

    /**
     * Returns the ticket type of the passenger.
     *
     * @return ticket type
     */
    public TicketType getTicketType() {
        return ticketType;
    }

    /**
     * Returns the check-in luggage type of the passenger.
     *
     * @return luggage type
     */
    public LuggageType getCheckinLuggageType() {
        return checkinLuggage;
    }

    /**
     * Returns whether the passenger is eligible for self check-in.
     *
     * @return true if eligible, otherwise false
     */
    public boolean isEligibleForSelfCheckin() {
        return eligibleForSelfCheckin;
    }

    /**
     * Returns the carry-on luggage weight.
     *
     * @return carry-on weight
     */
    public double getCarryOnWeight() {
        return carryOnWeight;
    }

    /**
     * Returns the system arrival time of the passenger.
     *
     * @return arrival time
     */
    public double getSystemArrivalTime() {
        return systemArrivalTime;
    }

    // =========================
    // Attribute Initialization
    // =========================
    /**
     * Initializes the passenger attributes using random generators
     * and configuration probabilities.
     *
     * The method generates flight type, ticket type, luggage type,
     * self check-in eligibility, carry-on weight, and arrival time.
     */
    public void initializeAttributes() {

        // --- Flight Type ---
        this.flightType = uniformGen.sample() < config.getDomesticProbability() ? FlightType.DOMESTIC : FlightType.INTERNATIONAL;

        // --- Ticket Type ---
        double rand = uniformGen.sample();
        if (rand < config.getEconomyProbability()) this.ticketType = TicketType.ECONOMY;
        else if (rand < config.getEconomyProbability() + config.getBusinessProbability())
            this.ticketType = TicketType.BUSINESS;
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
    /**
     * Generates carry-on luggage weight based on the passenger's ticket type.
     *
     * @return generated carry-on weight
     */
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

    /**
     * Samples a value from a normal distribution until it falls
     * within the given minimum and maximum range.
     *
     * @param dist distribution used for sampling
     * @param min minimum allowed value
     * @param max maximum allowed value
     * @return sampled value within the allowed range
     */
    private double sampleTruncatedNormal(ContinuousGenerator dist, double min, double max) {
        double value;
        do {
            value = dist.sample();
        } while (value < min || value > max);
        return value;
    }

    /**
     * Samples traversal time for a given transition between service points.
     *
     * The traversal time is based on transition parameters from the
     * configuration and adjusted according to the passenger's ticket type.
     *
     * @param transition transition to sample traversal time for
     * @return sampled traversal time
     */
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

    /**
     * Returns whether the passenger uses fast track security.
     *
     * @return true if the passenger has business or first class ticket,
     * otherwise false
     */
    public boolean usesFastTrackSecurity() {
        return TicketType.BUSINESS.equals(ticketType) ||
                TicketType.FIRST.equals(ticketType);
    }

    /**
     * Returns whether the passenger is travelling on an international flight.
     *
     * @return true if the flight is international, otherwise false
     */
    public boolean isInternationalFlight() {
        return FlightType.INTERNATIONAL.equals(flightType);
    }
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


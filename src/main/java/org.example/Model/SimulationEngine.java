package org.example.Model;

import javafx.application.Platform;
import javafx.scene.shape.Circle;
import org.example.Model.distributions.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.example.View.AirportView;


/**
 * Controls the execution of the airport queue simulation.
 *
 * This class manages the event list, simulation clock progression,
 * passenger arrivals, service points, system-level statistics,
 * and CSV export of the simulation results.
 *
 * The simulation is processed event by event in chronological order
 * until there are no more events to handle or the simulation ends.
 */
public class SimulationEngine {

    /**
     * List of all scheduled events in the simulation.
     */
    private EventList eventList;
    /**
     * Indicates whether the simulation is currently running.
     */
    private boolean running;
    private boolean drainingNotified = false;
    /**
     * Time at which the simulation should stop generating arrivals.
     */
    private double simulationEndTime;
    /**
     * Service point for normal check-in.
     */
    public NormalCheckin normalCheckin;
    /**
     * Service point for self check-in.
     */
    public SelfCheckin selfCheckin;
    /**
     * Service point for regular security screening.
     */
    public RegularSecurity regularSecurity;
    /**
     * Service point for fast track security screening.
     */
    public FastTrackSecurity fastTrackSecurity;
    /**
     * Service point for customs processing.
     */
    public Customs customs;
    /**
     * Service point for boarding.
     */
    public Boarding boarding;
    /**
     * Base random seed used to initialize the simulation generators.
     */
    private long baseSeed;
    /**
     * Indicates whether step-by-step execution is enabled.
     */
    private boolean stepMode = false;
    /**
     * Indicates that the next simulation step has been requested.
     *
     * This flag is typically set by the user interface when the user
     * triggers the next step during step-by-step execution.
     */
    private boolean nextStepRequested = false;

    /**
     * Total number of passengers who completed the system.
     */
    int totalPassengersCompleted;
    /**
     * Sum of total journey times of all completed passengers.
     */
    private double cumulativeSystemTime;


    /**
     * Configuration object containing simulation parameters.
     */
    private Configuration config;

    /**
     * Generator used for system arrival times.
     */
    private ContinuousGenerator arrivalGenerator;

    /**
     * Collection of all service points included in the simulation.
     */
    private ArrayList<ServicePoint> grouping;

    /**
     * Collection of all passengers created during the simulation.
     */
    public ArrayList<Passenger> allPassengers;



    /**
     * Indicates whether debug messages should be printed during simulation.
     */
    private boolean debugMode;
    /**
     * Time scaling factor used when converting simulation time to real-time delays.
     * This value is typically used when the simulation uses Thread.sleep to slow
     * down execution for visualization purposes.
     */
    private double timeScale; // used for time scaling for sleep
    /**
     * Multiplier that controls the overall simulation speed.
     * Higher values make the simulation run faster, while lower values slow it down.
     */
    private double speedMultiplier;
    /**
     * Maximum allowed sleep time (in milliseconds) when synchronizing
     * simulation time with real time.
     */
    private long maxSleep = 10000;
    /**
     * Real-world timestamp when the simulation started.
     * Used to align simulation time with wall-clock time.
     */
    private long realStartTime;
    /**
     * Simulation time when the simulation started.
     * Used together with realStartTime to calculate delays.
     */
    private double simStartTime;

    /**
     * Base arrival rate (lambda) used by the arrival generator.
     */
    private double baseArrivalLambda;
    /**
     * Current arrival rate (lambda) used during the simulation.
     * This value may change dynamically depending on system load.
     */
    private double currentArrivalLambda;
    /**
     * Scaling factor applied to passenger arrival speed.
     * Declared volatile because it may be updated by different threads.
     */
    private volatile double arrivalSpeedFactor = 1.0;
    /**
     * Scaling factor applied to service speed across service points.
     * Declared volatile because it may be updated dynamically.
     */
    private volatile double serviceSpeedFactor = 1.0;
    /**
     * Scaling factor applied to passenger traversal speed between service points.
     */
    private volatile double traversalSpeedFactor = 1.0;
    /**
     * Minimum allowed scaling factor for adaptive speed adjustments.
     */
    private final double MIN_FACTOR = 0.5;
    /**
     * Maximum allowed scaling factor for adaptive speed adjustments.
     */
    private final double MAX_FACTOR = 2.0;
    /**
     * Maximum allowed system load ratio before adjustments are applied.
     */
    private final double MAX_LOAD_RATIO = 1.29;
    /**
     * Clamps a value to remain within the allowed scaling range.
     *
     * @param value value to clamp
     * @return value limited between MIN_FACTOR and MAX_FACTOR
     */
    private double clamp(double value)
    {
        return Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, value));
    }

    /**
     * Creates a new simulation engine with the given end time and configuration.
     *
     * The constructor initializes the event list, service points,
     * random seed, statistics, passenger storage, and arrival generator.
     *
     * @param simulationEndTime time limit for generating new arrivals
     * @param config configuration used by the simulation
     */
    public SimulationEngine(double simulationEndTime, Configuration config) {
        this.eventList = new EventList();
        this.simulationEndTime = simulationEndTime;
        this.running = false;
        this.config = config;
        this.baseSeed = config.getRandomSeed();
        SeedGenerator.getDefaultSeedGenerator().setSeed(baseSeed); // made getDefaultSeedGenerator() public

        this.grouping = new ArrayList<>();

        this.normalCheckin = new NormalCheckin(this, "Normal Check-in");
        this.selfCheckin = new SelfCheckin(this, "Self Check-in");
        this.regularSecurity = new RegularSecurity(this, "Regular Security");
        this.fastTrackSecurity = new FastTrackSecurity(this, "Fast Track Security");
        this.customs = new Customs(this, "Customs");
        this.boarding = new Boarding(this, "Boarding");

        this.totalPassengersCompleted = 0;
        this.cumulativeSystemTime = 0;

        this.allPassengers = new ArrayList<>();

        this.debugMode = true;


        initArrivalGenerator();



    }
    /**
     * Returns a copy of the list containing all passengers in the simulation.
     *
     * A new ArrayList is created to prevent external code from modifying
     * the internal passenger list stored by the simulation engine.
     *
     * @return list of all passengers
     */
    public List<Passenger> getAllPassengers() {
        return new ArrayList<>(this.allPassengers);}

    /**
     * Returns whether debug mode is enabled.
     *
     * @return true if debug mode is enabled, otherwise false
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isRunning() {return running;}

    /**
     * Enables or disables debug mode.
     *
     * @param debugMode_answer new debug mode value
     */
    public void setDebugMode(boolean debugMode_answer) {
        this.debugMode = debugMode_answer;
    }

    /**
     * Returns the current time scaling factor.
     *
     * This value is used to convert simulation time into real-time delays
     * when slowing down the simulation using thread sleep.
     *
     * @return time scaling factor
     */
    public double getTimeScale() {
        return timeScale;
    }

    public double getServiceSpeedFactor() {
        return serviceSpeedFactor;
    }

    /**
     * Sets the time scaling factor used for real-time synchronization.
     *
     * @param timeScale new time scaling factor
     */
    public void setTimeScale(double timeScale) {
        this.timeScale = timeScale;
    }

    /**
     * Sets the simulation end time.
     *
     * @param simulationEndTime the desired simulation end time
     *
     * If debugMode is enabled, prints the new simulation duration to the console.
     */
    public void setSimulationEndTime(double simulationEndTime) {
        this.simulationEndTime = simulationEndTime;
        if (debugMode) System.out.println("Uusi simulaation kesto on asetettu: " + simulationEndTime);
    }


    // ---------- Formatting Helpers ----------


    /**
     * Formats a decimal value using two decimal places.
     *
     * @param v value to format
     * @return formatted string with two decimals
     */
    private String f2(double v) {                    // Rules we’ll follow: Times → 2 decimalsRates → 3 decimalsUtilization → percentErrors → percentCSV → numeric only (no % symbols)
        return String.format("%.2f", v);
    }



    /**
     * Returns the configuration used by the simulation.
     *
     * @return simulation configuration
     */
    public Configuration getConfiguration() {
        return this.config;
    }


    /**
     * Calculates the average Little's Law queue error across all service points.
     *
     * The method iterates over all service points in the grouping list and includes only
     * those that have completed at least one service. It then computes the average of
     * their Little's Law error percentages.
     *
     * @return the average Little's Law queue error in percent for all service points;
     *         returns 0 if no service points have completed any services
     */
    public double getLittleLawQueueError() {
        double totalErrorPercent = 0;
        int count = 0;

        for (ServicePoint sp : grouping) {
            if (sp.getCompletionCount() > 0) {
                totalErrorPercent += sp.getLittleLawQueueErrorPercent();
                count++;
            }
        }

        return count > 0 ? totalErrorPercent / count : 0;
    }


    /**
     * Initializes the arrival generator based on the configured arrival distribution.
     *
     * Currently, the simulation supports the NEGEXP arrival distribution.
     *
     * @throws IllegalArgumentException if the configured distribution type is not supported
     */
    private void initArrivalGenerator() {

        String type = config.getArrivalDistributionType();

        if (type.equalsIgnoreCase("NEGEXP")) {
            this.baseArrivalLambda = config.getArrivalLambda();
            this.currentArrivalLambda = baseArrivalLambda;

            arrivalGenerator =
                    new Negexp(config.getArrivalLambda());

        } else {
            throw new IllegalArgumentException(
                    "Unsupported arrival distribution: " + type
            );
        }
    }

    /**
     * Updates the arrival rate (lambda) used by the passenger arrival generator.
     *
     * The new lambda value must be positive. If the arrival generator is an
     * exponential distribution (Negexp), its mean is updated accordingly.
     *
     * @param newLambda new arrival rate parameter
     * @throws IllegalArgumentException if the lambda value is not positive
     */
    public void updateArrivalLambda(double newLambda) {
        if (newLambda <= 0) throw new IllegalArgumentException("Lambda must be positive.");

        this.currentArrivalLambda = newLambda;

        if (arrivalGenerator instanceof Negexp negexp) {
            negexp.setMean(newLambda);
        }
    }


    // Schedule a new event in the future
    /**
     * Schedules a new event into the event list.
     *
     * @param event event to be scheduled
     */
    public void scheduleEvent(Event event) {
        eventList.schedule(event); // Use EventList method, not getEvents()
    }



    /**
     * Returns the simulation end time.
     *
     * @return simulation end time
     */
    public double getSimulationEndTime() {
        return simulationEndTime;
    }

    /**
     * Calculates the system throughput.
     *
     * @return completed passengers divided by simulation end time
     */
    public double getSystemThroughput() {
        return simulationEndTime == 0 ? 0 : totalPassengersCompleted / simulationEndTime;
    }

    /**
     * Calculates the average total time spent in the system by completed passengers.
     *
     * @return average system time
     */
    public double getAverageSystemTime() {
        return totalPassengersCompleted == 0 ? 0 : cumulativeSystemTime / totalPassengersCompleted;
    }

    /**
     * Calculates the average number of passengers in the system using Little's Law.
     *
     * @return average number of passengers in the system
     */
    public double getAverageNumberInSystem() {

        double X = getSystemThroughput();
        double R = getAverageSystemTime();

        return X * R;
    }




    /**
     * Finalizes statistics for all service points after the simulation ends.
     */
    private void finalizeServicePointStatistics() {
        for (ServicePoint sp : grouping) {
            sp.finalizeStatistics();
        }
    }
    /**
     * Returns a copy of the list containing all service points in the simulation.
     *
     * A new list is returned to prevent external code from modifying the
     * internal collection of service points.
     *
     * @return list of all service points
     */
    public List<ServicePoint> getAllServicePoints() {
        return new ArrayList<>(this.grouping);
    }

    /**
     * Validates arrival, service, and traversal speed factors.
     *
     * The method checks that the factors stay within the allowed minimum
     * and maximum bounds and that the arrival-to-service load ratio does
     * not exceed the maximum allowed system load.
     *
     * @param arrival arrival speed factor
     * @param service service speed factor
     * @param traversal traversal speed factor
     * @return true if the factor combination is valid, otherwise false
     */
    private boolean validateFactors(double arrival, double service, double traversal) {
        // 1. Absoluuttiset rajat
        if (arrival < MIN_FACTOR || arrival > MAX_FACTOR) return false;
        if (service < MIN_FACTOR || service > MAX_FACTOR) return false;

        // 2. Vakaustarkistus (Pitäisi olla näin):
        double loadRatio = arrival / service;
        if (loadRatio > MAX_LOAD_RATIO) {
            return false; // Liian suuri kuorma -> hylätään muutos
        }

        return true;
    }
    /**
     * Updates the service speed factor used by all service points.
     *
     * The given factor is clamped to the allowed range and validated against
     * the current arrival and traversal factors. If valid, the new factor is
     * applied to every service point in the simulation.
     *
     * @param factor requested service speed factor
     * @throws Exception if the factor combination is not valid
     */
    public synchronized void setServiceSpeedFactor(double factor) throws Exception {
        double clampedFactor = clamp(factor);
        if (!validateFactors(arrivalSpeedFactor, clampedFactor, traversalSpeedFactor)) {
            throw new Exception("Invalid service speed factor");
        }
        this.serviceSpeedFactor = clampedFactor;
        for (ServicePoint sp : grouping) {
            sp.adjustServiceTime(this.serviceSpeedFactor);
        }
        System.out.println("ServiceSpeedFactor on päivitetty:" + clampedFactor);

    }
    /**
     * Returns the current arrival speed factor.
     *
     * @return arrival speed factor
     */
    public double getArrivalSpeedFactor() {
        return arrivalSpeedFactor;
    }

    /**
     * Returns the current traversal speed factor.
     *
     * @return traversal speed factor
     */
    public double getTraversalSpeedFactor() {
        return traversalSpeedFactor;
    }






    /**
     * Records that a passenger has completed the simulation.
     *
     * The method updates the total number of completed passengers
     * and adds the passenger's total journey time to cumulative statistics.
     *
     * @param passenger passenger that completed the system
     */
    public void recordPassengerCompletion(Passenger passenger) {
        totalPassengersCompleted++;
        double journeyTime = passenger.getTotalJourneyTime();
        cumulativeSystemTime += journeyTime;
    }
    /**
     * Returns the total number of passengers who completed the system.
     *
     * @return total completed passengers
     */
    public int getTotalPassengersCompleted() {
        return this.totalPassengersCompleted;
    }


    /**
     * Checks whether a service point should be dynamically sped up or slowed down.
     *
     * The decision is based on utilization, queue length, and an adjustment cooldown.
     * If the service point is overloaded, the service factor is reduced to speed it up.
     * If it is underloaded, the factor is increased toward the normal level.
     *
     * @param sp service point to evaluate
     * @param simulationTime current simulation time
     */
    private void checkAndAdjustServicePoint(ServicePoint sp, double simulationTime) {
        double utilization = sp.getUtilization(simulationTime);
        double avgQueue = sp.getLiveAverageQueueLength();

        final double HIGH_UTIL = 0.95;
        final double LOW_UTIL = 0.85;   // hysteresis lower bound

        final double HIGH_QUEUE = 50;
        final double LOW_QUEUE = 20;

        final double STEP = 0.05;        // 5% step change
        final double ADJUST_INTERVAL = 50.0; // only adjust every 50 time units

        // --- cooldown protection ---
        if (simulationTime - sp.getLastAdjustmentTime() < ADJUST_INTERVAL) {
            return;
        }

        double factor = sp.getCurrentFactor();

        // --- bottleneck detected → speed up ---
        if (utilization > HIGH_UTIL || avgQueue > HIGH_QUEUE) {

            factor -= STEP;

            if (debugMode) {
                System.out.printf("[ADJUST-UP] %s util=%.3f queue=%.1f → factor=%.2f%n",
                        sp.getServicePointName(), utilization, avgQueue, factor);
            }

        }
        // --- underloaded → slow back toward normal ---
        else if (utilization < LOW_UTIL && avgQueue < LOW_QUEUE) {

            factor += STEP;

            if (debugMode) {
                System.out.printf("[ADJUST-DOWN] %s util=%.3f queue=%.1f → factor=%.2f%n",
                        sp.getServicePointName(), utilization, avgQueue, factor);
            }
        } else {
            return; // inside stability band → do nothing
        }

        sp.adjustServiceTime(factor);
        sp.setLastAdjustmentTime(simulationTime);
    }
    /**
     * Changes the simulation time scale used for real-time synchronization.
     *
     * The method re-anchors the current real time and simulation time so that
     * future delays are calculated relative to the new speed.
     *
     * @param newTimeScale new time scale value
     * @throws IllegalArgumentException if the time scale is not positive
     */
    public synchronized void changeSpeed(double newTimeScale) {

        if (newTimeScale <= 0) {
            throw new IllegalArgumentException("Time scale must be positive.");
        }

        long nowReal = System.currentTimeMillis();
        double nowSim = Clock.getInstance().getTime();

        // Re-anchor using existing variables
        realStartTime = nowReal;
        simStartTime = nowSim;

        timeScale = newTimeScale;
    }


    /**
     * Sets the overall simulation speed multiplier.
     *
     * The multiplier is converted into a time scale value that is used to
     * synchronize simulation time with wall-clock time.
     *
     * @param multiplier new simulation speed multiplier
     * @throws IllegalArgumentException if the multiplier is not positive
     */
    public void setSpeedMultiplier(double multiplier) {

        if (multiplier <= 0) {
            throw new IllegalArgumentException("Multiplier must be positive.");
        }

        this.speedMultiplier = multiplier;
        double newTimeScale = 1000.0 / multiplier;
        this.timeScale = newTimeScale;
        changeSpeed(this.timeScale);
    }
    /**
     * Updates the arrival speed factor of the simulation.
     *
     * The factor is clamped to the allowed range and validated against the
     * current service and traversal factors. If valid, the arrival rate
     * generator is updated accordingly.
     *
     * @param factor requested arrival speed factor
     * @throws Exception if the factor combination is not valid
     */
    public synchronized void setArrivalSpeedFactor (double factor) throws Exception {
        double clampedFactor = clamp(factor);
        if (!validateFactors(clampedFactor, serviceSpeedFactor, traversalSpeedFactor)) {
            throw new Exception("Invalid arrival speed factor");
        }
        this.arrivalSpeedFactor = clampedFactor;
        double newLambda = baseArrivalLambda / arrivalSpeedFactor;
        updateArrivalLambda(newLambda);
        System.out.println("ArrivalSpeedFactor on päivitetty:" + clampedFactor);
    }
    /**
     * Updates the traversal speed factor of the simulation.
     *
     * The factor is clamped to the allowed range and validated against the
     * current arrival and service factors.
     *
     * @param factor requested traversal speed factor
     * @throws Exception if the factor combination is not valid
     */
    public synchronized void setTraversalSpeedFactor (double factor) throws Exception {
        double clampedFactor = clamp(factor);
        if (!validateFactors(arrivalSpeedFactor, serviceSpeedFactor, clampedFactor)) {
            throw new Exception("Invalid traversal speed factor");
        }
        this.traversalSpeedFactor = clampedFactor;
        System.out.println("TraversalSpeedFactor on päivitetty:" + clampedFactor);
    }






    /**
     * Applies a predefined simulation scenario.
     *
     * The scenario updates the passenger arrival rate and may also update
     * service-time means for the service points based on configuration data.
     *
     * @param scenario scenario to apply
     */
    public synchronized void applyScenario(Scenario scenario) {
        System.out.println(">>> Applying scenario: " + scenario);

        double newLambda;

        // Update arrival rate
        switch (scenario) {
            case NORMAL:
                newLambda = 3.333;
                config.setArrivalLambda(newLambda);
                updateArrivalLambda(newLambda);
                break;

            case PEAK_TIME:
                newLambda = 2.0;
                config.setArrivalLambda(newLambda);
                updateArrivalLambda(newLambda);
                break;

            case LOW_TRAFFIC:
                newLambda = 6.0;
                config.setArrivalLambda(newLambda);
                updateArrivalLambda(newLambda);
                break;

            case SYSTEM_STRESS:
                newLambda = 1.5;
                config.setArrivalLambda(newLambda);
                updateArrivalLambda(newLambda);
                break;

            case RECOVERY_MODE:
                newLambda = 5.0;
                config.setArrivalLambda(newLambda);
                updateArrivalLambda(newLambda);
                break;
        }


        // Update service means automatically
        Map<String, Double> means = config.getScenarioServiceMeans().get(scenario);
        if (means != null) {
            for (Map.Entry<String, Double> entry : means.entrySet()) {
                config.setServiceMeanFor(entry.getKey(), entry.getValue());
            }
        }

        for (ServicePoint sp : grouping) {
            double newBase = config.getServiceMeanFor(sp.getServicePointName());
            sp.updateBaseServiceMean(newBase);
        }
    }


    // Main simulation loop
    /**
     * Runs the main simulation loop.
     *
     * Events are processed in chronological order until the simulation stops
     * or the event list becomes empty. The method supports step mode, optional
     * real-time slowing using thread sleep, dynamic service-point adjustment,
     * and automatic export of results when the simulation ends.
     */
    public void run() {
        realStartTime = System.currentTimeMillis();
        simStartTime = Clock.getInstance().getTime();
        running = true;

        while (running && !eventList.isEmpty()) {

            // --- STEP-LOGIIKKA ---
            if (stepMode) {
                while (!nextStepRequested && stepMode && running) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                nextStepRequested = false;

                // TÄRKEÄ KORJAUS: Kun askel otetaan, synkronoidaan reaalimaailman kello
                // uudelleen simulaatioaikaan, jotta "sleep" ei hyppää askeleen yli.
                realStartTime = System.currentTimeMillis();
                simStartTime = Clock.getInstance().getTime();
            }

            Event event = eventList.getNextEvent();
            double simTime = event.getEventTime();
            Clock.getInstance().setTime(simTime);

            // Reaaliaikainen hidastus
            long desiredWallTime = (realStartTime + (long)((simTime - simStartTime) * getTimeScale()));
            long now = System.currentTimeMillis();
            long sleepTime = desiredWallTime - now;

            if (sleepTime > 0 && !stepMode) { // Ei nukkumista step-modessa, koska nappi hoiti odotuksen
                sleepTime = Math.min(sleepTime, maxSleep);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            processEvent(event);

            for (ServicePoint sp : grouping) {
                checkAndAdjustServicePoint(sp, Clock.getInstance().getTime());
            }
        }

        running = false;

        // --- SIMULAATIO VALMIS ---
        double simulationTime = Clock.getInstance().getTime();
        System.out.println("Simulation ended at time: " + f2(simulationTime));
        System.out.println("Total passengers processed: " + allPassengers.size());

        finalizeServicePointStatistics();

        // Tallennetaan tulokset
        exportServicePointCSV("ServicePoints.csv");
        exportSystemCSV("System.csv");
        exportPassengerCSV("Passengers.csv");

        // KÄYNNISTETÄÄN ANIMAATIO AUTOMAATTISESTI
        // Tämä siirtää ohjauksen SimulationEngineltä AirportView.java'lle
        Platform.runLater(() -> {
            AirportView view = AirportView.getInstance();
            if (view != null) {
                // Päivitetään matkustajataulukko
                view.updateTableFromCSV("Passengers.csv", view.getPassengerTableView());

                // Päivitetään palvelupistetaulukko
                view.updateTableFromCSV("ServicePoints.csv", view.getServicePointTableView());

                // Päivitetään järjestelmätilastot
                view.updateTableFromCSV("System.csv", view.getSystemTableView());
            }
        });
    }



    // Process individual events based on type
    /**
     * Processes a single event according to its event type.
     *
     * Depending on the event type, the passenger is routed to the correct
     * service point or handled as a new system arrival.
     *
     * @param event event to process
     * @throws IllegalArgumentException if the event type is not handled
     */
    private void processEvent(Event event) {

        Passenger passenger = event.getPassenger();

        switch (event.getType()) {

        /* ==============================
           SYSTEM ARRIVAL
        ============================== */

            case ARRIVAL_SYSTEM:
                handleArrival(passenger);
                break;

        /* ==============================
           NORMAL CHECK-IN
        ============================== */

            case ARRIVAL_NORMAL_CHECKIN:
                Platform.runLater(() ->
                        AirportView.getInstance().animatePassengerToNextService(
                                passenger,
                                EventType.ARRIVAL_NORMAL_CHECKIN
                        )
                );
                normalCheckin.handleArrival(passenger);
                break;

            case NORMAL_CHECKIN_COMPLETE:
                normalCheckin.handleCompletion(passenger);
                break;


        /* ==============================
           SELF CHECK-IN
        ============================== */

            case ARRIVAL_SELF_CHECKIN:
                Platform.runLater(() ->
                        AirportView.getInstance().animatePassengerToNextService(
                                passenger,
                                EventType.ARRIVAL_SELF_CHECKIN
                        )
                );
                selfCheckin.handleArrival(passenger);
                break;

            case SELF_CHECKIN_COMPLETE:
                selfCheckin.handleCompletion(passenger);
                break;


        /* ==============================
           REGULAR SECURITY
        ============================== */

            case ARRIVAL_REGULAR_SECURITY:
                Platform.runLater(() ->
                        AirportView.getInstance().animatePassengerToNextService(
                                passenger,
                                EventType.ARRIVAL_REGULAR_SECURITY
                        )
                );
                regularSecurity.handleArrival(passenger);
                break;

            case REGULAR_SECURITY_COMPLETE:
                regularSecurity.handleCompletion(passenger);
                break;


        /* ==============================
           FAST TRACK SECURITY
        ============================== */

            case ARRIVAL_FASTTRACK_SECURITY:
                Platform.runLater(() ->
                        AirportView.getInstance().animatePassengerToNextService(
                                passenger,
                                EventType.ARRIVAL_FASTTRACK_SECURITY
                        )
                );
                fastTrackSecurity.handleArrival(passenger);
                break;

            case FASTTRACK_SECURITY_COMPLETE:
                fastTrackSecurity.handleCompletion(passenger);
                break;


        /* ==============================
           CUSTOMS
        ============================== */

            case ARRIVAL_CUSTOMS:
                Platform.runLater(() ->
                        AirportView.getInstance().animatePassengerToNextService(
                                passenger,
                                EventType.ARRIVAL_CUSTOMS
                        )
                );
                customs.handleArrival(passenger);
                break;

            case CUSTOMS_COMPLETE:
                customs.handleCompletion(passenger);
                break;


        /* ==============================
           BOARDING
        ============================== */

            case ARRIVAL_BOARDING:
                Platform.runLater(() ->
                        AirportView.getInstance().animatePassengerToNextService(
                                passenger,
                                EventType.ARRIVAL_BOARDING
                        )
                );
                boarding.handleArrival(passenger);
                break;

            //case BOARDING_COMPLETE:
            //    boarding.handleCompletion(passenger);
            //    break;

            case BOARDING_COMPLETE:
                boarding.handleCompletion(passenger);
                Platform.runLater(() -> {
                    Circle node = AirportView.getInstance().passengerNodes.remove(passenger.getId());
                    if (node != null) {
                        AirportView.getInstance().animationPane.getChildren().remove(node);
                    }
                });
                break;


            default:
                throw new IllegalArgumentException(
                        "Unhandled event type: " + event.getType()
                );
        }
    }

    public void stopSimulation() {
        // Stop the main simulation loop
        running = false;

        // If step mode is active, release any passengers waiting for the "Next" button
        nextStepRequested = true;

        // Interrupt the thread if it is sleeping
        Thread.currentThread().interrupt();

        // Clear remaining events
        eventList.clear();

        // Optional: wake up the thread if it's sleeping in step mode
        synchronized (this) {
            notifyAll();
        }

        System.out.println("SimulationEngine: Simulation stopped.");
    }


    /**
     * Handles arrival of a passenger into the simulation system.
     *
     * The passenger is stored, animated in the user interface, routed to the
     * first appropriate service point, and the next system arrival is scheduled
     * if the simulation end time has not yet been reached.
     *
     * @param passenger arriving passenger
     */


    private void handleArrival(Passenger passenger) {

        allPassengers.add(passenger);

        Platform.runLater(() -> {
            AirportView.animateSinglePassenger(passenger);
        });

        EventType firstServiceArrival;

        if (passenger.getCheckinLuggageType().equals(LuggageType.OVERSIZED)) {
            firstServiceArrival = EventType.ARRIVAL_NORMAL_CHECKIN;

        } else if (passenger.isEligibleForSelfCheckin()) {
            firstServiceArrival = EventType.ARRIVAL_SELF_CHECKIN;

        } else {
            firstServiceArrival = EventType.ARRIVAL_NORMAL_CHECKIN;
        }

        scheduleEvent(new Event(
                Clock.getInstance().getTime(),
                firstServiceArrival,
                passenger
        ));

        // Schedule next system arrival
        double nextArrivalTime = Clock.getInstance().getTime() + arrivalGenerator.sample();

        if (nextArrivalTime <= simulationEndTime) {
            scheduleEvent(new Event(
                    nextArrivalTime,
                    EventType.ARRIVAL_SYSTEM,
                    new Passenger(this)
            ));
        } else if (!drainingNotified) {
            drainingNotified = true;

            if (debugMode) {
                System.out.println("=== ARRIVALS STOPPED (Soft Close Activated) ===");
            }

            // GUI notification
            Platform.runLater(() -> {
                AirportView view = AirportView.getInstance();
                if (view != null) {
                    view.getInfoArea().appendText(">>> Draining remaining passengers...\n");
                }
            });
        }

    }

    /**
     * Represents the normal check-in service point in the simulation.
     *
     * This service point handles passengers who use the regular check-in process.
     * Service times are generated from a normal distribution and scaled according
     * to the passenger's ticket type.
     */
    public class NormalCheckin extends ServicePoint {

        /**
         * Generator used to produce service times for this service point.
         */
        private ContinuousGenerator serviceGenerator;
        /**
         * Configuration object containing simulation parameters.
         */
        private Configuration config;
        /**
         * Base mean service time.
         */
        private double mean; // baseMean
        /**
         * Base standard deviation of the service time.
         */
        private double sd; // baseStd
        /**
         * Variance used by the normal distribution.
         */
        private double variance;

        /**
         * Temporary mean value used for service-time adjustments.
         */
        private double temp_mean; // NEW
        /**
         * Temporary standard deviation value used for service-time adjustments.
         */
        private double temp_sd; // NEW (just trying sum out 3.3)

        /**
         * Creates a normal check-in service point.
         *
         * @param engine simulation engine that owns this service point
         * @param servicePointName name of the service point
         */
        public NormalCheckin(SimulationEngine engine, String servicePointName) {
            super(engine, servicePointName);
            this.config = engine.getConfiguration();
            mean = config.getNormalCheckinMean(); // normal mean (baseMean)
            sd = config.getNormalCheckinStdDev(); // normal sd (baseStdDev)

            temp_mean = mean; // NEW
            temp_sd = sd; // NEW

            variance = sd * sd;
            grouping.add(this);

            this.serviceGenerator = new Normal(mean, variance);
        }

        /**
         * Generates a service time for a passenger at normal check-in.
         *
         * A positive base service time is sampled from a normal distribution
         * and scaled based on the passenger's ticket type.
         *
         * @param passenger passenger being served
         * @return generated service time
         */
        @Override
        protected double sampleServiceTime(Passenger passenger) {
            double baseTime;

            do {
                baseTime = serviceGenerator.sample();
            } while (baseTime <= 0);

            double scale;

            switch (passenger.getTicketType()) {
                case TicketType.ECONOMY:
                    scale = config.getEconomyCheckinScale();
                    break;
                case TicketType.BUSINESS:
                    scale = config.getBusinessCheckinScale();
                    break;
                default:
                    scale = config.getFirstCheckinScale();
            }

            return baseTime * scale;
        }

        /**
         * Returns the event type that marks completion of this service point.
         *
         * @return normal check-in completion event type
         */
        @Override
        protected EventType getCompletionEventType() {
            return EventType.NORMAL_CHECKIN_COMPLETE;
        }

        /**
         * Routes the passenger to the next service point after normal check-in.
         *
         * After check-in, the passenger is sent to either fast track security
         * or regular security depending on passenger properties.
         *
         * @param passenger passenger that completed service
         */
        @Override
        protected void routeAfterCompletion(Passenger passenger) {

            double traversal = passenger.sampleTraversalTime(Transition.CHECKIN_TO_SECURITY);
            // Record traversal history
            passenger.recordTraversalTime(servicePointName, traversal);

            EventType next =
                    passenger.usesFastTrackSecurity()
                            ? EventType.ARRIVAL_FASTTRACK_SECURITY
                            : EventType.ARRIVAL_REGULAR_SECURITY;

            engine.scheduleEvent(new Event(
                    Clock.getInstance().getTime() + traversal,
                    next,
                    passenger
            ));

            Platform.runLater(() ->
                    AirportView.getInstance().animatePassengerToNextService(passenger, next)
            );
        }
    }

    /**
     * Represents the self check-in service point in the simulation.
     *
     * This service point handles passengers who are eligible to use
     * self check-in. Service times are sampled from a normal distribution.
     */
    public class SelfCheckin extends ServicePoint {

        /**
         * Generator used to produce service times for this service point.
         */
        private ContinuousGenerator serviceGenerator;
        /**
         * Configuration object containing simulation parameters.
         */
        private Configuration config;
        /**
         * Base mean service time.
         */
        private double mean;
        /**
         * Base standard deviation of the service time.
         */
        private double sd;
        /**
         * Variance used by the normal distribution.
         */
        private double variance;

        /**
         * Temporary mean value used for service-time adjustments.
         */
        private double temp_mean;
        /**
         * Temporary standard deviation value used for service-time adjustments.
         */
        private double temp_sd;

        /**
         * Creates a self check-in service point.
         *
         * @param engine simulation engine that owns this service point
         * @param servicePointName name of the service point
         */
        public SelfCheckin(SimulationEngine engine, String servicePointName) {
            super(engine, servicePointName);
            this.config = engine.getConfiguration();
            this.mean = config.getSelfCheckinMean();
            this.sd = config.getSelfCheckinStdDev();

            temp_mean = mean;
            temp_sd = sd;

            this.variance = sd * sd;
            grouping.add(this);
            this.serviceGenerator = new Normal(mean, variance);
        }

        /**
         * Generates a service time for a passenger at self check-in.
         *
         * The sampled value must be positive.
         *
         * @param passenger passenger being served
         * @return generated service time
         */
        @Override
        protected double sampleServiceTime(Passenger passenger) {
            double value;

            // Rejection sampling for negative values
            do {
                value = serviceGenerator.sample();
            } while (value <= 0);

            return value;
        }

        /**
         * Returns the event type that marks completion of this service point.
         *
         * @return self check-in completion event type
         */
        @Override
        protected EventType getCompletionEventType() {
            return EventType.SELF_CHECKIN_COMPLETE;
        }

        /**
         * Routes the passenger to the next service point after self check-in.
         *
         * After self check-in, the passenger is sent to either fast track security
         * or regular security depending on passenger properties.
         *
         * @param passenger passenger that completed service
         */
        @Override
        protected void routeAfterCompletion(Passenger passenger) {

            double traversal = passenger.sampleTraversalTime(Transition.CHECKIN_TO_SECURITY);
            // Record traversal history
            passenger.recordTraversalTime(servicePointName, traversal);

            EventType next =
                    passenger.usesFastTrackSecurity()
                            ? EventType.ARRIVAL_FASTTRACK_SECURITY
                            : EventType.ARRIVAL_REGULAR_SECURITY;

            engine.scheduleEvent(new Event(
                    Clock.getInstance().getTime() + traversal,
                    next,
                    passenger
            ));

            Platform.runLater(() ->
                    AirportView.getInstance().animatePassengerToNextService(passenger, next)
            );
        }
    }

    /**
     * Represents the regular security service point in the simulation.
     *
     * This service point handles passengers using the standard security process.
     * Service times are sampled from a normal distribution and adjusted using
     * the passenger's carry-on weight.
     */
    public class RegularSecurity extends ServicePoint {
        /**
         * Generator used to produce service times for this service point.
         */
        private ContinuousGenerator serviceGenerator;
        /**
         * Configuration object containing simulation parameters.
         */
        private Configuration config;
        /**
         * Base mean service time.
         */
        private double mean;
        /**
         * Base standard deviation of the service time.
         */
        private double sd;
        /**
         * Variance used by the normal distribution.
         */
        private double variance;

        /**
         * Temporary mean value used for service-time adjustments.
         */
        private double temp_mean;
        /**
         * Temporary standard deviation value used for service-time adjustments.
         */
        private double temp_sd;

        /**
         * Creates a regular security service point.
         *
         * @param engine simulation engine that owns this service point
         * @param servicePointName name of the service point
         */
        public RegularSecurity(SimulationEngine engine, String servicePointName) {
            super(engine, servicePointName);
            this.config = engine.getConfiguration();
            this.mean = config.getRegularSecurityMean();
            this.sd = config.getRegularSecurityStdDev();

            temp_mean = mean;
            temp_sd = sd;

            this.variance = sd * sd;
            this.serviceGenerator = new Normal(mean, variance);
            grouping.add(this);
        }

        /**
         * Generates a service time for a passenger at regular security.
         *
         * The sampled value must be positive. The final service time includes
         * an additional penalty based on carry-on weight.
         *
         * @param passenger passenger being served
         * @return generated service time
         */
        @Override
        protected double sampleServiceTime(Passenger passenger) {
            double baseTime;

            // Rejection sampling (Normal is unbounded)
            do {
                baseTime = serviceGenerator.sample();
            } while (baseTime <= 0);

            // Add weight penalty
            double weight = passenger.getCarryOnWeight();
            double factor = config.getSecurityWeightFactor();

            return baseTime + (weight * factor);
        }

        /**
         * Returns the event type that marks completion of this service point.
         *
         * @return regular security completion event type
         */
        @Override
        protected EventType getCompletionEventType() {
            return EventType.REGULAR_SECURITY_COMPLETE;
        }

        /**
         * Routes the passenger to the next service point after regular security.
         *
         * International passengers are sent to customs, while other passengers
         * are sent directly to boarding.
         *
         * @param passenger passenger that completed service
         */
        @Override
        protected void routeAfterCompletion(Passenger passenger) {

            double traversal;
            EventType nextArrival;

            if (passenger.isInternationalFlight()) {

                traversal = passenger.sampleTraversalTime(Transition.SECURITY_TO_CUSTOMS);
                nextArrival = EventType.ARRIVAL_CUSTOMS;

            } else {

                traversal = passenger.sampleTraversalTime(Transition.SECURITY_TO_BOARDING);
                nextArrival = EventType.ARRIVAL_BOARDING;
            }

            // Record traversal history
            passenger.recordTraversalTime(servicePointName, traversal);

            engine.scheduleEvent(new Event(
                    Clock.getInstance().getTime() + traversal,
                    nextArrival,
                    passenger
            ));

            Platform.runLater(() ->
                    AirportView.getInstance().animatePassengerToNextService(passenger, nextArrival)
            );
        }
    }

    /**
     * Represents the fast track security service point in the simulation.
     *
     * This service point handles passengers who are eligible for fast track security.
     * Service times are sampled from a normal distribution and adjusted using
     * the passenger's carry-on weight.
     */
    public class FastTrackSecurity extends ServicePoint {

        /**
         * Generator used to produce service times for this service point.
         */
        private ContinuousGenerator serviceGenerator;
        /**
         * Configuration object containing simulation parameters.
         */
        private Configuration config;
        /**
         * Base mean service time.
         */
        private double mean;
        /**
         * Base standard deviation of the service time.
         */
        private double sd;
        /**
         * Variance used by the normal distribution.
         */
        private double variance;

        /**
         * Temporary mean value used for service-time adjustments.
         */
        private double temp_mean;
        /**
         * Temporary standard deviation value used for service-time adjustments.
         */
        private double temp_sd;

        /**
         * Creates a fast track security service point.
         *
         * @param engine simulation engine that owns this service point
         * @param servicePointName name of the service point
         */
        public FastTrackSecurity(SimulationEngine engine, String servicePointName) {
            super(engine, servicePointName);
            this.config = engine.getConfiguration();
            this.mean = config.getFasttrackSecurityMean();
            this.sd = config.getFasttrackSecurityStdDev();
            this.variance = sd * sd;

            temp_mean = mean;
            temp_sd = sd;

            this.serviceGenerator = new Normal(mean, variance);
            grouping.add(this);

        }

        /**
         * Generates a service time for a passenger at fast track security.
         *
         * The sampled value must be positive. The final service time includes
         * an additional penalty based on carry-on weight.
         *
         * @param passenger passenger being served
         * @return generated service time
         */
        @Override
        protected double sampleServiceTime(Passenger passenger) {
            double baseTime;

            // Rejection sampling
            do {
                baseTime = serviceGenerator.sample();
            } while (baseTime <= 0);

            // Same weight penalty logic
            double weight = passenger.getCarryOnWeight();
            double factor = config.getSecurityWeightFactor();

            return baseTime + (weight * factor);
        }

        /**
         * Returns the event type that marks completion of this service point.
         *
         * @return fast track security completion event type
         */
        @Override
        protected EventType getCompletionEventType() {
            return EventType.FASTTRACK_SECURITY_COMPLETE;
        }

        /**
         * Routes the passenger to the next service point after fast track security.
         *
         * International passengers are sent to customs, while other passengers
         * are sent directly to boarding.
         *
         * @param passenger passenger that completed service
         */
        @Override
        protected void routeAfterCompletion(Passenger passenger) {

            double traversal;
            EventType nextArrival;

            if (passenger.isInternationalFlight()) {

                traversal =
                        passenger.sampleTraversalTime(Transition.SECURITY_TO_CUSTOMS);

                nextArrival = EventType.ARRIVAL_CUSTOMS;

            } else {

                traversal =
                        passenger.sampleTraversalTime(Transition.SECURITY_TO_BOARDING);

                nextArrival = EventType.ARRIVAL_BOARDING;
            }

            // Record traversal history
            passenger.recordTraversalTime(servicePointName, traversal);

            engine.scheduleEvent(new Event(
                    Clock.getInstance().getTime() + traversal,
                    nextArrival,
                    passenger
            ));

            Platform.runLater(() ->
                    AirportView.getInstance().animatePassengerToNextService(passenger, nextArrival)
            );
        }
    }

    /**
     * Represents the customs service point in the simulation.
     *
     * This service point handles passengers who must pass through customs
     * before boarding. Service times are sampled from a normal distribution
     * and scaled according to the passenger's ticket type.
     */
    public class Customs extends ServicePoint {

        /**
         * Generator used to produce service times for this service point.
         */
        private ContinuousGenerator serviceGenerator;
        /**
         * Configuration object containing simulation parameters.
         */
        private Configuration config;
        /**
         * Base mean service time used by this service point.
         */
        private double mean;
        /**
         * Base standard deviation of the service time.
         */
        private double sd;
        /**
         * Variance used by the normal distribution.
         */
        private double variance;

        /**
         * Temporary mean value used for service-time adjustments.
         */
        private double temp_mean;
        /**
         * Temporary standard deviation value used for service-time adjustments.
         */
        private double temp_sd;

        /**
         * Creates a customs service point.
         *
         * @param engine simulation engine that owns this service point
         * @param servicePointName name of the service point
         */
        public Customs(SimulationEngine engine, String servicePointName) {
            super(engine, servicePointName);
            this.config = engine.getConfiguration();
            this.mean = config.getFasttrackSecurityMean();
            this.sd = config.getFasttrackSecurityStdDev();
            this.variance = sd * sd;

            temp_mean = mean;
            temp_sd = sd;

            this.serviceGenerator = new Normal(mean, variance);
            grouping.add(this);
        }

        /**
         * Generates a service time for a passenger at customs.
         *
         * A positive base service time is sampled from a normal distribution
         * and scaled based on the passenger's ticket type.
         *
         * @param passenger passenger being served
         * @return generated service time
         */
        @Override
        protected double sampleServiceTime(Passenger passenger) {

            double baseTime;

            // Rejection sampling
            do {
                baseTime = serviceGenerator.sample();
            } while (baseTime <= 0);

            double scale;

            switch (passenger.getTicketType()) {
                case TicketType.ECONOMY:
                    scale = config.getEconomyCustomsScale();
                    break;
                case TicketType.BUSINESS:
                    scale = config.getBusinessCustomsScale();
                    break;
                default:
                    scale = config.getFirstCustomsScale();
            }

            return baseTime * scale;
        }

        /**
         * Returns the event type that marks completion of this service point.
         *
         * @return customs completion event type
         */
        @Override
        protected EventType getCompletionEventType() {
            return EventType.CUSTOMS_COMPLETE;
        }

        /**
         * Routes the passenger to boarding after customs completion.
         *
         * @param passenger passenger that completed service
         */
        @Override
        protected void routeAfterCompletion(Passenger passenger) {

            double traversal =
                    passenger.sampleTraversalTime(Transition.CUSTOMS_TO_BOARDING);
            // Record traversal history
            passenger.recordTraversalTime(servicePointName, traversal);

            engine.scheduleEvent(new Event(
                    Clock.getInstance().getTime() + traversal,
                    EventType.ARRIVAL_BOARDING,
                    passenger
            ));

            Platform.runLater(() ->
                    AirportView.getInstance().animatePassengerToNextService(passenger, EventType.ARRIVAL_BOARDING)
            );
        }
    }

    /**
     * Represents the boarding service point in the simulation.
     *
     * This service point handles the final boarding stage of the passenger journey.
     * When boarding is completed, the passenger is marked as finished in the system.
     */
    public class Boarding extends ServicePoint {

        /**
         * Generator used to produce service times for this service point.
         */
        private ContinuousGenerator serviceGenerator;
        /**
         * Configuration object containing simulation parameters.
         */
        private Configuration config;
        /**
         * Base mean service time.
         */
        private double mean;
        /**
         * Base standard deviation of the service time.
         */
        private double sd;
        /**
         * Variance used by the normal distribution.
         */
        private double variance;

        /**
         * Temporary mean value used for service-time adjustments.
         */
        private double temp_mean;
        /**
         * Temporary standard deviation value used for service-time adjustments.
         */
        private double temp_sd;


        /**
         * Creates a boarding service point.
         *
         * @param engine simulation engine that owns this service point
         * @param servicePointName name of the service point
         */
        public Boarding(SimulationEngine engine, String servicePointName) {
            super(engine, servicePointName);
            this.config = engine.getConfiguration();
            this.mean = config.getBoardingMean();
            this.sd = config.getBoardingStdDev();
            this.variance = sd * sd;

            temp_mean = mean;
            temp_sd = sd;

            this.serviceGenerator = new Normal(mean, variance);
            grouping.add(this);
        }

        /**
         * Generates a service time for a passenger at boarding.
         *
         * The sampled value must be positive.
         *
         * @param passenger passenger being served
         * @return generated service time
         */
        @Override
        protected double sampleServiceTime(Passenger passenger) {
            double value;

            // Rejection sampling (Normal is unbounded)
            do {
                value = serviceGenerator.sample();
            } while (value <= 0);

            return value;
        }

        /**
         * Returns the event type that marks completion of this service point.
         *
         * @return boarding completion event type
         */
        @Override
        protected EventType getCompletionEventType() {
            return EventType.BOARDING_COMPLETE;
        }

        /**
         * Finalizes the passenger after boarding completion.
         *
         * The passenger departure time is recorded and the simulation engine
         * updates system-level completion statistics.
         *
         * @param passenger passenger that completed boarding
         */

        /*
        @Override
        protected void routeAfterCompletion(Passenger passenger) {
        passenger.setDepartureTime(finishTime);
        engine.recordPassengerCompletion(passenger);
        }

         */

        @Override
        protected void routeAfterCompletion(Passenger passenger) {

            double finishTime = Clock.getInstance().getTime();

            // Mark passenger as finished
            passenger.setDepartureTime(finishTime);

            // Notify engine for statistics
            engine.recordPassengerCompletion(passenger);

            // --- GUI logging (NEW) ---
            boolean draining = finishTime > engine.getSimulationEndTime();

            Platform.runLater(() -> {
                AirportView view = AirportView.getInstance();
                if (view != null) {

                    if (draining) {
                        view.getInfoArea().appendText(
                                String.format("[%03d] %s | Finished at: %.1f (DRAINING)\n",
                                        passenger.getId(),
                                        passenger.getTicketType(),
                                        finishTime));
                    } else {
                        view.getInfoArea().appendText(
                                String.format("[%03d] %s | Finished at: %.1f\n",
                                        passenger.getId(),
                                        passenger.getTicketType(),
                                        finishTime));
                    }

                    view.getInfoArea().setScrollTop(Double.MAX_VALUE);
                }
            });
        }

    }


    // ---------- CSV EXPORT METHODS ----------

    /**
     * Exports aggregated statistics of all service points to a CSV file.
     *
     * @param filename name of the output file
     */
    public void exportServicePointCSV(String filename) {
        double simulationTime = Clock.getInstance().getTime();

        try (FileWriter writer = new FileWriter(filename)) {
            // Header
            writer.write("ServicePoint,Arrivals,Completions,AvgWaitingTime,AvgServiceTime,AvgResponseTime,Utilization,AvgQueueLength,LqPredicted,LqErrorPercent,AvgNumberInSystem,MaxQueueLength\n");

            for (ServicePoint sp : grouping) {
                if (sp.getCompletionCount() == 0) continue;

                writer.write(String.format("%s,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%.3f,%d\n",
                        sp.getServicePointName(),
                        sp.getArrivalCount(),
                        sp.getCompletionCount(),
                        sp.getAverageWaitingTime(),
                        sp.getAverageServiceTime(),
                        sp.getAverageResponseTime(),
                        sp.getUtilization(simulationTime),
                        sp.getAverageQueueLength(),
                        sp.getPredictedAverageQueueLength(),
                        sp.getLittleLawQueueErrorPercent(),
                        sp.getAverageNumberInSystem(),
                        sp.getMaxQueueLength()
                ));
            }
            System.out.println("Service point CSV exported: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Exports system-level performance metrics to a CSV file.
     *
     * @param filename name of the output file
     */
    public void exportSystemCSV(String filename) {
        double simulationTime = Clock.getInstance().getTime();
        if (totalPassengersCompleted == 0) return;

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Metric,Value\n");
            writer.write(String.format("TotalCompleted,%d\n", totalPassengersCompleted));
            writer.write(String.format("Throughput,%.3f\n", getSystemThroughput()));
            writer.write(String.format("AvgJourneyTime,%.3f\n", getAverageSystemTime()));
            writer.write(String.format("AvgNumberInSystem,%.3f\n", getAverageNumberInSystem()));
            writer.write(String.format("SimulationTime,%.3f\n", simulationTime));
            System.out.println("System CSV exported: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Exports passenger-specific routes and timestamps to a CSV file.
     *
     * @param filename name of the output file
     */
    public void exportPassengerCSV(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            // HEADER
            writer.write("PassengerID,FlightType,TicketType,LuggageType,SelfCheckin,CarryOnWeight,SystemArrivalTime,DepartureTime,TotalJourneyTime");
            for (ServicePoint sp : grouping) {
                String name = sp.getServicePointName().replace(" ", "_");
                writer.write(",Queue_" + name + ",ServiceStart_" + name + ",ServiceCompletion_" + name + ",Traversal_" + name);
            }
            writer.write("\n");

            // ROWS
            int id = 1;
            for (Passenger p : allPassengers) {
                if (p.getDepartureTime() <= 0) continue;

                writer.write(String.format("%d,%s,%s,%s,%b,%.3f,%.3f,%.3f,%.3f",
                        id,
                        p.isInternationalFlight() ? "International" : "Domestic",
                        p.getTicketType(),
                        p.getCheckinLuggageType(),
                        p.isEligibleForSelfCheckin(),
                        p.getCarryOnWeight(),
                        p.getSystemArrivalTime(),
                        p.getDepartureTime(),
                        p.getTotalJourneyTime()
                ));

                for (ServicePoint sp : grouping) {
                    String name = sp.getServicePointName();
                    writer.write(String.format(",%.3f,%.3f,%.3f,%.3f",
                            p.getQueueEntryTimeFor(name),
                            p.getServiceStartTimeFor(name),
                            p.getServiceCompletionTimeFor(name),
                            p.getTraversalTimeFrom(name)
                    ));
                }
                writer.write("\n");
                id++;
            }
            System.out.println("Passenger CSV exported: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------- CONTROL METHODS ----------

    /**
     * Enables or disables step mode.
     *
     * @param mode true to enable step-by-step execution, false to disable it
     */
    public void setStepMode(boolean mode) { this.stepMode = mode; }
    /**
     * Requests execution of the next simulation step.
     *
     * This method is typically used by the user interface when step mode is enabled.
     */
    public void requestNextStep() { this.nextStepRequested = true; }
    /**
     * Returns whether step mode is currently enabled.
     *
     * @return true if step mode is enabled, otherwise false
     */
    public boolean isStepMode() { return stepMode; }

    /**
     * Resets the simulation to its initial state.
     *
     * The event list, passenger data, statistics, simulation clock,
     * random seed, and service points are reset, and the first arrival
     * event is scheduled again.
     */
    public void resetSimulation() {

        running = false;

        stepMode = false;
        nextStepRequested = false;

        eventList = new EventList();
        allPassengers.clear();
        totalPassengersCompleted = 0;
        cumulativeSystemTime = 0.0;

        Clock.getInstance().setTime(0);

        SeedGenerator.getDefaultSeedGenerator().setSeed(this.baseSeed);

        for (ServicePoint sp : grouping) {
            sp.reset();
        }

        initArrivalGenerator();

        scheduleEvent(new Event(0, EventType.ARRIVAL_SYSTEM, new Passenger(this)));

    }


}


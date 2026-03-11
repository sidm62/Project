package org.example;

import javafx.application.Platform;
import org.example.distributions.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class SimulationEngine {

    private EventList eventList;
    private boolean running;
    private double simulationEndTime;
    private NormalCheckin normalCheckin;
    private SelfCheckin selfCheckin;
    private RegularSecurity regularSecurity;
    private FastTrackSecurity fastTrackSecurity;
    private Customs customs;
    private Boarding boarding;
    private long baseSeed;
    private boolean stepMode = false;
    private boolean nextStepRequested = false;

    int totalPassengersCompleted;
    private double cumulativeSystemTime;


    private Configuration config;

    private ContinuousGenerator arrivalGenerator;

    private ArrayList<ServicePoint> grouping;

    public ArrayList<Passenger> allPassengers;



    private boolean debugMode;

    private double timeScale; // used for time scaling for sleep
    private double speedMultiplier;
    private long maxSleep = 10000;
    private long realStartTime;
    private double simStartTime;

    private double baseArrivalLambda;
    private double currentArrivalLambda;
    private volatile double arrivalSpeedFactor = 1.0;
    private volatile double serviceSpeedFactor = 1.0;
    private volatile double traversalSpeedFactor = 1.0;
    private final double MIN_FACTOR = 0.5;
    private final double MAX_FACTOR = 2.0;
    private final double MAX_LOAD_RATIO = 1.29;

    private double clamp(double value) {
        return Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, value));
    }


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
    public List<Passenger> getAllPassengers() {
        return new ArrayList<>(this.allPassengers);}

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode_answer) {
        this.debugMode = debugMode_answer;
    }

    public double getTimeScale() {
        return timeScale;
    }

    public void setTimeScale(double timeScale) {
        this.timeScale = timeScale;
    }



    // ---------- Formatting Helpers ----------


    private String f2(double v) {                    // Rules we’ll follow: Times → 2 decimalsRates → 3 decimalsUtilization → percentErrors → percentCSV → numeric only (no % symbols)
        return String.format("%.2f", v);
    }



    public Configuration getConfiguration() {
        return this.config;
    }


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

    public void updateArrivalLambda(double newLambda) {
        if (newLambda <= 0) throw new IllegalArgumentException("Lambda must be positive.");

        this.currentArrivalLambda = newLambda;

        if (arrivalGenerator instanceof Negexp negexp) {
            negexp.setMean(newLambda);
        }
    }


    // Schedule a new event in the future
    public void scheduleEvent(Event event) {
        eventList.schedule(event); // Use EventList method, not getEvents()
    }



    public double getSimulationEndTime() {
        return simulationEndTime;
    }

    public double getSystemThroughput() {
        return simulationEndTime == 0 ? 0 : totalPassengersCompleted / simulationEndTime;
    }

    public double getAverageSystemTime() {
        return totalPassengersCompleted == 0 ? 0 : cumulativeSystemTime / totalPassengersCompleted;
    }

    public double getAverageNumberInSystem() {

        double X = getSystemThroughput();
        double R = getAverageSystemTime();

        return X * R;
    }




    private void finalizeServicePointStatistics() {
        for (ServicePoint sp : grouping) {
            sp.finalizeStatistics();
        }
    }
    public List<ServicePoint> getAllServicePoints() {
        return new ArrayList<>(this.grouping);
    }

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
    public double getArrivalSpeedFactor() {
        return arrivalSpeedFactor;
    }

    public double getTraversalSpeedFactor() {
        return traversalSpeedFactor;
    }






    public void recordPassengerCompletion(Passenger passenger) {
        totalPassengersCompleted++;
        double journeyTime = passenger.getTotalJourneyTime();
        cumulativeSystemTime += journeyTime;
    }
    public int getTotalPassengersCompleted() {
        return this.totalPassengersCompleted;
    }


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


    public void setSpeedMultiplier(double multiplier) {

        if (multiplier <= 0) {
            throw new IllegalArgumentException("Multiplier must be positive.");
        }

        this.speedMultiplier = multiplier;
        double newTimeScale = 1000.0 / multiplier;
        this.timeScale = newTimeScale;
        changeSpeed(this.timeScale);
    }
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
    public synchronized void setTraversalSpeedFactor (double factor) throws Exception {
        double clampedFactor = clamp(factor);
        if (!validateFactors(arrivalSpeedFactor, serviceSpeedFactor, clampedFactor)) {
            throw new Exception("Invalid traversal speed factor");
        }
        this.traversalSpeedFactor = clampedFactor;
        System.out.println("TraversalSpeedFactor on päivitetty:" + clampedFactor);
    }






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

                // 🔥 TÄRKEÄ KORJAUS: Kun askel otetaan, synkronoidaan reaalimaailman kello
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
        // Tämä siirtää ohjauksen SimulationEngineltä AirportView'lle

    }

    // Process individual events based on type
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
                normalCheckin.handleArrival(passenger);
                break;

            case NORMAL_CHECKIN_COMPLETE:
                normalCheckin.handleCompletion(passenger);
                break;


        /* ==============================
           SELF CHECK-IN
        ============================== */

            case ARRIVAL_SELF_CHECKIN:
                selfCheckin.handleArrival(passenger);
                break;

            case SELF_CHECKIN_COMPLETE:
                selfCheckin.handleCompletion(passenger);
                break;


        /* ==============================
           REGULAR SECURITY
        ============================== */

            case ARRIVAL_REGULAR_SECURITY:
                regularSecurity.handleArrival(passenger);
                break;

            case REGULAR_SECURITY_COMPLETE:
                regularSecurity.handleCompletion(passenger);
                break;


        /* ==============================
           FAST TRACK SECURITY
        ============================== */

            case ARRIVAL_FASTTRACK_SECURITY:
                fastTrackSecurity.handleArrival(passenger);
                break;

            case FASTTRACK_SECURITY_COMPLETE:
                fastTrackSecurity.handleCompletion(passenger);
                break;


        /* ==============================
           CUSTOMS
        ============================== */

            case ARRIVAL_CUSTOMS:
                customs.handleArrival(passenger);
                break;

            case CUSTOMS_COMPLETE:
                customs.handleCompletion(passenger);
                break;


        /* ==============================
           BOARDING
        ============================== */

            case ARRIVAL_BOARDING:
                boarding.handleArrival(passenger);
                break;

            case BOARDING_COMPLETE:
                boarding.handleCompletion(passenger);
                break;


            default:
                throw new IllegalArgumentException(
                        "Unhandled event type: " + event.getType()
                );
        }
    }

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
        } else if (debugMode) {
            System.out.println("=== ARRIVALS STOPPED (Soft Close Activated) ===");
        }

    }

    public class NormalCheckin extends ServicePoint {

        private ContinuousGenerator serviceGenerator;
        private Configuration config;
        private double mean; // baseMean
        private double sd; // baseStd
        private double variance;

        private double temp_mean; // NEW
        private double temp_sd; // NEW (just trying sum out 3.3)

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

        @Override
        protected EventType getCompletionEventType() {
            return EventType.NORMAL_CHECKIN_COMPLETE;
        }

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
        }
    }

    public class SelfCheckin extends ServicePoint {

        private ContinuousGenerator serviceGenerator;
        private Configuration config;
        private double mean;
        private double sd;
        private double variance;

        private double temp_mean;
        private double temp_sd;

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

        @Override
        protected double sampleServiceTime(Passenger passenger) {
            double value;

            // Rejection sampling for negative values
            do {
                value = serviceGenerator.sample();
            } while (value <= 0);

            return value;
        }

        @Override
        protected EventType getCompletionEventType() {
            return EventType.SELF_CHECKIN_COMPLETE;
        }

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
        }
    }

    public class RegularSecurity extends ServicePoint {
        private ContinuousGenerator serviceGenerator;
        private Configuration config;
        private double mean;
        private double sd;
        private double variance;

        private double temp_mean;
        private double temp_sd;

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

        @Override
        protected EventType getCompletionEventType() {
            return EventType.REGULAR_SECURITY_COMPLETE;
        }

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
        }
    }

    public class FastTrackSecurity extends ServicePoint {

        private ContinuousGenerator serviceGenerator;
        private Configuration config;
        private double mean;
        private double sd;
        private double variance;

        private double temp_mean;
        private double temp_sd;

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

        @Override
        protected EventType getCompletionEventType() {
            return EventType.FASTTRACK_SECURITY_COMPLETE;
        }

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
        }
    }

    public class Customs extends ServicePoint {

        private ContinuousGenerator serviceGenerator;
        private Configuration config;
        private double mean;
        private double sd;
        private double variance;

        private double temp_mean;
        private double temp_sd;

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

        @Override
        protected EventType getCompletionEventType() {
            return EventType.CUSTOMS_COMPLETE;
        }

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
        }
    }

    public class Boarding extends ServicePoint {

        private ContinuousGenerator serviceGenerator;
        private Configuration config;
        private double mean;
        private double sd;
        private double variance;

        private double temp_mean;
        private double temp_sd;


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

        @Override
        protected double sampleServiceTime(Passenger passenger) {
            double value;

            // Rejection sampling (Normal is unbounded)
            do {
                value = serviceGenerator.sample();
            } while (value <= 0);

            return value;
        }

        @Override
        protected EventType getCompletionEventType() {
            return EventType.BOARDING_COMPLETE;
        }

        @Override
        protected void routeAfterCompletion(Passenger passenger) {

            // Mark passenger as finished
            passenger.setDepartureTime(Clock.getInstance().getTime());

            // Notify engine for statistics
            engine.recordPassengerCompletion(passenger);
        }
    }


    // ---------- CSV EXPORT METHODS ----------

    /**
     * Tallentaa jokaisen palvelupisteen kootut tilastot.
     */
    public void exportServicePointCSV(String filename) {
        double simulationTime = Clock.getInstance().getTime();

        try (FileWriter writer = new FileWriter(filename)) {
            // Header
            writer.write("ServicePoint,Arrivals,Completions,AvgWaitingTime,AvgServiceTime,AvgResponseTime,Utilization,AvgQueueLength,LqPredicted,LqErrorPercent,MaxQueueLength\n");

            for (ServicePoint sp : grouping) {
                if (sp.getCompletionCount() == 0) continue;

                writer.write(String.format("%s,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%d\n",
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
                        sp.getMaxQueueLength()
                ));
            }
            System.out.println("Service point CSV exported: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tallentaa järjestelmän yleiset suorituskykymittarit.
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
     * Tallentaa matkustajakohtaiset reitit ja aikaleimat.
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

    public void setStepMode(boolean mode) { this.stepMode = mode; }
    public void requestNextStep() { this.nextStepRequested = true; }
    public boolean isStepMode() { return stepMode; }

    public void resetSimulation() {
        this.running = false;
        this.eventList = new EventList();
        this.allPassengers.clear();
        this.totalPassengersCompleted = 0;
        this.cumulativeSystemTime = 0.0;
        Clock.getInstance().setTime(0);

        SeedGenerator.getDefaultSeedGenerator().setSeed(this.baseSeed);
        for (ServicePoint sp : grouping) {
            sp.reset();
        }
        initArrivalGenerator();

        scheduleEvent(new Event(0, EventType.ARRIVAL_SYSTEM, new Passenger(this)));
        this.running = true;
    }

}


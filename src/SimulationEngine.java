import distributions.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

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

    private int totalPassengersCompleted;
    private double cumulativeSystemTime;


    private Configuration config;

    private ContinuousGenerator arrivalGenerator;

    private ArrayList<ServicePoint> grouping;

    private ArrayList<Passenger> allPassengers;

    private boolean debugMode;

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

    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode_answer) {
        this.debugMode = debugMode_answer;
    }

    // ---------- Formatting Helpers ----------
    private String f3(double v) {
        return String.format("%.3f", v);
    }

    private String f2(double v) {                    // Rules we’ll follow: Times → 2 decimalsRates → 3 decimalsUtilization → percentErrors → percentCSV → numeric only (no % symbols)
        return String.format("%.2f", v);
    }

    private String percent(double v) {
        return String.format("%.2f%%", v * 100);
    }

    public Configuration getConfiguration() {
        return this.config;
    }

    private void initArrivalGenerator() {

        String type = config.getArrivalDistributionType();

        if (type.equalsIgnoreCase("NEGEXP")) {

            arrivalGenerator =
                    new Negexp(config.getArrivalLambda());

        } else {
            throw new IllegalArgumentException(
                    "Unsupported arrival distribution: " + type
            );
        }
    }



    // Schedule a new event in the future
    public void scheduleEvent(Event event) {
        eventList.schedule(event); // Use EventList method, not getEvents()
    }

    public int getTotalPassengersCompleted() {
        return totalPassengersCompleted;
    }

    public double getSimulationEndTime() { return simulationEndTime; }

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

    public void printSystemStatistics() {

        double X = getSystemThroughput();
        double R = getAverageSystemTime();
        double L = getAverageNumberInSystem();

        System.out.println("========== SYSTEM STATISTICS ==========");
        System.out.printf("%-28s %d%n", "Total Completed:", totalPassengersCompleted);
        System.out.printf("%-28s %s%n", "Throughput (X):", f3(X));
        System.out.printf("%-28s %s%n", "Average Journey Time (R):", f2(R));
        System.out.printf("%-28s %s%n", "Average Number In System (L):", f2(L));
        System.out.println("=======================================\n");

        /*

        double X = getSystemThroughput();
        double R = getAverageSystemTime();
        double L = getAverageNumberInSystem();

        System.out.println("===== SYSTEM STATISTICS =====");
        System.out.println("Total Completed: " + totalPassengersCompleted);
        System.out.printf("Throughput (X): " + X, f3(X));
        System.out.printf("Average Journey Time (R): " + R, f2(R));
        System.out.printf("Average Number In System (L): " + L, f2(L));
        System.out.printf("=============================");

         */
    }


    private void finalizeServicePointStatistics() {
        for (ServicePoint sp : grouping) {
            sp.finalizeStatistics();
        }
    }

    private void printStatistics() {

        double simulationTime = Clock.getInstance().getTime();

        System.out.println("===== SERVICE POINT RESULTS =====");
        System.out.println("Total Simulation Time: " + f2(simulationTime));
        System.out.println();

        for (ServicePoint sp : grouping) {

            System.out.println("Service Point: " + sp.getServicePointName());
            System.out.println("Arrivals: " + sp.getArrivalCount());
            System.out.println("Completions: " + sp.getCompletionCount());

            System.out.println("Avg Waiting Time: " +
                    f2(sp.getAverageWaitingTime()));

            System.out.println("Avg Service Time: " +
                    f2(sp.getAverageServiceTime()));

            System.out.println("Avg Response Time: " +
                    f2(sp.getAverageResponseTime()));

            System.out.println("Utilization: " +
                    percent(sp.getUtilization(simulationTime)));

            System.out.println("Avg Queue Length (Measured): " +
                    f2(sp.getAverageQueueLength()));

            System.out.println("Little's Law Lq (Predicted): " +
                    f2(sp.getPredictedAverageQueueLength()));

            System.out.println("Little's Law Error (%): " +
                    percent(sp.getLittleLawQueueErrorPercent()));

            System.out.println("-----------------------------------");
        }
    }

    public void recordPassengerCompletion(Passenger passenger) {
        totalPassengersCompleted++;
        double journeyTime = passenger.getTotalJourneyTime();
        cumulativeSystemTime += journeyTime;
    }

    /*

    private void checkAndAdjustServicePoint(ServicePoint sp, double simulationTime) {
        double utilization = sp.getUtilization(simulationTime);
        double avgQueue = sp.getLiveAverageQueueLength();

        final double BOTTLENECK_UTIL = 0.95;
        final double BOTTLENECK_QUEUE = 50;
        final double ADJUST_FACTOR = 0.9;

        if (utilization > BOTTLENECK_UTIL || avgQueue > BOTTLENECK_QUEUE) {
            sp.adjustServiceTime(ADJUST_FACTOR);
            if (debugMode) {
                System.out.printf("[ADJUST] %s bottlenecked: scale service by %.2f%n",
                        sp.getServicePointName(), ADJUST_FACTOR);
            }
        } else {
            sp.adjustServiceTime(1.0); // Reset to original mean/stdDev
        }
    }

     */

    private void checkAndAdjustServicePoint(ServicePoint sp, double simulationTime) {
        double utilization = sp.getUtilization(simulationTime);
        double avgQueue = sp.getLiveAverageQueueLength();

        final double HIGH_UTIL = 0.95;
        final double LOW_UTIL  = 0.85;   // hysteresis lower bound

        final double HIGH_QUEUE = 50;
        final double LOW_QUEUE  = 20;

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
        }
        else {
            return; // inside stability band → do nothing
        }

        sp.adjustServiceTime(factor);
        sp.setLastAdjustmentTime(simulationTime);
    }

    // Main simulation loop
    public void run() {
        running = true;

        // Loop until no events remain or simulation time ends
        while (running && !eventList.isEmpty()) {

            Event event = eventList.getNextEvent(); // Get next scheduled event

            // Advance the global clock to this event's time
            Clock.getInstance().setTime(event.getEventTime());

            if (debugMode) {
                Passenger p = event.getPassenger();
                if (p != null) {
                    System.out.printf(
                            "[TIME %.3f] Processing event: %s | PassengerID: %d%n",
                            Clock.getInstance().getTime(),
                            event.getType(),
                            p.getId()
                    );
                } else {
                    System.out.printf(
                            "[TIME %.3f] Processing event: %s%n",
                            Clock.getInstance().getTime(),
                            event.getType()
                    );
                }
            }

            // Process this event (arrival, check-in complete, security, etc.)
            processEvent(event);

            for (ServicePoint sp : grouping) {
                checkAndAdjustServicePoint(sp, Clock.getInstance().getTime()); // dynamically adjust service times
            }
        }

        running = false;

        double simulationTime = Clock.getInstance().getTime();
        System.out.println("Simulation ended at time: " + simulationTime);

        finalizeServicePointStatistics();
        printSystemStatistics();
        printStatistics();

        exportServicePointCSV("ServicePoints.csv");
        exportSystemCSV("System.csv");
        exportPassengerCSV("Passengers.csv");

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

        variance = sd*sd;
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
        this.sd   = config.getRegularSecurityStdDev();

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


public void exportServicePointCSV(String filename) {

    double simulationTime = Clock.getInstance().getTime();

    try (FileWriter writer = new FileWriter(filename)) {

        // Header
        writer.write("ServicePoint,Arrivals,Completions,AvgWaitingTime,AvgServiceTime,AvgResponseTime,Utilization,AvgQueueLength,LqPredicted,LqErrorPercent,MaxQueueLength\n");

        // Rows
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

public void exportSystemCSV(String filename) {

    double simulationTime = Clock.getInstance().getTime();

    if (totalPassengersCompleted == 0) {
        System.out.println("No completed passengers. System CSV not exported.");
        return;
    }


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

public void exportPassengerCSV(String filename) {

    try (FileWriter writer = new FileWriter(filename)) {

        // =============================
        // HEADER
        // =============================
        writer.write("PassengerID,FlightType,TicketType,LuggageType,SelfCheckin,CarryOnWeight,SystemArrivalTime,DepartureTime,TotalJourneyTime");

        for (ServicePoint sp : grouping) {
            String name = sp.getServicePointName().replace(" ", "_");
            writer.write(",Queue_" + name);
            writer.write(",ServiceStart_" + name);
            writer.write(",ServiceCompletion_" + name);
            writer.write(",Traversal_" + name);
        }

        writer.write("\n");

        // =============================
        // ROWS
        // =============================
        int id = 1;

        for (Passenger p : allPassengers) {

            if (p.getDepartureTime() <= 0) continue;

            writer.write(String.format("%d,%s,%s,%s,%b,%.3f,%.3f,%.3f,%.3f",
                    id,
                    p.getFlightType(),
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

                double queueTime = p.getQueueEntryTimeFor(name);
                double startTime = p.getServiceStartTimeFor(name);
                double completionTime = p.getServiceCompletionTimeFor(name);
                double traversalTime = p.getTraversalTimeFrom(name);

                writer.write(String.format(",%.3f,%.3f,%.3f,%.3f",
                        queueTime,
                        startTime,
                        completionTime,
                        traversalTime
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




}

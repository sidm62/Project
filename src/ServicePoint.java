package org.example;

import java.util.LinkedList;
import org.example.distributions.ContinuousGenerator;
import org.example.distributions.Normal;

import static java.lang.Math.clamp;

public abstract class ServicePoint {

    protected LinkedList<Passenger> queue;
    protected boolean serverBusy;

    protected SimulationEngine engine;
    protected double simulationEndTime_from_engine;

    // Performance Counters
    protected int arrivalCount;     // A
    protected int completionCount;  // C
    protected double busyTime;      // B
    protected double cumulativeResponseTime; // sigma(R)
    protected double cumulativeWaitingTime; // sigma(Wq)

    // Queue statistics
    protected double areaUnderQueueLengthCurve;
    protected double lastQueueLengthUpdateTime;
    protected int maxQueueLength;


    // Identifier for this service point (used for historical tracking)
    protected String servicePointName;

    protected double baseMean;
    protected double baseStdDev;
    protected double temp_mean;
    protected double temp_stdDev;

    private double currentFactor = 1.0;
    private double lastAdjustmentTime = 0.0;

    private double baseServiceMean;
    private double currentServiceMean;

    public static final String NORMAL_CHECKIN = "Normal Check-in";
    public static final String SELF_CHECKIN = "Self Check-in";
    public static final String REGULAR_SECURITY = "Regular Security";
    public static final String FASTTRACK_SECURITY = "Fast Track Security";
    public static final String CUSTOMS = "Customs";
    public static final String BOARDING = "Boarding";

    /*
    How the Service Point Strings are:

        this.normalCheckin = new NormalCheckin(this, "Normal Check-in");
        this.selfCheckin = new SelfCheckin(this, "Self Check-in");
        this.regularSecurity = new RegularSecurity(this, "Regular Security");
        this.fastTrackSecurity = new FastTrackSecurity(this, "Fast Track Security");
        this.customs = new Customs(this, "Customs");
        this.boarding = new Boarding(this, "Boarding");
     */


    protected ContinuousGenerator serviceGenerator;


    public ServicePoint(SimulationEngine engine, String servicePointName) {
        this.queue = new LinkedList<>();
        this.serverBusy = false;
        this.engine = engine;
        this.servicePointName = servicePointName;
        this.simulationEndTime_from_engine = engine.getSimulationEndTime();

        this.arrivalCount = 0;
        this.completionCount = 0;
        this.busyTime = 0;
        this.cumulativeResponseTime = 0;

        this.areaUnderQueueLengthCurve = 0;
        this.lastQueueLengthUpdateTime = 0;
        this.maxQueueLength = 0;

        this.baseServiceMean = engine.getConfiguration().getServiceMeanFor(servicePointName);
        this.currentServiceMean = baseServiceMean;

    }

    public void updateBaseServiceMean(double newBaseMean) {
        this.baseServiceMean = newBaseMean;

        // Reset runtime mean to new base
        this.currentServiceMean = newBaseMean;

        // Recreate generator if needed

        if (serviceGenerator instanceof Normal normal) {
            normal.setMean(newBaseMean);
        }
    }

    public void adjustServiceTime(double factor) {
        // 1. Määritellään rajat (samat kuin aiemmin)
        double MIN_FACTOR = 0.6;   // Max 40% nopeutus
        double MAX_FACTOR = 1.0;   // Ei hidastusta yli perusnopeuden

        // 2. Asetetaan kerroin rajojen sisään
        this.currentFactor = Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, factor));
        this.lastAdjustmentTime = Clock.getInstance().getTime();

        // 3. Lasketaan uudet arvot käyttäen baseServiceMean-muuttujaa
        // Jos baseStdDev on 0, käytetään oletuksena 20% hajontaa keskiarvosta
        if (baseStdDev <= 0) baseStdDev = baseServiceMean * 0.2;

        this.temp_mean = baseServiceMean * currentFactor;
        this.temp_stdDev = baseStdDev * currentFactor;

        // Varmistetaan, ettei hajonta mene nollaan (Normal-jakauma vaatii varianssin > 0)
        double min_stdDev = 0.001;
        this.temp_stdDev = Math.max(this.temp_stdDev, min_stdDev);

        // 4. Päivitetään generaattori uudella skaalatulla Normal-jakaumalla
        // Huom: Normal ottaa parametreina (keskiarvo, varianssi eli stdDev^2)
        this.serviceGenerator = new Normal(temp_mean, temp_stdDev * temp_stdDev);

        if (engine.isDebugMode()) {
            System.out.printf("[SCALING] %s: Factor %.2f -> New Mean: %.2f%n",
                    servicePointName, currentFactor, temp_mean);
        }
    }

    public void setLastAdjustmentTime(double lastAdjustmentTime) {
        this.lastAdjustmentTime = lastAdjustmentTime;
    }
    public double getCurrentFactor() {
        return this.currentFactor;
    }
    public double getLastAdjustmentTime() {
        return this.lastAdjustmentTime;
    }



    public Passenger dequeue() {
        updateQueueStatistics();
        Passenger next = queue.poll();
        if (engine.isDebugMode() & next != null) {
            System.out.printf(
                    "[TIME %.3f] %s QUEUED SERVICE START | PassengerID: %d | Queue size: %d%n",
                    Clock.getInstance().getTime(),
                    servicePointName,
                    next.getId(),
                    queue.size()
            );
        }
        return next;

    }

    public void startService(Passenger passenger) {
        serverBusy = true;
        double currentTime = Clock.getInstance().getTime();
        passenger.setServiceStartTime(currentTime);
        passenger.recordServiceStartTime(servicePointName, currentTime); // Historical record
    }

    protected abstract double sampleServiceTime(Passenger passenger);

    protected abstract EventType getCompletionEventType();

    protected abstract void routeAfterCompletion(Passenger passenger);

    public String getServicePointName() { return this.servicePointName; }
    public int getArrivalCount() { return this.arrivalCount; }
    public int getCompletionCount() { return this.completionCount; }

    public double getUtilization(double simulationTime) {
        return simulationTime == 0 ? 0 : busyTime / simulationTime;
    }

    public double getThroughput() {
        return simulationEndTime_from_engine == 0 ? 0 : completionCount / simulationEndTime_from_engine;
    }

    public double getAverageServiceTime() {
        return completionCount == 0 ? 0 : busyTime / completionCount;
    }

    public double getAverageResponseTime() {
        return completionCount == 0 ? 0 : cumulativeResponseTime / completionCount;
    }

    // TODO: Should we include this in export Service CSV?
    public double getAverageNumberInSystem() {
        double X = getThroughput();
        double R = getAverageResponseTime();
        return X * R;
    }

    public double getAverageWaitingTime() {
        return completionCount == 0 ? 0 : cumulativeWaitingTime / completionCount;
    }

    public boolean isQueueEmpty() { return queue.isEmpty(); }
    public void enqueue(Passenger passenger) {
        updateQueueStatistics();
        queue.add(passenger);

        if (engine.isDebugMode()) {
            System.out.printf(
                    "[TIME %.3f] %s ENQUEUE | PassengerID: %d | Queue size: %d%n",
                    Clock.getInstance().getTime(),
                    servicePointName,
                    passenger.getId(),
                    queue.size()
            );
        }
    }

    private void updateQueueStatistics() {

        double currentTime = Clock.getInstance().getTime();
        double timeSinceLastUpdate = currentTime - lastQueueLengthUpdateTime;

        areaUnderQueueLengthCurve +=
                queue.size() * timeSinceLastUpdate;

        lastQueueLengthUpdateTime = currentTime;

        // Track maximum queue size
        if (queue.size() > maxQueueLength) {
            maxQueueLength = queue.size();
        }
    }

    public double getLiveAverageQueueLength() {
        double currentSimTime = Clock.getInstance().getTime();
        return currentSimTime <= 0
                ? 0
                : areaUnderQueueLengthCurve / currentSimTime;
    }

    public double getAverageQueueLength() {
        return simulationEndTime_from_engine == 0
                ? 0
                : areaUnderQueueLengthCurve / simulationEndTime_from_engine;
    }

    public int getMaxQueueLength() {
        return maxQueueLength;
    }

    public double getPredictedAverageQueueLength() {

        if (simulationEndTime_from_engine == 0 || completionCount == 0) {
            return 0;
        }

        double X = completionCount / simulationEndTime_from_engine;
        double Wq = cumulativeWaitingTime / completionCount;

        return X * Wq;
    }

    public double getLittleLawQueueError() {

        double measured = getAverageQueueLength();
        double predicted = getPredictedAverageQueueLength();

        return Math.abs(measured - predicted);
    }

    public double getLittleLawQueueErrorPercent() {

        double predicted = getPredictedAverageQueueLength();

        if (predicted == 0) return 0;

        double measured = getAverageQueueLength();

        return Math.abs(measured - predicted) / predicted * 100.0;
    }

    public void finalizeStatistics() {

        // Final update of queue area
        double timeSinceLastUpdate = simulationEndTime_from_engine - lastQueueLengthUpdateTime;
        areaUnderQueueLengthCurve += queue.size() * timeSinceLastUpdate;

        lastQueueLengthUpdateTime = simulationEndTime_from_engine;
    }

    public void printLittleLawValidation() {

        double measured = getAverageQueueLength();
        double predicted = getPredictedAverageQueueLength();
        double errorPercent = getLittleLawQueueErrorPercent();

        System.out.println("Service Point: " + servicePointName);
        System.out.println("Measured Lq: " + measured);
        System.out.println("Predicted Lq (Little): " + predicted);
        System.out.println("Error (%): " + errorPercent);
        System.out.println("---------------------------");
    }

    public void handleArrival(Passenger passenger) {

        arrivalCount++;
        double currentTime = Clock.getInstance().getTime();

        // Update both current timestamp and history
        passenger.setQueueEntryTime(currentTime);
        passenger.recordQueueEntryTime(servicePointName, currentTime);

        if (!serverBusy) {

            startService(passenger);

            if (engine.isDebugMode()) {
                System.out.printf(
                        "[TIME %.3f] %s SERVICE START | PassengerID: %d | Queue size: %d%n",
                        currentTime,
                        servicePointName,
                        passenger.getId(),
                        queue.size() // still zero if no queue
                );
            }

            double serviceTime = sampleServiceTime(passenger);

            engine.scheduleEvent(new Event(
                    Clock.getInstance().getTime() + serviceTime,
                    getCompletionEventType(),
                    passenger
            ));

        } else {
            enqueue(passenger);
        }
    }

    public void handleCompletion(Passenger passenger) {

        double completionTime = Clock.getInstance().getTime();
        passenger.setServiceCompletionTime(completionTime);
        passenger.recordServiceCompletionTime(servicePointName, completionTime); // Historical record
        completionCount++; // Increment C
        if (engine.isDebugMode()) {
            System.out.printf(
                    "[TIME %.3f] %s SERVICE COMPLETE (Immediate) | PassengerID: %d | Total completed: %d%n",
                    completionTime,
                    servicePointName,
                    passenger.getId(),
                    completionCount
            );
        }

        double individualServiceTime = passenger.getServiceCompletionTime() - passenger.getServiceStartTime();
        // this.setServiceTime(individualServiceTime);

        busyTime += individualServiceTime; // B accumulation

        double responseTime = passenger.getServiceCompletionTime() - passenger.getQueueEntryTime();
        cumulativeResponseTime += responseTime; // W accumulation

        double waitingTime = passenger.getServiceStartTime() - passenger.getQueueEntryTime();
        cumulativeWaitingTime += waitingTime;


        // finishService(passenger);
        serverBusy = false;


        if (!isQueueEmpty()) {

            Passenger next = dequeue();

            startService(next);

            double nextServiceTime = sampleServiceTime(next);

            engine.scheduleEvent(new Event(
                    Clock.getInstance().getTime() + nextServiceTime,
                    getCompletionEventType(),
                    next
            ));
        }

        routeAfterCompletion(passenger);
    }
    public void reset() {
        this.arrivalCount = 0;
        this.completionCount = 0;
        this.queue.clear();
        this.busyTime = 0;
        this.cumulativeResponseTime = 0;
        this.cumulativeWaitingTime = 0;

        this.areaUnderQueueLengthCurve = 0;
        this.lastQueueLengthUpdateTime = 0;
        this.maxQueueLength = 0;

        this.currentFactor = 1.0;
        this.lastAdjustmentTime = 0.0;

        this.currentServiceMean = baseServiceMean;
        if (serviceGenerator instanceof Normal normal) {
            normal.setMean(baseServiceMean);
        }

    }


}

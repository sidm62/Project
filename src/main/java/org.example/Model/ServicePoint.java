package org.example.Model;

import java.util.LinkedList;
import org.example.Model.distributions.ContinuousGenerator;
import org.example.Model.distributions.Normal;

import static java.lang.Math.clamp;

/**
 * Represents an abstract service point in the airport simulation.
 *
 * A service point manages a queue of passengers, service execution,
 * event scheduling, and performance statistics such as utilization,
 * throughput, waiting time, response time, and queue length.
 *
 * Concrete service points such as check-in, security, customs,
 * and boarding extend this class and implement their own service logic.
 */
public abstract class ServicePoint {

    /**
     * Queue of passengers waiting for service.
     */
    protected LinkedList<Passenger> queue;

    public LinkedList<Passenger> getQueue() {
        return queue;
    }

    /**
     * Indicates whether the server is currently busy.
     */
    protected boolean serverBusy;

    /**
     * Simulation engine that owns this service point.
     */
    protected SimulationEngine engine;

    /**
     * Simulation end time received from the simulation engine.
     */
    protected double simulationEndTime_from_engine;

    /**
     * Number of passenger arrivals to this service point.
     */
    protected int arrivalCount;

    /**
     * Number of completed services at this service point.
     */
    protected int completionCount;

    /**
     * Total busy time of the server.
     */
    protected double busyTime;

    /**
     * Sum of response times of completed passengers.
     */
    protected double cumulativeResponseTime;

    /**
     * Sum of waiting times of completed passengers.
     */
    protected double cumulativeWaitingTime;

    /**
     * Area under the queue length curve for average queue calculation.
     */
    protected double areaUnderQueueLengthCurve;

    /**
     * Simulation time of the last queue length update.
     */
    protected double lastQueueLengthUpdateTime;

    /**
     * Maximum observed queue length.
     */
    protected int maxQueueLength;

    /**
     * Name of the service point.
     */
    protected String servicePointName;

    /**
     * Base mean service time.
     */
    protected double baseMean;

    /**
     * Base standard deviation of service time.
     */
    protected double baseStdDev;

    /**
     * Temporary mean service time used for adjustments.
     */
    protected double temp_mean;

    /**
     * Temporary standard deviation used for adjustments.
     */
    protected double temp_stdDev;

    /**
     * Current service speed adjustment factor.
     */
    private double currentFactor = 1.0;

    /**
     * Simulation time of the last speed adjustment.
     */
    private double lastAdjustmentTime = 0.0;

    /**
     * Original base service mean for this service point.
     */
    private double baseServiceMean;

    /**
     * Current service mean in use.
     */
    private double currentServiceMean;

    /**
     * Constant name for normal check-in service point.
     */
    public static final String NORMAL_CHECKIN = "Normal Check-in";

    /**
     * Constant name for self check-in service point.
     */
    public static final String SELF_CHECKIN = "Self Check-in";

    /**
     * Constant name for regular security service point.
     */
    public static final String REGULAR_SECURITY = "Regular Security";

    /**
     * Constant name for fast track security service point.
     */
    public static final String FASTTRACK_SECURITY = "Fast Track Security";

    /**
     * Constant name for customs service point.
     */
    public static final String CUSTOMS = "Customs";

    /**
     * Constant name for boarding service point.
     */
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


    /**
     * Generator used to produce service times.
     */
    protected ContinuousGenerator serviceGenerator;


    private double baseMeanTime; // raw mean time
    private double effectiveMeanTime; // after factor applied

    /**
     * Creates a new service point.
     *
     * The constructor initializes the queue, engine reference,
     * service point name, performance counters, queue statistics,
     * and base service mean.
     *
     * @param engine simulation engine that owns this service point
     * @param servicePointName name of the service point
     */
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

    public double getBaseServiceMean() {
        return baseServiceMean;
    }

    /**
     * Updates the base service mean of this service point.
     *
     * The current service mean is reset to the new base value.
     * If the service generator is a normal distribution, its mean is updated.
     *
     * @param newBaseMean new base service mean
     */

    public void updateBaseServiceMean(double newBaseMean) {
        this.baseServiceMean = newBaseMean;

        // Reset runtime mean to new base
        this.currentServiceMean = newBaseMean;

        // Recreate generator if needed

        if (serviceGenerator instanceof Normal normal) {
            normal.setMean(newBaseMean);
        }
    }

    /**
     * Adjusts the service time parameters using the given factor.
     *
     * The factor is limited to an allowed range and used to update
     * temporary mean and standard deviation values for the service generator.
     *
     * @param factor adjustment factor for service speed
     */
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

    /**
     * Sets the time of the last service adjustment.
     *
     * @param lastAdjustmentTime time of last adjustment
     */
    public void setLastAdjustmentTime(double lastAdjustmentTime) {
        this.lastAdjustmentTime = lastAdjustmentTime;
    }
    /**
     * Returns the current service adjustment factor.
     *
     * @return current adjustment factor
     */
    public double getCurrentFactor() {
        return this.currentFactor;
    }
    /**
     * Returns the time of the last service adjustment.
     *
     * @return last adjustment time
     */
    public double getLastAdjustmentTime() {
        return this.lastAdjustmentTime;
    }



    /**
     * Removes and returns the next passenger from the queue.
     *
     * Queue statistics are updated before removal.
     *
     * @return next passenger in the queue, or null if queue is empty
     */
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

    /**
     * Starts service for the given passenger.
     *
     * The method marks the server as busy and records the service start time
     * for the passenger.
     *
     * @param passenger passenger whose service starts
     */
    public void startService(Passenger passenger) {
        serverBusy = true;
        double currentTime = Clock.getInstance().getTime();
        passenger.setServiceStartTime(currentTime);
        passenger.recordServiceStartTime(servicePointName, currentTime); // Historical record
    }

    /**
     * Generates a service time for the given passenger.
     *
     * @param passenger passenger being served
     * @return sampled service time
     */
    protected abstract double sampleServiceTime(Passenger passenger);

    /**
     * Returns the completion event type of this service point.
     *
     * @return completion event type
     */
    protected abstract EventType getCompletionEventType();

    /**
     * Routes the passenger to the next stage after service completion.
     *
     * @param passenger passenger that completed service
     */
    protected abstract void routeAfterCompletion(Passenger passenger);

    /**
     * Returns the name of this service point.
     *
     * @return service point name
     */
    public String getServicePointName() { return this.servicePointName; }
    /**
     * Returns the number of arrivals to this service point.
     *
     * @return arrival count
     */
    public int getArrivalCount() { return this.arrivalCount; }
    /**
     * Returns the number of completed services.
     *
     * @return completion count
     */
    public int getCompletionCount() { return this.completionCount; }

    /**
     * Calculates the utilization of this service point.
     *
     * @param simulationTime current simulation time
     * @return utilization ratio
     */
    public double getUtilization(double simulationTime) {
        return simulationTime == 0 ? 0 : busyTime / simulationTime;
    }

    /**
     * Calculates the throughput of this service point.
     *
     * @return throughput
     */
    public double getThroughput() {
        return simulationEndTime_from_engine == 0 ? 0 : completionCount / simulationEndTime_from_engine;
    }

    /**
     * Calculates the average service time.
     *
     * @return average service time
     */
    public double getAverageServiceTime() {
        return completionCount == 0 ? 0 : busyTime / completionCount;
    }

    /**
     * Calculates the average response time.
     *
     * @return average response time
     */
    public double getAverageResponseTime() {
        return completionCount == 0 ? 0 : cumulativeResponseTime / completionCount;
    }

    // TODO: Should we include this in export Service CSV?
    /**
     * Calculates the average number of passengers in the service point system.
     *
     * @return average number in system
     */
    public double getAverageNumberInSystem() {
        double X = getThroughput();
        double R = getAverageResponseTime();
        return X * R;
    }

    /**
     * Calculates the average waiting time in the queue.
     *
     * @return average waiting time
     */
    public double getAverageWaitingTime() {
        return completionCount == 0 ? 0 : cumulativeWaitingTime / completionCount;
    }

    /**
     * Returns whether the queue is empty.
     *
     * @return true if the queue is empty, otherwise false
     */
    public boolean isQueueEmpty() { return queue.isEmpty(); }
    /**
     * Adds a passenger to the queue.
     *
     * Queue statistics are updated before adding the passenger.
     *
     * @param passenger passenger to enqueue
     */
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

    /**
     * Updates queue statistics based on the current queue length
     * and elapsed simulation time.
     */
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

    /**
     * Calculates the live average queue length using the current simulation time.
     *
     * @return live average queue length
     */
    public double getLiveAverageQueueLength() {
        double currentSimTime = Clock.getInstance().getTime();
        return currentSimTime <= 0
                ? 0
                : areaUnderQueueLengthCurve / currentSimTime;
    }

    /**
     * Calculates the average queue length over the full simulation.
     *
     * @return average queue length
     */
    public double getAverageQueueLength() {
        return simulationEndTime_from_engine == 0
                ? 0
                : areaUnderQueueLengthCurve / simulationEndTime_from_engine;
    }

    /**
     * Returns the maximum observed queue length.
     *
     * @return maximum queue length
     */
    public int getMaxQueueLength() {
        return maxQueueLength;
    }

    /**
     * Calculates the predicted average queue length using Little's Law.
     *
     * @return predicted average queue length
     */
    public double getPredictedAverageQueueLength() {

        if (simulationEndTime_from_engine == 0 || completionCount == 0) {
            return 0;
        }

        double X = completionCount / simulationEndTime_from_engine;
        double Wq = cumulativeWaitingTime / completionCount;

        return X * Wq;
    }

    /**
     * Calculates the absolute error between measured and predicted
     * average queue length.
     *
     * @return absolute queue length error
     */
    public double getLittleLawQueueError() {

        double measured = getAverageQueueLength();
        double predicted = getPredictedAverageQueueLength();

        return Math.abs(measured - predicted);
    }

    /**
     * Calculates the percentage error between measured and predicted
     * average queue length.
     *
     * @return queue length error percentage
     */
    public double getLittleLawQueueErrorPercent() {

        double predicted = getPredictedAverageQueueLength();

        if (predicted == 0) return 0;

        double measured = getAverageQueueLength();

        return Math.abs(measured - predicted) / predicted * 100.0;
    }

    /**
     * Finalizes queue statistics at the end of the simulation.
     *
     * The remaining queue area is added using the simulation end time.
     */
    public void finalizeStatistics() {

        // Final update of queue area
        double timeSinceLastUpdate = simulationEndTime_from_engine - lastQueueLengthUpdateTime;
        areaUnderQueueLengthCurve += queue.size() * timeSinceLastUpdate;

        lastQueueLengthUpdateTime = simulationEndTime_from_engine;
    }

    /**
     * Prints Little's Law validation results for this service point.
     */
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

    /**
     * Handles the arrival of a passenger to this service point.
     *
     * If the server is free, service starts immediately and a completion event
     * is scheduled. Otherwise, the passenger is added to the queue.
     *
     * @param passenger arriving passenger
     */
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

            // double serviceTime = sampleServiceTime(passenger);
            double serviceTime = sampleServiceTime(passenger) / engine.getServiceSpeedFactor();

            engine.scheduleEvent(new Event(
                    Clock.getInstance().getTime() + serviceTime,
                    getCompletionEventType(),
                    passenger
            ));

        } else {
            enqueue(passenger);
        }
    }

    /**
     * Handles service completion for a passenger.
     *
     * The method updates timestamps, performance statistics,
     * starts service for the next queued passenger if available,
     * and routes the completed passenger forward.
     *
     * @param passenger passenger that completed service
     */
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

            // double nextServiceTime = sampleServiceTime(next);
            double nextServiceTime = sampleServiceTime(next) / engine.getServiceSpeedFactor();

            engine.scheduleEvent(new Event(
                    Clock.getInstance().getTime() + nextServiceTime,
                    getCompletionEventType(),
                    next
            ));
        }

        routeAfterCompletion(passenger);
    }
    /**
     * Resets this service point to its initial state.
     *
     * Queue contents, counters, statistics, and adjustment values are cleared.
     */
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

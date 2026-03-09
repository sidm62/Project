public class Main {

    public static void main(String[] args) throws InterruptedException {

        // 1. Test normal scenario with all factors 1.0 (baseline)
        // runSimulation(Scenario.NORMAL, 1.0, 1.0, 1.0, 60);

        // 2. Slow service by 50%
        // runSimulation(Scenario.NORMAL, 0.5, 1.0, 1.0, 60);

        // 3. Speed up traversal 2x
        // runSimulation(Scenario.NORMAL, 1.0, 2.0, 1.0, 60);

        // 4. Slow arrivals 0.7x
        // runSimulation(Scenario.NORMAL, 1.0, 1.0, 0.7, 60);

        // 5. Combine factors (optional)
        // runSimulation(Scenario.NORMAL, 0.5, 2.0, 0.7, 60);

        runSimulation(Scenario.SYSTEM_STRESS, 37298, 39749238, 2943728, 60);


    }



    private static void logSystemStats(SimulationEngine engine) {
        double currentTime = Clock.getInstance().getTime();
        System.out.printf("\n--- SYSTEM STATS @ time %.2f ---\n", currentTime);

        for (ServicePoint sp : engine.getGrouping()) {
            double avgQueue = sp.getLiveAverageQueueLength();
            double util = sp.getUtilization(currentTime);
            System.out.printf("%s | AvgQueue=%.2f | Util=%.2f%%\n",
                    sp.getServicePointName(), avgQueue, util * 100);
        }

        System.out.printf("Throughput (X) = %.3f passengers/unit time\n", engine.getLiveThroughput());
        System.out.println("--------------------------------------\n");
    }

    private static void runSimulation(
            Scenario scenario,
            double serviceFactor,
            double traversalFactor,
            double arrivalFactor,
            long runtimeSeconds
    ){

        System.out.println("\n====================================");
        System.out.println("Scenario: " + scenario);
        System.out.println("ServiceFactor: " + serviceFactor);
        System.out.println("TraversalFactor: " + traversalFactor);
        System.out.println("ArrivalFactor: " + arrivalFactor);
        System.out.println("====================================");

        Configuration config = new Configuration();
        double simulationEndTime = runtimeSeconds;

        Clock.getInstance().reset();

        SimulationEngine engine = new SimulationEngine(simulationEndTime, config);

        engine.setDebugMode(true);
        engine.setTimeScale(1000);
        engine.setSpeedMultiplier(1.0);

        try {
            engine.setArrivalSpeedFactor(arrivalFactor);
        } catch (Exception e) {
            System.out.println("Not valid factor set. " + "Forced value: " + engine.getArrivalSpeedFactor());
        }

        try {
            engine.setServiceSpeedFactor(serviceFactor);
        } catch (Exception e) {
            System.out.println("Not valid factor set. " + "Forced value: " + engine.getServiceSpeedFactor());
        }

        try {
            engine.setTraversalSpeedFactor(traversalFactor);
        } catch (Exception e) {
            System.out.println("Not valid factor set. " + "Forced value: " + engine.getTraversalSpeedFactor());
        }

        System.out.println("Arrival Speed Factor: " + engine.getArrivalSpeedFactor() + " Service Speed Factor: " + engine.getServiceSpeedFactor() + " Traversal Speed Factor: " + engine.getTraversalSpeedFactor());
        engine.applyScenario(scenario);



        // Seed first arrival
        engine.scheduleEvent(new Event(
                0.0,
                EventType.ARRIVAL_SYSTEM,
                new Passenger(engine)
        ));

        // Run simulation to completion (no monitoring loop needed)
        try {
            engine.run();
        } catch (Exception e) {
            System.out.println("Exception occured. Wow! ");
        }

        // Log stats after simulation finishes
        logSystemStats(engine);

        System.out.println("Simulation finished.\n");
    }
}

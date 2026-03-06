public class Main {

    public static void main(String[] args) throws InterruptedException {

        // 1. Test normal scenario with all factors 1.0 (baseline)
        // runSimulation(Scenario.NORMAL, 1.0, 1.0, 1.0, 60);

        // 2. Slow service by 50%
        // runSimulation(Scenario.NORMAL, 0.5, 1.0, 1.0, 60);

        // 3. Speed up traversal 2x
        runSimulation(Scenario.NORMAL, 1.0, 2.0, 1.0, 60);

        // 4. Slow arrivals 0.7x
        runSimulation(Scenario.NORMAL, 1.0, 1.0, 0.7, 60);

        // 5. Combine factors (optional)
        runSimulation(Scenario.NORMAL, 0.5, 2.0, 0.7, 60);
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
    ) throws InterruptedException {

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

        engine.applyScenario(scenario);

        engine.setServiceSpeedFactor(serviceFactor);
        engine.setTraversalSpeedFactor(traversalFactor);
        engine.setArrivalSpeedFactor(arrivalFactor);

        // Seed first arrival
        engine.scheduleEvent(new Event(
                0.0,
                EventType.ARRIVAL_SYSTEM,
                new Passenger(engine)
        ));

        // Run simulation to completion (no monitoring loop needed)
        engine.run();

        // Log stats after simulation finishes
        logSystemStats(engine);

        System.out.println("Simulation finished.\n");
    }
}

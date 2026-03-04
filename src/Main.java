public class Main {

    public static void main(String[] args) throws InterruptedException {

        Configuration config = new Configuration();
        double simulationEndTime = 400;

        SimulationEngine engine = new SimulationEngine(simulationEndTime, config);

        engine.setDebugMode(true);
        engine.setTimeScale(1000);
        engine.setSpeedMultiplier(1.0);

        // Seed first arrival
        engine.scheduleEvent(new Event(
                0.0,
                EventType.ARRIVAL_SYSTEM,
                new Passenger(engine)
        ));

        // Run simulation in separate thread
        Thread simThread = new Thread(engine::run);
        simThread.start();

        // --- Dynamic Day Timeline (TEST ONLY) ---

        switchScenario(engine, Scenario.NORMAL, 0);
        switchScenario(engine, Scenario.PEAK_TIME, 100);
        switchScenario(engine, Scenario.SYSTEM_STRESS, 200);
        switchScenario(engine, Scenario.RECOVERY_MODE, 300);

        simThread.join();
    }

    private static void switchScenario(SimulationEngine engine,
                                       Scenario scenario,
                                       double simulatedTime)
            throws InterruptedException {

        double realSleepTime =
                simulatedTime * engine.getTimeScale() / engine.getSpeedMultiplier();

        Thread.sleep((long) realSleepTime);

        System.out.println("\n>>> SWITCHING TO " + scenario);
        engine.applyScenario(scenario);
    }
}

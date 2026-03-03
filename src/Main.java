public class Main {

    public static void main(String[] args) {

        Configuration config = new Configuration();

        // (Optional) set parameters manually here if needed
        // config.setArrivalLambda(0.5);
        // config.setRandomSeed(12345);

        double simulationEndTime = 30;
        SimulationEngine engine = new SimulationEngine(simulationEndTime, config);

        engine.setDebugMode(true);

        engine.scheduleEvent(
                new Event(
                        0.0,
                        EventType.ARRIVAL_SYSTEM,
                        new Passenger(engine)
                )
        );

        engine.run();
    }
}

public class Main {

    public static void main(String[] args) {

        // 1️⃣ Create configuration
        Configuration config = new Configuration();

        // (Optional) set parameters manually here if needed
        // config.setArrivalLambda(0.5);
        // config.setRandomSeed(12345);

        // 2️⃣ Create simulation engine
        double simulationEndTime = 120.0; // e.g., 120 minutes
        SimulationEngine engine = new SimulationEngine(simulationEndTime, config);

        // 3️⃣ Turn debug on/off
        engine.setDebugMode(true);

        // 4️⃣ VERY IMPORTANT — schedule first arrival
        engine.scheduleEvent(
                new Event(
                        0.0,
                        EventType.ARRIVAL_SYSTEM,
                        new Passenger(engine)
                )
        );

        // 5️⃣ Run simulation
        engine.run();
    }
}

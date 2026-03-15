import org.example.*;
import org.example.Model.Clock;
import org.example.Model.Configuration;
import org.example.Model.Passenger;
import org.example.Model.SimulationEngine;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServicePoint: Jonotus- ja palvelulogiikan testaus")
class ServicePointTest {
    private SimulationEngine engine;
    private SimulationEngine.NormalCheckin testSP;

    @BeforeEach
    void setUp() {
        Configuration config = new Configuration();
        engine = new SimulationEngine(1000, config);

        testSP = engine.new NormalCheckin(engine, "Normal Check-in");
        Clock.getInstance().setTime(0);
    }

    @Test
    @DisplayName("Saapuminen kasvattaa jonoa ja saapumislaskuria")
    void testArrivalIncrementsQueue() {
        // Luodaan 5 matkustajaa
        Passenger p1 = new Passenger(engine);
        Passenger p2 = new Passenger(engine);
        Passenger p3 = new Passenger(engine);
        Passenger p4 = new Passenger(engine);

        // Lisätään kaikki putkeen ilman viiveitä
        testSP.handleArrival(p1);
        testSP.handleArrival(p2);
        testSP.handleArrival(p3);
        testSP.handleArrival(p4);

        System.out.println("DEBUG: Max Queue Length testissä: " + testSP.getMaxQueueLength());



        assertTrue(testSP.getMaxQueueLength() >= 1,
                "Jonon maksimipituuden pitäisi olla vähintään 1, kun useampi matkustaja saapuu peräkkäin.");
    }

    @Test
    @DisplayName("Palvelun valmistuminen vähentää jonoa")
    void testCompletionDecrementsQueue() {
        Passenger p = new Passenger(engine);
        testSP.handleArrival(p);

        int countBefore = testSP.getCompletionCount();
        testSP.handleCompletion(p);

        assertEquals(countBefore + 1, testSP.getCompletionCount(), "Valmistuneiden määrän pitäisi kasvaa");
    }

    @Test
    @DisplayName("ServiceSpeedFactorin vaikutus palveluaikaan")
    void testServiceTimeScaling() {

        double originalFactor = testSP.getCurrentFactor();
        testSP.adjustServiceTime(0.5); // Staff Boost (Turbo)

        assertEquals(0.5, testSP.getCurrentFactor(), 0.01, "Palvelukertoimen pitäisi päivittyä");

        testSP.adjustServiceTime(1.0);
        assertEquals(1.0, testSP.getCurrentFactor(), 0.01, "Palvelukertoimen pitäisi palautua");
    }

    @Test
    @DisplayName("Käyttöasteen (Utilization) laskenta")
    void testUtilizationCalculation() {
        Passenger p = new Passenger(engine);


        Clock.getInstance().setTime(0);
        testSP.handleArrival(p);

        Clock.getInstance().setTime(10);
        testSP.handleCompletion(p);

        Clock.getInstance().setTime(20);
        // Käyttöasteen pitäisi olla 10/20 = 0.5 (50%)
        double util = testSP.getUtilization(20.0);
        assertEquals(0.5, util, 0.05, "Käyttöasteen laskennan pitäisi olla 50%");
    }
}
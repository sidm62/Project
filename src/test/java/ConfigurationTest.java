import org.example.Model.Configuration;
import org.example.Model.Scenario;
import org.example.Model.ServicePoint;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Configuration: Syvällinen asetusten testaus")
class ConfigurationTest {
    private Configuration config;

    @BeforeEach
    void setUp() {
        config = new Configuration();
    }

    @Test
    @DisplayName("Skenaariokohtaisten arvojen latautuminen")
    void testScenarioServiceMeans() {
        var scenarioMap = config.getScenarioServiceMeans();

        assertTrue(scenarioMap.containsKey(Scenario.NORMAL), "Normal-skenaario puuttuu");
        assertTrue(scenarioMap.containsKey(Scenario.SYSTEM_STRESS), "Stress-skenaario puuttuu");

        // Tarkistetaan, että Stress-skenaariossa on hitaammat ajat (suurempi mean)
        double normalCheckin = scenarioMap.get(Scenario.NORMAL).get(ServicePoint.NORMAL_CHECKIN);
        double stressCheckin = scenarioMap.get(Scenario.SYSTEM_STRESS).get(ServicePoint.NORMAL_CHECKIN);

        assertTrue(stressCheckin > normalCheckin, "Stress-skenaarion pitäisi olla hitaampi kuin normaali");
    }

    @Test
    @DisplayName("Yksilöllisten nopeuskertoimien (Speed Factor) vaikutus")
    void testIndividualSpeedFactors() {
        // Oletusarvo on 4.0. Asetetaan kerroin 2.0 (tuplanopeus)
        config.setIndividualServiceSpeedFactors(ServicePoint.NORMAL_CHECKIN, 2.0);

        // getServiceMeanFor palauttaa: baseMean / factor -> 4.0 / 2.0 = 2.0
        double result = config.getServiceMeanFor(ServicePoint.NORMAL_CHECKIN);

        assertEquals(2.0, result, 0.001, "Keskiarvon pitäisi puolittua, kun nopeuskerroin on 2.0");
    }

    @Test
    @DisplayName("Tuntemattoman palvelupisteen haku")
    void testUnknownServicePointThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            config.getServiceMeanFor("Hogwarts Express Gate");
        }, "Tuntemattoman pisteen pitäisi heittää poikkeus");
    }

    @Test
    @DisplayName("Todennäköisyyksien eheys (Lipputyypit)")
    void testTicketTypeProbabilities() {
        double total = config.getEconomyProbability() +
                config.getBusinessProbability() +
                config.getFirstProbability();

        assertEquals(1.0, total, 0.001, "Lipputyyppien summan on oltava 100%");
    }

    @Test
    @DisplayName("Asetusmetodien toimivuus (Setterit)")
    void testSetters() {
        config.setServiceMeanFor(ServicePoint.CUSTOMS, 12.5);
        assertEquals(12.5, config.getServiceMeanFor(ServicePoint.CUSTOMS), "Customs-arvon päivitys ei toiminut");

        config.setArrivalLambda(5.5);
        assertEquals(5.5, config.getArrivalLambda(), "Arrival lambdan päivitys ei toiminut");
    }
}
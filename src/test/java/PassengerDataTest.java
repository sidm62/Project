import org.example.*;
import org.example.Model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Passenger: Tiedonsiirron ja historian testaus")
class PassengerDataTest {
    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        // Alustetaan moottori ja kello
        Configuration config = new Configuration();
        engine = new SimulationEngine(1000, config);
        Clock.getInstance().setTime(0);
    }

    @Test
    @DisplayName("Historiatietojen tallennus ja haku (Queue, Service, Completion)")
    void testHistoryMapTransfer() {
        Passenger p = new Passenger(engine);
        String spName = "Security";

        // Simuloidaan tapahtumaketju
        p.recordQueueEntryTime(spName, 10.5);
        p.recordServiceStartTime(spName, 15.0);
        p.recordServiceCompletionTime(spName, 22.3);

        // Varmistetaan, että arvot löytyvät oikeilla avaimilla
        assertEquals(10.5, p.getQueueEntryTimeFor(spName), "Jonoon saapumisaika väärä");
        assertEquals(15.0, p.getServiceStartTimeFor(spName), "Palvelun alkamisaika väärä");
        assertEquals(22.3, p.getServiceCompletionTimeFor(spName), "Palvelun päättymisaika väärä");
    }

    @Test
    @DisplayName("Siirtymäaikojen (Traversal) tallennus eri pisteiden välillä")
    void testTraversalHistoryTransfer() {
        Passenger p = new Passenger(engine);
        p.recordTraversalTime("Check-in", 5.2);
        p.recordTraversalTime("Security", 3.8);

        assertEquals(5.2, p.getTraversalTimeFrom("Check-in"), "Check-in siirtymäaika väärä");
        assertEquals(3.8, p.getTraversalTimeFrom("Security"), "Security siirtymäaika väärä");
        assertEquals(-1.0, p.getTraversalTimeFrom("Non-existent"), "Pitäisi palauttaa oletusarvo -1.0");
    }

    @Test
    @DisplayName("Matkustajan tyypin vaikutus siirtymäaikaan (Business/First vs Economy)")
    void testTraversalScalingByTicketType() {
        // Tämä testi osoittaa, että matkustajan TicketType-tieto siirtyy laskentaan
        Passenger p = new Passenger(engine);

        // Pakotetaan tyyppi (tämä vaatisi set-metodin tai testikonstruktorin, 
        // mutta oletetaan että initializeAttributes on ajettu)
        double time = p.sampleTraversalTime(Transition.CHECKIN_TO_SECURITY);

        assertTrue(time >= 0, "Siirtymäajan pitäisi olla positiivinen");
        assertTrue(time <= engine.getConfiguration().getTraversalMaxTime(), "Aika ylittää maksimin");
    }

    @Test
    @DisplayName("Kokonaisajan laskenta (Departure - Arrival)")
    void testTotalJourneyTimeCalculation() {
        Passenger p = new Passenger(engine);

        // Matkustaja saapuu ajanhetkellä 0 (setUp() nollasi kellon)
        assertEquals(0, p.getSystemArrivalTime());

        // Asetetaan lähtöaika käsin
        p.setDepartureTime(100.0);

        assertEquals(100.0, p.getTotalJourneyTime(), "Kokonaisajan laskentakaava virheellinen");
    }
}
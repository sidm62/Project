import org.example.Model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class PassengerJourneyTest {

    private SimulationEngine engine;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.getInstance();
        clock.reset();
        // Alustetaan moottori (varmista, että Configuration on olemassa)
        engine = new SimulationEngine(1000.0, new Configuration());
        Passenger.resetIdCounter();
    }


    @Test
    @DisplayName("Testaa matkustajan elinkaari EventType-arvoilla")
    void testPassengerFullEventJourney() {
        clock.setTime(10.0);
        Passenger p = new Passenger(engine); // systemArrivalTime = 10.0

        clock.setTime(100.0);
        // KORJAUS: Käytä setDepartureTime-metodia, jota getTotalJourneyTime hyödyntää
        p.setDepartureTime(clock.getTime());

        assertEquals(10.0, p.getSystemArrivalTime());
        assertEquals(100.0, p.getDepartureTime());
        assertEquals(90.0, p.getTotalJourneyTime(), 0.001, "Kokonaisajan laskenta epäonnistui");
    }

    @Test
    @DisplayName("Varmista että kello etenee EventType-ketjun mukaisesti")
    void testClockProgressionWithEvents() {
        Passenger p = new Passenger(engine);

        // Simuloidaan tapahtumaketjua
        double t1 = 10.0; // ARRIVAL_SYSTEM
        double t2 = 40.0; // NORMAL_CHECKIN_COMPLETE
        double t3 = 80.0; // BOARDING_COMPLETE

        clock.setTime(t1);
        double arrival = clock.getTime();

        clock.setTime(t2);
        double checkin = clock.getTime();

        clock.setTime(t3);
        p.setRemovalTime(clock.getTime());

        assertTrue(arrival < checkin, "Checkin-tapahtuman pitäisi olla saapumisen jälkeen");
        assertTrue(checkin < p.getRemovalTime(), "Boarding-tapahtuman pitäisi olla checkinin jälkeen");
    }

    @Test
    @DisplayName("Testaa historiatiedot palvelukohtaisilla EventType-tyypeillä")
    void testServiceHistoryWithSpecificEvents() {
        Passenger p = new Passenger(engine);

        // Käytetään nimiä, jotka vastaavat EventType-logiikkaa
        String checkinPoint = EventType.NORMAL_CHECKIN_COMPLETE.name();
        String boardingPoint = EventType.BOARDING_COMPLETE.name();

        clock.setTime(50.0);
        p.recordServiceCompletionTime(checkinPoint, clock.getTime());

        clock.setTime(120.0);
        p.recordServiceCompletionTime(boardingPoint, clock.getTime());

        assertEquals(50.0, p.getServiceCompletionTimeFor(checkinPoint));
        assertEquals(120.0, p.getServiceCompletionTimeFor(boardingPoint));
    }
}
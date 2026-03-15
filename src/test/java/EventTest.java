import org.example.Model.Event;
import org.example.Model.EventType;
import org.example.Model.Passenger;
import org.example.Model.SimulationEngine;
import org.example.Model.Configuration;
import org.junit.jupiter.api.*;
import java.util.PriorityQueue;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Event: Tapahtumien ja aikajärjestyksen testaus")
class EventTest {
    private Passenger mockPassenger;
    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        Configuration config = new Configuration();
        engine = new SimulationEngine(1000, config);
        mockPassenger = new Passenger(engine);
    }

    @Test
    @DisplayName("Tapahtuman tiedot tallentuvat oikein")
    void testEventCreation() {
        double time = 45.5;
        // Käytetään sinun enumisi tyyppiä ARRIVAL_SYSTEM
        EventType type = EventType.ARRIVAL_SYSTEM;

        Event e = new Event(time, type, mockPassenger);

        assertEquals(time, e.getEventTime(), "Ajan pitäisi täsmätä");
        assertEquals(type, e.getType(), "Tyypin pitäisi täsmätä");
        assertEquals(mockPassenger, e.getPassenger(), "Matkustajan pitäisi täsmätä");
    }

    @Test
    @DisplayName("Tapahtumien vertailu (CompareTo) ajan perusteella")
    void testEventComparison() {
        // Käytetään olemassa olevia enum-arvoja
        Event early = new Event(10.0, EventType.ARRIVAL_SYSTEM, mockPassenger);
        Event late = new Event(20.0, EventType.ARRIVAL_SYSTEM, mockPassenger);
        Event sameTime = new Event(10.0, EventType.BOARDING_COMPLETE, mockPassenger);

        // early (10.0) vs late (20.0)
        assertTrue(early.compareTo(late) < 0, "Aikaisemman tapahtuman pitäisi olla pienempi");
        assertTrue(late.compareTo(early) > 0, "Myöhemmän tapahtuman pitäisi olla suurempi");

        // Samanaikaiset (10.0 vs 10.0)
        assertEquals(0, early.compareTo(sameTime), "Samanaikaisten tapahtumien vertailun pitäisi olla 0");
    }

    @Test
    @DisplayName("PriorityQueue järjestää tapahtumat oikein")
    void testPriorityQueueOrdering() {
        PriorityQueue<Event> eventList = new PriorityQueue<>();

        // Lisätään tapahtumia "väärässä" järjestyksessä
        eventList.add(new Event(50.0, EventType.BOARDING_COMPLETE, mockPassenger));
        eventList.add(new Event(10.0, EventType.ARRIVAL_SYSTEM, mockPassenger));
        eventList.add(new Event(30.0, EventType.NORMAL_CHECKIN_COMPLETE, mockPassenger));

        // poll() hakee aina ajallisesti lähimmän (pienimmän) tapahtuman
        assertEquals(10.0, eventList.poll().getEventTime(), "Ensimmäisen pitäisi olla 10.0");
        assertEquals(30.0, eventList.poll().getEventTime(), "Toisen pitäisi olla 30.0");
        assertEquals(50.0, eventList.poll().getEventTime(), "Kolmannen pitäisi olla 50.0");
    }
}
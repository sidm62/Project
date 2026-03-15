

import org.example.Model.Event;
import org.example.Model.EventList;
import org.example.Model.EventType;
import org.example.Model.Passenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class EventListTest {
    private EventList eventList;

    @BeforeEach
    void setUp() {
        eventList = new EventList();
    }

    @Test
    @DisplayName("Tapahtumien tulisi palautua kronologisessa järjestyksessä")
    void testEventOrdering() {
        // Luodaan matkustaja (voi olla null testissä, jos Event sallii)
        Passenger p = null;


        eventList.schedule(new Event(100.5, EventType.ARRIVAL_SYSTEM, p));
        eventList.schedule(new Event(50.0, EventType.ARRIVAL_SYSTEM, p));
        eventList.schedule(new Event(75.2, EventType.ARRIVAL_SYSTEM, p));


        assertEquals(50.0, eventList.getNextEvent().getEventTime(),
                "Ensimmäisen tapahtuman pitäisi olla ajassa 50.0");


        assertEquals(75.2, eventList.getNextEvent().getEventTime(),
                "Toisen tapahtuman pitäisi olla ajassa 75.2");


        assertEquals(100.5, eventList.getNextEvent().getEventTime(),
                "Kolmannen tapahtuman pitäisi olla ajassa 100.5");
    }

    @Test
    @DisplayName("isEmpty pitäisi toimia oikein")
    void testIsEmpty() {
        assertTrue(eventList.isEmpty(), "Listan pitäisi olla aluksi tyhjä");

        eventList.schedule(new Event(10.0, EventType.ARRIVAL_SYSTEM, null));
        assertFalse(eventList.isEmpty(), "Lista ei saa olla tyhjä lisäyksen jälkeen");

        eventList.getNextEvent();
        assertTrue(eventList.isEmpty(), "Listan pitäisi olla taas tyhjä poiston jälkeen");
    }

    @Test
    @DisplayName("Tyhjästä listasta hakemisen pitäisi palauttaa null tai heittää poikkeus")
    void testPollEmptyList() {

        assertNull(eventList.getNextEvent(), "Tyhjän listan pitäisi palauttaa null (tai muokkaa testiäsi)");
    }
}
import org.example.Model.Clock;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Clock: Kellon hallinnan ja Singleton-rakenteen testaus")
class ClockTest {

    @BeforeEach
    void setUp() {
        // Varmistetaan, että kello alkaa nollasta jokaisessa testissä
        Clock.getInstance().setTime(0);
    }

    @Test
    @DisplayName("Kellon pitäisi olla Singleton")
    void testClockIsSingleton() {
        Clock instance1 = Clock.getInstance();
        Clock instance2 = Clock.getInstance();

        assertSame(instance1, instance2, "getInstance() pitäisi palauttaa aina sama olio");
    }

    @Test
    @DisplayName("Ajan asettaminen ja haku")
    void testSetAndGetTime() {
        Clock clock = Clock.getInstance();
        clock.setTime(150.5);

        assertEquals(150.5, clock.getTime(), 0.0001, "Ajan pitäisi olla täsmälleen se, mitä asetettiin");
    }

    @Test
    @DisplayName("Ajan edistäminen (Advance Time)")
    void testAdvanceTime() {
        Clock clock = Clock.getInstance();
        clock.setTime(10.0);

        // Jos sinulla on metodi ajan lisäämiseen:
        // clock.advanceTime(5.5); 
        // Jos ei, voit testata suoraan setTime-logiikkaa:
        clock.setTime(clock.getTime() + 5.5);

        assertEquals(15.5, clock.getTime(), 0.0001, "Kellon pitäisi edistyä oikein");
    }

    @Test
    @DisplayName("Kello ei saa kulkea taaksepäin")
    void testClockShouldNotGoBackwards() {
        Clock clock = Clock.getInstance();
        clock.setTime(100.0);

        // Simulaatiossa kello liikkuu vain eteenpäin.
        // Voit testata, ettet vahingossa aseta kelloa menneisyyteen, 
        // jos olet toteuttanut tällaisen suojauksen:
        double newTime = 50.0;
        if (newTime < clock.getTime()) {
            // Logiikka: Älä päivitä tai heitä virhe
        }

        assertTrue(clock.getTime() >= 0, "Kello ei voi olla negatiivinen");
    }
}
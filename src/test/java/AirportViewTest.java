import org.example.View.AirportView;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AirportView: Käyttöliittymän perusrakenne")
class AirportViewTest {
    private AirportView view;

    @Test
    @DisplayName("Näkymän pitäisi alustua ilman virheitä")
    void testViewInitialization() {

        assertDoesNotThrow(() -> {

        }, "Näkymän luomisen ei pitäisi kaataa ohjelmaa");
    }
}
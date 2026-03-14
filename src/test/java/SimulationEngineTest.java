import org.example.Model.Configuration;
import org.example.Model.Passenger;
import org.example.Model.SimulationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SimulationEngineTest {
    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        Configuration config = new Configuration();
        engine = new SimulationEngine(1000, config);
    }
    @Test
    @DisplayName("Kertoimien validointi: Load ratio ei saa ylittyä")
    void testLoadRatioValidation() {
        // Testataan, että liian suuri matkustajavirta suhteessa palveluun heittää poikkeuksen
        assertThrows(Exception.class, () -> {
            engine.setServiceSpeedFactor(1.5); // Palvelu hitaaksi (kerroin 2.0)
            engine.setArrivalSpeedFactor(2.0);  // Saapuminen nopeaksi (kerroin 2.0)
            // loadRatio = 2.0 / 0.5 (jos käänteinen) tai muu suhde > 1.29
        }, "Moottorin pitäisi estää epävakaa tila (loadRatio > 1.29)");
    }
    @Test
    @DisplayName("Reset-toiminto: AllPassengers pitäisi tyhjentyä")
    void testResetSimulation() {
        // Simuloidaan tilanne, jossa on matkustajia
        engine.allPassengers.add(new Passenger(engine));
        engine.resetSimulation();

        assertTrue(engine.getAllPassengers().isEmpty(), "Matkustajalistan pitäisi olla tyhjä resetin jälkeen");
    }

}
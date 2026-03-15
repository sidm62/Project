

import org.example.Model.distributions.Negexp;
import org.example.Model.distributions.Normal;
import org.example.Model.distributions.Uniform;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Distributions: Satunnaisjakaumien validointi")
class DistributionsTest {

    @Test
    @DisplayName("Negexp (Exponential) -jakauma: Ei negatiivisia lukuja")
    void testNegexpNonNegative() {
        // Parametrit: mean = 5.0, seed = 123
        Negexp dist = new Negexp(5.0, 123);

        for (int i = 0; i < 1000; i++) {
            double value = dist.sample();
            assertTrue(value >= 0, "Negexp tuotti negatiivisen luvun: " + value);
        }
    }

    @Test
    @DisplayName("Normal-jakauma: Pysyy positiivisena")
    void testNormalPositive() {
        // Parametrit: mean = 10.0, variance = 2.0 (huom: jotkut kirjastot käyttävät varianssia, jotkut keskihajontaa)
        Normal dist = new Normal(10.0, 2.0, 123);

        for (int i = 0; i < 1000; i++) {
            double value = dist.sample();
            // Testataan, ettei simulaatioon päädy mahdottomia aikoja
            assertTrue(value >= 0, "Normal-jakauma tuotti negatiivisen luvun: " + value);
        }
    }

    @Test
    @DisplayName("Uniform-jakauma: Pysyy annettujen rajojen sisällä")
    void testUniformBounds() {
        double min = 1.0;
        double max = 3.0;
        Uniform dist = new Uniform(min, max, 123);

        for (int i = 0; i < 1000; i++) {
            double value = dist.sample();
            assertTrue(value >= min && value <= max,
                    "Uniform-arvo " + value + " on rajojen [" + min + "," + max + "] ulkopuolella");
        }
    }
}
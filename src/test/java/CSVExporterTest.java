import org.example.Model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class CSVExporterTest {


    @TempDir
    Path tempDir;

    @Test
    void testCSVExportContent() throws Exception {

        SimulationEngine engine = new SimulationEngine(100.0, new Configuration());
        List<Passenger> passengers = new ArrayList<>();

        Passenger p1 = new Passenger(engine);
        p1.setDepartureTime(100.0);
        passengers.add(p1);


        File testFile = tempDir.resolve("test_results.csv").toFile();

        // TÄSSÄ KUTSUTAAN OMAA EXPORT-METODIASI
        // Esimerkki: DataManager.exportToCSV(passengers, testFile);
        exportToCSV(passengers, testFile);

        // 3. Tarkistus: Onko tiedosto olemassa?
        assertTrue(testFile.exists(), "CSV-tiedostoa ei luotu");

        // 4. Sisällön tarkistus: Luetaan rivit
        List<String> lines = Files.readAllLines(testFile.toPath());

        assertFalse(lines.isEmpty(), "Tiedosto on tyhjä");

        // Tarkistetaan otsikkorivi (header)
        String header = lines.get(0);
        assertTrue(header.contains("ID") || header.contains("id"), "Otsikkoriviltä puuttuu ID");
        assertTrue(header.contains("JourneyTime"), "Otsikkoriviltä puuttuu JourneyTime");

        // Tarkistetaan, että data-rivillä on oikea määrä erottimia (esim. pilkkuja tai puolipisteitä)
        String dataLine = lines.get(1);
        String[] parts = dataLine.split(","); // Vaihda ";" jos käytät puolipistettä
        assertTrue(parts.length >= 2, "Datarivillä on liian vähän sarakkeita");
    }

    // Apumetodi testausta varten (ellet käytä jo olemassa olevaa metodia)
    private void exportToCSV(List<Passenger> passengers, File file) throws Exception {
        java.io.PrintWriter writer = new java.io.PrintWriter(file);
        writer.println("ID,FlightType,TicketType,JourneyTime");
        for (Passenger p : passengers) {
            writer.println(p.getId() + "," + p.getFlightType() + "," + p.getTicketType() + "," + p.getTotalJourneyTime());
        }
        writer.close();
    }
}
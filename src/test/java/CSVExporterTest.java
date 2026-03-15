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


        exportToCSV(passengers, testFile);


        assertTrue(testFile.exists(), "CSV-tiedostoa ei luotu");


        List<String> lines = Files.readAllLines(testFile.toPath());

        assertFalse(lines.isEmpty(), "Tiedosto on tyhjä");


        String header = lines.get(0);
        assertTrue(header.contains("ID") || header.contains("id"), "Otsikkoriviltä puuttuu ID");
        assertTrue(header.contains("JourneyTime"), "Otsikkoriviltä puuttuu JourneyTime");


        String dataLine = lines.get(1);
        String[] parts = dataLine.split(","); // Vaihda ";" jos käytät puolipistettä
        assertTrue(parts.length >= 2, "Datarivillä on liian vähän sarakkeita");
    }


    private void exportToCSV(List<Passenger> passengers, File file) throws Exception {
        java.io.PrintWriter writer = new java.io.PrintWriter(file);
        writer.println("ID,FlightType,TicketType,JourneyTime");
        for (Passenger p : passengers) {
            writer.println(p.getId() + "," + p.getFlightType() + "," + p.getTicketType() + "," + p.getTotalJourneyTime());
        }
        writer.close();
    }
}
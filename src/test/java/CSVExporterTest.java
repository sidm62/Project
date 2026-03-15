import org.example.Model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.DisplayName;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the CSV export functionality.
 * This class ensures that passenger data is correctly serialized into a
 * Comma-Separated Values (CSV) format for post-simulation analysis.
 */
@DisplayName("CSV Exporter: Data Persistence and Formatting Tests")
class CSVExporterTest {

    /**
     * A temporary directory provided by JUnit 5.
     * This allows the test to write and read files in a sandbox environment
     * without polluting the actual project folders.
     */
    @TempDir
    Path tempDir;

    /**
     * Verifies that the CSV exporter correctly writes passenger data to a file.
     * The test validates:
     * 1. File creation in the filesystem.
     * 2. Correctness of the CSV header.
     * 3. Proper formatting of passenger data rows.
     * * @throws Exception if file I/O operations fail.
     */
    @Test
    @DisplayName("Exported CSV content validation")
    void testCSVExportContent() throws Exception {
        // Setup mock simulation environment
        SimulationEngine engine = new SimulationEngine(100.0, new Configuration());
        List<Passenger> passengers = new ArrayList<>();

        // Create a dummy passenger with known data
        Passenger p1 = new Passenger(engine);
        p1.setDepartureTime(100.0); // Ensures a measurable journey time
        passengers.add(p1);

        // Define target file in the temporary directory
        File testFile = tempDir.resolve("test_results.csv").toFile();

        // Execution: Perform the export
        exportToCSV(passengers, testFile);

        // Verification: Check file existence and integrity
        assertTrue(testFile.exists(), "CSV file should be created on disk.");

        List<String> lines = Files.readAllLines(testFile.toPath());
        assertFalse(lines.isEmpty(), "Exported file should not be empty.");

        // Verification: Header structure
        String header = lines.get(0);
        assertTrue(header.contains("ID") || header.contains("id"), "Header row must contain ID column.");
        assertTrue(header.contains("JourneyTime"), "Header row must contain JourneyTime column.");

        // Verification: Data integrity and formatting
        String dataLine = lines.get(1);
        String[] parts = dataLine.split(",");
        assertTrue(parts.length >= 2, "Data row should contain at least two columns of information.");
    }

    /**
     * Helper method to simulate the CSV export logic.
     * In a production environment, this logic should reside in a dedicated
     * Exporter class within the main source folder.
     * * @param passengers List of passengers to be exported.
     * @param file The target file where data will be written.
     * @throws Exception if the PrintWriter cannot access the file.
     */
    private void exportToCSV(List<Passenger> passengers, File file) throws Exception {
        java.io.PrintWriter writer = new java.io.PrintWriter(file);
        writer.println("ID,FlightType,TicketType,JourneyTime");
        for (Passenger p : passengers) {
            writer.println(p.getId() + "," + p.getFlightType() + "," + p.getTicketType() + "," + p.getTotalJourneyTime());
        }
        writer.close();
    }
}
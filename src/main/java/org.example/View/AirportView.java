package org.example.View;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;


import org.example.Controller.AirportController;
import org.example.Model.*;

/**
 * Provides the JavaFX user interface for the airport simulation.
 *
 * This class is responsible for visualizing service points, animating
 * passengers, showing simulation logs, and providing user controls
 * such as scenario selection, step mode, replay, and speed sliders.
 *
 * The view uses a shared simulation engine instance and updates the
 * interface through JavaFX components.
 */
public class AirportView extends Application {


    // Controls that need to be accessed across methods
    private TextField boostField;
    private ToggleButton stepToggle;
    private Button nextBtn;
    private ComboBox<String> quickBoostPicker;
    private Button startBtn;
    private Spinner<Double> durationSpinner;
    private Button replayBtn;
    private Slider timeScaleSlider;
    private Slider walkSlider;
    private Slider arrivalSlider;
    private ComboBox<Scenario> scenarioChooser;



    /**
     * Shared simulation engine instance used by the view.
     */

    /**
     * Current AirportView instance.
     */
    private static AirportView instance;
    /**
     * Controller managing the airport simulation.
     */
    private AirportController controller;

    /**
     * The simulation engine instance used for running the simulation.
     */
    private SimulationEngine engine;

    /**
     * Configuration settings for the simulation, containing parameters
     * such as service speeds, arrival rates, and scenario-specific adjustments.
     */

    Configuration config = new Configuration();

    /**
     * Pane where passenger animations are displayed.
     */
    public Pane animationPane;

    /**
     * Text area for showing simulation info and messages.
     */
    private TextArea infoArea = new TextArea();
    /**
     * @return The text area used for simulation event logging.
     */

    public TextArea getInfoArea() {
        return infoArea;
    }

    /**
     * Passengers waiting for Step Mode permission.
     */
    private static Map<Integer, Runnable> waitingPassengers = new HashMap<>();

    /** Table for individual passenger results. */
    private TableView<String[]> passengerTableView = new TableView<>();

    /** Table for service point statistics and utilization. */
    private TableView<String[]> servicePointTableView = new TableView<>();

    /** Table for overall system performance metrics. */
    private TableView<String[]> systemTableView = new TableView<>();
    /**
     * Starting X coordinate for passenger animation nodes.
     */
    private static final double START_X = 50;

    /**
     * Starting Y coordinate for passenger animation nodes.
     */
    private static final double START_Y = 280;

    /** Current multiplier for service speeds (default 1.0). */
    private double serviceBoost = 1.0;

    /** Fastest allowed service speed multiplier. */
    private static final double MIN_BOOST = 0.5;

    /** Normal service speed multiplier. */
    private static final double MAX_BOOST = 1.0;


    /**
     * Sets the simulation engine for the view.
     *
     * @param engine the SimulationEngine instance to set
     */
    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
    }
    /**
     * @return The currently active simulation engine instance.
     */
    public SimulationEngine getEngine() {
        return engine;
    }

    /**
     * @return The table view containing individual passenger results.
     */
    public TableView<String[]> getPassengerTableView() {
        return passengerTableView;
    }

    /**
     * @return The table view containing service point statistics and utilization.
     */
    public TableView<String[]> getServicePointTableView() {
        return servicePointTableView;
    }

    /**
     * @return The table view containing overall system performance metrics.
     */
    public TableView<String[]> getSystemTableView() {
        return systemTableView;
    }


    /**
     * Returns the singleton instance of AirportView.
     *
     * @return the AirportView instance
     */
    public static AirportView getInstance() {
        return instance;
    }

    /**
     * Map of passenger IDs to their corresponding UI animation nodes.
     */

    public Map<Integer, Circle> passengerNodes = new HashMap<>();

    /**
     * Initializes the JavaFX stage, sets up the scene and animation pane,
     * and prepares service points and bottom panel.
     *
     * @param stage the primary stage provided by JavaFX
     */
    @Override
    public void start(Stage stage) {
        instance = this;

        this.controller = new AirportController(this, config);
        this.engine = null;

        BorderPane root = new BorderPane();
        animationPane = new Pane();
        animationPane.setPrefSize(1000, 500);
        animationPane.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ccc;");

        root.setCenter(animationPane);
        createServicePoints();
        createBottomPanel(root);

        setControlsForSimulation(false);

        Scene scene = new Scene(root, 1100, 850);
        stage.setTitle("Airport Simulation - Visualizer (MVC)");
        stage.setScene(scene);
        stage.show();

        infoArea.setEditable(false);
        infoArea.appendText(">>> System ready. Set duration and press Start.\n");
    }

    /**
     * Creates and adds the service point nodes to the animation pane.
     */
    private void createServicePoints() {
        animationPane.getChildren().addAll(
                createServiceNode(150, 180, "Normal Check-in", Color.LIGHTBLUE),
                createServiceNode(150, 320, "Self Check-in", Color.DEEPSKYBLUE),
                createServiceNode(400, 180, "Regular Security", Color.LIGHTGREEN),
                createServiceNode(400, 320, "Fast Track", Color.GREEN),
                createServiceNode(600, 250, "Customs", Color.PINK),
                createServiceNode(800, 250, "Boarding", Color.ORANGE)
        );
    }

    /**
     * Creates a StackPane representing a service point.
     *
     * @param x     x-coordinate of the service point
     * @param y     y-coordinate of the service point
     * @param label text label for the service point
     * @param color background color for the service point
     * @return a StackPane representing the service point
     */
    private StackPane createServiceNode(double x, double y, String label, Color color) {
        Rectangle r = new Rectangle(120, 60, color);
        r.setArcWidth(10);
        r.setArcHeight(10);
        r.setStroke(Color.BLACK);
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        StackPane sp = new StackPane(r, l);
        sp.setLayoutX(x);
        sp.setLayoutY(y);
        return sp;
    }
    /**
     * Executes a GUI action only if the triggering engine is still active.
     * * @param callbackEngine The engine that requested the update.
     * @param guiAction The task to run on the JavaFX thread.
     */

    private void runIfCurrentEngine(SimulationEngine callbackEngine, Runnable guiAction) {
        Platform.runLater(() -> {
            if (callbackEngine == engine) {
                guiAction.run();
            }
        });
    }

    /**
     * Animates a single passenger on the UI.
     *
     * @param p the passenger to animate
     */


    public static void animateSinglePassenger(Passenger p) {
        if (instance != null) {
            SimulationEngine currentEngine = instance.getEngine();
            instance.runIfCurrentEngine(currentEngine, () -> instance.createPassengerAnimation(p));
        }
    }


    /**
     * Creates the animation for a passenger.
     *
     * @param p the passenger to animate
     */
    private void createPassengerAnimation(Passenger p) {
        Color passengerColor = (p.getTicketType() == TicketType.ECONOMY) ? Color.RED : Color.BLUE;
        Circle node = new Circle(7, passengerColor);
        node.setStroke(Color.BLACK);
        node.setLayoutX(START_X);
        node.setLayoutY(START_Y);

        animationPane.getChildren().add(node);
        passengerNodes.put(p.getId(), node);
        // animateStep(p, node, 0);
    }
    /**
     * Animates a passenger's movement to the next service point based on the event.
     * * @param p The passenger to move.
     * @param nextEvent The event triggering the movement.
     */


    public void animatePassengerToNextService(Passenger p, EventType nextEvent) {

        if (engine == null) return;

        SimulationEngine callbackEngine = engine; // capture engine

        runIfCurrentEngine(callbackEngine, () -> {
            Circle node = passengerNodes.get(p.getId());
            if (node == null) return;

            double targetX = START_X;
            double targetY = START_Y;

            switch (nextEvent) {

                case ARRIVAL_NORMAL_CHECKIN:
                    targetX = 150 + 60;
                    targetY = 180 + 30;
                    break;

                case ARRIVAL_SELF_CHECKIN:
                    targetX = 150 + 60;
                    targetY = 320 + 30;
                    break;

                case ARRIVAL_REGULAR_SECURITY:
                    targetX = 400 + 60;
                    targetY = 180 + 30;
                    break;

                case ARRIVAL_FASTTRACK_SECURITY:
                    targetX = 400 + 60;
                    targetY = 320 + 30;
                    break;

                case ARRIVAL_CUSTOMS:
                    targetX = 600 + 60;
                    targetY = 250 + 30;
                    break;

                case ARRIVAL_BOARDING:
                    targetX = 800 + 60;
                    targetY = 250 + 30;
                    break;
            }

            double duration = Math.max(engine.getTimeScale() / 1000.0, 0.25);

            TranslateTransition move = new TranslateTransition(Duration.seconds(duration), node);

            double currentX = node.getTranslateX();
            double currentY = node.getTranslateY();

            move.setToX(currentX + (targetX - (START_X + currentX)));
            move.setToY(currentY + (targetY - (START_Y + currentY)));

            move.play();


        });
    }










    /**
     * Creates the bottom control panel containing sliders, buttons, and scenario selection.
     *
     * @param root the main BorderPane of the UI
     */
    /**
     * Creates the bottom control panel containing sliders, buttons, scenario selection,
     * individual service point tuning, and result tables.
     *
     * @param root the main BorderPane of the UI
     */
    private void createBottomPanel(BorderPane root) {
        VBox bottom = new VBox(10);
        bottom.setPadding(new Insets(15));
        bottom.setStyle("-fx-background-color: #eee; -fx-border-color: #bbb; -fx-border-width: 1 0 0 0;");

        // --- GRID: GENERAL SETTINGS (Sliderit ja Spinner) ---
        GridPane sliderGrid = new GridPane();
        sliderGrid.setHgap(30);
        sliderGrid.setVgap(10);

        // Animation Delay
        double defaultTimeScale = (engine != null) ? engine.getTimeScale() : 500;
        timeScaleSlider = new Slider(100, 2000, defaultTimeScale);

        timeScaleSlider.setShowTickLabels(true);
        timeScaleSlider.setShowTickMarks(true);
        timeScaleSlider.setMajorTickUnit(500);
        timeScaleSlider.setMinorTickCount(4);
        timeScaleSlider.setBlockIncrement(100);

        timeScaleSlider.valueProperty().addListener((obs, old, val) -> {
            SimulationEngine callbackEngine = engine;
            runIfCurrentEngine(callbackEngine, () -> engine.setTimeScale(val.doubleValue()));
        });
        sliderGrid.add(new Label("Animation delay (ms):"), 0, 0);
        sliderGrid.add(timeScaleSlider, 1, 0);



        // Walking Speed
        double defaultWalkSpeed = (engine != null) ? engine.getTraversalSpeedFactor() : 1.0;
        walkSlider = new Slider(0.5, 2.0, defaultWalkSpeed);

        walkSlider.setShowTickLabels(true);
        walkSlider.setShowTickMarks(true);
        walkSlider.setMajorTickUnit(0.5);
        walkSlider.setMinorTickCount(4);
        walkSlider.setBlockIncrement(0.1);
        walkSlider.setSnapToTicks(true);


        walkSlider.valueProperty().addListener((obs, old, val) -> {
            SimulationEngine callbackEngine = engine;
            runIfCurrentEngine(callbackEngine, () -> {
                try {
                    engine.setTraversalSpeedFactor(val.doubleValue());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        });
        sliderGrid.add(new Label("Walking speed (factor):"), 0, 1);
        sliderGrid.add(walkSlider, 1, 1);


        // Passenger Flow Slider
        double defaultArrival = (engine != null) ? engine.getArrivalSpeedFactor() : 1.0;
        arrivalSlider = new Slider(0.5, 1.2, defaultArrival);

        arrivalSlider.setShowTickLabels(true);
        arrivalSlider.setShowTickMarks(true);
        arrivalSlider.setMajorTickUnit(0.2);
        arrivalSlider.setMinorTickCount(1);
        arrivalSlider.setBlockIncrement(0.1);
        arrivalSlider.setSnapToTicks(true);

        arrivalSlider.valueProperty().addListener((obs, old, val) -> {
            SimulationEngine callbackEngine = engine; // capture the current engine

            runIfCurrentEngine(callbackEngine, () -> {
                try {
                    engine.setArrivalSpeedFactor(val.doubleValue());
                } catch (Exception ex) {
                    arrivalSlider.setValue(old.doubleValue());
                    infoArea.appendText("!!! BLOCKED: Load too high.\n");
                }
            });
        });
        sliderGrid.add(new Label("Passenger flow (factor):"), 2, 0);
        sliderGrid.add(arrivalSlider, 3, 0);

        // Duration Spinner
        Label durationLabel = new Label("Simulation duration:");
        double defaultDuration = (engine != null) ? engine.getSimulationEndTime() : 100.0;
        durationSpinner = new Spinner<>(100.0, 10000.0, defaultDuration, 100.0);
        durationSpinner.setEditable(true);
        durationSpinner.setPrefWidth(100);
        durationSpinner.valueProperty().addListener((obs, old, val) -> {
            SimulationEngine callbackEngine = engine; // capture current engine
            runIfCurrentEngine(callbackEngine, () -> {
                engine.setSimulationEndTime(val);
            });
        });
        sliderGrid.add(durationLabel, 2, 1);
        sliderGrid.add(durationSpinner, 3, 1);


        // --- HBOX: CONTROLS (Painikkeet ja Uusi Valikko) ---
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        scenarioChooser = new ComboBox<>();
        scenarioChooser.getItems().addAll(Scenario.values());
        scenarioChooser.setValue(Scenario.NORMAL);
        scenarioChooser.setOnAction(e -> {
            if (engine != null) {
                engine.applyScenario(scenarioChooser.getValue());
                infoArea.appendText(">>> SCENARIO CHANGED: " + scenarioChooser.getValue() + "\n");
            }
        });

        startBtn = new Button("Start Simulation ▶");
        startBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        stepToggle = new ToggleButton("Step Mode");
        nextBtn = new Button("Next Event >>");
        nextBtn.setDisable(true);

        Label boostLabel = new Label("Staff Boost +0-20%:");
        boostField = new TextField(String.format("%.2f", serviceBoost));
        boostField.setPrefWidth(60);


        replayBtn = new Button("Reset ↺");
        replayBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");

        // --- UUSI: PIKATEHOSTUS-VALIKKO (Kuten skenaariovalinta) ---
        quickBoostPicker = new ComboBox<>();
        quickBoostPicker.getItems().addAll(
                "Normal Check-in", "Self Check-in",
                "Regular Security", "Fast Track",
                "Customs", "Boarding"
        );
        quickBoostPicker.setPromptText("Boost Service Point...");
        quickBoostPicker.setPrefWidth(180);

        quickBoostPicker.setOnAction(e -> {
            String selected = quickBoostPicker.getValue();
            if (selected != null && config != null) {
                config.setIndividualServiceSpeedFactors(selected, 1.2); // +20% tehostus
                infoArea.appendText(">>> QUICK BOOST: " + selected + " set to (+20%)\n");
                // Tyhjennetään valinta, jotta se on heti valmis uuteen valintaan
                Platform.runLater(() -> quickBoostPicker.setValue(null));
            }
        });


        // --- ACTIONS ---
        startBtn.setOnAction(e -> {
            engine = new SimulationEngine(durationSpinner.getValue(), config);

            // Apply GUI parameters to the engine
            try {
                engine.setTimeScale(timeScaleSlider.getValue());
                engine.setTraversalSpeedFactor(walkSlider.getValue());
                engine.setArrivalSpeedFactor(arrivalSlider.getValue());
                engine.setServiceSpeedFactor(serviceBoost);
                engine.setSimulationEndTime(durationSpinner.getValue());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }


            engine.setDebugMode(true);
            engine.setSpeedMultiplier(5.0);
            engine.scheduleEvent(new Event(0.0, EventType.ARRIVAL_SYSTEM, new Passenger(engine)));
            setEngine(engine);
            controller.setEngine(engine);
            infoArea.appendText(">>> Simulation starting! (Duration: " + durationSpinner.getValue() + "s)\n");
            startBtn.setDisable(true);
            durationSpinner.setDisable(true);
            setControlsForSimulation(true);
            controller.startSimulation(durationSpinner.getValue());
        });

        replayBtn.setOnAction(e -> {
            if (engine != null) {engine.stopSimulation(); engine = null;}
            animationPane.getChildren().removeIf(n -> n instanceof Circle);
            infoArea.clear();
            passengerTableView.getItems().clear();
            servicePointTableView.getItems().clear();
            systemTableView.getItems().clear();
            waitingPassengers.clear();

            setControlsForSimulation(false);

            // Reset boost
            serviceBoost = 1.0;
            boostField.setText("0"); // sync field
            boostLabel.setText("Staff Boost: +0%");
            timeScaleSlider.setValue(500);
            infoArea.appendText(">>> Animation delay reset to default (500 ms)\n");


            // Reset simulation globals
            Clock.getInstance().reset();  // <--- RESET CLOCK HERE

            engine = new SimulationEngine(durationSpinner.getValue(), config);
            engine.setDebugMode(false);
            engine.setSpeedMultiplier(5.0);
            engine.scheduleEvent(new Event(0.0, EventType.ARRIVAL_SYSTEM, new Passenger(engine)));

            setEngine(engine);
            controller.setEngine(engine);

            Clock.getInstance().reset();
            Passenger.resetIdCounter();
            startBtn.setDisable(false);
            durationSpinner.setDisable(false);
            infoArea.appendText(">>> Reset complete. Ready for new run.\n");
        });

        // Initialize boostField at start
        boostField.setText("0"); // 0% boost = normal speed
        serviceBoost = 1.0;

        boostField.setOnAction(e -> {
            if (engine == null) return;

            try {
                double input = Double.parseDouble(boostField.getText());
                if (input < 0) input = 0;
                if (input > 50) input = 50;// max 50% faster

                serviceBoost = 1.0 + (input / 100.0);
                engine.setServiceSpeedFactor(serviceBoost);

                boostField.setText(String.valueOf((int)input));
                infoArea.appendText(String.format(">>> Staff Boost set to +%d%%\n", (int)input));
            } catch (NumberFormatException ex) {
                // Reset to last valid value
                int currentPercent = (int) ((serviceBoost - 1.0) * 100);
                boostField.setText(String.valueOf(currentPercent));
                infoArea.appendText("!!! Invalid input. Enter a valid number. \n");
            } catch (Exception ex) {
                infoArea.appendText("!!! Failed to apply service boost.\n");
            }
        });


        stepToggle.setOnAction(e -> {
            boolean active = stepToggle.isSelected();
            if (engine != null) engine.setStepMode(active);
            nextBtn.setDisable(!active);
            if (!active) releaseWaitingPassengers();
        });

        nextBtn.setOnAction(e -> {
            if (engine != null) engine.requestNextStep();
            releaseWaitingPassengers();
        });


        // Kokoa kontrollit riviin
        controls.getChildren().addAll(
                new Label("Scenario:"), scenarioChooser,
                startBtn,
                stepToggle,
                nextBtn,
                boostLabel,
                boostField,
                replayBtn,
                new Separator(Orientation.VERTICAL),
                new Label("Quick Boost:"), quickBoostPicker
        );


        // --- TABPANE: TULOKSET JA LOGI ---
        TabPane tabPane = new TabPane();
        tabPane.setPrefHeight(250);

        // Alustetaan sarakkeet (HUOM: System sarakkeet korjattu vastaamaan CSV-dataa)
        setupTableColumns(passengerTableView, new String[]{
                "ID", "Flight", "Ticket", "Luggage", "Self", "Weight", "Arrival", "Departure", "Total",
                "Q_Normal", "S_Normal", "C_Normal", "T_Normal",
                "Q_Self", "S_Self", "C_Self", "T_Self",
                "Q_RegSec", "S_RegSec", "C_RegSec", "T_RegSec",
                "Q_Fast", "S_Fast", "C_Fast", "T_Fast",
                "Q_Cust", "S_Cust", "C_Cust", "T_Cust",
                "Q_Board", "S_Board", "C_Board", "T_Board"
        });
        setupTableColumns(servicePointTableView, new String[]{
                "ServicePoint", "Arrivals", "Completions", "AvgWait", "AvgService",
                "AvgResponse", "Utilization", "AvgQLen", "LqPredicted",
                "LqError%", "AvgInSystem", "MaxQLen"
        });
        setupTableColumns(systemTableView, new String[]{"Label", "Value"}); // Korjattu 2 sarakkeeseen

        Tab logTab = new Tab("Event Log", infoArea);
        Tab passTab = new Tab("Passengers", passengerTableView);
        Tab spTab = new Tab("Service Points", servicePointTableView);
        Tab sysTab = new Tab("System Stats", systemTableView);

        logTab.setClosable(false);
        passTab.setClosable(false);
        spTab.setClosable(false);
        sysTab.setClosable(false);

        tabPane.getTabs().addAll(logTab, passTab, spTab, sysTab);

        // --- KOKOAMINEN ---

        bottom.getChildren().addAll(sliderGrid, controls, tabPane);

        ScrollPane scroll = new ScrollPane(bottom);
        scroll.setFitToWidth(true);          // make it expand horizontally
        scroll.setPrefHeight(300);          // max visible height, adjust as needed
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        root.setBottom(scroll);



    }

    /**
     * Updates the interactivity of UI controls based on the simulation state.
     * @param running True to disable parameters during execution, false to enable.
     */


    private void setControlsForSimulation(boolean running) {

        // Start button enabled only when simulation is NOT running
        // startBtn.setDisable(running);

        // Step mode & next event
        //stepToggle.setDisable(!running);
        //nextBtn.setDisable(!running);

        // Replay/reset button enabled only when simulation is running (optional)
        // replayBtn.setDisable(!running);

        // Staff boost controls
        //boostField.setDisable(!running);
        //quickBoostPicker.setDisable(!running);

        // Step controls
        stepToggle.setDisable(!running);
        nextBtn.setDisable(!running);

        // Staff controls
        boostField.setEditable(running);

        // Sliders
        timeScaleSlider.setMouseTransparent(!running);
        walkSlider.setMouseTransparent(!running);
        arrivalSlider.setMouseTransparent(!running);

        // Spinner
        durationSpinner.setEditable(running);

        scenarioChooser.setMouseTransparent(!running);
        quickBoostPicker.setMouseTransparent(!running);

    }

    /**
     * Releases all passengers waiting in Step Mode.
     */

    private void releaseWaitingPassengers() {
        SimulationEngine callbackEngine = engine;
        runIfCurrentEngine(callbackEngine, () -> {
            new ArrayList<>(waitingPassengers.values()).forEach(Runnable::run);
            waitingPassengers.clear();
        });
    }




    /**
     * Displays the final simulation results in the info area.
     *
     * @param avgTime the average system time
     * @param totalCompleted total number of passengers completed
     * @param validationError average validation error in percent
     */
    public void showFinalResults(double avgTime, int totalCompleted, double validationError) {

        Platform.runLater(() -> {
            if (engine == null) {
                return;
            }

            // 1. Päivitetään taulukko CSV-tiedostosta heti tulosten valmistuttua
            // Varmista, että tiedostonimi täsmää SimulationEnginen export-nimeen
            updateTableFromCSV("passengers.csv", passengerTableView);
            updateTableFromCSV("servicepoints.csv",servicePointTableView);

            // 2. Tulostetaan tekstimuotoiset tulokset infoArea-kenttään
            infoArea.appendText("\n" + "=".repeat(45) + "\n");
            infoArea.appendText("       SIMULATION RESULTS\n");
            infoArea.appendText(String.format(" Completed passengers: %d\n", totalCompleted));
            infoArea.appendText(String.format(" Avg. time in system:   %.2f\n", avgTime));
            infoArea.appendText(String.format(" Validation error (avg): %.2f%%\n", validationError));

            if (validationError < 1.0) infoArea.appendText(" >>> Status: Very accurate (Valid).\n");
            else if (validationError < 5.0) infoArea.appendText(" >>> Status: Acceptable deviation.\n");
            else infoArea.appendText(" >>> Status: Unstable (Check load).\n");

            infoArea.appendText("=".repeat(45) + "\n");
            infoArea.setScrollTop(Double.MAX_VALUE);

            setControlsForSimulation(false);
        });
    }
    /**
     * Reads a CSV file and populates the table, skipping the header row.
     * @param filename Path to the CSV file.
     * @param table The TableView to update.
     */
    public void updateTableFromCSV(String filename, TableView<String[]> table) {
        Platform.runLater(() -> {
            try {
                java.io.File file = new java.io.File(filename);
                if (!file.exists()) return;
                java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
                table.getItems().clear();
                for (int i = 1; i < lines.size(); i++) {
                    table.getItems().add(lines.get(i).split(","));
                }
            } catch (Exception e) { System.err.println("CSV Error: " + e.getMessage()); }
        });
    }

    /**
     * Configures table columns and maps data indices to specific cells.
     * @param table The TableView to configure.
     * @param columnNames Array of headers for the columns.
     */

    private void setupTableColumns(TableView<String[]> table, String[] columnNames) {
        table.getColumns().clear();
        for (int i = 0; i < columnNames.length; i++) {
            final int colIndex = i;
            TableColumn<String[], String> col = new TableColumn<>(columnNames[i]);

            col.setCellValueFactory(cd -> {
                String[] row = cd.getValue();
                // TARKISTUS: Jos rivi on olemassa ja siinä on tarpeeksi sarakkeita
                if (row != null && colIndex < row.length) {
                    return new javafx.beans.property.SimpleStringProperty(row[colIndex]);
                } else {
                    // Jos dataa puuttuu, palautetaan tyhjä merkkijono kaatumisen sijaan
                    return new javafx.beans.property.SimpleStringProperty("-");
                }
            });

            table.getColumns().add(col);
        }

    }
}
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

    Configuration config = new Configuration();

    /**
     * Pane where passenger animations are displayed.
     */
    private Pane animationPane;

    /**
     * Text area for showing simulation info and messages.
     */
    private TextArea infoArea = new TextArea();

    /**
     * Passengers waiting for Step Mode permission.
     */
    private static Map<Integer, Runnable> waitingPassengers = new HashMap<>();

    private TableView<String[]> passengerTableView = new TableView<>();
    private TableView<String[]> servicePointTableView = new TableView<>();
    private TableView<String[]> systemTableView = new TableView<>();

    /**
     * Starting X coordinate for passenger animation nodes.
     */
    private static final double START_X = 50;

    /**
     * Starting Y coordinate for passenger animation nodes.
     */
    private static final double START_Y = 280;

    private double serviceBoost = 1.0;

    private static final double BOOST_STEP = 0.1;
    private static final double MIN_BOOST = 0.5; // fastest allowed
    private static final double MAX_BOOST = 1.0; // normal speed


    /**
     * Sets the simulation engine for the view.
     * @param engine the SimulationEngine instance to set
     */
    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
    }

    public SimulationEngine getEngine() {
        return engine;
    }
    /**
     * Palauttaa matkustajakohtaiset tulokset sisältävän taulukon.
     * @return TableView matkustajadatalla
     */
    public TableView<String[]> getPassengerTableView() {
        return passengerTableView;
    }

    /**
     * Palauttaa palvelupistekohtaiset tilastot (jonot, käyttöasteet jne.).
     * @return TableView palvelupistedatalla
     */
    public TableView<String[]> getServicePointTableView() {
        return servicePointTableView;
    }

    /**
     * Palauttaa koko järjestelmän kattavat tunnusluvut.
     * @return TableView järjestelmädatalla
     */
    public TableView<String[]> getSystemTableView() {
        return systemTableView;
    }



    /**
     * Returns the singleton instance of AirportView.
     * @return the AirportView instance
     */
    public static AirportView getInstance() {
        return instance;
    }

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

        Scene scene = new Scene(root, 1100, 850);
        stage.setTitle("Airport Simulation - Visualizer (MVC)");
        stage.setScene(scene);
        stage.show();

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
     * @param x x-coordinate of the service point
     * @param y y-coordinate of the service point
     * @param label text label for the service point
     * @param color background color for the service point
     * @return a StackPane representing the service point
     */
    private StackPane createServiceNode(double x, double y, String label, Color color) {
        Rectangle r = new Rectangle(120, 60, color);
        r.setArcWidth(10); r.setArcHeight(10); r.setStroke(Color.BLACK);
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        StackPane sp = new StackPane(r, l);
        sp.setLayoutX(x); sp.setLayoutY(y);
        return sp;
    }

    /**
     * Animates a single passenger on the UI.
     *
     * @param p the passenger to animate
     */
    public static void animateSinglePassenger(Passenger p) {
        if (instance != null) {
            Platform.runLater(() -> instance.createPassengerAnimation(p));
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
        animateStep(p, node, 0);
    }

    /**
     * Performs a single step of passenger animation through the service points.
     *
     * @param p the passenger
     * @param node the Circle representing the passenger
     * @param stage the current stage in the simulation
     */
    private void animateStep(Passenger p, Circle node, int stage) {
        double targetX, targetY;
        int nextStage = stage + 1;

        switch (stage) {
            case 0: // Check-in
                if (p.getCheckinLuggageType() == LuggageType.OVERSIZED || !p.isEligibleForSelfCheckin()) {
                    targetX = 150 + 60; targetY = 180 + 30;
                } else {
                    targetX = 150 + 60; targetY = 320 + 30;
                }
                break;
            case 1: // Security
                if (p.usesFastTrackSecurity()) {
                    targetX = 400 + 60; targetY = 320 + 30;
                } else {
                    targetX = 400 + 60; targetY = 180 + 30;
                }
                break;
            case 2: // Customs
                if (p.isInternationalFlight()) {
                    targetX = 600 + 60; targetY = 250 + 30;
                } else {
                    animateStep(p, node, 3);
                    return;
                }
                break;
            case 3: // Boarding
                targetX = 800 + 60; targetY = 250 + 30;
                break;
            default: // Finished
                animationPane.getChildren().remove(node);
                showPassengerInfo(p);
                return;
        }

        double durationSeconds = engine.getTimeScale() / 1000.0;
        TranslateTransition move = new TranslateTransition(Duration.seconds(durationSeconds), node);
        move.setToX(targetX - START_X);
        move.setToY(targetY - START_Y);

        move.setOnFinished(e -> {
            if (engine.isStepMode()) {
                waitingPassengers.put(p.getId(), () -> animateStep(p, node, nextStage));
            } else {
                animateStep(p, node, nextStage);
            }
        });
        move.play();
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
        sliderGrid.setHgap(30); sliderGrid.setVgap(10);

        // Animation Delay
        double defaultTimeScale = (engine != null) ? engine.getTimeScale() : 500;
        Slider timeScaleSlider = new Slider(100, 2000, defaultTimeScale);
        timeScaleSlider.valueProperty().addListener((obs, old, val) -> {
            if (engine != null) engine.setTimeScale(val.doubleValue());
        });
        sliderGrid.add(new Label("Animation delay (ms):"), 0, 0);
        sliderGrid.add(timeScaleSlider, 1, 0);

        // Walking Speed
        double defaultWalkSpeed = (engine != null) ? engine.getTraversalSpeedFactor() : 1.0;
        Slider walkSlider = new Slider(0.5, 2.0, defaultWalkSpeed);
        walkSlider.valueProperty().addListener((obs, old, val) -> {
            if (engine != null) {
                try { engine.setTraversalSpeedFactor(val.doubleValue()); } catch (Exception ignored) {}
            }
        });
        sliderGrid.add(new Label("Walking speed (factor):"), 0, 1);
        sliderGrid.add(walkSlider, 1, 1);

        // Passenger Flow
        double defaultArrival = (engine != null) ? engine.getArrivalSpeedFactor() : 1.0;
        Slider arrivalSlider = new Slider(0.5, 2.0, defaultArrival);
        arrivalSlider.valueProperty().addListener((obs, old, val) -> {
            if (engine != null) {
                try { engine.setArrivalSpeedFactor(val.doubleValue()); } catch (Exception ex) {
                    Platform.runLater(() -> {
                        arrivalSlider.setValue(old.doubleValue());
                        infoArea.appendText("!!! BLOCKED: Load too high.\n");
                    });
                }
            }
        });
        sliderGrid.add(new Label("Passenger flow (factor):"), 2, 0);
        sliderGrid.add(arrivalSlider, 3, 0);

        // Duration Spinner
        Label durationLabel = new Label("Simulation duration:");
        double defaultDuration = (engine != null) ? engine.getSimulationEndTime() : 100.0;
        Spinner<Double> durationSpinner = new Spinner<>(100.0, 10000.0, defaultDuration, 100.0);
        durationSpinner.setEditable(true);
        durationSpinner.setPrefWidth(100);
        durationSpinner.valueProperty().addListener((obs, old, val) -> {
            if (engine != null) engine.setSimulationEndTime(val);
        });
        sliderGrid.add(durationLabel, 2, 1);
        sliderGrid.add(durationSpinner, 3, 1);

        // --- HBOX: CONTROLS (Painikkeet ja Uusi Valikko) ---
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Scenario> scenarioChooser = new ComboBox<>();
        scenarioChooser.getItems().addAll(Scenario.values());
        scenarioChooser.setValue(Scenario.NORMAL);
        scenarioChooser.setOnAction(e -> {
            if (engine != null) {
                engine.applyScenario(scenarioChooser.getValue());
                infoArea.appendText(">>> SCENARIO CHANGED: " + scenarioChooser.getValue() + "\n");
            }
        });

        Button startBtn = new Button("Start Simulation ▶");
        startBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        ToggleButton stepToggle = new ToggleButton("Step Mode");
        Button nextBtn = new Button("Next Event >>");
        nextBtn.setDisable(true);

        Button boostMinusBtn = new Button("−");
        Button boostPlusBtn = new Button("+");

        Label boostLabel = new Label("Staff Boost: +0%");

        boostMinusBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
        boostPlusBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");

        ToggleButton boostToggle = new ToggleButton("Staff Boost (Turbo)");
        boostToggle.setStyle("-fx-font-weight: bold; -fx-base: #2ecc71;");

        Button replayBtn = new Button("Reset ↺");
        replayBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");

        // --- UUSI: PIKATEHOSTUS-VALIKKO (Kuten skenaariovalinta) ---
        ComboBox<String> quickBoostPicker = new ComboBox<>();
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
                infoArea.appendText(">>> QUICK BOOST: " + selected + " activated (+20%)\n");
                // Tyhjennetään valinta, jotta se on heti valmis uuteen valintaan
                Platform.runLater(() -> quickBoostPicker.setValue(null));
            }
        });

        // --- ACTIONS ---
        startBtn.setOnAction(e -> {
            engine = new SimulationEngine(durationSpinner.getValue(), config);
            engine.setDebugMode(false);
            engine.setSpeedMultiplier(5.0);
            engine.scheduleEvent(new Event(0.0, EventType.ARRIVAL_SYSTEM, new Passenger(engine)));
            setEngine(engine);
            controller.setEngine(engine);
            infoArea.appendText(">>> Simulation starting! (Duration: " + durationSpinner.getValue() + "s)\n");
            startBtn.setDisable(true);
            durationSpinner.setDisable(true);
            controller.startSimulation(durationSpinner.getValue());
        });


        boostPlusBtn.setOnAction(e -> {

            if (engine == null) return;

            if (serviceBoost - BOOST_STEP >= MIN_BOOST) {
                serviceBoost -= BOOST_STEP;
            }

            try {
                engine.setServiceSpeedFactor(serviceBoost);
            } catch (Exception ex) {
                infoArea.appendText("!!! Failed to apply service boost.\n");
                return;
            }

            int boostPercent = (int)((1 - serviceBoost) * 100);

            boostLabel.setText(String.format("Staff Boost: %d%%", boostPercent));

            infoArea.appendText(
                    String.format(">>> BOOST INCREASED → +%d%% service speed\n", boostPercent)
            );
        });


        boostMinusBtn.setOnAction(e -> {

            if (engine == null) return;

            if (serviceBoost + BOOST_STEP <= MAX_BOOST) {
                serviceBoost += BOOST_STEP;
            }

            try {
                engine.setServiceSpeedFactor(serviceBoost);
            } catch (Exception ex) {
                infoArea.appendText("!!! Failed to apply service boost.\n");
                return;
            }

            int boostPercent = (int)((1 - serviceBoost) * 100);

            boostLabel.setText(String.format("Staff Boost: %d%%", boostPercent));

            infoArea.appendText(
                    String.format(">>> BOOST DECREASED → +%d%% service speed\n", boostPercent)
            );
        });



        boostToggle.setOnAction(e -> {
            double factor = boostToggle.isSelected() ? 0.6 : 1.0;
            if (engine != null) {
                try { engine.setServiceSpeedFactor(factor); } catch (Exception ex) { throw new RuntimeException(ex); }
            }
            boostToggle.setStyle(boostToggle.isSelected() ? "-fx-font-weight: bold; -fx-base: #e74c3c; -fx-text-fill: white;" : "-fx-font-weight: bold; -fx-base: #2ecc71;");
            infoArea.appendText(boostToggle.isSelected() ? ">>> STAFF BOOST activated.\n" : ">>> STAFF BOOST deactivated.\n");
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

        replayBtn.setOnAction(e -> {
            if (engine != null) engine.stopSimulation();
            animationPane.getChildren().removeIf(n -> n instanceof Circle);
            infoArea.clear();
            passengerTableView.getItems().clear();
            servicePointTableView.getItems().clear();
            systemTableView.getItems().clear();
            waitingPassengers.clear();

            serviceBoost = 1.0;
            boostLabel.setText("Staff Boost: +0%");


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

        // Kokoa kontrollit riviin
        controls.getChildren().addAll(
                new Label("Scenario:"), scenarioChooser,
                startBtn,
                stepToggle,
                nextBtn,
                boostMinusBtn,
                boostLabel,
                boostPlusBtn,
                replayBtn
                startBtn, stepToggle, nextBtn, replayBtn,
                new Separator(Orientation.VERTICAL),
                new Label("Quick Boost:"), quickBoostPicker
        );


        // --- TABPANE: TULOKSET JA LOGI ---
        TabPane tabPane = new TabPane();
        tabPane.setPrefHeight(250);

        // Alustetaan sarakkeet (HUOM: System sarakkeet korjattu vastaamaan CSV-dataa)
        setupTableColumns(passengerTableView, new String[]{"ID", "Ticket", "Arrival", "Removal", "Duration"});
        setupTableColumns(servicePointTableView, new String[]{"Station", "In", "Out", "Wait", "Svc", "Util", "Q-Len"});
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
        // Huom: Poistin 'speedTuning' -osan, koska valikko on nyt 'controls' -rivissä
        bottom.getChildren().addAll(sliderGrid, controls, tabPane);
        root.setBottom(bottom);
    }

    /**
     * Releases all passengers waiting in Step Mode.
     */
    private void releaseWaitingPassengers() {
        new ArrayList<>(waitingPassengers.values()).forEach(Runnable::run);
        waitingPassengers.clear();
    }

    /**
     * Displays information about a passenger in the info area.
     *
     * @param p the passenger whose info to display
     */
    public void showPassengerInfo(Passenger p) {
        Platform.runLater(() -> {
            infoArea.appendText(String.format("[%03d] %s | Finished at: %.1f\n",
                    p.getId(), p.getTicketType(), Clock.getInstance().getTime()));
            infoArea.setScrollTop(Double.MAX_VALUE);
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
        });
    }
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
    /**
     * Luo paneelin, jossa on painikkeet palvelupisteiden nopeuden säätämiseen.
     * @return VBox-komponentti, joka sisältää painikkeet.
     */
    /**
     * Luo paneelin, jossa on painikkeet palvelupisteiden nopeuden kasvattamiseen 20 % kerrallaan.
     */
    /**
     * Luo paneelin, jossa palvelupisteiden nimet toimivat Boost-painikkeina.
     */
    private VBox createIndividualSpeedButtons() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: #ddd; -fx-border-color: #bbb; -fx-border-radius: 8;");

        Label title = new Label("Pikatehostus (+20%):");
        title.setStyle("-fx-font-weight: bold;");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        // 1. Luodaan pudotusvalikko
        ComboBox<String> pointPicker = new ComboBox<>();
        pointPicker.getItems().addAll(
                "Normal Check-in", "Self Check-in",
                "Regular Security", "Fast Track",
                "Customs", "Boarding"
        );
        pointPicker.setPromptText("Valitse tehostettava piste...");
        pointPicker.setPrefWidth(200);

        // 2. Toiminto: Heti kun arvo muuttuu (valitaan listasta), tehostus aktivoituu
        pointPicker.setOnAction(e -> {
            String selectedPoint = pointPicker.getValue();

            if (selectedPoint != null && config != null) {
                // Asetetaan tehostus Configuration-olioon (1.2x nopeus)
                config.setIndividualServiceSpeedFactors(selectedPoint,1.2);

                // Lokiviesti ja palaute
                infoArea.appendText(">>> BOOST AKTIVOITU: " + selectedPoint + "\n");

                // Valinnaisesti: poistetaan valittu piste listalta,
                // jotta sitä ei voi "boostata" monta kertaa vahingossa
                // Platform.runLater(() -> pointPicker.getItems().remove(selectedPoint));

                // Tyhjennetään valinta, jotta valikon teksti palaa ennalleen
                Platform.runLater(() -> pointPicker.setValue(null));
            }
        });

        row.getChildren().addAll(new Label("Kohde:"), pointPicker);
        container.getChildren().addAll(title, row);

        return container;
    }
}
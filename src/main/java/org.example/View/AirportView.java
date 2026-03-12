package org.example.View;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
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
import org.example.Model.SimulationEngine;
import org.example.Model.Passenger;
import org.example.Model.Clock;
import org.example.Model.Scenario;
import org.example.Model.LuggageType;
import org.example.Model.TicketType;

public class AirportView extends Application {

    private static SimulationEngine sharedEngine;
    private static AirportView instance;
    private AirportController controller;

    private SimulationEngine engine;
    private Pane animationPane;
    private TextArea infoArea = new TextArea();

    // Matkustajat, jotka odottavat Step Mode -lupaa
    private static Map<Integer, Runnable> waitingPassengers = new HashMap<>();

    private static final double START_X = 50;
    private static final double START_Y = 280;

    public static void setEngine(SimulationEngine engine) {
        sharedEngine = engine;
    }

    public static AirportView getInstance() {
        return instance;
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        this.controller = new AirportController(this);

        if (sharedEngine == null) throw new IllegalStateException("Engine not set!");
        this.engine = sharedEngine;

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

        infoArea.appendText(">>> Järjestelmä valmis. Aseta kesto ja paina Start.\n");
    }

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

    private StackPane createServiceNode(double x, double y, String label, Color color) {
        Rectangle r = new Rectangle(120, 60, color);
        r.setArcWidth(10); r.setArcHeight(10); r.setStroke(Color.BLACK);
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        StackPane sp = new StackPane(r, l);
        sp.setLayoutX(x); sp.setLayoutY(y);
        return sp;
    }

    public static void animateSinglePassenger(Passenger p) {
        if (instance != null) {
            Platform.runLater(() -> instance.createPassengerAnimation(p));
        }
    }

    private void createPassengerAnimation(Passenger p) {
        Color passengerColor = (p.getTicketType() == TicketType.ECONOMY) ? Color.RED : Color.BLUE;
        Circle node = new Circle(7, passengerColor);
        node.setStroke(Color.BLACK);
        node.setLayoutX(START_X);
        node.setLayoutY(START_Y);

        animationPane.getChildren().add(node);
        animateStep(p, node, 0);
    }

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
            default: // Valmis
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

    private void createBottomPanel(BorderPane root) {
        VBox bottom = new VBox(10);
        bottom.setPadding(new Insets(15));
        bottom.setStyle("-fx-background-color: #eee; -fx-border-color: #bbb; -fx-border-width: 1 0 0 0;");

        // --- GRID: ASETUKSET ---
        GridPane sliderGrid = new GridPane();
        sliderGrid.setHgap(30); sliderGrid.setVgap(10);

        Slider timeScaleSlider = new Slider(100, 2000, engine.getTimeScale());
        timeScaleSlider.valueProperty().addListener((obs, old, val) -> engine.setTimeScale(val.doubleValue()));
        sliderGrid.add(new Label("Animaation viive (ms):"), 0, 0);
        sliderGrid.add(timeScaleSlider, 1, 0);

        Slider walkSlider = new Slider(0.5, 2.0, engine.getTraversalSpeedFactor());
        walkSlider.valueProperty().addListener((obs, old, val) -> {
            try { engine.setTraversalSpeedFactor(val.doubleValue()); } catch (Exception ignored) {}
        });
        sliderGrid.add(new Label("Kävelynopeus (kerroin):"), 0, 1);
        sliderGrid.add(walkSlider, 1, 1);

        Slider arrivalSlider = new Slider(0.5, 2.0, engine.getArrivalSpeedFactor());
        arrivalSlider.valueProperty().addListener((obs, old, val) -> {
            try { engine.setArrivalSpeedFactor(val.doubleValue()); } catch (Exception ex) {
                Platform.runLater(() -> {
                    arrivalSlider.setValue(old.doubleValue());
                    infoArea.appendText("!!! ESTETTY: Liian korkea kuorma.\n");
                });
            }
        });
        sliderGrid.add(new Label("Matkustajavirta (kerroin):"), 2, 0);
        sliderGrid.add(arrivalSlider, 3, 0);

        Label durationLabel = new Label("Simulaation kesto:");
        Spinner<Double> durationSpinner = new Spinner<>(100.0, 10000.0, engine.getSimulationEndTime(), 100.0);
        durationSpinner.setEditable(true);
        durationSpinner.setPrefWidth(100);
        durationSpinner.valueProperty().addListener((obs, old, val) -> engine.setSimulationEndTime(val));
        sliderGrid.add(durationLabel, 2, 1);
        sliderGrid.add(durationSpinner, 3, 1);

        // --- HBOX: KONTROLLIT ---
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Scenario> scenarioChooser = new ComboBox<>();
        scenarioChooser.getItems().addAll(Scenario.values());
        scenarioChooser.setValue(Scenario.NORMAL);
        // TÄRKEÄÄ: Skenaarion vaihto lennosta
        scenarioChooser.setOnAction(e -> {
            engine.applyScenario(scenarioChooser.getValue());
            infoArea.appendText(">>> SKENAARIO VAIHDETTU: " + scenarioChooser.getValue() + "\n");
        });

        Button startBtn = new Button("Start Simulation ▶");
        startBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        ToggleButton stepToggle = new ToggleButton("Step Mode");
        Button nextBtn = new Button("Next Event >>");
        nextBtn.setDisable(true);

        ToggleButton boostToggle = new ToggleButton("Staff Boost (Turbo)");
        boostToggle.setStyle("-fx-font-weight: bold; -fx-base: #2ecc71;");

        Button replayBtn = new Button("Replay ↺");
        replayBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");

        // --- TOIMINNALLISUUDET ---
        startBtn.setOnAction(e -> {
            infoArea.appendText(">>> Simulaatio alkaa! (Kesto: " + engine.getSimulationEndTime() + "s)\n");
            startBtn.setDisable(true);
            durationSpinner.setDisable(true);
            // Huom: scenarioChooser JÄÄ PÄÄLLE, jotta sitä voi vaihtaa lennosta
            controller.startSimulation(engine);
        });

        boostToggle.setOnAction(e -> {
            double factor = boostToggle.isSelected() ? 0.6 : 1.0;
            try {
                engine.setServiceSpeedFactor(factor);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            boostToggle.setStyle(boostToggle.isSelected() ? "-fx-font-weight: bold; -fx-base: #e74c3c; -fx-text-fill: white;" : "-fx-font-weight: bold; -fx-base: #2ecc71;");
            infoArea.appendText(boostToggle.isSelected() ? ">>> STAFF BOOST aktivoitu.\n" : ">>> STAFF BOOST pois.\n");
        });

        stepToggle.setOnAction(e -> {
            boolean active = stepToggle.isSelected();
            engine.setStepMode(active);
            nextBtn.setDisable(!active);
            if (!active) releaseWaitingPassengers();
        });

        nextBtn.setOnAction(e -> {
            engine.requestNextStep();
            releaseWaitingPassengers();
        });

        replayBtn.setOnAction(e -> {
            engine.resetSimulation();
            animationPane.getChildren().removeIf(n -> n instanceof Circle);
            infoArea.clear();
            waitingPassengers.clear();

            // Palautetaan alkutilaan
            startBtn.setDisable(false);
            durationSpinner.setDisable(false);
            scenarioChooser.setDisable(false);
            stepToggle.setSelected(false);
            boostToggle.setSelected(false);
            boostToggle.setStyle("-fx-font-weight: bold; -fx-base: #2ecc71;");
            nextBtn.setDisable(true);

            infoArea.appendText(">>> Nollattu. Voit aloittaa uuden ajon.\n");
        });

        controls.getChildren().addAll(
                new Label("Skenaario:"), scenarioChooser,
                startBtn, stepToggle, nextBtn, boostToggle, replayBtn
        );

        infoArea.setEditable(false);
        infoArea.setPrefHeight(150);
        infoArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

        bottom.getChildren().addAll(sliderGrid, controls, new Label("Tapahtumaloki:"), infoArea);
        root.setBottom(bottom);
    }

    private void releaseWaitingPassengers() {
        new ArrayList<>(waitingPassengers.values()).forEach(Runnable::run);
        waitingPassengers.clear();
    }

    public void showPassengerInfo(Passenger p) {
        Platform.runLater(() -> {
            infoArea.appendText(String.format("[%03d] %s | Valmis ajassa: %.1f\n",
                    p.getId(), p.getTicketType(), Clock.getInstance().getTime()));
            infoArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void showFinalResults(double avgTime, int totalCompleted, double validationError) {
        Platform.runLater(() -> {
            infoArea.appendText("\n" + "=".repeat(45) + "\n");
            infoArea.appendText("       SIMULAATION LOPPUTULOKSET\n");
            infoArea.appendText(String.format(" Valmistuneet matkustajat: %d kpl\n", totalCompleted));
            infoArea.appendText(String.format(" Keskim. viipymäaika:     %.2f\n", avgTime));
            infoArea.appendText(String.format(" Validiteettivirhe (avg):  %.2f%%\n", validationError));

            if (validationError < 1.0) infoArea.appendText(" >>> Tila: Erittäin tarkka (Valid).\n");
            else if (validationError < 5.0) infoArea.appendText(" >>> Tila: Hyväksyttävä poikkeama.\n");
            else infoArea.appendText(" >>> Tila: Epävakaa (Tarkista kuormitus).\n");

            infoArea.appendText("=".repeat(45) + "\n");
            infoArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}
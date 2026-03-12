package org.example.Controller;


import javafx.application.Platform;
import org.example.Model.SimulationEngine;
import org.example.View.AirportView;

public class AirportController {
    private AirportView view;

    public AirportController(AirportView view) {
        this.view = view;
    }

    /**
     * Käynnistää simulaation ja välittää analyyttiset tulokset näkymälle.
     */
    public void startSimulation(SimulationEngine engine) {

            Thread simThread = new Thread(() -> {
                try {
                    Thread.sleep(500);
                    System.out.println("Controller: Käynnistetään simulaatio...");

                    // 1. Suoritetaan simulaatio (Model suorittaa laskennan)
                    engine.run();

                    // 2. Kerätään tarvittavat tilastot moottorilta
                    double avgTime = engine.getAverageSystemTime();
                    int totalCompleted = engine.getTotalPassengersCompleted();

                    // TÄRKEÄÄ: Tämä hakee nyt sen laskemasi keskiarvoprosentin kaikilta pisteiltä
                    double validationErrorPercent = engine.getLittleLawQueueError();

                    // 3. Päivitetään UI (View näyttää tulokset)
                    Platform.runLater(() -> {
                        view.showFinalResults(
                                avgTime,
                                totalCompleted,
                                validationErrorPercent // UI saa prosentin (esim. 0.45)
                        );
                    });

                } catch (InterruptedException e) {
                    System.err.println("Controller: Simulaatiosäie keskeytyi.");
                } catch (Exception e) {
                    System.err.println("Controller: Virhe simulaation suorituksessa.");
                    e.printStackTrace();
                }
            });

            simThread.setDaemon(true); // Varmistaa, että säie kuolee, jos ohjelma suljetaan
            simThread.start();
        }


    }

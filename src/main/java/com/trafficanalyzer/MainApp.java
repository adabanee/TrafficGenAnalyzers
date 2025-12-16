package com.trafficanalyzer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Main GUI application for Network Load Tester.
 * Provides controls to start traffic generator and receiver, and displays real-time logs.
 */
public class MainApp extends Application {

    private TextArea logArea;
    private TrafficReceiver activeReceiver = null;
    private Thread receiverThread = null;

    /**
     * Initializes and displays the main application window.
     *
     * @param stage the primary stage for this application
     */
    @Override
    public void start(Stage stage) {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(15);

        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("9999");
        TextField countField = new TextField("10");
        TextField sizeField = new TextField("1024");
        TextField freqField = new TextField("5");

        Button startGeneratorBtn = new Button("Start Traffic Generator");
        Button startReceiverBtn = new Button("Start Traffic Receiver");
        Button stopReceiverBtn = new Button("Stop Receiver and Show Stats");

        startGeneratorBtn.setOnAction(e -> startGenerator(hostField, portField, countField, sizeField, freqField));
        startReceiverBtn.setOnAction(e -> startReceiver(portField));
        stopReceiverBtn.setOnAction(e -> stopReceiver());

        VBox root = new VBox(10,
                new Label("=== Traffic Generator ==="),
                new Label("Host:"), hostField,
                new Label("Port:"), portField,
                new Label("Packets:"), countField,
                new Label("Size (bytes):"), sizeField,
                new Label("Frequency (packets/sec):"), freqField,
                startGeneratorBtn,
                new Label("=== Traffic Receiver ==="),
                startReceiverBtn,
                stopReceiverBtn,
                new Label("Log:"),
                logArea
        );

        Scene scene = new Scene(root, 600, 750);
        stage.setTitle("Network Load Tester");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Logs a message to both the GUI log area and the console.
     *
     * @param msg the message to log
     */
    private void log(String msg) {
        logArea.appendText(msg + "\n");
        System.out.println(msg);
    }

    /**
     * Starts the traffic generator in a background thread.
     *
     * @param host host to send traffic to
     * @param port destination port
     * @param count number of packets to send
     * @param size packet size in bytes
     * @param freq frequency in packets per second
     */
    private void startGenerator(TextField host, TextField port, TextField count, TextField size, TextField freq) {
        new Thread(() -> {
            try {
                String h = host.getText();
                int p = Integer.parseInt(port.getText());
                int c = Integer.parseInt(count.getText());
                int s = Integer.parseInt(size.getText());
                int f = Integer.parseInt(freq.getText());

                log("Starting generator: " + h + ":" + p);
                TrafficGenerator gen = new TrafficGenerator(h, p, c, s, f);
                gen.start();
                log("Generator finished.");
            } catch (Exception ex) {
                log("Generator error: " + ex.getMessage());
            }
        }).start();
    }

    /**
     * Starts the traffic receiver in a background thread.
     *
     * @param portField text field containing the port number
     */
    private void startReceiver(TextField portField) {
        new Thread(() -> {
            try {
                int p = Integer.parseInt(portField.getText());
                log("Starting receiver on port " + p);
                activeReceiver = new TrafficReceiver(p);
                receiverThread = new Thread(activeReceiver::start);
                receiverThread.start();
                log("Receiver is running. Send traffic, then click 'Stop Receiver'.");
            } catch (Exception ex) {
                log("Receiver startup error: " + ex.getMessage());
            }
        }).start();
    }

    /**
     * Stops the active receiver and displays statistics.
     */
    private void stopReceiver() {
        if (activeReceiver != null && receiverThread != null && receiverThread.isAlive()) {
            log("Stopping receiver...");
            activeReceiver.stop();
            try {
                receiverThread.join(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            activeReceiver.printStats();
            activeReceiver = null;
            receiverThread = null;
            log("Receiver stopped.");
        } else {
            log("Receiver is not running.");
        }
    }

    /**
     * Entry point of the application.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
package com.evgeniy_mh.paddingoracle;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

public class FXMLController {

    private MainApp mainApp;
    final BlockingQueue<String> messageQueue = new ArrayBlockingQueue<>(1);

    @FXML
    Button testButton;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void initialize() {
        testButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Test");
            alert.showAndWait();
        });

        ClientSocketProcessor processor = new ClientSocketProcessor(messageQueue);
        Thread client = new Thread(processor);
        client.setDaemon(true);
        client.start();
    }
}

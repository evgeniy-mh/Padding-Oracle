package com.evgeniy_mh.paddingoracle;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.AnimationTimer;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FXMLController {

    final BlockingQueue<String> messageQueue = new ArrayBlockingQueue<>(1);

    private MainApp mainApp;
    private FileChooser fileChooser = new FileChooser();
    private Stage stage;

    private File encryptedFile;

    @FXML
    TextArea ClientOutputTextArea;
    @FXML
    Button openFileButton;
    @FXML
    Button startDecodeButton;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void initialize() {
        try {
            fileChooser.setInitialDirectory(new File(MainApp.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile());
        } catch (URISyntaxException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }

        ClientSocketProcessor clientSocket = new ClientSocketProcessor(messageQueue);
        Thread client = new Thread(clientSocket);
        client.setDaemon(true);
        client.start();

        openFileButton.setOnAction(event -> {
            encryptedFile = openFile();
        });

        startDecodeButton.setOnAction(event -> {
            try {
                clientSocket.sendFileToServer(Files.readAllBytes(encryptedFile.toPath()));
            } catch (IOException ex) {
                Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        final LongProperty lastUpdate = new SimpleLongProperty();
        final long minUpdateInterval = 0;
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdate.get() > minUpdateInterval) {
                    final String message = messageQueue.poll();
                    if (message != null) {
                        ClientOutputTextArea.appendText(message + "\n");
                    }
                    lastUpdate.set(now);
                }
            }
        };
        timer.start();
    }

    private File openFile() {
        File file = fileChooser.showOpenDialog(stage);
        return file;
    }
}

package com.evgeniy_mh.paddingoracle;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.AnimationTimer;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.concurrent.Task;
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

        openFileButton.setOnAction(event -> {
            encryptedFile = openFile();
        });

        startDecodeButton.setOnAction(event -> {
            SendFileToServer();
        });
    }

    private void SendFileToServer() {
        byte[] fileToSend = null;
        try {
            fileToSend = Files.readAllBytes(encryptedFile.toPath());
        } catch (IOException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (fileToSend != null) {
            /*Task task = sender.SendFile(fileToSend);
            task.setOnSucceeded(value -> {
                putMessage(String.valueOf(task.getValue()));

            });*/
            Callable c = new FileSender(fileToSend);
            FutureTask<Integer> ftask = new FutureTask<>(c);
            Thread thread = new Thread(ftask);
            thread.start();

            int response = 0;
            try {
                response = ftask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }

            switch (response) {
                case 200:
                    putMessage("200 OK");

                    break;
                case 500:
                    putMessage("500 Error");

                    break;
                default:
                    putMessage("Server response error");
                    break;
            }

        }
    }

    private void putMessage(String message) {
        ClientOutputTextArea.appendText(message + "\n");
    }

    private File openFile() {
        File file = fileChooser.showOpenDialog(stage);
        return file;
    }
}

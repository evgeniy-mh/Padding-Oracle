package com.evgeniy_mh.paddingoracle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.AnimationTimer;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FXMLController {

    final int AES_BLOCK_SIZE = 16;

    private MainApp mainApp;
    private FileChooser fileChooser = new FileChooser();
    private Stage stage;

    private File encryptedFile;
    private File resultFile;
    private DecodeInfo decodeInfo;

    @FXML
    public Label blocksCountLabel;
    @FXML
    public Label currentBlockLabel;
    @FXML
    public Label currentByteLabel;

    @FXML
    Button openFileButton;
    @FXML
    Button startDecodeButton;
    @FXML
    Button openResultFile;
    @FXML
    Button createResultFile;
    @FXML
    ProgressBar decodeProgressBar;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void initialize() {
        decodeInfo = new DecodeInfo();
        updateDecryptInfo();

        try {
            fileChooser.setInitialDirectory(new File(MainApp.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile());
        } catch (URISyntaxException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }

        openFileButton.setOnAction(event -> {
            encryptedFile = openFile("Выбрать зашифрованный файл");
        });

        startDecodeButton.setOnAction(event -> {
            startDecode();
        });

        openResultFile.setOnAction(event -> {
            resultFile = openFile("Выбрать файл результата");
        });

        createResultFile.setOnAction(event -> {
            resultFile = createNewFile("Создать файл для сохранения результата");
        });
    }

    private void startDecode() {

        if (encryptedFile == null || resultFile == null) {
            return;
        }

        if (resultFile.length() != 0) {
            try {
                clearFile(resultFile);
            } catch (IOException ex) {
                Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        Task bruteforce = new AES_CBCBruteforcer(decodeProgressBar, decodeInfo).Bruteforce(encryptedFile, resultFile);

        bruteforce.setOnSucceeded(value -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Файл расшифрован");
            alert.showAndWait();

            decodeInfo.currentBlock.set(0);
            decodeInfo.currentByte.set(0);
            decodeProgressBar.setProgress(0);
        });

        final LongProperty lastUpdate = new SimpleLongProperty();
        final long minUpdateInterval = 0;
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdate.get() > minUpdateInterval) {
                    updateDecryptInfo();
                    lastUpdate.set(now);
                }
            }
        };
        timer.start();

        Thread t = new Thread(bruteforce);
        t.start();
    }

    private void updateDecryptInfo() {
        blocksCountLabel.setText(String.valueOf(decodeInfo.blocksCount.get()));
        currentBlockLabel.setText(decodeInfo.currentBlock.get() + " / " + decodeInfo.blocksCount.get());
        currentByteLabel.setText(decodeInfo.currentByte.get() + " / " + AES_BLOCK_SIZE);
    }

    private File openFile(String dialogTitle) {
        fileChooser.setTitle(dialogTitle);
        File file = fileChooser.showOpenDialog(stage);
        return file;
    }

    private void clearFile(File file) throws IOException {
        RandomAccessFile ras = new RandomAccessFile(file, "rw");
        ras.setLength(0);
        ras.close();
    }

    private File createNewFile(String dialogTitle) {
        fileChooser.setTitle(dialogTitle);
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return file;
    }

    static public void debugPrintByteArray(String mes, byte[] array) {
        System.out.println(mes);
        for (int i = 0; i < array.length; i++) {
            System.out.print(String.format("0x%08X", array[i]) + " ");
        }
        System.out.println();
    }
}

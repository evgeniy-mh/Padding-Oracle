package com.evgeniy_mh.paddingoracle;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FXMLController {

    final int AES_BLOCK_SIZE = 16;
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

        if (fileToSend == null) {
            return;
        }

        //debugPrintByteArray("File", fileToSend);        
        int blocksCount = fileToSend.length / AES_BLOCK_SIZE;
        ArrayList<byte[]> fileBlocks = new ArrayList<>();
        for (int i = 0; i < blocksCount; i++) {
            byte[] buff = new byte[AES_BLOCK_SIZE];
            System.arraycopy(fileToSend, i * 16, buff, 0, 16);
            fileBlocks.add(buff);
            //debugPrintByteArray("file block #" + i, fileBlocks.get(i));
        }

        for (byte i = 0; i < 2; i++) {        
            fileBlocks.get(blocksCount - 2)[AES_BLOCK_SIZE - 1] = i; //C1`
            byte[] tempFile = new byte[AES_BLOCK_SIZE * 2];
            System.arraycopy(fileBlocks.get(blocksCount - 2), 0, tempFile, 0, AES_BLOCK_SIZE);
            System.arraycopy(fileBlocks.get(blocksCount - 1), 0, tempFile, AES_BLOCK_SIZE, AES_BLOCK_SIZE); //C1` + C2

            //debugPrintByteArray("C2", fileBlocks.get(blocksCount-1));
            //debugPrintByteArray("C1` + C2", tempFile);

            Callable callable = new FileSender(tempFile);
            FutureTask<Integer> ftask = new FutureTask<>(callable);
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
            //if(response==200) break;
        }

    }

    private void putMessage(String message) {
        ClientOutputTextArea.appendText(message + "\n");
    }

    private File openFile() {
        File file = fileChooser.showOpenDialog(stage);
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

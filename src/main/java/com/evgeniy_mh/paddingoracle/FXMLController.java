package com.evgeniy_mh.paddingoracle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
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

        ArrayList<byte[]> paddings = new ArrayList<>();
        for (int i = 1; i <= AES_BLOCK_SIZE; i++) {
            byte[] p = new byte[i];
            for (int j = 0; j < i; j++) {
                p[j] = (byte) i;
            }
            paddings.add(p);
        }
        /*paddings.forEach((padding) -> {
            debugPrintByteArray("pad", padding);
        });*/

        if (encryptedFile == null) {
            return;
        }

        byte[] fileToSend = null;
        try {
            fileToSend = Files.readAllBytes(encryptedFile.toPath());
        } catch (IOException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }

        //debugPrintByteArray("File", fileToSend);        
        int blocksCount = fileToSend.length / AES_BLOCK_SIZE;
        ArrayList<byte[]> fileBlocks = new ArrayList<>();
        for (int i = 0; i < blocksCount; i++) {
            byte[] buff = new byte[AES_BLOCK_SIZE];
            System.arraycopy(fileToSend, i * 16, buff, 0, 16);
            fileBlocks.add(buff);
            debugPrintByteArray("file block #" + i, fileBlocks.get(i));
        }

        for (int i = 1; i < blocksCount; i++) {
            byte[] I2 = new byte[AES_BLOCK_SIZE];
            int I2cnt = AES_BLOCK_SIZE - 1;

            for (int b = 0; b < 16; b++) { //по байтам
                for (int g = 0; g < 256; g++) { //по 1 байту
                    //C1`
                    //byte[] C1d = fileBlocks.get(blocksCount - 2).clone();
                    byte[] C1d = new byte[AES_BLOCK_SIZE];

                    byte[] Pad = paddings.get(b).clone();
                    System.arraycopy(Pad, 0, C1d, AES_BLOCK_SIZE - Pad.length, Pad.length);//C1..|..Pad

                    //System.out.println("-----------------------------------------------------------------");
                    //debugPrintByteArray("c1, j=" + j, C1d);
                    C1d[AES_BLOCK_SIZE - Pad.length] = (byte) (C1d[AES_BLOCK_SIZE - Pad.length] ^ g);//01^j
                    if (b != 0) {
                        for (int p = 0; p < AES_BLOCK_SIZE; p++) {
                            C1d[p] = (byte) (C1d[p] ^ I2[p]);
                        }
                    }

                    //debugPrintByteArray("c1d, j=" + j, C1d);
                    byte[] tempFile = new byte[AES_BLOCK_SIZE * 2];
                    byte[] C2 = fileBlocks.get(i);

                    System.arraycopy(C1d, 0, tempFile, 0, AES_BLOCK_SIZE);
                    System.arraycopy(C2, 0, tempFile, AES_BLOCK_SIZE, AES_BLOCK_SIZE); //C1` + C2

                    //debugPrintByteArray("C2", fileBlocks.get(blocksCount-1));
                    //debugPrintByteArray("C1` + C2", tempFile);
                    Callable<Integer> callable = new FileSender(tempFile);
                    FutureTask<Integer> ftask = new FutureTask<>(callable);
                    Thread thread = new Thread(ftask);
                    thread.start();

                    int response = 0;
                    try {
                        response = ftask.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    if (response == 200) {
                        putMessage("OK, j=" + g);
                        System.out.println("j=" + g);
                        I2[I2cnt--] = (byte) g;

                        System.out.println("-------------------------------------------------");
                        debugPrintByteArray("I2", I2);
                        break;
                    }
                }
            }

            byte[] C1 = fileBlocks.get(i - 1).clone();
            for (int p = 0; p < AES_BLOCK_SIZE; p++) {
                C1[p] = (byte) (C1[p] ^ I2[p]);
            }
            debugPrintByteArray("C1", C1);
            File out = new File("/home/evgeniy/Files/Downloads/test/out");
            try {
                out.createNewFile();

                FileOutputStream fos = new FileOutputStream(out, true);
                fos.write(C1);
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /*byte[] I2 = new byte[AES_BLOCK_SIZE];
        int I2cnt = AES_BLOCK_SIZE - 1;
        
        for (int i = 0; i < 16; i++) { //по байтам
            for (int j = 0; j < 256; j++) { //по 1 байту
                //C1`
                //byte[] C1d = fileBlocks.get(blocksCount - 2).clone();
                byte[] C1d = new byte[AES_BLOCK_SIZE];

                byte[] Pad = paddings.get(i).clone();
                System.arraycopy(Pad, 0, C1d, AES_BLOCK_SIZE - Pad.length, Pad.length);//C1..|..Pad

                //System.out.println("-----------------------------------------------------------------");
                //debugPrintByteArray("c1, j=" + j, C1d);
                C1d[AES_BLOCK_SIZE - Pad.length] = (byte) (C1d[AES_BLOCK_SIZE - Pad.length] ^ j);//01^j
                if (i != 0) {
                    for (int p = 0; p < AES_BLOCK_SIZE; p++) {
                        C1d[p] = (byte) (C1d[p] ^ I2[p]);
                    }
                }

                //debugPrintByteArray("c1d, j=" + j, C1d);
                byte[] tempFile = new byte[AES_BLOCK_SIZE * 2];
                byte[] C2 = fileBlocks.get(1);

                System.arraycopy(C1d, 0, tempFile, 0, AES_BLOCK_SIZE);
                System.arraycopy(C2, 0, tempFile, AES_BLOCK_SIZE, AES_BLOCK_SIZE); //C1` + C2

                //debugPrintByteArray("C2", fileBlocks.get(blocksCount-1));
                //debugPrintByteArray("C1` + C2", tempFile);
                Callable<Integer> callable = new FileSender(tempFile);
                FutureTask<Integer> ftask = new FutureTask<>(callable);
                Thread thread = new Thread(ftask);
                thread.start();

                int response = 0;
                try {
                    response = ftask.get();
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (response == 200) {
                    putMessage("OK, j=" + j);
                    System.out.println("j=" + j);
                    I2[I2cnt--] = (byte) j;

                    System.out.println("-------------------------------------------------");
                    debugPrintByteArray("I2", I2);
                    break;
                }
            }
        }

        byte[] C1 = fileBlocks.get(0).clone();
        for (int p = 0; p < AES_BLOCK_SIZE; p++) {
            C1[p] = (byte) (C1[p] ^ I2[p]);
        }
        debugPrintByteArray("C1", C1);
        File out = new File("/home/evgeniy/Files/Downloads/test/out");
        try {
            out.createNewFile();

            FileOutputStream fos = new FileOutputStream(out, true);
            fos.write(C1);
            fos.close();
        } catch (IOException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }*/
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

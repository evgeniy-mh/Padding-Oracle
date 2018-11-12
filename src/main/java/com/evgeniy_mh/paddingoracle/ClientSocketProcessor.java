package com.evgeniy_mh.paddingoracle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientSocketProcessor implements Runnable {

    private final BlockingQueue<String> messageQueue;

    ClientSocketProcessor(BlockingQueue<String> messageQueue) {
        this.messageQueue = messageQueue;
    }

    @Override
    public void run() {
        try {
            initClient();
        } catch (IOException ex) {
            Logger.getLogger(ClientSocketProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void putMessage(String message) {
        try {
            messageQueue.put(message);
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientSocketProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initClient() throws IOException {
        int serverPort = 55555;
        String address = "127.0.0.1";

        Socket socket = new Socket(address, serverPort);

        InputStream sin = socket.getInputStream();
        OutputStream sout = socket.getOutputStream();
        DataInputStream in = new DataInputStream(sin);
        DataOutputStream out = new DataOutputStream(sout);

        out.writeUTF("Hello"); // отсылаем введенную строку текста серверу.
        out.flush(); // заставляем поток закончить передачу данных.
    }

}

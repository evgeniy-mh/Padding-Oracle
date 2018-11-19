package com.evgeniy_mh.paddingoracle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientSocketProcessor implements Runnable {

    private final BlockingQueue<String> messageQueue;
    //private DataInputStream in;
    private DataOutputStream out;
    InputStream sin;
    OutputStream sout;
    Socket socket;

    ClientSocketProcessor(BlockingQueue<String> messageQueue) {
        this.messageQueue = messageQueue;
    }

    @Override
    public void run() {

        putMessage("test");
        putMessage("test");
    }

    private void putMessage(String message) {
        try {
            messageQueue.put(message);
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientSocketProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendFileToServer(byte[] b) {
        try {
            //putMessage("Connecting to Server...");
            int serverPort = 55555;
            String address = "127.0.0.1";

            socket = new Socket(address, serverPort);
            sin = socket.getInputStream();
            sout = socket.getOutputStream();
            //in = new DataInputStream(sin);
            out = new DataOutputStream(sout);

            out.writeUTF("new file");
            out.writeLong(b.length);
            sout.write(b);
            sout.flush();
            putMessage("Done sending file");
            
        } catch (IOException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            stopSocket();
        }
    }

    public void stopSocket() {
        try {
            socket.close();
            sin.close();
            sout.close();
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientSocketProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

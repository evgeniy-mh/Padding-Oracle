package com.evgeniy_mh.paddingoracle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import javafx.concurrent.Task;

public class FileSender {

    final int serverPort = 55555;
    final String address = "127.0.0.1";

    public Task SendFile(byte[] file) {
        return new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {

                Socket socket = new Socket(address, serverPort);
                InputStream sin = socket.getInputStream();
                OutputStream sout = socket.getOutputStream();
                DataInputStream in = new DataInputStream(sin);
                DataOutputStream out = new DataOutputStream(sout);

                out.writeUTF("new file");
                out.writeLong(file.length);
                sout.write(file);
                sout.flush();

                int response = in.readInt();

                socket.close();
                sin.close();
                out.close();
                in.close();
                return response;
            }
        };
    }
}

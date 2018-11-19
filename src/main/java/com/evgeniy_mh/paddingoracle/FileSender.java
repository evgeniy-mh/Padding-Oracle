package com.evgeniy_mh.paddingoracle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Callable;

public class FileSender implements Callable<Integer> {

    final int serverPort = 55555;
    final String address = "127.0.0.1";
    final byte[] file;

    public FileSender(byte[] file) {
        this.file = file;
    }

    @Override
    public Integer call() throws Exception {
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
}

package com.clemesu1.network.server;

import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogFile {

    private Writer output;

    public LogFile(File file)  {
        try {
            FileWriter fileWriter = new FileWriter(file);
            this.output = new BufferedWriter(fileWriter);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void writeEntry(String message, InetAddress clientAddress) {
        synchronized (output) {
            try {
                String timeStamp = new SimpleDateFormat("[d/MMM/yyyy:HH:mm:ss Z]").format(new Date());
                output.write(clientAddress.getHostAddress());
                output.write(" ");
                output.write(timeStamp);
                output.write(" ");
                output.write(message);
                output.write("\r\n");
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}

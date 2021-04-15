package com.clemesu1.network.client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Client {

    private String name, password, address;
    private int port;
    private DatagramSocket socket;
    private Socket clientSocket;
    private InetAddress ip;
    private Thread connect, send;
    private int ID = -1;

    private ObjectInputStream input;
    private ObjectOutputStream output;

    private static final String sendKey = "thisisthekey";

    public Client(String name, String password, String address, int port) {
        this.name = name;
        this.password = password;
        this.address = address;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }


    public boolean openConnection(String address, int port) {
        try {
            socket = new DatagramSocket();
            ip = InetAddress.getByName(address);
            connect = new Thread(() -> {
                try {
                    clientSocket = new Socket(address, port);
                    output = new ObjectOutputStream(clientSocket.getOutputStream());
                    input = new ObjectInputStream(clientSocket.getInputStream());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            });
            connect.start();
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void send(final String text) {
        send = new Thread("Send") {
            @Override
            public void run() {
                String message = AES.encrypt(text, sendKey);
                if (message != null) {
                    byte[] data = message.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
                    try {
                        socket.send(packet);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        };
        send.start();
    }

    public String receive() {
        byte[] data = new byte[1024];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        try {
            socket.receive(packet);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        String message = new String(packet.getData());
        message = AES.decrypt(message, sendKey);
        return message;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public int getID() {
        return ID;
    }

    public void close() {
        new Thread(() -> {
            synchronized (socket) {
                socket.close();
                try {
                    clientSocket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }).start();
    }

    public boolean uploadFile(File file) {
        try {
            FileInputStream fileIn = new FileInputStream(file);
            int fileSize = (int) file.length();
            byte[] fileData = new byte[fileSize];
            fileIn.read(fileData);
            fileIn.close();
            output.writeObject(fileData);
            output.flush();
            return true;
        } catch (IOException ioException) {
            ioException.printStackTrace();
            return false;
        }
    }

    public boolean downloadFile(String fileLocation) {
        try {
            // Read file into byte array.
            byte[] byteArray = (byte[]) input.readObject();
            // Create file output stream for received audio file.
            FileOutputStream fileStream = new FileOutputStream(fileLocation);

            // Write received data into file.
            fileStream.write(byteArray);

            fileStream.close();
            return true;
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            return false;
        }
    }

}

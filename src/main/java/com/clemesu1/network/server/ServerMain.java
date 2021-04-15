package com.clemesu1.network.server;

public class ServerMain {

    private int port;
    private String folder;
    private Server server;

    public ServerMain(int port, String folder) {
        this.port = port;
        this.folder = folder;
        server = new Server(port, folder);
    }

    public static void main(String[] args) {
        int port;
        String folder;
        if (args.length != 2) {
            System.out.println("Usage: java -jar ChatServer.jar [port] [folder]");
            return;
        }
        port = Integer.parseInt(args[0]);
        folder = args[1];
        new ServerMain(port, folder);
    }

}

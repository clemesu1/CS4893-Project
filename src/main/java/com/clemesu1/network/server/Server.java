package com.clemesu1.network.server;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class Server implements Runnable {

    private final int port;
    private final String filePath;

    private LogFile logger;

    private ServerSocket serverSocket;
    private DatagramSocket socket;
    private Socket clientSocket;

    private Thread run, manage, send, receive, update;

    private boolean running = false, raw = false;

    private final int MAX_ATTEMPTS = 10;

    private ObjectInputStream input;
    private ObjectOutputStream output;

    private List<ServerClient> clients = new ArrayList<>();
    private List<File> files;
    private List<Integer> clientResponse = new ArrayList<>();

    private Map<String, String> userPasswordMap = new HashMap<>();
    private Map<String, Integer> userSaltMap = new HashMap<>();
    private Map<String, Integer> userIDMap = new HashMap<>();

    private static final String receiveKey = "thisisthekey";
    private static SecretKeySpec secretKey;
    private static byte[] key;

    private Connection connection = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;

    /**
     * Constructor
     * @param port server port
     * @param filePath path where files will be stored for the file server.
     */
    public Server(int port, String filePath) {

        this.port = port;
        this.filePath = filePath;
        files = new ArrayList<>(Arrays.asList((Objects.requireNonNull(new File(filePath).listFiles()))));
        logger = new LogFile(new File("server.log"));

        createConnection();
        try {
            socket = new DatagramSocket(port);
            serverSocket = new ServerSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        run = new Thread(this, "Server");
        run.start();
    }

    /**
     * Creates a connection to the MySQL database.
     */
    public void createConnection() {
        try {
            System.out.println("Connecting to the MySQL database...");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/networkchat", "root", "password");
            System.out.println("Connected database successfully...");
        } catch (SQLException ex) {
            System.err.println("Error with MySQL Connection.");
            ex.printStackTrace();
        }
    }

    /**
     * Main method for processing server requests.
     */
    @Override
    public void run() {
        running = true;

        System.out.println("Server started on port " + port + "...");

        manageClients();
        getUsersFromDatabase();

        if (clientSocket == null) {
            try {
                clientSocket = serverSocket.accept();
                output = new ObjectOutputStream(clientSocket.getOutputStream());
                input = new ObjectInputStream(clientSocket.getInputStream());

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        receive();

        Scanner scan = new Scanner(System.in);
        while (running) {
            String text = scan.nextLine();
            if (!text.startsWith("/")) {
                sendToAll("/m/Server: " + text + "/e/", serverSocket.getInetAddress());
            } else {
                text = text.substring(1);
                if (text.equalsIgnoreCase("raw")) {
                    if (raw) System.out.println("Raw mode disabled.");
                    else System.out.println("Raw mode enabled.");
                    raw = !raw;
                } else if (text.equalsIgnoreCase("clients")) {
                    System.out.println("Clients:\n=========================================================");
                    for (ServerClient c : clients) {
                        System.out.println(c.name + "(" + c.getID() + "): " + c.address.toString() + ":" + c.port);
                    }
                    System.out.println("=========================================================");
                } else if (text.startsWith("kick")) {
                    String name = text.split(" ")[1];
                    int id = -1;
                    boolean number = true;
                    try {
                        id = Integer.parseInt(name);
                    } catch (NumberFormatException e) {
                        number = false;
                    }
                    if (number) {
                        boolean exists = false;
                        for (ServerClient client : clients) {
                            if (client.getID() == id) {
                                exists = true;
                                break;
                            }
                        }
                        if (exists) disconnect(id, true);
                        else System.out.println("User " + id + "doesn't exist! Check ID number.");
                    } else {
                        for (ServerClient client : clients) {
                            if (name.equals(client.name)) {
                                disconnect(id, true);
                                break;
                            }
                        }
                    }
                } else if (text.equals("help")) {
                    printHelp();

                } else if (text.equals("quit")) {
                    quit();
                } else {
                    System.out.println("Unknown Command.");
                    printHelp();
                }
            }
        }
        scan.close();
    }

    /**
     * Print list of commands to console.
     */
    private void printHelp() {
        System.out.println("Here is a list of all available commands:");
        System.out.println("=========================================");
        System.out.println("/raw - Enables raw mode.");
        System.out.println("/clients - Shows all connected clients.");
        System.out.println("/kick [user ID or username] - Kicks a user.");
        System.out.println("/help - Shows all available commands.");
        System.out.println("/quit - Shuts down the server.");
        System.out.println("=========================================");
    }

    /**
     * Method containing thread to manage client requests.
     */
    private void manageClients() {
        manage = new Thread("Manage") {
            public void run() {
                while (running) {
                    sendToAll("/i/server", serverSocket.getInetAddress());
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (ServerClient client : clients) {
                        if (!clientResponse.contains(client.getID())) {
                            if (client.attempt > MAX_ATTEMPTS) {
                                disconnect(client.getID(), false);
                            } else {
                                client.attempt++;
                            }
                        } else {
                            clientResponse.remove(new Integer(client.getID()));
                            client.attempt = 0;
                        }
                    }
                }
            }
        };
        manage.start();
    }

    /**
     * Method to get all users in the database.
     */
    public void getUsersFromDatabase() {
        try {
            statement = connection.createStatement();

            String sql = "SELECT * FROM Users";

            resultSet = statement.executeQuery(sql);

            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                int salt = resultSet.getInt("salt");

                userPasswordMap.put(username, password);
                userSaltMap.put(username, salt);
                userIDMap.put(username, id);
            }

            resultSet.close();

        } catch (SQLException ex) {
            System.err.println("Error with MySQL Connection.");
            ex.printStackTrace();
        }
    }

    /**
     * Receive data from Datagram socket.
     */
    private void receive() {
        receive = new Thread("Receive") {
            public void run() {
                while (running) {
                    byte[] data = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    try {
                        socket.receive(packet);
                    } catch (SocketException ignored) {
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    process(packet);
                }
            }
        };
        receive.start();
    }

    /**
     * Process the datagram packet received from the socket.
     * @param packet datagram packet from socket.
     */
    private void process(DatagramPacket packet) {
        byte[] data = packet.getData();
        String s = new String(data);
        InetAddress clientAddress = packet.getAddress();
        // Decrypt received packet.
        String string = decrypt(s, receiveKey);

        if (raw) System.out.println(string);
        assert string != null;
        if (string.startsWith("/c/")) {
            /* Connection Packet */

            // Get username, password and user command.
            String name = string.split("/c/|/p/|/b/|/e/")[1];
            String password = string.split("/c/|/p/|/b/|/e/")[2];
            String command =  string.split("/c/|/p/|/b/|/e/")[3];

            int ID;
            if (command.equals("register")) {
                if (!isUsernameTaken(name)) {
                    ID = Math.abs((int) UUID.randomUUID().getLeastSignificantBits());
                    registerAccount(name, password, ID, clientAddress);
                }
            } else if (command.equals("login")) {
                if (!isUsernameCorrect(name)) {
                    // Invalid username.
                    String error = "/error/Login";
                    sendMessage(error, packet.getAddress(), packet.getPort());
                } else if (!isPasswordCorrect(name, password)) {
                    // Invalid password.
                    String error = "/error/Password";
                    sendMessage(error, packet.getAddress(), packet.getPort());
                } else {
                    // No errors detected. Attempting login.

                    // Get ID from ID hashmap.
                    ID = userIDMap.get(name);

                    // Send user connection alert.
                    System.out.println("User " + name + " (" + ID + ") @ " + packet.getAddress() + ":" + packet.getPort() + " connected.");
                    String online = "/m/User " + name + " has connected./e/";
                    sendToAll(online, clientAddress);

                    // Add client to list of clients.
                    clients.add(new ServerClient(name, packet.getAddress(), packet.getPort(), ID));

                    // send ID to user
                    String id = "/c/" + ID;
                    sendMessage(id, packet.getAddress(), packet.getPort());

                    // Update users and files.
                    updateUsers();
                    updateFiles();
                }
            }
        } else if (string.startsWith("/m/")) {
            sendToAll(string, clientAddress);
        } else if (string.startsWith("/d/")) {
            String id = string.split("/d/|/e/")[1];
            disconnect(Integer.parseInt(id), true);
            updateUsers();
        } else if (string.startsWith("/i/")) {
            clientResponse.add(Integer.valueOf(string.split("/i/|/e/")[1]));
        } else if (string.startsWith("/up/")) {
            String fileName = string.split("/up/|/e/")[1];
            receiveFile(fileName, clientAddress);
        } else if (string.startsWith("/down/")) {
            String fileName = string.split("/down/|/e/")[1];
            sendFile(fileName, clientAddress);
        } else if (string.startsWith("/mu/")) {
            // /mu/<Username>/n/<Message>/e/
            sendToUser(string, clientAddress);
        } else {
            System.out.println(string);
        }
    }

    /**
     * Check if the username is taken
     * @param username name chosen by client.
     * @return whether or not the username is taken
     */
    public boolean isUsernameTaken(String username) {
        return userPasswordMap.containsKey(username);
    }

    /**
     * Check if username is correct
     * @param username name chosen by client.
     * @return boolean
     */
    public boolean isUsernameCorrect(String username) {
        // Username is not registered.
        return userPasswordMap.containsKey(username) && userSaltMap.containsKey(username);
    }

    /**
     * Check if password is correct
     * @param username client username
     * @param password client password
     * @return boolean
     */
    private boolean isPasswordCorrect(String username, String password) {
        int salt = userSaltMap.get(username);
        String saltedPassword = password + salt;
        String passwordHash = getSimpleHash(saltedPassword);
        String storedPasswordHash = userPasswordMap.get(username);

        assert passwordHash != null;
        return passwordHash.equals(storedPasswordHash);
    }

    /**
     * Register client account
     * @param username client username
     * @param password client password
     * @param ID client ID
     */
    private void registerAccount(String username, String password, int ID, InetAddress address) {
        int salt = getRandomSalt();
        String saltedPassword = password + salt;
        String passwordHash = getSimpleHash(saltedPassword);
        userPasswordMap.put(username, passwordHash);
        userSaltMap.put(username, salt);
        userIDMap.put(username, ID);

        String logMessage = "User registration: " + username;
        logger.writeEntry(logMessage, address);

        writeUserData(ID, username, passwordHash, salt);
    }

    /**
     * Write user data to the MySQL database.
     * @param id client ID number
     * @param username client username
     * @param passwordHash hashed client password
     * @param salt salt for client password
     */
    private void writeUserData(int id, String username, String passwordHash, int salt) {
        try {
            statement = connection.createStatement();

            String sql = "INSERT INTO Users (id, username, password, salt) VALUES (?, ?, ?, ?)";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, id);
            preparedStatement.setString(2, username);
            preparedStatement.setString(3, passwordHash);
            preparedStatement.setInt(4, salt);

            preparedStatement.execute();

        } catch (SQLException ex) {
            System.err.println("Error with MySQL Connection.");
            ex.printStackTrace();
        }
    }

    /**
     * Update user list.
     */
    private void updateUsers() {
        if (clients.size() <= 0) return;
        String users = "/u/";
        for (int i=0; i<clients.size()-1; i++) {
            users += clients.get(i).name + "/n/";
        }
        users += clients.get(clients.size() - 1).name + "/e/";
        sendToAll(users, serverSocket.getInetAddress());
    }

    /**
     * Update file list.
     */
    private void updateFiles() {
        update = new Thread(() -> {
            if (files.size() <= 0) return;

            File directory = new File(filePath);
            File[] directoryFiles = directory.listFiles();
            String fileString = "/f/";

            for (int i=0; i<files.size()-1; i++) {
                fileString += files.get(i).getName() + "/n/";
            }
            fileString += files.get(files.size() - 1).getName() + "/e/";
            sendToAll(fileString, serverSocket.getInetAddress());
        });
        update.start();
    }

    /**
     * Send file to client.
     * @param fileName name of the file
     * @param address IP address of the client who requested the file.
     */
    private void sendFile(String fileName, InetAddress address) {

        try {
            FileInputStream fileIn = new FileInputStream(filePath + "\\" + fileName);
            // Get the length of the requested file.
            int fileSize = (int) (new File(filePath + "\\" + fileName)).length();

            String logMessage = "File Requested: " + fileName + " (" + fileSize + " bytes)";
            logger.writeEntry(logMessage, address);

            // Create a byte array for the requested file.
            byte[] byteArray = new byte[fileSize];

            // Read the file into the byte array.
            fileIn.read(byteArray);
            fileIn.close();

            // Write the byte array to the socket output stream.
            output.writeObject(byteArray);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Receive file from client.
     * @param fileName name of file received.
     * @param address address of client who sent the file.
     */
    private void receiveFile(String fileName, InetAddress address) {
        try {
            byte[] fileData = (byte[]) input.readObject();
            FileOutputStream fileOut = new FileOutputStream(filePath + "\\" + fileName);
            fileOut.write(fileData);
            fileOut.close();
            File receivedFile = new File(filePath + "\\" + fileName);

            String logMessage = "File Received: " + receivedFile.getName() + " (" + receivedFile.length() + " bytes)";
            logger.writeEntry(logMessage, address);

            files.add(receivedFile);
            updateFiles();
        } catch (IOException | ClassNotFoundException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * Send message to all clients on the network.
     * @param message text message
     * @param address address of client who sent message.
     */
    private void sendToAll(String message, InetAddress address) {
        if (message.startsWith("/m/")) {
            String text = message.substring(3);
            text = text.split("/e/")[0];
            logger.writeEntry(text, address);
            String timeStamp = new SimpleDateFormat("h:mm:ss a").format(new Date());
            message = "/m/" + "[" + timeStamp + "] " + text + "/e/";
        }
        for (ServerClient client : clients) {
            send(message, client.address, client.port);
        }
    }

    /**
     * Send message to specified user
     * @param message string message.
     * @param address Address of client who sent message.
     */
    private void sendToUser(String message, InetAddress address) {

        String toUser = message.split("/mu/|/n/|/e/")[1];
        String text = message.split("/mu/|/n/|/e/")[2];

        logger.writeEntry(text + " -> " + toUser, address);

        for (ServerClient client : clients) {
            if (client.getName().equals(toUser)) {
                send(message, client.address, client.port);
                break;
            }
        }
    }

    /**
     * Add ending indicator to message and send to socket
     * @param message string message
     * @param address IP address
     * @param port port number
     */
    private void sendMessage(String message, InetAddress address, int port) {
        message += "/e/";
        send(message, address, port);
    }

    /**
     * Send text to a client.
     * @param text message text
     * @param address IP address
     * @param port port number
     */
    private void send(final String text, final InetAddress address, final int port) {
        send = new Thread("Send") {
            public void run() {
                String message = encrypt(text, receiveKey);
                if (message != null) {
                    byte[] data = message.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
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

    /**
     * Call disconnect method specifying the status boolean to be true.
     */
    private void quit() {
        for (ServerClient client : clients) {
            disconnect(client.getID(), true);
        }
        running = false;
    }

    /**
     * Disconnect from the server.
     * @param id client ID.
     * @param status distinguishes whether the client disconnected or timed out.
     */
    private void disconnect(int id, boolean status) {
        ServerClient c = null;
        boolean existed = false;
        for (ServerClient client : clients) {
            if (client.getID() == id) {
                c = client;
                clients.remove(client);
                existed = true;
                break;
            }
        }

        if (!existed) return;

        String message = "", send = "";
        if (status) {
            message = "User " + c.name + " (" + c.getID() + ") @ " + c.address.toString() + ":" + c.port + " disconnected.";
            send = "User " + c.name + " has disconnected.";
            sendToAll(send, c.getAddress());

        } else {
            message = "User " + c.name + " (" + c.getID() + ") @ " + c.address.toString() + ":" + c.port + " timed out.";
            send = "User " + c.name + " has timed out.";

        }
        System.out.println(message);
        sendToAll("/m/" + send + "/e/", c.getAddress());
    }

    /**
     * Returns a random number between 0 and 1000.
     */
    private int getRandomSalt() {
        return (int) (Math.random() * 1000);
    }

    /**
     * https://www.geeksforgeeks.org/sha-512-hash-in-java/
     * Returns a hash for the given password.
     * @param password
     * @return hashed text.
     */
    private String getSimpleHash(String password) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = sha.digest(password.getBytes(StandardCharsets.UTF_8));
            BigInteger no = new BigInteger(1, messageDigest);
            String hashText = no.toString(16);
            while (hashText.length() < 32) {
                hashText = "0" + hashText;
            }
            return hashText;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sets the key for the cipher.
     * @param keyString key string.
     */
    private static void setKey(String keyString) {
        try {
            key = keyString.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Encryption of the message string passed through the parameters.
     * @param message string to be encrypted
     * @param secret secret key
     * @return encrypted string
     */
    private static String encrypt(String message, String secret) {
        try {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] cipherText = cipher.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(cipherText);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | BadPaddingException | IllegalBlockSizeException e) {
            System.err.println("Error while encrypting: " + e);
        }
        return null;
    }

    /**
     * Decrypts a message received from the client.
     * @param cipherText message to be decrypted
     * @param secret secret key
     * @return decrypted string
     */
    private static String decrypt(String cipherText, String secret) {
        try {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] plainText = cipher.doFinal(Base64.getMimeDecoder().decode(cipherText));
            return new String(plainText);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                | BadPaddingException | IllegalBlockSizeException e) {
            System.err.println("Error while encrypting: " + e);
        }
        return null;
    }
}

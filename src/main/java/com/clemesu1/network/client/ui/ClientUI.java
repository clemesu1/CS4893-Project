package com.clemesu1.network.client.ui;

import com.clemesu1.network.client.Client;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Arrays;

public class ClientUI extends JFrame implements Runnable {
    private JPanel contentPane;
    private JTabbedPane tabbedPane;
    private JPanel tabFileSharing;
    private JPanel tabChatroom;
    private JList<String> userList;
    private JTextField txtMessage;
    private JButton btnSend;
    private JTextArea txtHistory;
    private JTextField txtSelectedFile;
    private JButton btnBrowse;
    private JButton btnDownload;
    private JButton btnUpload;
    private JPanel interfacePanel;
    private JPanel mediaPanel;
    private JList<String> fileList;
    private JPopupMenu popupMenu;
    private MessagePane messagePane;

    private final JFrame loginWindow;

    private DefaultCaret caret;
    private Thread run, listen;
    private Client client;

    private boolean running = false;

    private File selectedFile;

    public ClientUI(String name, String password, String address, int port, boolean isLogin, JFrame loginWindow) {
        this.loginWindow = loginWindow;

        setTitle("Chat Client - " + name);
        client = new Client(name, password, address, port);

        boolean connect = client.openConnection(address, port);

        if (!connect) {
            System.err.println("Connection failed!");
            console("Connection failed!");
        }

        console("Attempting to connect to " + address + ":" + port + ", user: " + name);

        if (isLogin) {
            // User logging into existing account.
            String connection = "/c/" + name + "/p/" + password + "/b/login/e/";
            client.send(connection);
            createWindow();
            running = true;
            run = new Thread(this, "Running");
            run.start();
        } else {
            String connection = "/c/" + name + "/p/" + password + "/b/register/e/";
            client.send(connection);
        }
    }

    private void createWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 480);
        setLocationRelativeTo(null);
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        txtHistory.setMargin(new Insets(0, 5, 5, 5));
        txtHistory.setEditable(false);

        btnUpload.setEnabled(false);

        caret = (DefaultCaret) txtHistory.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        popupMenu = new JPopupMenu("Message");
        JMenuItem directMessage = new JMenuItem("Direct Message");
        JMenuItem close = new JMenuItem("Close");

        popupMenu.add(directMessage, close);

        btnSend.addActionListener(e -> send(txtMessage.getText(), true));
        txtMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    send(txtMessage.getText(), true);
                }
            }
        });

        btnBrowse.addActionListener(e -> handleBrowse());
        btnDownload.addActionListener(e -> handleDownload());
        btnUpload.addActionListener(e -> handleUpload());
        fileList.addListSelectionListener(e -> txtSelectedFile.setText(fileList.getSelectedValue()));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                String disconnect = "/d/" + client.getID() + "/e/";
                send(disconnect, false);
                running = false;
                client.close();
            }
        });

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    userList.setSelectedIndex(userList.locationToIndex(e.getPoint())); // Highlight right-clicked value in list.
                    popupMenu.show(e.getComponent(), e.getX(), e.getY()); // Show popup menu
                }            }
        });

        directMessage.addActionListener(e -> {
            String username = userList.getSelectedValue();
            if (!client.getName().equals(username)) {
                messagePane = new MessagePane(client, username);
                JFrame f = new JFrame("Messaging: " + username);
                f.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                f.setSize(500, 500);
                f.getContentPane().add(messagePane, BorderLayout.CENTER);
                f.setVisible(true);
            }
        });
    }

    public void handleUpload() {
        String fileName = this.selectedFile.getName();
        int choice = JOptionPane.showConfirmDialog(null, "Upload " + fileName + "?", "Upload File", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            String command = "/up/" + this.selectedFile.getName() + "/e/";
            client.send(command);
            client.uploadFile(this.selectedFile);
            String uploadMessage = "[File Uploaded]: " + this.selectedFile.getName();
            send(uploadMessage, true);
            if (client.uploadFile(this.selectedFile))
                JOptionPane.showMessageDialog(this, "File Successfully Uploaded.", "Success!", JOptionPane.PLAIN_MESSAGE);
            else {
                JOptionPane.showMessageDialog(this, "Error Uploading File.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void handleDownload() {
        if (fileList.getSelectedValue() != null) {
            String fileName = "/down/" + fileList.getSelectedValue() + "/e/";
            client.send(fileName);
            String fileLocation;
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(fileList.getSelectedValue()));
            int choice = fc.showSaveDialog(this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                fileLocation = fc.getSelectedFile().getAbsolutePath();
                if (client.downloadFile(fileLocation)) {
                    JOptionPane.showMessageDialog(this, "File Successfully Downloaded.", "Success!", JOptionPane.PLAIN_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Error Downloading File.", "Error", JOptionPane.ERROR_MESSAGE);

                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "You must first select a file to download.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void handleBrowse() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(System.getProperty("user.name")));
        fc.setAcceptAllFileFilterUsed(false);
        int choice = fc.showOpenDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            this.selectedFile = fc.getSelectedFile();
            txtSelectedFile.setText(this.selectedFile.getAbsolutePath());
            btnUpload.setEnabled(true);
        }
    }

    public void updateUsers(String[] users) {
        userList.setListData(users);
    }

    public void updateFiles(String[] files) {
        fileList.setListData(files);
    }

    public void send(String message, boolean text) {
        if (message.equals("")) return;
        if (text) {
            message = "/m/" + client.getName() + ": " + message + "/e/";
            txtMessage.setText("");
        }
        client.send(message);
    }

    public void listen() {
        listen = new Thread("Listen") {
            @Override
            public void run() {
                while (running) {
                    String message = client.receive();
                    if (message != null) {
                        if (message.startsWith("/c/")) {
                            setVisible(true);
                            loginWindow.dispose();
                            client.setID(Integer.parseInt(message.split("/c/|/e/")[1]));
                            console("Successfully connected to server! ID: " + client.getID());
                        } else if (message.startsWith("/m/")) {
                            String text = message.substring(3);
                            text = text.split("/e/")[0];
                            console(text);
                            notification();
                        } else if (message.startsWith("/i/")) {
                            String text = "/i/" + client.getID() + "/e/";
                            send(text, false);
                        } else if (message.startsWith("/u/")) {
                            String[] u = message.split("/u/|/n/|/e/");

                            String[] addUser = new String[u.length + 1];
                            System.arraycopy(u, 0, addUser, 0, u.length);
                            addUser[u.length] = "";

                            updateUsers(Arrays.copyOfRange(addUser, 1, addUser.length - 1));
                        } else if (message.startsWith("/f/")) {
                            String[] f = message.split("/f/|/n/|/e/");
                            String[] addFile = new String[f.length + 1];
                            System.arraycopy(f, 0, addFile, 0, f.length);
                            addFile[f.length] = "";
                            updateFiles(Arrays.copyOfRange(addFile, 1, addFile.length - 1));
                        } else if (message.startsWith("/mu/")) {
                            String messageReceived = message.split("/mu/|/n/|/e/")[2];
                            messagePane.receiveMessage(messageReceived);
                        } else if (message.startsWith("/error/")) {
                            String error = message.split("/error/|/e/")[1];
                            if (error.equals("Login")) {
                                JOptionPane.showMessageDialog(null, "Invalid Username", "Error", JOptionPane.ERROR_MESSAGE);
                            } else if (error.equals("Password")) {
                                JOptionPane.showMessageDialog(null, "Invalid Password", "Error", JOptionPane.ERROR_MESSAGE);
                            } else if (error.equals("Register")) {
                                JOptionPane.showMessageDialog(null, "Username is already taken!", "Register Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } else {
                            System.out.println(message.substring(3));
                        }
                    }
                }
            }
        };
        listen.start();
    }

    public static void notification() {
        try {
            Synthesizer midiSynth = MidiSystem.getSynthesizer();
            midiSynth.open();

            // Get and load default instrument and channel lists.
            Instrument[] instr = midiSynth.getDefaultSoundbank().getInstruments();
            MidiChannel[] mChannels = midiSynth.getChannels();

            midiSynth.loadInstrument(instr[0]);

            for (int i = 0; i < 1; i++) {
                mChannels[0].noteOn(60, 75); // On channel 0, play note number 60 with velocity 75
                try {
                    Thread.sleep(35);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                mChannels[0].noteOff(60); // Turn off the note.

                mChannels[0].noteOn(67, 75); // On channel 0, play note number 67 with velocity 75
                try {
                    Thread.sleep(35);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                mChannels[0].noteOff(67); // Turn off the note.
            }
        } catch (MidiUnavailableException ignored) { }
    }

    @Override
    public void run() {
        listen();
    }

    public void console(String message) {
        txtHistory.append(message + "\n\r");
    }

}

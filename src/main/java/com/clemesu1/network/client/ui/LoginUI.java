package com.clemesu1.network.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class LoginUI extends JFrame {
    private JPanel contentPane;
    private JButton btnLogin;
    private JLabel lblCreateAccount;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JTextField txtIPAddress;
    private JTextField txtPort;

    public LoginUI() {
        setTitle("Login");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        setResizable(false);
        setBounds(100, 100, 300, 380);
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);


        String text = lblCreateAccount.getText();
        lblCreateAccount.setForeground(Color.BLUE.darker());
        lblCreateAccount.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblCreateAccount.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new RegisterUI();
                dispose();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                lblCreateAccount.setText("<html><a href=''>" + text + "</a></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                lblCreateAccount.setText(text);

            }
        });
        btnLogin.addActionListener(e -> {
            String username = txtUsername.getText();
            String password = new String(txtPassword.getPassword());
            String ipAddress = txtIPAddress.getText();
            String portString = txtPort.getText();

            if (username.equals("")) {
                JOptionPane.showMessageDialog(null, "Please enter your username.", "Login Error", JOptionPane.ERROR_MESSAGE);
            } else if (password.equals("")) {
                JOptionPane.showMessageDialog(null, "Please enter your password.", "Login Error", JOptionPane.ERROR_MESSAGE);
            } else if (ipAddress.equals("")) {
                JOptionPane.showMessageDialog(null, "Please enter the server IP address.", "Login Error", JOptionPane.ERROR_MESSAGE);
            } else if (portString.equals("")) {
                JOptionPane.showMessageDialog(null, "Please enter a server port.", "Login Error", JOptionPane.ERROR_MESSAGE);
            } else {
                int port = Integer.parseInt(portString);
                if (port >= 0 && port <= 65353) {
                    handleLogin(username, password, ipAddress, port);
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid Server Port.", "Port Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        txtPort.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c))
                    e.consume();
            }
        });
    }

    private void handleLogin(String username, String password, String address, int port) {
        new ClientUI(username, password, address, port, true, this);
    }
}

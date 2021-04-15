package com.clemesu1.network.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class RegisterUI extends JFrame {

    private JPanel contentPane;
    private JButton btnRegister;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirm;
    private JTextField txtIPAddress;
    private JTextField txtPort;
    private JLabel lblLoginAccount;

    public RegisterUI() {
        setTitle("Register");

        setBounds(100, 100, 300, 420);
        setLocationRelativeTo(null);
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        String text = lblLoginAccount.getText();
        lblLoginAccount.setForeground(Color.BLUE.darker());
        lblLoginAccount.setCursor(new Cursor(Cursor.HAND_CURSOR));

        lblLoginAccount.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new LoginUI();
                dispose();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                lblLoginAccount.setText("<html><a href=''>" + text + "</a></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                lblLoginAccount.setText(text);
            }
        });

        btnRegister.addActionListener(e -> {
            String username = txtUsername.getText();
            String password = new String(txtPassword.getPassword());
            String reenter = new String(txtConfirm.getPassword());
            String ipAddress = txtIPAddress.getText();
            String portString = txtPort.getText();

            if (username.equals("")) {
                JOptionPane.showMessageDialog(null, "Please enter a username.", "Register Error", JOptionPane.ERROR_MESSAGE);
            } else if (password.equals("") || reenter.equals("")) {
                JOptionPane.showMessageDialog(null, "Please enter a password.", "Register Error", JOptionPane.ERROR_MESSAGE);
            } else if (ipAddress.equals("")) {
                JOptionPane.showMessageDialog(null, "Please enter the server IP address.", "Register Error", JOptionPane.ERROR_MESSAGE);
            } else if (portString.equals("")) {
                JOptionPane.showMessageDialog(null, "Please enter a server port.", "Register Error", JOptionPane.ERROR_MESSAGE);
            } else {
                if (!password.equals(reenter)) {
                    JOptionPane.showMessageDialog(null, "Passwords do not match. Please retry.", "Password Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    int port = Integer.parseInt(portString);
                    if (port < 0 || port > 65353) {
                        JOptionPane.showMessageDialog(null, "Invalid Server Port.", "Port Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        handleRegister(username, password, ipAddress, port);
                    }
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

    private void handleRegister(String username, String password, String address, int port) {
        new ClientUI(username, password, address, port, false, this);
        JOptionPane.showMessageDialog(this, "Account Registered Successfully!", "Account Registration", JOptionPane.PLAIN_MESSAGE);
        new LoginUI();
        dispose();
    }


}

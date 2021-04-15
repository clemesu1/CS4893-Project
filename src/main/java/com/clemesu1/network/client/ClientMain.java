package com.clemesu1.network.client;

import com.clemesu1.network.client.ui.LoginUI;

import java.awt.*;

public class ClientMain {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                new LoginUI();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

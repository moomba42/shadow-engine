package com.alexdl.shadowhaven.engine;


import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;

public class EditorLauncher {
    public static void main(String[] args) {
        System.setProperty("apple.awt.application.appearance", "system");
        FlatDarkLaf.setup();
        JFrame frame = new JFrame("Editor");
        frame.setBackground(Color.black);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(new JButton("Click me!"));
        panel.add(new JTextArea("Input something"));
        frame.add(panel);

        frame.setMinimumSize(new Dimension(200, 400));
        frame.pack();
        frame.setVisible(true);
    }
}

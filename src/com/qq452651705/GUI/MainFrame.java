package com.qq452651705.GUI;

import javax.swing.*;
import java.awt.*;

public class MainFrame {

    public static int screenWidth;
    public static int screenHeight;

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("LoginActivity");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            Toolkit kit = Toolkit.getDefaultToolkit();
            Dimension screensize = kit.getScreenSize();
            screenWidth = screensize.width;
            screenHeight = screensize.height;
            frame.setSize(screenWidth *2/ 3, screenHeight *2/ 3);
            frame.setTitle("智能无线物联网");
            frame.setResizable(true);
            frame.setLocationByPlatform(true);
            Image icon = new ImageIcon("timg.jpg").getImage();
            frame.setIconImage(icon);


            frame.setVisible(true);
            LoginActivity loginActivity = new LoginActivity(frame);
        });
    }
}
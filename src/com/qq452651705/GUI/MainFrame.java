package com.qq452651705.GUI;

import javax.swing.*;
import java.awt.*;

/**
 * The type Main frame. GUI主框架入口.设置全局属性
 */
public class MainFrame {

    /**
     * The constant screenWidth. 屏宽
     */
    public static int screenWidth;
    /**
     * The constant screenHeight. 屏高
     */
    public static int screenHeight;

    static JFrame frame;

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            frame = new JFrame("LoginActivity");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            Toolkit kit = Toolkit.getDefaultToolkit();
            Dimension screensize = kit.getScreenSize();
            screenWidth = screensize.width;
            screenHeight = screensize.height;

            setframeSize(screenWidth *2/ 3, screenHeight *2/ 3,true);
            frame.setTitle("智能无线物联网");      //标题
            frame.setResizable(true);            //窗口大小可调
            frame.setLocationByPlatform(true);    //默认显示位置
            ImageIcon imageIcon=new ImageIcon("timg.jpg"); //图标
            if(imageIcon!=null) {
                Image icon = new ImageIcon().getImage();
                frame.setIconImage(icon);
            }
            frame.setVisible(true);
            LoginActivity loginActivity = new LoginActivity(frame);  //加载登陆界面
        });
    }

    public static void setframeSize(int width,int height,Boolean resizable){
        frame.setResizable(resizable);
        frame.setSize(width, height);
    }
}
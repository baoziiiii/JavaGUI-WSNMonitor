package com.qq452651705.GUI;

import com.qq452651705.JDBC.MyDatabase;
import com.qq452651705.Utils.TxtUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static String dbconfigpath=File.separator+"config"+File.separator+"db.config";

    private static JFrame frame;

    private static Boolean noconfig;

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
            TxtUtils txtUtils=new TxtUtils(dbconfigpath);
            String config;
            try {
                config = txtUtils.read();
                if(config!=null) {
                    String[] groups = config.split("\\s+");
                    if (groups.length == 2) {
                        String usrname = groups[0].replace("username=", "").trim();
                        String pswd = groups[1].replace("password=", "").trim();
                        MyDatabase.configDatabase(usrname, pswd);
                    }
                }
                noconfig=false;
            } catch (IOException e) {
                noconfig=true;
                JOptionPane.showConfirmDialog(frame,"欢迎您使用智能无线传感器网络上位机系统，第一次登陆请设置MySQL的账户和密码。","欢迎",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE);
            }
            try {
                MyDatabase.loadMySQL();
                LoginActivity loginActivity = new LoginActivity(frame);  //加载登陆界面
            } catch (Exception e) {
                if(noconfig==false)
                    JOptionPane.showConfirmDialog(frame, "无法连接MySQL数据库！请设置数据库账号密码！", "异常", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                JDialog jDialog = new DatabaseConfigDiaglog(frame);
                jDialog.setLocationByPlatform(true);
                jDialog.setVisible(true);
                jDialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowDeactivated(WindowEvent e) {
                        System.exit(0);
                    }
                });
            }
        });
    }

    static class DatabaseConfigDiaglog extends JDialog {
        /**
         * Instantiates a new Register dialog. 初始化注册窗口框
         *
         * @param owner the owner   父窗口
         */
        public DatabaseConfigDiaglog(JFrame owner) {

            super(owner, "MySQL设置", true);
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(3,2));
            panel.add(new JLabel("数据库账户: "));
            JTextField usernameField = new JTextField();
            panel.add(usernameField);
            panel.add(new JLabel("数据库密码: "));
            JPasswordField passwordField = new JPasswordField();
            panel.add(passwordField);

            panel.add(new JLabel(""));
            JButton commit=new JButton("确认");
            commit.addActionListener((l)->{
                String username=usernameField.getText();
                String password=new String(passwordField.getPassword());
                TxtUtils txtUtils=new TxtUtils(dbconfigpath);
                try {
                    txtUtils.writeToTxt("username="+username+"\r\n"+"password="+password);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                MyDatabase.configDatabase(username,password);
                try {
                    MyDatabase.loadMySQL();
                    JOptionPane.showConfirmDialog(frame,"连接成功！","成功",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE);
                    LoginActivity loginActivity=new LoginActivity(frame);
                    dispose();
                } catch (Exception e){
                    JOptionPane.showConfirmDialog(frame,"数据库连接失败！请确认MySQL正确安装且用户名密码无误！","错误",JOptionPane.DEFAULT_OPTION,JOptionPane.ERROR_MESSAGE);
                }
            });
            panel.add(commit);
            add(panel);
            pack();
        }
    }

    public static void setframeSize(int width,int height,Boolean resizable){
        frame.setResizable(resizable);
        frame.setSize(width, height);
    }
}
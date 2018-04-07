package com.qq452651705.SerialComm;

import javax.swing.*;
import java.awt.*;

public class test {
    static TextView mTextView=new TextView();
    static JPanel buttonPanel=new JPanel();
    public static void main(String[] args) throws InterruptedException {
        Thread serialCommThread = new SerialCommThread();
        EventQueue.invokeLater(() -> {
            SimpleFrame frame = new SimpleFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            frame.add(mTextView);

            JButton clearButton=new JButton("清空");
            buttonPanel=new JPanel();
            buttonPanel.add(clearButton);
            clearButton.addActionListener(event->{
                mTextView.setText("");
                mTextView.repaint();
            });
            frame.add(buttonPanel);

        });
        serialCommThread.start();
    }

    static class SerialCommThread extends Thread {
        private StringBuffer stringBuffer = new StringBuffer();

        @Override
        public void run() {
//            com.qq452651705.SerialComm serialComm = com.qq452651705.SerialComm.getSerialComm();
//            ArrayList<String> arrayList = serialComm.findPort();
//            System.out.println(arrayList);
//            System.out.println(System.getProperty("java.library.path"));
//            serialComm.addListener(com3, new SerialPortEventListener() {
//                @Override
//                public void serialEvent(SerialPortEvent serialPortEvent) {
//                    switch (serialPortEvent.getEventType()) {
//
//                        case SerialPortEvent.BI: // 10 通讯中断
//                            break;
//
//                        case SerialPortEvent.OE: // 7 溢位（溢出）错误
//
//                        case SerialPortEvent.FE: // 9 帧错误
//
//                        case SerialPortEvent.PE: // 8 奇偶校验错误
//
//                        case SerialPortEvent.CD: // 6 载波检测
//
//                        case SerialPortEvent.CTS: // 3 清除待发送数据
//
//                        case SerialPortEvent.DSR: // 4 待发送数据准备好了
//
//                        case SerialPortEvent.RI: // 5 振铃指示
//
//                        case SerialPortEvent.OUTPUT_BUFFER_EMPTY: // 2 输出缓冲区已清空
//                            break;
//
//                        case SerialPortEvent.DATA_AVAILABLE: // 1 串口存在可用数据
//                            byte[] bytes = serialComm.readFromPort(com3);
//                            if (bytes != null) {
//                                stringBuffer.append(new String(bytes));
//                                EventQueue.invokeLater(() -> {
//                                    mTextView.setText(stringBuffer.toString());
//                                    mTextView.repaint();
//                                });
//                            }
//                            break;
//                    }
//                }
//            });
//            while (true) {
//            }
        }
    }
}

class TextView extends JComponent {
    public static final int MESSAGE_X = 100;
    public static final int MESSAGE_Y = 100;
    private String text="";

    public void paintComponent(Graphics g) {
        g.drawString(text, MESSAGE_X, MESSAGE_Y);
    }

    public void setText(String s) {
        this.text = s;
    }
}

class SimpleFrame extends JFrame {
    public SimpleFrame() {
        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension screensize = kit.getScreenSize();
        int screenWidth = screensize.width;
        int screenHeight = screensize.height;
        setSize(screenWidth / 2, screenHeight / 2);
        setTitle("智能无线物联网");
        setResizable(true);
        setLocationByPlatform(true);
        Image icon = new ImageIcon("timg.jpg").getImage();
        setIconImage(icon);
    }
}



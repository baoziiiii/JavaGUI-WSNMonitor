package com.qq452651705.SerialComm;

import gnu.io.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TooManyListenersException;

/**
 *   串口类
 */
public class SerialComm {

    private static SerialComm serialComm=new SerialComm();

    private SerialComm(){}

    public static SerialComm getSerialComm(){
        return serialComm;
    }

    /**
     * 查找所有可用端口
     * @return 可用端口名称列表
     */
    public final ArrayList<String> findPort() {

        //获得当前所有可用串口
        Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();

        ArrayList<String> portNameList = new ArrayList<>();

        //将可用串口名添加到List并返回该List
        while (portList.hasMoreElements()) {
            String portName = portList.nextElement().getName();
            portNameList.add(portName);
        }
        return portNameList;
    }

    /**
     * 打开串口
     * @param portName 端口名称
     * @param baudrate 波特率
     * @return 串口对象
     */
    public final SerialPort openPort(String portName, int baudrate , int databits , int stopbits , int parity)throws NoSuchPortException,PortInUseException{

        try {

            //通过端口名识别端口
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

            //打开端口，并给端口名字和一个timeout（打开操作的超时时间）
            CommPort commPort = portIdentifier.open(portName, 2000);

            //判断是不是串口
            if (commPort instanceof SerialPort) {

                SerialPort serialPort = (SerialPort) commPort;

                try {
                    //设置一下串口的波特率等参数
                    serialPort.setSerialPortParams(baudrate, databits, stopbits, parity);
                } catch (UnsupportedCommOperationException e) {
                }

                //System.out.println("Open " + portName + " sucessfully !");
                return serialPort;

            }
            else {
                //不是串口
                System.out.println("Not a serial.");
            }
        } catch (NoSuchPortException e1) {
            throw e1;
        } catch (PortInUseException e2) {
            throw e2;
        }
        return null;
    }

    /**
     * 关闭串口
     */
    public void closePort(SerialPort serialPort) {
        if (serialPort != null) {
            serialPort.close();
            serialPort = null;
        }
    }

    /**
     * 往串口发送数据
     * @param serialPort 串口对象
     * @param order    待发送数据
     */
    public void sendToPort(SerialPort serialPort, byte[] order) {

        OutputStream out = null;

        try {

            out = serialPort.getOutputStream();
            out.write(order);
            out.flush();

        } catch (IOException e) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                    out = null;
                }
            } catch (IOException e) {
            }
        }

    }

    /**
     * 从串口读取数据
     * @param serialPort 当前已建立连接的SerialPort对象
     * @return 读取到的数据
     */
    public byte[] readFromPort(SerialPort serialPort) throws IOException {

        Boolean exception=false;
        InputStream in = null;
        byte[] bytes = new byte[100];

        try {
            in = serialPort.getInputStream();
            int bufflenth = in.available();        //获取buffer里的数据长度

            while (bufflenth != 0) {
                bytes = new byte[bufflenth];    //初始化byte数组为buffer中数据的长度
                in.read(bytes);
                bufflenth = in.available();
            }
        } catch (IOException e) {
            exception=true;
            throw e;
        } finally {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
            } catch(IOException e) {
                exception=true;
                throw e;
            }
            if(!exception) {
                return bytes;
            }
        }
        return null;
    }

    /**
     * 添加监听器
     * @param port     串口对象
     * @param listener 串口监听器
     */
    public void addListener(SerialPort port, SerialPortEventListener listener) {

        try {

            //给串口添加监听器
            port.addEventListener(listener);
            //设置当有数据到达时唤醒监听接收线程
            port.notifyOnDataAvailable(true);
            //设置当通信中断时唤醒中断线程
            port.notifyOnBreakInterrupt(true);

        } catch (TooManyListenersException e) {
        }
    }

}

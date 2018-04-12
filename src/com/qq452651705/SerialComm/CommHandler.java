package com.qq452651705.SerialComm;

import java.sql.Timestamp;
import java.util.*;

import com.qq452651705.DataMGM.Node.NodeManager;
import com.qq452651705.DataMGM.Node.NodeTree;
import com.qq452651705.DataMGM.Tourists.TouristManager;
import com.qq452651705.DataMGM.Tourists.TouristManager.Tourist;
import com.qq452651705.GUI.RealTimeChart;
import com.qq452651705.Utils.Bean;
import com.sun.jndi.cosnaming.IiopUrl;
import gnu.io.SerialPort;
import com.qq452651705.DataMGM.Node.NodeTree.SinkNode;
import com.qq452651705.DataMGM.Node.NodeTree.Node;

import javax.swing.*;


public class CommHandler {

    /*******************************上位机通信数据格式************************************
     * <属性名称:属性值>
     * 可嵌套：<属性名称：<子属性名称:属性值><子属性名称：属性值>>
     *
     * *****************************上位机通信指令表****************************
     * %d为数字(字符串形式) ；%c为一个字节编码 ；%s为字符串 ；%f为浮点数
     *
     * 【上位机->单片机】
     * <Cookie:%c> : 通知单片机释放编号为%c的cookie。%c为值为0~255的单个字节 ; 当%c为':'，通知单片机清除所有cookie。
     * <SinkNodeLS:>  : 请求所有Sink节点地址。返回n个<SinkNodeLS:Address>。
     * <SensorLS:%d>    :  请求地址为%d的Sink节点下所有的传感器地址及名称。返回<SensorLS:<Address:<%d:%d>><Name:%s>>。
     * <SensorRSD:<%d:%d>> : 请求地址为%d(Sink节点地址):%d(传感器地址)的数据。返回<SensorRSD:<Address:<%d:%d>><Data:%f>>
     * <SensorRASD:%d>  :   请求地址为%d的Sink节点下所有传感器的值。返回n个<SensorRSD:<Address:<%d:%d>><Data:%f>>
     *
     * 【单片机->上位机】
     * 指令：
     * <GUEST:<IMEI:%s{15}><Cookie:%d>> : 通知上位机发生新连接。%s{15}为15个字节的IMEI号，%d为给该IMEI所分配的cookie字符串格式（注意和上面区别）
     * <GUEST:<PHONE:%s><Cookie:%d>> ：通知上位机来自APP发来的手机号。
     * <Connected:%d> : 通知上位机成功收到来自编号%d的APP的任何请求（代表与该APP的连接正常）。%d为cookie的字符串格式
     * <Disconnected:%d>:通知上位机收到来自编号%d的APP发来的断开连接指令。
     * <SinkNodeLS:%d> ：应答上位机地址为%d的Sink节点可用。
     * <SensorLS:<Address:<%d1:%d2>><Name:%s>> ：应答上位机地址为%d1的Sink节点下的地址为%d2的传感器节点可用，其英文名称为%s。
     * <SensorRSD:<Address:<%d1:%d2>><Data:%f>> : 应答上位机地址为%d1:%d2的传感器节点的数据。
     *
     * 其他信息：
     * CookieCleared:All :应答上位机，成功清除所有cookie。
     * CookieCleared:%d  :应答上位机，成功清除指定cookie。
     * Invalid Command！ :应答上位机，来自上位机的指令不正确。
     *
     *
     *******************************************************************************/

    public final static String REQUEST_SINKNODE_LIST = "<SinkNodeLS:>";
    public final static String REQUEST_SENSOR_LIST = "<SensorLS:#>";
    public final static String REQUEST_SENSOR_DATA = "<SensorRSD:<#:#>>";
    public final static String REQUEST_SENSOR_DATA_ALL = "<SensorRASD:#>";
    public final static String CLEAR_ALL_COOKIES = "<Cookie::>";


    public final static String RESPOND_PREFIX_GUEST_INFO = "GUEST:";
    public final static String RESPOND_PREFIX_CONNECTED = "Connected:";
    public final static String RESPOND_PREFIX_DISCONNECTED = "Disconnected:";
    public final static String RESPOND_PREFIX_SINKNODE_LIST = "SinkNodeLS:";
    public final static String RESPOND_PREFIX_SENSOR_LIST = "SensorLS:";
    public final static String RESPOND_PREFIX_SENSOR_DATA = "SensorRSD:";


    private static StringBuffer buffer = new StringBuffer();
    private static Map<String, TouristThread> cookieMap = new HashMap<>();
    private static SerialComm serialComm = SerialComm.getSerialComm();
    public static SerialPort comm;
    public static Boolean commSwitch=false;
    private static RealTimeChart realTimeChart;
    private static Node nodeForChart;

    public static void setChart(RealTimeChart realTimeChart2,Node nodeForChart2){
        realTimeChart=realTimeChart2;
        nodeForChart=nodeForChart2;
    }

    public static Boolean parseRawData(byte[] bytes, SerialPort comm2,NodeTree nodeTree) {
        comm = comm2;
        String data = new String(bytes);
        List<String> groups;
        try {
            groups = extractGroups(buffer.append(data).toString());
        } catch (Exception e) {
            buffer = new StringBuffer();
            return false;
        }
        if (groups == null) {
            return false;
        } else {
            buffer = new StringBuffer();
        }
        for (String t : groups) {
            String[] f = t.split(":");
            if (f.length < 2)
                continue;
            if (t.startsWith(RESPOND_PREFIX_GUEST_INFO)) {
                GuestHandler(t.replace(RESPOND_PREFIX_GUEST_INFO, ""));
            } else if (t.startsWith(RESPOND_PREFIX_CONNECTED)) {
                String cookie = t.replace(RESPOND_PREFIX_CONNECTED, "");
                TouristThread thread = cookieMap.get(cookie);
                if (thread != null) {
                    thread.connectCount = 10;
                    System.out.println("【Cookie:" + cookie + "】 Refresh CountDown!");
                }
            } else if (t.startsWith(RESPOND_PREFIX_DISCONNECTED)) {
                String cookie = t.replace(RESPOND_PREFIX_DISCONNECTED, "");
                TouristThread thread = cookieMap.get(cookie);
                if (thread != null) {
                    thread.connectCount = -10;
                    System.out.println("【Cookie:" + cookie + "】 Connection Shutdown");
                }
                break;
            } else if (t.startsWith(RESPOND_PREFIX_SINKNODE_LIST)) {
                sinkNodeListHandler(t.replace(RESPOND_PREFIX_SINKNODE_LIST, ""),nodeTree);
            } else if (t.startsWith(RESPOND_PREFIX_SENSOR_LIST)) {
                nodeListHandler(t.replace(RESPOND_PREFIX_SENSOR_LIST, ""),nodeTree);
            } else if (t.startsWith(RESPOND_PREFIX_SENSOR_DATA)) {
                sensorDataHandler(t.replace(RESPOND_PREFIX_SENSOR_DATA, ""),nodeTree);
            }
        }
        return true;
    }


    private static Boolean GuestHandler(String guestField) {
        List<String> groups;
        try {
            groups = extractGroups(guestField);
        } catch (Exception e) {
            buffer = new StringBuffer();
            return false;
        }
        if (groups.size() != 2)
            return false;
        String[] firstgroup = groups.get(0).split(":");
        String[] Cookiegroup = groups.get(1).split(":");

        if (firstgroup.length == 2 && Cookiegroup.length == 2 ) {
            if(firstgroup[0].equals("IMEI")&&firstgroup[1].matches("[0-9]{15}")) {
                String IMEI = firstgroup[1];
                String cookie = Cookiegroup[1];
                try {
                    TouristThread touristThread = new TouristThread(IMEI, cookie);
                    cookieMap.put(cookie, touristThread);
                    touristThread.start();
                } catch (NumberFormatException e) {}
            }else if(firstgroup[0].equals("Phone")&&firstgroup[1].matches("\\+*[0-9]+")){
                String phone=firstgroup[1];
                String cookie=Cookiegroup[1];
                cookieMap.get(cookie).tourist.setPhoneNumber(phone);
                System.out.println("【New Phone】："+phone);
            }
        }
        return true;
    }

    //SinkNodeLS:Address
    private static Boolean sinkNodeListHandler(String sinkNodeAddress,NodeTree nodeTree) {
        SinkNode newSinkNode=new SinkNode("",sinkNodeAddress,"");
        if(nodeTree.addSinkNode(newSinkNode)) {
            System.out.println("【New SinkNode】" + "Address: " + sinkNodeAddress);
            requestNodeList(newSinkNode);
            return true;
        }
        requestNodeList(newSinkNode);
        return false;
    }


    //<Address:<%d:%d>><Name:%s>
    private static Boolean nodeListHandler(String nodeListField,NodeTree nodeTree) {
        try {
            List<String> groups;
            groups = extractGroups(nodeListField);
            if (groups.size() != 2)
                return false;
            String addressField = groups.get(0);
            String nameField = groups.get(1);
            if (addressField.startsWith("Address:") && nameField.startsWith("Name:")) {
                addressField.replace("Address:", "");
                List<String> groups2;
                groups2 = extractGroups(addressField);
                String[] addresses = groups2.get(0).split(":");
                if (addresses.length != 2)
                    return false;
                String parentAddress = addresses[0];
                String sensorAddress = addresses[1];
                String name=nameField.replace("Name:","");
                if(nodeTree.addNode(new Node(parentAddress,name,"",sensorAddress))){
                    System.out.println("【New Sensor】sensorAddress:" + sensorAddress+" SensorName:"+name+" parentAddress:"+ parentAddress);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    //返回<Address:<%d:%d>><Data:%f>
    public static Boolean sensorDataHandler(String sensorDataField,NodeTree nodeTree) {
        try {
            List<String> groups;
            groups = extractGroups(sensorDataField);
            if (groups.size() != 2)
                return false;
            String addressField = groups.get(0);
            String dataField = groups.get(1);
            if (addressField.startsWith("Address:") && dataField.startsWith("Data:")) {
                addressField.replace("Address:", "");
                List<String> groups2;
                groups2 = extractGroups(addressField);
                String[] addresses = groups2.get(0).split(":");
                if (addresses.length != 2)
                    return false;
                String parentAddress = addresses[0];
                String sensorAddress = addresses[1];
                Node sensor=nodeTree.searchNode(parentAddress,sensorAddress);
                if(sensor!=null) {
                    String data = dataField.replace("Data:", "");
                    Float value = Float.parseFloat(data);
                    System.out.println("【New Data】sensorAddress:" + sensorAddress + " SensorData:" + value + " parentAddress:" + parentAddress);
                    Date now=new Date();
                    sensor.addNewValue(now,value);
                    if(nodeForChart!=null&&nodeForChart.getAddress().equals(sensorAddress)&&realTimeChart!=null){
                        realTimeChart.addNewData(now,value);
                        SwingUtilities.invokeLater(()->realTimeChart.getChartPanel().updateUI());
                    }
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }


    static class TouristThread extends Thread {

        String IMEI;
        String cookie;

        Tourist tourist;
        Integer connectCount = 10;

        TouristThread(String IMEI, String cookie) {
            super();
            tourist = new Tourist(IMEI, "", "", "");
            this.IMEI=IMEI;
            this.cookie = cookie;
        }

        @Override
        public void run() {
            tourist.connect();
            System.out.println("【Cookie:" + cookie + "】 Connected!");
            while (connectCount-- > 0) {
                try {
                    Thread.sleep(3000);
                    System.out.println("【Cookie:" + cookie + "】 CountDown:" + connectCount);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("【Cookie:" + cookie + "】 Disconnected!");
            tourist.disconnect();
            cookieMap.remove(cookie);
            System.out.println("cookieMapSize:" + cookieMap.size());
            if (cookieMap.size() == 0) {
                clearCookie(":");
            } else {
                if (connectCount > -10) {
                    clearCookie(cookie);
                }
            }
        }
    }

    public static void clearCookie(String cookie) {
        System.out.println("Clear cookie:" + cookie);
        byte c;
        if (cookie.equals(":")) {
            c = ':';
        } else {
            int ck = Integer.parseInt(cookie);
            if (ck >= 128)
                c = (byte) (ck - 256);
            else
                c = (byte) (ck);
        }
        byte[] s = "<Cookie:".getBytes();
        byte[] e = ">".getBytes();
        byte[] data = Arrays.copyOf(s, s.length + 1 + e.length);
        data[s.length] = c;
        System.arraycopy(e, 0, data, s.length + 1, e.length);
        System.out.println("Clear cookie array:" + Arrays.toString(data));
        System.out.println("Clear cookie string:" + new String(data));
        serialComm.sendToPort(comm, data);
    }


    public static void requestSinkNodeList() {
        serialComm.sendToPort(comm, REQUEST_SINKNODE_LIST.getBytes());
    }

    public static void requestNodeList(SinkNode sinkNode) {
        serialComm.sendToPort(comm, REQUEST_SENSOR_LIST.replaceAll("#", sinkNode.getAddress()).getBytes());
    }

    public static void requestSensorData(SinkNode sinkNode, Node node) {
        serialComm.sendToPort(comm, REQUEST_SENSOR_DATA.replaceFirst("#", sinkNode.getAddress()).replaceFirst("#", node.getParent()).getBytes());
    }

    public static void requestSensorDataAll(SinkNode sinkNode) {
        serialComm.sendToPort(comm, REQUEST_SENSOR_DATA_ALL.replaceFirst("#", sinkNode.getAddress()).getBytes());
    }

    public static void requestSensorDataAll(String sinkNodeAddress) {
        serialComm.sendToPort(comm, REQUEST_SENSOR_DATA_ALL.replaceFirst("#", sinkNodeAddress).getBytes());
    }


    private static List<String> extractGroups(String s) throws Exception {
        List<String> groups = new ArrayList<>();
        int count = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                if (count == 0) {
                    start = i + 1;
                }
                count++;
            } else if (c == '>') {
                count--;
                if (count == 0) {
                    groups.add(s.substring(start, i));
                } else if (count < 0)
                    throw new Exception();
            }
        }
        if (count == 0)
            return groups;
        return null;
    }


    public static List<Command> presetCommand(){
        List<Command> commands=new ArrayList<>();
         Command command=new Command(REQUEST_SINKNODE_LIST,"请求所有Sink节点地址列表");
         commands.add(command);
         command=new Command(REQUEST_SENSOR_LIST,"请求地址为#的Sink节点的所有传感器列表。(#取值0~255)");
         commands.add(command);
         command=new Command(REQUEST_SENSOR_DATA,"请求传感器数据，第一个#为其所属Sink地址，第二个#为传感器地址。(#取值0~255)");
         commands.add(command);
         command=new Command(REQUEST_SENSOR_DATA_ALL,"请求地址为#的Sink节点下所有传感器数据");
         commands.add(command);
         command=new Command(CLEAR_ALL_COOKIES,"清除所有Cookies");
         commands.add(command);
         return commands;
    }


    public static class Command implements Bean {
        public static final String KEY_COMMAND="指令";
        public static final String KEY_COMMENT="注释";

        private String command;
        private String comment;

        public Command(){}

        public Command(String command,String comment){
            this.command=command;
            this.comment=comment;
        }

        public Command(Map<String,Object> map){
            setFields(map);
        }

        @Override
        public List<String> getFieldNames() {
            List<String> fields=new ArrayList<>();
            fields.add(KEY_COMMAND);
            fields.add(KEY_COMMENT);
            return fields;
        }

        @Override
        public void setFields(Map<String, Object> map) {
            Object o=map.get(KEY_COMMAND);
            if(o!=null) {
                command = (String) o;
            }
            o=map.get(KEY_COMMENT);
            if(o!=null) {
                comment = (String)o;
            }
        }

        @Override
        public Map<String, Object> toTable() {
            Map<String,Object> map=new HashMap<>();
            map.put(KEY_COMMAND,command);
            map.put(KEY_COMMENT,comment);
            return map;
        }

        @Override
        public Bean getCopy() {
            return new Command(command,comment);
        }
    }
}

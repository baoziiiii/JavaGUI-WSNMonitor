package com.qq452651705.SerialComm;

import java.util.*;

import com.qq452651705.DataMGM.Node.NodeTree;
import com.qq452651705.DataMGM.Tourists.TouristManager.Tourist;
import com.qq452651705.GUI.RealTimeChart;
import com.qq452651705.Utils.TBean;
import gnu.io.SerialPort;
import com.qq452651705.DataMGM.Node.NodeTree.SinkNode;
import com.qq452651705.DataMGM.Node.NodeTree.Node;

import javax.swing.*;


/**
 * The type Comm handler.  串口数据格式服务类
 */
public class CommHandler {

    /*******************************上位机通信数据格式************************************
     * <属性名称:属性值>
     * 支持多层嵌套：<属性名称：<子属性名称1:子属性值1><子属性名称2：子属性值2>> 实现多个数据之间的绑定
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
     */


    /*******************************************发送指令宏定义*******************************************/

    /**
     * The constant REQUEST_SINKNODE_LIST.
     */
    public final static String REQUEST_SINKNODE_LIST = "<SinkNodeLS:>";
    /**
     * The constant REQUEST_SENSOR_LIST.
     */
    public final static String REQUEST_SENSOR_LIST = "<SensorLS:#>";
    /**
     * The constant REQUEST_SENSOR_DATA.
     */
    public final static String REQUEST_SENSOR_DATA = "<SensorRSD:<#:#>>";
    /**
     * The constant REQUEST_SENSOR_DATA_ALL.
     */
    public final static String REQUEST_SENSOR_DATA_ALL = "<SensorRASD:#>";
    /**
     * The constant CLEAR_ALL_COOKIES.
     */
    public final static String CLEAR_ALL_COOKIES = "<Cookie::>";

    /*******************************************发送指令宏定义*******************************************/


    /*******************************************接受指令前缀*******************************************/

    /**
     * The constant RESPOND_PREFIX_GUEST_INFO.
     */
    public final static String RESPOND_PREFIX_GUEST_INFO = "GUEST:";
    /**
     * The constant RESPOND_PREFIX_CONNECTED.
     */
    public final static String RESPOND_PREFIX_CONNECTED = "Connected:";
    /**
     * The constant RESPOND_PREFIX_DISCONNECTED.
     */
    public final static String RESPOND_PREFIX_DISCONNECTED = "Disconnected:";
    /**
     * The constant RESPOND_PREFIX_SINKNODE_LIST.
     */
    public final static String RESPOND_PREFIX_SINKNODE_LIST = "SinkNodeLS:";
    /**
     * The constant RESPOND_PREFIX_SENSOR_LIST.
     */
    public final static String RESPOND_PREFIX_SENSOR_LIST = "SensorLS:";
    /**
     * The constant RESPOND_PREFIX_SENSOR_DATA.
     */
    public final static String RESPOND_PREFIX_SENSOR_DATA = "SensorRSD:";

    /*******************************************接受指令前缀*******************************************/




    /******************************************Cookie机制*******************************************/
    /**
     *   cookie是单片机给安卓设备分配的单字节标识码，最多有255-4=251个cookie，编号从0~255(去掉0,'<','>',':')。
     *   每一个和单片机保持蓝牙伪连接的安卓设备都会占用一个cookie，即cookie机制最多允许同一时刻保持251个安卓设备的蓝牙通信。
     *   所以cookie需要在断开通信时释放。
     *
     *   安卓设备通过唯一的IMEI号向单片机请求建立蓝牙通信通道，单片机会寻找一个可用的cookie作为应答信号的一部分回复安卓
     *   设备。同时将这个cookie与IMEI号通过GUEST指令打包一起传输给上位机，上位机收到后会使用该IMEI号和绑定的cookie，
     *   创建一个独立的新线程touristThread管理该cookie的通信情况，并在cookieMap中注册。每个线程管理一个倒计时,
     *   安卓设备收到cookie之后，每一次向单片机请求数据都会携带被分配的cookie来标识请求的身份,单片机通过Connected指令将
     *   cookie发送给上位机,上位机收到该cookie后从cookieMap中找到其绑定的线程,并刷新其中的倒计时.如果倒计时过久没有被
     *   刷新,从而计数到0则判断连接超时,关闭和注销该线程,同时回复Cookie指令给单片机释放该cookie的占用.
     *
     *
     *  @param cookieMap  cookie注册表 touristThread管理一个倒计时.倒计时总时长COUNT_DOWN*COUNT_DOWN_INTERVAL,单位ms
     */
    private static Map<String, TouristThread> cookieMap = new HashMap<>();

    private static final Integer COUNT_DOWN=10;
    private static final Integer COUNT_DOWN_INTERVAL=3000;

    /******************************************Cookie机制*******************************************/


    /**
     * The constant comm.   串口实例
     */
    private static SerialComm serialComm = SerialComm.getSerialComm();
    /**
     * The constant comm.  串口号对象
     */
    public static SerialPort comm;

    /**
     * The constant commSwitch.  串口开关
     */
    public static Boolean commSwitch=false;




    /**
     * Set chart.   图表对象,便于接收到数据更新图表.
     *
     * @param realTimeChart2 the real time chart 2
     * @param nodeForChart2  the node for chart 2
     */
    private static RealTimeChart realTimeChart;
    private static Node nodeForChart;
    public static void setChart(RealTimeChart realTimeChart2,Node nodeForChart2){
        realTimeChart=realTimeChart2;
        nodeForChart=nodeForChart2;
    }


    /**
     *   extractGroups <>指令格式解析,将第一层的<>删去,从而提取其中的内容,可同时处理多条第一层指令
     *                比如接收到<SinkNodeLS:0><SensorLS:<Address:<0:0>><Name:Temperature>>,会提取出两条第一层内容.
     *                第一条SinkNodeLS:0,第二条SensorLS:<Address:<0:0>><Name:Temperature> .第二条指令中有第二层内容,
     *                则重复调用extractGroups即可提取.
     *
     *                鉴于串口通信可能会把一条字符串自动拆分成多条指令,所以引入缓存机制以及指令纠错机制
     *                extractGroups会对各种错误情况进行处理,当处理过程中'>'符号数量大于'<'，则判断指令出现不可挽回的错误,
     *                则抛弃清空缓存.当一次处理完毕,发现'>'小于'<',则表示数据可能不完整,需要保留缓存内容,等待下一次串口数据,
     *                下一次串口接收到数据,会将数据添加到缓存末尾,extractGroup将从头开始处理缓存.当一次处理完毕‘<’等于'>'
     *                则指令处理成功.
     *
     *
     *   @params buffer   缓存
     *   @params s        处理指令
     *   @return List<String>   返回null:'<'大于'>'指令不完整
     *   @Exception Exception   Exception:'<'小于'>'指令出错
     */
    private static StringBuffer buffer = new StringBuffer();


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

    /**
     * Parse raw data.                 未处理串口接收数据解析.调用extractGroups提取一个或多个第一层内容,并识别其中的指令进行分别处理.
     * @param bytes    the bytes       未处理串口接收数据
     * @param com    the com           当前串口号
     * @param nodeTree the node tree   用于同步节点树
     * @return the boolean             false:处理未成功
     */
    public static Boolean parseRawData(byte[] bytes, SerialPort com,NodeTree nodeTree) {
        comm = com;
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
                    thread.connectCount = COUNT_DOWN;
                    System.out.println("【Cookie:" + cookie + "】 Refresh CountDown!");
                }
            } else if (t.startsWith(RESPOND_PREFIX_DISCONNECTED)) {
                String cookie = t.replace(RESPOND_PREFIX_DISCONNECTED, "");
                TouristThread thread = cookieMap.get(cookie);
                if (thread != null) {
                    thread.connectCount = 0-COUNT_DOWN;
                    System.out.println("【Cookie:" + cookie + "】 Connection Shutdown");
                }
                break;
            } else if (t.startsWith(RESPOND_PREFIX_SINKNODE_LIST)) {
                sinkNodeListHandler(t.replace(RESPOND_PREFIX_SINKNODE_LIST, ""),nodeTree);
            } else if (t.startsWith(RESPOND_PREFIX_SENSOR_LIST)) {
                sensorListHandler(t.replace(RESPOND_PREFIX_SENSOR_LIST, ""),nodeTree);
            } else if (t.startsWith(RESPOND_PREFIX_SENSOR_DATA)) {
                sensorDataHandler(t.replace(RESPOND_PREFIX_SENSOR_DATA, ""),nodeTree);
            }
        }
        return true;
    }


    /** GUEST指令处理
     * <GUEST:<IMEI:%s{15}><Cookie:%d>> : 通知上位机发生新连接。%s{15}为15个字节的IMEI号，%d为给该IMEI所分配的cookie字符串格式
     * <GUEST:<PHONE:%s><Cookie:%d>> ：通知上位机来自APP发来的手机号。
     * @param guestField   guest属性值(已经在parseRawData中剥离'GUEST:'前缀)
     * @return
     */
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

    /** SinkNodeLS指令
     * <SinkNodeLS:%d> ：地址为%d的Sink节点可用。
     * @return
     */
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


    /** SensorLS指令处理
     *  <SensorLS:<Address:<%d1:%d2>><Name:%s>> ：应答上位机地址为%d1的Sink节点下的地址为%d2的传感器节点可用，其英文名称为%s。
     * @return
     */
    private static Boolean sensorListHandler(String nodeListField, NodeTree nodeTree) {
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

    /**
     * Sensor data handler boolean. SensorRSD指令处理
     * <SensorRSD:<Address:<%d1:%d2>><Data:%f>> : 应答上位机地址为%d1:%d2的传感器节点的数据。
     *
     * @param sensorDataField the sensor data field
     * @param nodeTree        the node tree
     * @return the boolean
     */
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


    /**
     * The type Tourist thread.  游客线程
     */
    static class TouristThread extends Thread {

        /**
         * The Imei.
         */
        String IMEI;
        /**
         * The Cookie.
         */
        String cookie;

        /**
         * The Tourist.
         */
        Tourist tourist;
        /**
         * The Connect count.  倒计时COUNT_DOWN*COUNT_DOWN_INTERVAL，单位ms
         */
        Integer connectCount = COUNT_DOWN;
        Integer countInterval= COUNT_DOWN_INTERVAL;

        /**
         * Instantiates a new Tourist thread. 游客线程初始化.
         *
         * @param IMEI   the imei
         * @param cookie the cookie
         */
        TouristThread(String IMEI, String cookie) {
            super();
            tourist = new Tourist(IMEI, "", "", "");
            this.IMEI=IMEI;
            this.cookie = cookie;
        }

        /**
         *   游客线程主函数
         *
         */
        @Override
        public void run() {
            tourist.connect();
            System.out.println("【Cookie:" + cookie + "】 Connected!");
            while (connectCount-- > 0) {
                try {
                    Thread.sleep(countInterval);
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
                if (connectCount > -COUNT_DOWN) {
                    clearCookie(cookie);
                }
            }
        }
    }

    /**
     *
     * Clear cookie.  通知单片机释放cookie
     * <Cookie:%c> : 通知单片机释放编号为%c的cookie。%c为值为0~255的单个字节 ; 当%c为':'，通知单片机清除所有cookie。
     *
     * @param cookie the cookie
     */
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



    /**
     * <SinkNodeLS:>  : 请求所有Sink节点地址。返回n个<SinkNodeLS:Address>。
     * <SensorLS:%d>    :  请求地址为%d的Sink节点下所有的传感器地址及名称。返回<SensorLS:<Address:<%d:%d>><Name:%s>>。
     * <SensorRSD:<%d:%d>> : 请求地址为%d(Sink节点地址):%d(传感器地址)的数据。返回<SensorRSD:<Address:<%d:%d>><Data:%f>>
     * <SensorRASD:%d>  :   请求地址为%d的Sink节点下所有传感器的值。返回n个<SensorRSD:<Address:<%d:%d>><Data:%f>>
     * */

    /**
     * Request sink node list.
     */
    public static void requestSinkNodeList() {
        serialComm.sendToPort(comm, REQUEST_SINKNODE_LIST.getBytes());
    }

    /**
     * Request node list.
     *
     * @param sinkNode the sink node
     */
    public static void requestNodeList(SinkNode sinkNode) {
        serialComm.sendToPort(comm, REQUEST_SENSOR_LIST.replaceAll("#", sinkNode.getAddress()).getBytes());
    }

    /**
     * Request sensor data.   上位机会定时向单片机请求数据
     * @See MainActivity.SerialCommThread
     *
     * @param sinkNode the sink node
     * @param node     the node
     */
    public static void requestSensorData(SinkNode sinkNode, Node node) {
        serialComm.sendToPort(comm, REQUEST_SENSOR_DATA.replaceFirst("#", sinkNode.getAddress()).replaceFirst("#", node.getParent()).getBytes());
    }

    /**
     * Request sensor data all.
     *
     * @param sinkNodeAddress the sink node address
     * @See MainActivity.SerialCommThread
     */
    public static void requestSensorDataAll(String sinkNodeAddress) {
        serialComm.sendToPort(comm, REQUEST_SENSOR_DATA_ALL.replaceFirst("#", sinkNodeAddress).getBytes());
    }




    /**
     * Preset command list.  预设指令集(用于监测控制界面下的指令集设置)
     *
     * @return the list
     */
    public static List<Command> presetCommand(List<Command> commands){
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


    /**
     * The type Command.   指令类.(用于监测控制界面下的指令集设置）包含指令和指令注释
     */
    public static class Command implements TBean {
        /**
         * The constant KEY_COMMAND.
         */
        public static final String KEY_COMMAND="指令";
        /**
         * The constant KEY_COMMENT.
         */
        public static final String KEY_COMMENT="注释";

        private String command;
        private String comment;

        /**
         * Instantiates a new Command.
         */
        public Command(){}

        /**
         * Instantiates a new Command.
         *
         * @param command the command
         * @param comment the comment
         */
        public Command(String command,String comment){
            this.command=command;
            this.comment=comment;
        }

        /**
         * Instantiates a new Command.
         *
         * @param map the map
         */
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
        public TBean getCopy() {
            return new Command(command,comment);
        }
    }
}

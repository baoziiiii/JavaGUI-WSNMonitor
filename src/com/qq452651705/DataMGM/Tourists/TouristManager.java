package com.qq452651705.DataMGM.Tourists;

import com.qq452651705.GUI.BeanTableModel;
import com.qq452651705.JDBC.Exception.MySQLException;
import com.qq452651705.JDBC.MyDatabase;
import com.qq452651705.Utils.TBean;

import javax.swing.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * The type Tourist manager.   管理游客信息与数据库的同步与交互
 */
public class TouristManager {

    private static MyDatabase myDatabase = MyDatabase.getMyDatabase();

    private static TouristManager touristManager=new TouristManager();

    private TouristManager(){
        System.out.println("new TouristManager");
    }

    /**
     * Gets tourist manager.  Singleton
     *˝
     * @return the tourist manager
     */
    public static TouristManager getTouristManager() {
        return touristManager;
    }


    /**
     * 表1: 游客信息表
     * -------主键-----------------------------------
     * 列:   IMEI号   姓名   手机电话   其他   注册时间
     */
    public final static String TABLE_TOURIST = "TouristInfo";
    /**
     * The constant KEY_TOURIST_IMEI.
     */
    public static final String KEY_TOURIST_IMEI="IMEI";
    /**
     * The constant KEY_TOURIST_NAME.
     */
    public static final String KEY_TOURIST_NAME="姓名";
    /**
     * The constant KEY_TOURIST_PHONE.
     */
    public static final String KEY_TOURIST_PHONE="手机电话";
    /**
     * The constant KEY_TOURIST_DETAIL.
     */
    public static final String KEY_TOURIST_DETAIL="其他";
    /**
     * The constant KEY_TOURIST_REGISTER_TIME.
     */
    public static final String KEY_TOURIST_REGISTER_TIME="注册时间";

    /**
     * 表2: 游客连接记录表
     * -------主键------主键---------------------
     * 列:   IMEI号   连接时刻   断开时刻
     *
     */

    /**
     * The constant TABLE_CONNECT.
     */
    public final static String TABLE_CONNECT = "TouristConnect";
    /**
     * The constant KEY_CONNECT_START.
     */
    public static final String KEY_CONNECT_START="连接时刻";
    /**
     * The constant KEY_CONNECT_END.
     */
    public static final String KEY_CONNECT_END="断开时刻";

    static {
        Map<String, String> name_type_map = new HashMap<>();
        List<String> primaryKey = new ArrayList<>();
        try {
            name_type_map.put(KEY_TOURIST_NAME, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_TOURIST_PHONE, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_TOURIST_DETAIL, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_TOURIST_IMEI, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_TOURIST_REGISTER_TIME,MyDatabase.TYPE_TIMESTAMP);
            primaryKey.add(KEY_TOURIST_IMEI);
            myDatabase.createTable(TABLE_TOURIST, name_type_map, primaryKey, null);
        }catch (MySQLException e){
        }
        try {
            name_type_map.clear();
            primaryKey.clear();
            name_type_map.put(KEY_CONNECT_START, MyDatabase.TYPE_DATETIME);
            name_type_map.put(KEY_CONNECT_END, MyDatabase.TYPE_DATETIME);
            name_type_map.put(KEY_TOURIST_IMEI,MyDatabase.TYPE_VARCHAR);
            primaryKey.add(KEY_CONNECT_START);
            primaryKey.add(KEY_TOURIST_IMEI);
            myDatabase.createTable(TABLE_CONNECT, name_type_map, primaryKey, null);
        }catch (SQLException e){
            System.out.println(e.getErrorCode());
        }
    }

    /**
     *  游客信息列名
     */
    private static List<String> touristInfofields;

    /**
     *  游客信息缓存列表（用于显示）
     */
    private static List<TBean> touristInfoList =new ArrayList<>();

    /**
     *  创建自定义的JTable模型BeanTable，将其与游客信息缓存列表绑定
     */
    private static BeanTableModel<Tourist> beanTableModel =new BeanTableModel(new Tourist().getFieldNames(), touristInfoList,false);

    /**
     *  创建Swing表格组件JTable，将其与BeanTableb绑定
     */
    private static JTable jTable=new JTable(beanTableModel);

    /**
     * Sync by database.  从touristinfo数据表同步游客信息.
     */
    public void syncByDatabase(){
        ResultSet resultSet = null;
        try {
            resultSet = myDatabase.query("SELECT * FROM " + TABLE_TOURIST,null);
            while (resultSet.next()) {
                Map<String,Object> map=new HashMap<>();
                map.put(KEY_TOURIST_NAME,resultSet.getString(KEY_TOURIST_NAME));
                map.put(KEY_TOURIST_PHONE,resultSet.getString(KEY_TOURIST_PHONE));
                map.put(KEY_TOURIST_DETAIL,resultSet.getString(KEY_TOURIST_DETAIL));
                map.put(KEY_TOURIST_IMEI,resultSet.getString(KEY_TOURIST_IMEI));
                touristInfoList.add(new Tourist(map));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                    myDatabase.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Clear database.   清空游客信息表
     */
    public void clearDatabase(){
        try {
            myDatabase.clearTable(TABLE_TOURIST);
            myDatabase.clearTable(TABLE_CONNECT);
        } catch (MySQLException e) {
            System.out.println(e.print());
        }
    }

    /**
     * Add tourist.     向链表缓存以及数据库同时添加一个游客
     *
     * @param tourist the tourist
     * @return the boolean   false:重复主键IMEI
     */
    public Boolean addTourist(Tourist tourist){
        Map<String, Object> map = tourist.toTable();
        try {
            myDatabase.insertRow(TABLE_TOURIST, map);
            touristInfoList.add(tourist);            //注意顺序,防止重复主键（重复跳异常）
            System.out.println("add new tourist success");
            return true;
        } catch (MySQLException e) {
            return false;
        }
    }

    /**
     * Remove tourist.    移除一个游客(输入Tourist只需要一个IMEI属性)
     *
     * @param tourist_IMEI the tourist_IMEI
     * @return the boolean
     */
    public Boolean removeTourist(String tourist_IMEI) {
        String where = KEY_TOURIST_IMEI + " = \'" + tourist_IMEI+"\'";
        try {
            myDatabase.deleteRow(TABLE_TOURIST,where);
            myDatabase.deleteRow(TABLE_CONNECT,where);
            for (TBean tBean : touristInfoList) {
                Tourist tourist1=(Tourist) tBean;
                if(tourist1.IMEI==tourist_IMEI){
                    touristInfoList.remove(tourist1);
                    return true;
                }
            }
        } catch (MySQLException e) {
            e.print();
            return false;
        }
        return false;
    }

    /**
     * Connect tourist.        启动一个游客连接,向tableconnect表中插入新的连接记录
     *
     * @param tourist the tourist  游客IMEI作为主键
     * @param start   the start    连接时刻作为主键
     * @return the boolean
     */
    public Boolean connectTourist(Tourist tourist,Date start){
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_TOURIST_IMEI,tourist.IMEI);
        map.put(KEY_CONNECT_START,new Timestamp(start.getTime()));
        try {
            myDatabase.insertRow(TABLE_CONNECT, map);
            return true;
        } catch (MySQLException e) {
            e.print();
            return false;
        }
    }

    /**
     * Disconnect tourist.          游客连接断开,通过IMEI和连接时刻更新tableconnect表中相应的断开时刻字段
     *
     * @param tourist the tourist
     * @param start   the start
     * @param end     the end       连接断开时刻
     * @return the boolean
     */
    public Boolean disconnectTourist(Tourist tourist, Date start ,Date end){
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_TOURIST_IMEI,tourist.IMEI);
        map.put(KEY_CONNECT_END,new Timestamp(end.getTime()));
        try {
            myDatabase.updateRow(TABLE_CONNECT, map,KEY_TOURIST_IMEI+" = \'"+tourist.IMEI+"\' AND "+KEY_CONNECT_START+" = \'"+MyDatabase.dateTimeFormmatter(start)+"\'");
            return true;
        } catch (MySQLException e) {
            e.print();
            return false;
        }
    }


    /**
     * Update tourist info.  更新游客信息
     *
     * @param map the map
     * @return the boolean
     */
    public Boolean updateTouristInfo(Map<String,Object> map){
        String IMEI=(String) map.get(KEY_TOURIST_IMEI);
        map.remove(KEY_TOURIST_IMEI);
        try {
            myDatabase.updateRow(TABLE_TOURIST, map,KEY_TOURIST_IMEI+" = \'"+IMEI+"\'");
            return true;
        } catch (MySQLException e) {
            e.print();
            return false;
        }
    }

    /**
     * Search tourist by imei.  通过IMEI从缓存列表中获取游客对象
     *
     * @param IMEI the imei
     * @return the tourist
     */
    public Tourist searchTouristByIMEI(String IMEI){
        for (TBean tourist: touristInfoList) {
             if(((Tourist) tourist).IMEI.equals(IMEI)){
                 return (Tourist) tourist;
             }
        }
        return null;
    }

    /**
     * Look all connect log list. 获取touristconnect表所有记录
     *
     * @return the list
     * @See lookConnectLog
     */
    public List<TBean> lookAllConnectLog(){
        return lookConnectLog(null);
    }

    /**
     * Look connect log list.   获取游客的连接记录.输入null返回所有游客的登陆记录
     *
     * @param tourist_IMEI the tourist IMEI   tourist_IMEI=null:返回所有游客的登陆记录
     * @return the list
     */
    public List<TBean> lookConnectLog(String tourist_IMEI){
        String sql="SELECT * FROM "+TABLE_CONNECT;
        if(tourist_IMEI!=null) {
            String where = " WHERE "+KEY_TOURIST_IMEI + " = \'" + tourist_IMEI + "\'";
            sql=sql+where;
        }
        ResultSet resultSet=null;
        List<TBean> logs=new ArrayList<>();
        try{
            resultSet=myDatabase.query(sql,null);
            while(resultSet.next()){
                Map<String,Object> map=new HashMap<>();
                Timestamp connecttime=resultSet.getTimestamp(KEY_CONNECT_START);
                Timestamp disconnecttime=resultSet.getTimestamp(KEY_CONNECT_END);
               map.put(KEY_TOURIST_IMEI,resultSet.getString(KEY_TOURIST_IMEI));
               map.put(KEY_CONNECT_START,MyDatabase.timestampFormmatter(connecttime));
               map.put(KEY_CONNECT_END,MyDatabase.timestampFormmatter(disconnecttime));
               logs.add(new ConnectLog(map));
            }
        }catch (MySQLException e){e.print();}
        catch (SQLException e){}
        finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                    myDatabase.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return logs;
    }


    /**
     * Gets tourist list.       获取游客信息缓存链表
     *
     * @return the tourist list
     */
    public List<TBean> getTouristList() {
        return touristInfoList;
    }

    /**
     * Gets j table.           获取与缓存链表绑定的JTable
     *
     * @return the j table
     */
    public JTable getJTable() {
        return jTable;
    }


    /**
     * The type Tourist.   游客类
     */
    public static class Tourist implements TBean {
        /**
         * The Imei.
         */
        String IMEI;
        /**
         * The Name.
         */
        String name;
        /**
         * The Phone number.
         */
        String phoneNumber;
        /**
         * The Detail.
         */
        String detail;
        /**
         * The Registertime.   注册时间
         */
        Timestamp registertime;

        /**
         * The Start.          缓存游客当前连接的连入时刻
         */
        Date start;

        /**
         * Instantiates a new Tourist.
         */
        private Tourist(){}

        /**
         * Instantiates a new Tourist.
         *
         * @param IMEI        the imei
         * @param name        the name
         * @param phoneNumber the phone number
         * @param detail      the detail
         */
        public Tourist(String IMEI,String name,String phoneNumber,String detail){
           this.IMEI=IMEI;
           this.name=name;
           this.phoneNumber=phoneNumber;
           this.detail=detail;
           registertime=new Timestamp(System.currentTimeMillis());
       }

        /**
         * Instantiates a new Tourist.
         *
         * @param map the map
         */
        public Tourist(Map<String,Object> map){
           setFields(map);
           registertime=new Timestamp(System.currentTimeMillis());
       }

        /**
         * Connect.    游客建立连接
         */
        public void connect(){
           touristManager.addTourist(this);
           start=new Date();
           touristManager.connectTourist(this,start);
           SwingUtilities.invokeLater(()-> jTable.updateUI());
       }

        /**
         * Disconnect.  游客断开连接
         */
        public void disconnect(){
           touristManager.disconnectTourist(this,start,new Date());
       }


        /**
         * Sets phone number.  更新游客手机号码信息
         *
         * @param phoneNumber the phone number
         */
        public void setPhoneNumber(String phoneNumber) {
            Tourist tourist=touristManager.searchTouristByIMEI(IMEI);
            tourist.phoneNumber=phoneNumber;
            Map<String,Object> map=new HashMap<>();
            map.put(KEY_TOURIST_IMEI,IMEI);
            map.put(KEY_TOURIST_PHONE,phoneNumber);
            touristManager.updateTouristInfo(map);
            SwingUtilities.invokeLater(()-> jTable.updateUI());
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public List<String> getFieldNames() {
            if(touristInfofields ==null) {
                touristInfofields = new ArrayList<>();
                touristInfofields.add(KEY_TOURIST_NAME);
                touristInfofields.add(KEY_TOURIST_PHONE);
                touristInfofields.add(KEY_TOURIST_DETAIL);
                touristInfofields.add(KEY_TOURIST_IMEI);
                touristInfofields.add(KEY_TOURIST_REGISTER_TIME);
            }
            return touristInfofields;
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public void setFields(Map<String, Object> map) {
           Object o=map.get(KEY_TOURIST_IMEI);
           if(o!=null){
               IMEI=(String)map.get(KEY_TOURIST_IMEI);
           }
           o=map.get(KEY_TOURIST_NAME);
           if(o!=null){
               name=(String) map.get(KEY_TOURIST_NAME);
           }
           o=map.get(KEY_TOURIST_PHONE);
           if(o!=null){
               phoneNumber=(String) map.get(KEY_TOURIST_PHONE);
           }
           o=map.get(KEY_TOURIST_DETAIL);
           if(o!=null){
               detail=(String) map.get(KEY_TOURIST_DETAIL);
           }
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public Map<String, Object> toTable() {
            Map<String,Object> map=new HashMap<>();
            map.put(KEY_TOURIST_NAME,name);
            map.put(KEY_TOURIST_PHONE,phoneNumber);
            map.put(KEY_TOURIST_DETAIL,detail);
            map.put(KEY_TOURIST_IMEI,IMEI);
            map.put(KEY_TOURIST_REGISTER_TIME,MyDatabase.timestampFormmatter(registertime));
            return map;
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public TBean getCopy() {
            return new Tourist(this.toTable());
        }
    }

    /**
     * The type Connect log.  游客连接记录类
     */
    public static class ConnectLog implements TBean {
        /**
         * The Imei.
         */
        String IMEI;
        /**
         * The Connect.   连接时刻
         */
        String connect;
        /**
         * The Disconnect. 断开时刻
         */
        String disconnect;

        /**
         * Instantiates a new Connect log.
         */
        public ConnectLog(){}

        /**
         * Instantiates a new Connect log.
         *
         * @param map the map
         */
        public ConnectLog(Map<String,Object> map){
            setFields(map);
        }

        /**
         * Instantiates a new Connect log.
         *
         * @param IMEI       the imei
         * @param connect    the connect
         * @param disconnect the disconnect
         */
        public ConnectLog(String IMEI,String connect,String disconnect){
            this.IMEI=IMEI;
            this.connect=connect;
            this.disconnect=disconnect;
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public List<String> getFieldNames() {
            List<String> fieldnames=new ArrayList<>();
            fieldnames.add(KEY_TOURIST_IMEI);
            fieldnames.add(KEY_CONNECT_START);
            fieldnames.add(KEY_CONNECT_END);
            return fieldnames;
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public void setFields(Map<String, Object> map) {
            IMEI=(String)map.get(KEY_TOURIST_IMEI);
            connect=(String)map.get(KEY_CONNECT_START);
            disconnect=(String)map.get(KEY_CONNECT_END);
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public Map<String, Object> toTable() {
            Map<String,Object> map=new HashMap<>();
            map.put(KEY_TOURIST_IMEI,IMEI);
            map.put(KEY_CONNECT_START,connect);
            map.put(KEY_CONNECT_END,disconnect);
            return map;
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public TBean getCopy() {
            return null;
        }
    }
}

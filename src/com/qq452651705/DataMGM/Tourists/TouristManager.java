package com.qq452651705.DataMGM.Tourists;

import com.qq452651705.GUI.BeanTable;
import com.qq452651705.JDBC.Exception.MySQLException;
import com.qq452651705.JDBC.MyDatabase;
import com.qq452651705.Utils.Bean;

import javax.swing.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class TouristManager {


    private static MyDatabase myDatabase = MyDatabase.getMyDatabase();
    private static TouristManager touristManager=new TouristManager();

    private TouristManager(){
        System.out.println("new TouristManager");
    }

    public static TouristManager getTouristManager() {
        return touristManager;
    }



    public static final String KEY_TOURIST_IMEI="IMEI";
    public static final String KEY_TOURIST_NAME="姓名";
    public static final String KEY_TOURIST_PHONE="手机电话";
    public static final String KEY_TOURIST_DETAIL="其他";
    public static final String KEY_TOURIST_REGISTER_TIME="注册时间";

    public static final String KEY_CONNECT_START="连接时刻";
    public static final String KEY_CONNECT_END="断开时刻";


    public final static String TABLE_TOURIST = "TouristInfo";
    public final static String TABLE_CONNECT = "TouristConnect";


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
    private static List<String> fields;
    private static List<Bean> touristList=new ArrayList<>();
    private static BeanTable<Tourist> beanTable =new BeanTable(new Tourist().getFieldNames(),touristList);
    private static JTable jTable=new JTable(beanTable);

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
                touristList.add(new Tourist(map));
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

    public void clearDatabase(){
        try {
            myDatabase.clearTable(TABLE_TOURIST);
        } catch (MySQLException e) {
            System.out.println(e.print());
        }
    }

    public Boolean addTourist(Tourist tourist){
        Map<String, Object> map = tourist.toTable();
        try {
            myDatabase.insertRow(TABLE_TOURIST, map);
            touristList.add(tourist);//防止重复主键（重复跳异常）
            System.out.println("add new tourist success");
            return true;
        } catch (MySQLException e) {
            return false;
        }
    }

    public Boolean connectTourist(Tourist tourist,Date start){
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_TOURIST_IMEI,tourist.IMEI);
        map.put(KEY_CONNECT_START,new Timestamp(start.getTime()));
        try {
            myDatabase.insertRow(TABLE_CONNECT, map);
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean disconnectTourist(Tourist tourist, Date start ,Date end){
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_TOURIST_IMEI,tourist.IMEI);
        map.put(KEY_CONNECT_END,new Timestamp(end.getTime()));
        try {
            myDatabase.updateRow(TABLE_CONNECT, map,KEY_TOURIST_IMEI+" = \'"+tourist.IMEI+"\' AND "+KEY_CONNECT_START+" = \'"+MyDatabase.dateTimeFormmatter(start)+"\'");
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean removeTourist(Tourist tourist) {
        String where = KEY_TOURIST_IMEI + " = \'" + tourist.IMEI+"\'";
        try {
            myDatabase.deleteRow(TABLE_TOURIST,where);
            myDatabase.deleteRow(TABLE_CONNECT,where);
            for (Bean bean:touristList) {
                Tourist tourist1=(Tourist)bean;
               if(tourist1.IMEI==tourist.IMEI){
                   touristList.remove(tourist1);
                   return true;
               }
            }
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }


    public Boolean updateTouristInfo(Map<String,Object> map){
        String IMEI=(String) map.get(KEY_TOURIST_IMEI);
        map.remove(KEY_TOURIST_IMEI);
        try {
            myDatabase.updateRow(TABLE_TOURIST, map,KEY_TOURIST_IMEI+" = \'"+IMEI+"\'");
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Tourist searchTouristByIMEI(String IMEI){
        for (Bean tourist:touristList) {
             if(((Tourist) tourist).IMEI.equals(IMEI)){
                 return (Tourist) tourist;
             }
        }
        return null;
    }

    public List<Bean> lookAllConnectLog(){
        return lookConnectLog(null);
    }

    public List<Bean> lookConnectLog(Tourist tourist){
        String sql="SELECT * FROM "+TABLE_CONNECT;
        if(tourist!=null) {
            String where = " WHERE "+KEY_TOURIST_IMEI + " = \'" + tourist.IMEI + "\'";
            sql=sql+where;
        }
        ResultSet resultSet=null;
        List<Bean> logs=new ArrayList<>();
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


    public List<Bean> getTouristList() {
        return touristList;
    }

    public JTable getJTable() {
        return jTable;
    }


    public static class Tourist implements Bean{
       String IMEI;
       String name;
       String phoneNumber;
       String detail;
       Timestamp registertime;
       Date start;

       public Tourist(){}

       public Tourist(String IMEI){
           this.IMEI=IMEI;
       }

       public Tourist(String IMEI,String name,String phoneNumber,String detail){
           this.IMEI=IMEI;
           this.name=name;
           this.phoneNumber=phoneNumber;
           this.detail=detail;
           registertime=new Timestamp(System.currentTimeMillis());
       }

       public Tourist(Map<String,Object> map){
           setFields(map);
           registertime=new Timestamp(System.currentTimeMillis());
       }

       public void connect(){
           touristManager.addTourist(this);
           start=new Date();
           touristManager.connectTourist(this,start);
           SwingUtilities.invokeLater(()-> jTable.updateUI());
       }

       public void disconnect(){
           touristManager.disconnectTourist(this,start,new Date());
       }


        public void setPhoneNumber(String phoneNumber) {
            Tourist tourist=touristManager.searchTouristByIMEI(IMEI);
            tourist.phoneNumber=phoneNumber;
            Map<String,Object> map=new HashMap<>();
            map.put(KEY_TOURIST_IMEI,IMEI);
            map.put(KEY_TOURIST_PHONE,phoneNumber);
            touristManager.updateTouristInfo(map);
            SwingUtilities.invokeLater(()-> jTable.updateUI());
        }

        @Override
        public List<String> getFieldNames() {
            if(fields==null) {
                fields = new ArrayList<>();
                fields.add(KEY_TOURIST_NAME);
                fields.add(KEY_TOURIST_PHONE);
                fields.add(KEY_TOURIST_DETAIL);
                fields.add(KEY_TOURIST_IMEI);
                fields.add(KEY_TOURIST_REGISTER_TIME);
            }
            return fields;
        }

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

        @Override
        public Bean getCopy() {
            return new Tourist(this.toTable());
        }
    }

    public static class ConnectLog implements Bean{
        String IMEI;
        String connect;
        String disconnect;

        public ConnectLog(){}

        public ConnectLog(Map<String,Object> map){
            setFields(map);
        }

        public ConnectLog(String IMEI,String connect,String disconnect){
            this.IMEI=IMEI;
            this.connect=connect;
            this.disconnect=disconnect;
        }

        @Override
        public List<String> getFieldNames() {
            List<String> fieldnames=new ArrayList<>();
            fieldnames.add(KEY_TOURIST_IMEI);
            fieldnames.add(KEY_CONNECT_START);
            fieldnames.add(KEY_CONNECT_END);
            return fieldnames;
        }

        @Override
        public void setFields(Map<String, Object> map) {
            IMEI=(String)map.get(KEY_TOURIST_IMEI);
            connect=(String)map.get(KEY_CONNECT_START);
            disconnect=(String)map.get(KEY_CONNECT_END);
        }

        @Override
        public Map<String, Object> toTable() {
            Map<String,Object> map=new HashMap<>();
            map.put(KEY_TOURIST_IMEI,IMEI);
            map.put(KEY_CONNECT_START,connect);
            map.put(KEY_CONNECT_END,disconnect);
            return map;
        }

        @Override
        public Bean getCopy() {
            return null;
        }
    }
}

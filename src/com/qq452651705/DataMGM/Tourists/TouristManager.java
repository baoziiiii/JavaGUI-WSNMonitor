package com.qq452651705.DataMGM.Tourists;

import com.qq452651705.DataMGM.Node.NodeTree;
import com.qq452651705.GUI.MyTable;
import com.qq452651705.JDBC.Exception.MySQLException;
import com.qq452651705.JDBC.MyDatabase;
import com.qq452651705.Utils.Bean;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

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
    public static final String KEY_TOURIST_NAME="Name";
    public static final String KEY_TOURIST_PHONE="Phone";
    public static final String KEY_TOURIST_DETAIL="Detail";

    public static final String KEY_CONNECT_ID="ID";
    public static final String KEY_CONNECT_START="ConnectTime";
    public static final String KEY_CONNECT_END="DisonnectTime";


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
            primaryKey.add(KEY_TOURIST_IMEI);
            myDatabase.createTable(TABLE_TOURIST, name_type_map, primaryKey, null);
        }catch (MySQLException e){
            e.printStackTrace();
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
            e.printStackTrace();
            System.out.println(e.getErrorCode());
        }
    }
    private static List<String> fields;
    private List<Tourist> touristList=new ArrayList<>();
    private MyTable myTable=new MyTable(new Tourist().getFieldNames(),touristList);

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
            touristList.add(new Tourist(map));//防止重复主键（重复跳异常）
            System.out.println("add new tourist success");
            return true;
        } catch (MySQLException e) {
            return false;
        }
    }

    public Boolean connectTourist(Tourist tourist,Date start){
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_TOURIST_IMEI,tourist.IMEI);
        map.put(KEY_CONNECT_START,MyDatabase.dateTimeFormmatter(start));
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
        map.put(KEY_CONNECT_END,MyDatabase.dateTimeFormmatter(end));
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
            myDatabase.deleteRow(TABLE_TOURIST, where);
            for (Tourist tourist1:touristList) {
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

    public MyTable getMyTable() {
        return myTable;
    }


    public static class Tourist implements Bean{
       String IMEI;
       String name;
       String phoneNumber;
       String detail;
       Date start;

       public Tourist(){}

       public Tourist(String IMEI,String name,String phoneNumber,String detail){
           this.IMEI=IMEI;
           this.name=name;
           this.phoneNumber=phoneNumber;
           this.detail=detail;
       }

       public Tourist(Map<String,Object> map){
           setFields(map);
       }

       public void connect(){
           touristManager.addTourist(this);
           start=new Date();
           touristManager.connectTourist(this,start);
       }

       public void disconnect(){
           touristManager.disconnectTourist(this,start,new Date());
       }

        @Override
        public List<String> getFieldNames() {
            if(fields==null) {
                fields = new ArrayList<>();
                fields.add(KEY_TOURIST_NAME);
                fields.add(KEY_TOURIST_PHONE);
                fields.add(KEY_TOURIST_DETAIL);
                fields.add(KEY_TOURIST_IMEI);
            }
            return fields;
        }

        @Override
        public void setFields(Map<String, Object> map) {
            name=(String) map.get(KEY_TOURIST_NAME);
            phoneNumber=(String) map.get(KEY_TOURIST_PHONE);
            detail=(String) map.get(KEY_TOURIST_DETAIL);
            IMEI=(String)map.get(KEY_TOURIST_IMEI);
        }

        @Override
        public Map<String, Object> toTable() {
            Map<String,Object> map=new HashMap<>();
            map.put(KEY_TOURIST_NAME,name);
            map.put(KEY_TOURIST_PHONE,phoneNumber);
            map.put(KEY_TOURIST_DETAIL,detail);
            map.put(KEY_TOURIST_IMEI,IMEI);
            return map;
        }

        @Override
        public Bean getCopy() {
            return new Tourist(this.toTable());
        }
    }
}

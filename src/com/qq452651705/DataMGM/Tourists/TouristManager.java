package com.qq452651705.DataMGM.Tourists;

import com.qq452651705.DataMGM.Node.NodeTree;
import com.qq452651705.JDBC.Exception.MySQLException;
import com.qq452651705.JDBC.MyDatabase;
import com.qq452651705.Utils.Bean;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TouristManager {

    private static TouristManager touristManager=new TouristManager();

    private TouristManager(){}

    public static TouristManager getTouristManager() {
        return touristManager;
    }

    public static final String KEY_TOURIST_ID="TouristID";
    public static final String KEY_TOURIST_NAME="TouristName";
    public static final String KEY_TOURIST_PHONE="TouristPhone";
    public static final String KEY_TOURIST_DETAIL="TouristDetail";

    private static MyDatabase myDatabase = MyDatabase.getMyDatabase();

    public final static String TABLE_TOURIST = "Tourist";

    static {
        Map<String, String> name_type_map = new HashMap<>();
        List<String> primaryKey = new ArrayList<>();
        try {
            name_type_map.put(KEY_TOURIST_ID, MyDatabase.TYPE_INT);
            name_type_map.put(KEY_TOURIST_NAME, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_TOURIST_PHONE, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_TOURIST_DETAIL, MyDatabase.TYPE_VARCHAR);
            primaryKey.add(KEY_TOURIST_ID);
            myDatabase.createTable(TABLE_TOURIST, name_type_map, primaryKey, KEY_TOURIST_ID);
        }catch (MySQLException e){
        }
    }
    private List<String> fields;
    private List<Tourist> touristList=new ArrayList<>();

    public void syncByDatabase(){
        ResultSet resultSet = null;
        try {
            resultSet = myDatabase.query("SELECT * FROM " + TABLE_TOURIST,null);
            while (resultSet.next()) {
                Map<String,Object> map=new HashMap<>();
                map.put(KEY_TOURIST_ID,resultSet.getString(KEY_TOURIST_ID));
                map.put(KEY_TOURIST_NAME,resultSet.getString(KEY_TOURIST_NAME));
                map.put(KEY_TOURIST_PHONE,resultSet.getString(KEY_TOURIST_PHONE));
                map.put(KEY_TOURIST_DETAIL,resultSet.getString(KEY_TOURIST_DETAIL));
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

    public Boolean addTourist(Tourist tourist){
        Map<String, Object> map = tourist.toTable();
        try {
            myDatabase.insertRow(TABLE_TOURIST, map);
            touristList.add(new Tourist(map));//防止重复ID主键（重复会跳异常）
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean removeTourist(Tourist tourist) {
        String where = KEY_TOURIST_ID + " = \'" + tourist.ID+"\'";
        try {
            myDatabase.deleteRow(TABLE_TOURIST, where);
            for (Tourist tourist1:touristList) {
               if(tourist1.ID==tourist.ID){
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

    class Tourist implements Bean{
       Integer ID;
       String name;
       String phoneNumber;
       String detail;

       public Tourist(Map<String,Object> map){
           setFields(map);
       }
        @Override
        public List<String> getFieldNames() {
            if(fields==null) {
                fields = new ArrayList<>();
                fields.add(KEY_TOURIST_ID);
                fields.add(KEY_TOURIST_NAME);
                fields.add(KEY_TOURIST_PHONE);
                fields.add(KEY_TOURIST_DETAIL);
            }
            return fields;
        }

        @Override
        public void setFields(Map<String, Object> map) {
            ID=(Integer) map.get(KEY_TOURIST_ID);
            name=(String) map.get(KEY_TOURIST_NAME);
            phoneNumber=(String) map.get(KEY_TOURIST_PHONE);
            detail=(String) map.get(KEY_TOURIST_DETAIL);
        }

        @Override
        public Map<String, Object> toTable() {
            Map<String,Object> map=new HashMap<>();
            map.put(KEY_TOURIST_ID,ID);
            map.put(KEY_TOURIST_NAME,name);
            map.put(KEY_TOURIST_PHONE,phoneNumber);
            map.put(KEY_TOURIST_DETAIL,detail);
            return map;
        }

        @Override
        public Bean getCopy() {
            return new Tourist(this.toTable());
        }
    }

}

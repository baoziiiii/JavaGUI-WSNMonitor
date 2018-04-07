package com.qq452651705.DataMGM.Monitor;

import com.qq452651705.JDBC.Exception.MySQLException;
import com.qq452651705.JDBC.MyDatabase;

import java.util.HashMap;
import java.util.Map;

public class MonitorManagement {

    MyDatabase db;
    static Map<String, String> map;
    static {
        map = new HashMap<>();
        map.put("id", MyDatabase.TYPE_INT);
        map.put("subject", MyDatabase.TYPE_VARCHAR);
        map.put("unit", MyDatabase.TYPE_VARCHAR);
        map.put("sensor_address", MyDatabase.TYPE_VARCHAR);
    }

    private static MonitorManagement monitorManagement=new MonitorManagement();
    private MonitorManagement(){}

    public MonitorManagement getInstance(){
        db=MyDatabase.getMyDatabase();
        try {
            db.createDatabase("com/qq452651705/DataMGM");
        } catch (MySQLException e) {
            e.printStackTrace();
        }

        try {
            db.openDatabase("com/qq452651705/DataMGM");
        } catch (MySQLException e) {
            e.printStackTrace();
        }
        try {
            db.createTable("Monitor",map,null,null);
        } catch (MySQLException e) {
            e.printStackTrace();
        }
        return monitorManagement;
    }

}

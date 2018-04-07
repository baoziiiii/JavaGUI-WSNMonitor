package com.qq452651705.DataMGM.Account;

import com.qq452651705.JDBC.Exception.MySQLException;
import com.qq452651705.JDBC.MyDatabase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountManager {

    private static AccountManager accountManager = new AccountManager();

    private AccountManager() {
        myDatabase = MyDatabase.getMyDatabase();
    }

    public static AccountManager getAccountManager() {
        return accountManager;
    }


    private static MyDatabase myDatabase;
    private final static String KEY_USERNAME = "username";
    private final static String KEY_PASSWORD = "password";
    private final static String TABLE_NAME = "account";

    static {
        try {
            Map<String, String> name_type_map = new HashMap<>();
            name_type_map.put(KEY_USERNAME, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_PASSWORD, MyDatabase.TYPE_VARCHAR);
            List<String> primaryKey = new ArrayList<>();
            primaryKey.add(KEY_USERNAME);
            myDatabase.createTable(TABLE_NAME, name_type_map, primaryKey, null);
        } catch (MySQLException e) {
        }
    }


    private String nowUsername;


    public Boolean register(String username, String password) {
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_USERNAME, username);
        map.put(KEY_PASSWORD, new String(password));
        try {
            myDatabase.insertRow(TABLE_NAME, map);
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean login(String username, String password) {
        Boolean result = false;
        ResultSet resultSet = null;
        List<Object> holders = new ArrayList<>();
        holders.add(username);
        holders.add(new String(password));
        try {
            resultSet = myDatabase.query("SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_USERNAME + " = ? AND " +
                    KEY_PASSWORD + " = ?", holders);
            if (resultSet.next()) {
                result = true;
                setNowUserName(username);
            } else {
                result = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                    myDatabase.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }
    }

    public void setNowUserName(String username) {
        this.nowUsername = username;
    }

    public String getNowUsername() {
        return nowUsername;
    }

    public Boolean changePassword(String username, String password) {
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_USERNAME, username);
        map.put(KEY_PASSWORD, password);
        try {
            myDatabase.updateRow(TABLE_NAME, map, "username = " + "\'"+username+"\'");
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<Object[]> getAllAccounts() {
        List<Object[]> list = new ArrayList<>();
        ResultSet resultSet = null;
        try {
            resultSet = myDatabase.query("SELECT * FROM " + TABLE_NAME, null);
            while (resultSet.next()) {
                Object[] o = new Object[2];
                o[0] = resultSet.getString(KEY_USERNAME);
                o[1] = resultSet.getString(KEY_PASSWORD);
                list.add(o);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                    myDatabase.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return list;
        }
    }
}

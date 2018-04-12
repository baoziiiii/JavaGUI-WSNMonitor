package com.qq452651705.DataMGM.Account;

import com.qq452651705.JDBC.Exception.MySQLException;
import com.qq452651705.JDBC.MyDatabase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Account manager. 管理客户账户相关操作,实现与数据库交互,管理account数据表
 */
public class AccountManager {

    private static MyDatabase myDatabase=MyDatabase.getMyDatabase();
    private final static String TABLE_ACCOUNT = "account";
    private final static String KEY_USERNAME = "username";
    private final static String KEY_PASSWORD = "password";

    static {
        try {
            Map<String, String> name_type_map = new HashMap<>();
            name_type_map.put(KEY_USERNAME, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_PASSWORD, MyDatabase.TYPE_VARCHAR);
            List<String> primaryKey = new ArrayList<>();
            primaryKey.add(KEY_USERNAME);
            myDatabase.createTable(TABLE_ACCOUNT, name_type_map, primaryKey, null);
        } catch (MySQLException e) {
            e.print();
        }
    }


    /** Singleton **/
    private static AccountManager accountManager = new AccountManager();

    private AccountManager() {
        myDatabase = MyDatabase.getMyDatabase();
    }

    /**
     * Gets account manager.
     *
     * @return the account manager
     */
    public static AccountManager getAccountManager() {
        return accountManager;
    }


    /** 保存当前登陆账号 **/
    private String nowUsername;


    /**
     * Register 客户注册.
     *
     * @param username the username
     * @param password the password
     * @return the boolean  false:注册名重复 true:注册成功
     */
    public Boolean register(String username, String password) {

        Map<String, Object> map = new HashMap<>();
        map.put(KEY_USERNAME, username);
        map.put(KEY_PASSWORD, new String(password));
        try {
            myDatabase.insertRow(TABLE_ACCOUNT, map);
            return true;
        } catch (MySQLException e) {
            System.out.println("注册用户名重复");
            e.print();
            return false;
        }
    }


    /**
     * Login 客户登陆.
     *
     * @param username the username
     * @param password the password
     * @return the boolean
     */
    public Boolean login(String username, String password) {
        Boolean result = false;
        ResultSet resultSet = null;
        List<Object> holders = new ArrayList<>();
        holders.add(username);
        holders.add(new String(password));
        try {
            resultSet = myDatabase.query("SELECT * FROM " + TABLE_ACCOUNT + " WHERE " + KEY_USERNAME + " = ? AND " +
                    KEY_PASSWORD + " = ?", holders);
            if (resultSet.next()) {
                result = true;
                setNowUserName(username);
            } else {
                result = false;
            }
        } catch (Exception e) {
            result = false;
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                    myDatabase.close();
                } catch (SQLException e) {}
            }
            return result;
        }
    }

    /**
     * Sets nowUsername. 保存已登陆用户名
     *
     * @param username the username
     */
    public void setNowUserName(String username) {
        this.nowUsername = username;
    }

    /**
     * Gets now username.
     *
     * @return the now username
     */
    public String getNowUsername() {
        return nowUsername;
    }

    /**
     * Change password. 修改密码
     *
     * @param username the username
     * @param password the password  新密码
     * @return the boolean  false:用户名不存在
     */
    public Boolean changePassword(String username, String password) {
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_USERNAME, username);
        map.put(KEY_PASSWORD, password);
        try {
            myDatabase.updateRow(TABLE_ACCOUNT, map, "username = " + "\'" + username + "\'");
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Gets all accounts. 获取所有账户的用户名密码
     *
     * @return the all accounts
     * List<Object[]>
     *     Object[0]:用户名 Object[1]:密码。
     */
    public List<Object[]> getAllAccounts() {
        List<Object[]> list = new ArrayList<>();
        ResultSet resultSet = null;
        try {
            resultSet = myDatabase.query("SELECT * FROM " + TABLE_ACCOUNT, null);
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

    /**
     * Delete all accounts. 清空所有账户
     */
    public void deleteAllAccounts() {
        try {
            myDatabase.clearTable(TABLE_ACCOUNT);
        } catch (MySQLException e) {
            e.printStackTrace();
        }
    }
}

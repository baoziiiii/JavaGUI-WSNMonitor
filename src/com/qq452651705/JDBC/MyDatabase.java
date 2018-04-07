package com.qq452651705.JDBC;

import com.qq452651705.JDBC.Exception.MySQLException;
import com.mysql.jdbc.Connection;

import java.sql.*;
import java.util.List;
import java.util.Map;

import static java.lang.System.out;

public class MyDatabase {

    private static MyDatabase myDatabase=new MyDatabase();

    private MyDatabase() {
    }

    public static MyDatabase getMyDatabase(){
        return myDatabase;
    }

    public static final String TYPE_INT = "INT";
    public static final String TYPE_FLOAT = "FLOAT";
    public static final String TYPE_DOUBLE = "DOUBLE";
    public static final String TYPE_DATE = "DATE";
    public static final String TYPE_TIME = "TIME";
    public static final String TYPE_TIMESTAMP = "TIMESTAMP";
    public static final String TYPE_VARCHAR = "VARCHAR(255)";


    private static String db = "wsn";
    private static String url = "jdbc:mysql://localhost:3306/";
    private static String usrname = "root";
    private static String password = "ABC123";

    static {
        try {
            Class.forName("org.gjt.mm.mysql.Driver");
            out.println("成功加载驱动");
            Connection conn = (Connection) DriverManager.getConnection(url + "mysql" + "?useSSL=false", usrname, password);
            Statement statement = conn.createStatement();
            statement.execute("create database " + db);
            statement.execute("set global character_set_database=utf8");
            statement.execute("set global character_set_server=utf8");
            statement.close();
            conn.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Database " + db + " already exists...");
        }
    }

    private Connection conn;
    private PreparedStatement ps;




    private Connection getConnection() throws SQLException {
        return (Connection) DriverManager.getConnection(url + db + "?useSSL=false", usrname, password);
    }

    public void createDatabase(String dbName) throws MySQLException {
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement("create database " + dbName);
            ps.executeUpdate();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            throw new MySQLException(MySQLException.DB_CREATE_FAIL, "Failed to create " + dbName);
        }
    }

    public void dropDatabase(String dbName) {
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement("drop database " + dbName);
            ps.executeUpdate();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void openDatabase(String dbName) throws MySQLException {
        try {
            Connection conn = getConnection();
            Statement statement = conn.createStatement();
            statement.execute("use " + dbName);
            statement.close();
            conn.close();
            this.db = dbName;
        } catch (SQLException e) {
            throw new MySQLException(MySQLException.DB_OPEN_FAIL, "Failed to open " + dbName);
        }
    }

    public void createTable(String tbName, Map<String, String> name_type_map, List<String> PrimaryKey, String AUTO_INCREMENT) throws MySQLException {
        StringBuffer stringBuffer = new StringBuffer();
        for (Map.Entry<String, String> entry : name_type_map.entrySet()) {
            stringBuffer.append(entry.getKey() + " ")
                    .append(entry.getValue() + " ");
            if(entry.getKey().equals(AUTO_INCREMENT)){
                stringBuffer.append("AUTO_INCREMENT");
            }
            stringBuffer.append(",");
        }
        if(PrimaryKey!=null) {
            stringBuffer.append("PRIMARY KEY(");
            for (String s : PrimaryKey) {
                stringBuffer.append(s+",");
            }
            stringBuffer.deleteCharAt(stringBuffer.length()-1);
            stringBuffer.append("),");
        }
        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement("create table " + tbName + "(" + stringBuffer + ")default charset=utf8");
            ps.executeUpdate();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            throw new MySQLException(MySQLException.TB_OPEN_FAIL, "Failed to create " + tbName);
        }
    }

    public void dropTable(String tbName) {
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement("drop table " + tbName);
            ps.executeUpdate();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertRow(String tbName, Map<String, Object> map) throws MySQLException {
        StringBuffer holder = new StringBuffer();
        StringBuffer cols = new StringBuffer();
        int size = map.size();
        Object[] vals = new Object[size];
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            holder.append("?,");
            cols.append(entry.getKey()).append(",");
            vals[i] = entry.getValue();
            i++;
        }
        holder.deleteCharAt(holder.length() - 1);
        cols.deleteCharAt(cols.length() - 1);

        String sql = "insert into " + tbName + "(" + cols + ")values(" + holder + ")";
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            int j = 1;
            for (Object val : vals) {
                ps.setString(j, val.toString());
                j++;
            }
            ps.executeUpdate();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException(MySQLException.TB_INSERT_FAIL, "Failed to insert row into " + tbName);
        }
    }

    public void updateRow(String tbName, Map<String, Object> map, String where) throws MySQLException {
        StringBuffer holder = new StringBuffer();
        int size = map.size();
        Object[] vals = new Object[size];
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            holder.append(entry.getKey()).append("=?,");
            vals[i] = entry.getValue();
            i++;
        }
        holder.deleteCharAt(holder.length() - 1);
        String sql = "update " + tbName + " set " + holder + " WHERE " + where;
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            int j = 1;
            for (Object val : vals) {
                ps.setString(j, val.toString());
                j++;
            }
            ps.executeUpdate();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException(MySQLException.TB_UPDATE_FAIL, "Failed to update row into " + tbName);
        }
    }

    public void deleteRow(String tbName, String where) throws MySQLException {
        String sql = "delete from " + tbName + " where " + where;
        try {
            conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.executeUpdate();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException(MySQLException.TB_DELETE_ROW_FAIL, "Failed to delete row into " + tbName);
        }
    }

    public void clearTable(String tbName)throws MySQLException{
        String sql="delete from "+tbName;
        try {
            conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.executeUpdate();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException(MySQLException.TB_CLEAR_FAIL, "Failed to clear " + tbName);
        }
    }

    public ResultSet query(String sql, List<Object> holders) throws MySQLException{
          try {
              conn = getConnection();
              ps = conn.prepareStatement(sql);
            int n=1;
            if(holders!=null) {
                for (Object holder : holders) {
                    ps.setObject(n, holder);
                    n++;
                }
            }
            ResultSet resultSet= ps.executeQuery();
            return resultSet;
        } catch (SQLException e) {
            e.printStackTrace();
              throw new MySQLException(MySQLException.TB_QUERY_FAIL, "Failed to execute query:" + sql );
        }
    }

    public void close(){
        try {
            ps.close();
            conn.close();
            ps=null;
            conn=null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}

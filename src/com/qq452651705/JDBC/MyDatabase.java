package com.qq452651705.JDBC;

import com.qq452651705.JDBC.Exception.MySQLException;
import com.mysql.jdbc.Connection;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The type My database.  数据库服务类,实现数据库的底层操作
 */
public class MyDatabase {

    /***************************数据类型********************************/
    /**
     * The constant TYPE_INT.
     */
    public static final String TYPE_INT = "INT";
    /**
     * The constant TYPE_FLOAT.
     */
    public static final String TYPE_FLOAT = "FLOAT";
    /**
     * The constant TYPE_DOUBLE.
     */
    public static final String TYPE_DOUBLE = "DOUBLE";
    /**
     * The constant TYPE_DATE.
     */
    public static final String TYPE_DATE = "DATE";
    /**
     * The constant TYPE_TIME.
     */
    public static final String TYPE_TIME = "TIME";
    /**
     * The constant TYPE_DATETIME.
     */
    public static final String TYPE_DATETIME = "DATETIME";
    /**
     * The constant TYPE_TIMESTAMP.
     */
    public static final String TYPE_TIMESTAMP = "TIMESTAMP";
    /**
     * The constant TYPE_VARCHAR.
     */
    public static final String TYPE_VARCHAR = "VARCHAR(255)";

    /***************************数据类型********************************/

    /**
     * @param db 数据库名称
     */
    private static String db = "wsn";

    /**
     * @param url 数据库url
     */
    private static String url = "jdbc:mysql://localhost:3306/";

    /**
     * @param usrname 数据库用户名
     */
    private static String usrname = "root";

    /**
     * @param pwrd 数据库密码
     */
    private static String pwrd = "ABC";


    /**
     * Load database. 数据库加载
     */

    public static void loadMySQL() throws SQLException, ClassNotFoundException {
        Connection conn;
        Class.forName("org.gjt.mm.mysql.Driver");
        System.out.println("成功加载驱动");
        conn = (Connection) DriverManager.getConnection(url + "mysql" + "?useSSL=false", usrname, pwrd);
        try {
            Statement statement = conn.createStatement();
            statement.execute("create database " + db);
            statement.execute("set global character_set_database=utf8");
            statement.execute("set global character_set_server=utf8");
            statement.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println("Database " + db + " already exists...");
        }
    }

    public static void configDatabase(String username,String password){
        usrname=username;
        pwrd=password;
    }


    private static MyDatabase myDatabase = new MyDatabase();

    private MyDatabase() {}

    /**
     * Get my database my database. Singleton
     *
     * @return the my database
     */
    public static MyDatabase getMyDatabase() {
        try {
            loadMySQL();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return myDatabase;
    }


    /**
     * @param conn 数据库连接
     */
    private Connection conn;

    /**
     * @param ps   预处理sql语句
     */
    private PreparedStatement ps;


    /**
     * Date time formmatter string.  Date类型格式化字符串
     *
     * @param date the date
     * @return the string
     */
    public static String dateTimeFormmatter(java.util.Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    /**
     * Timestamp formmatter string.   Timestamp类型格式化字符串
     *
     * @param timestamp the timestamp
     * @return the string
     */
    public static String timestampFormmatter(Timestamp timestamp) {
        if (timestamp == null)
            return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp.getTime()));
    }


    /**
     * Get database connection. 建立数据库连接
     *
     * @return
     * @throws SQLException
     */
    private Connection getConnection() throws SQLException {
        return (Connection) DriverManager.getConnection(url + db + "?useSSL=false", usrname, pwrd);
    }

    /**
     * Create database.           创建数据库
     *
     * @param dbName the db name  数据库名称
     * @throws MySQLException the my sql exception
     */
    public void createDatabase(String dbName) throws MySQLException {
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement("create database " + dbName);
            ps.executeUpdate();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException(MySQLException.DB_CREATE_FAIL, "Failed to create " + dbName);
        }
    }

    /**
     * Drop database.             删除数据库
     *
     * @param dbName the db name  数据库名称
     */
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

    /**
     * Open database.             打开数据库
     *
     * @param dbName the db name  数据库名称
     * @throws MySQLException the my sql exception
     */
    public void openDatabase(String dbName) throws MySQLException {
        try {
            Connection conn = getConnection();
            Statement statement = conn.createStatement();
            statement.execute("use " + dbName);
            statement.close();
            conn.close();
            this.db = dbName;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException(MySQLException.DB_OPEN_FAIL, "Failed to open " + dbName);
        }
    }

    /**
     * Create table.           创建表
     *
     * @param tbName         the tb name            表名
     * @param name_type_map  the name type map      列名-列类型Map
     * @param PrimaryKey     the primary key        主键
     * @param AUTO_INCREMENT the auto increment     自增列名
     * @throws MySQLException the my sql exception
     */
    public void createTable(String tbName, Map<String, String> name_type_map, List<String> PrimaryKey, String AUTO_INCREMENT) throws MySQLException {
        StringBuffer stringBuffer = new StringBuffer();
        for (Map.Entry<String, String> entry : name_type_map.entrySet()) {
            stringBuffer.append(entry.getKey() + " ")
                    .append(entry.getValue() + " ");
            if (entry.getKey().equals(AUTO_INCREMENT)) {
                stringBuffer.append("AUTO_INCREMENT");
            }
            stringBuffer.append(",");
        }
        if (PrimaryKey != null) {
            stringBuffer.append("PRIMARY KEY(");
            for (String s : PrimaryKey) {
                stringBuffer.append(s + ",");
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);
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
            throw new MySQLException(MySQLException.TB_OPEN_FAIL, "Failed to create " + tbName + ".Possibly already existed.");
        }
    }

    /**
     * Drop table.     删除表
     *
     * @param tbName the tb name  表名
     */
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

    /**
     * Insert row.     添加一条记录
     *
     * @param tbName the tb name   表名
     * @param map    the map       列名-值Map
     * @throws MySQLException the my sql exception
     */
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
                String val_s;
                if (val == null) {
                    val_s = "";
                } else {
                    val_s = val.toString();
                }
                ps.setString(j, val_s);
                j++;
            }
            ps.executeUpdate();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            throw new MySQLException(MySQLException.TB_INSERT_FAIL, "Failed to insert row into " + tbName);
        }
    }

    /**
     * Update row.     更新记录
     *
     * @param tbName the tb name   表名
     * @param map    the map       列名-值Map(不一定全部列)
     * @param where  the where     where语句(不包括"where")
     * @throws MySQLException the my sql exception
     */
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
                String val_s;
                if (val == null) {
                    val_s = "";
                } else {
                    val_s = val.toString();
                }
                ps.setString(j, val_s);
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

    /**
     * Delete row.                删除记录
     *
     * @param tbName the tb name  表名
     * @param where  the where    where语句(不包括"where")
     * @throws MySQLException the my sql exception
     */
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

    /**
     * Clear table.              清空表(表结构仍存在)
     *
     * @param tbName the tb name  表名
     * @throws MySQLException the my sql exception
     */
    public void clearTable(String tbName) throws MySQLException {
        String sql = "delete from " + tbName;
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

    /**
     * Query result set.            查询. query方法不会自动关闭数据库,务必在操作完resultset后调用resultset.close()和myDatabase.close()两句话关闭.
     *
     * @param sql     the sql       查询sql语句，可以使用"?"作为占位符，使用holders链表依次存入"?"的值
     * @param holders the holders   占位值链表，输入null不使用占位符
     * @return the result set       返回结果集
     * @throws MySQLException the my sql exception
     */
    public ResultSet query(String sql, List<Object> holders) throws MySQLException {
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            int n = 1;
            if (holders != null) {
                for (Object holder : holders) {
                    ps.setObject(n, holder);
                    n++;
                }
            }
            ResultSet resultSet = ps.executeQuery();
            return resultSet;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException(MySQLException.TB_QUERY_FAIL, "Failed to execute query:" + sql);
        }

    }

    /**
     * Close.   query方法不会自动关闭数据库,务必在操作完resultset后调用resultset.close()和myDatabase.close()两句话关闭.
     */
    public void close() {
        try {
            ps.close();
            conn.close();
            ps = null;
            conn = null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}

package com.qq452651705.JDBC.Exception;

import java.sql.SQLException;

/**
 * The type My sql exception.  数据库自定义异常类
 */
public class MySQLException extends SQLException {

    /**
     * The constant DB_CREATE_FAIL.  数据库创建失败
     */
    public static final String DB_CREATE_FAIL = "Failed to create database.";
    /**
     * The constant DB_OPEN_FAIL.    数据库打开失败
     */
    public static final String DB_OPEN_FAIL= "Failed to open database.";
    /**
     * The constant TB_OPEN_FAIL.    打开表失败
     */
    public static final String TB_OPEN_FAIL= "Failed to open table.";
    /**
     * The constant TB_INSERT_FAIL.  添加记录失败
     */
    public static final String TB_INSERT_FAIL= "Failed to insert values into table.";
    /**
     * The constant TB_UPDATE_FAIL.  更新记录失败
     */
    public static final String TB_UPDATE_FAIL= "Failed to update values.";
    /**
     * The constant TB_DELETE_ROW_FAIL. 删除记录失败
     */
    public static final String TB_DELETE_ROW_FAIL= "Failed to delete the row.";
    /**
     * The constant TB_CLEAR_FAIL.    清空表失败
     */
    public static final String TB_CLEAR_FAIL= "Failed to clear table.";
    /**
     * The constant TB_QUERY_FAIL.    查询表失败
     */
    public static final String TB_QUERY_FAIL= "Failed to query.";

    private static String msg;
    private static String detail;

    /**
     * Instantiates a new My sql exception.
     */
    public MySQLException(){
        super();
    }

    /**
     * Instantiates a new My sql exception.
     *
     * @param msg    the msg     异常信息
     * @param detail the detail  异常额外信息
     */
    public MySQLException(String msg,String detail)
    {
        this.msg = msg;
        this.detail=detail;
    }

    /**
     * Print string.
     *
     * @return the string
     */
    public String print() {
        return "***********MySQLException***********\n"+msg+'\n'+detail
            +"\n************************************\n";
    }

}

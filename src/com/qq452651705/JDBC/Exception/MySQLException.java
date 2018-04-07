package com.qq452651705.JDBC.Exception;

import java.sql.SQLException;

public class MySQLException extends SQLException {
    public static final String DB_CREATE_FAIL = "Failed to create database.";
    public static final String DB_OPEN_FAIL= "Failed to open database.";
    public static final String TB_OPEN_FAIL= "Failed to open table.";
    public static final String TB_INSERT_FAIL= "Failed to insert values into table.";
    public static final String TB_UPDATE_FAIL= "Failed to update values.";
    public static final String TB_DELETE_ROW_FAIL= "Failed to delete the row.";
    public static final String TB_CLEAR_FAIL= "Failed to clear table.";
    public static final String TB_QUERY_FAIL= "Failed to query.";


    private static String msg;
    private static String detail;

    // 无参数的构造器
    public MySQLException(){
        super();
    }       //①
    // 带一个字符串参数的构造器
    public MySQLException(String msg,String detail)    //②
    {
        this.msg = msg;
        this.detail=detail;
    }

    public String print() {
        return "***********MySQLException***********\n"+msg+'\n'+detail
            +"\n************************************\n";
    }

}

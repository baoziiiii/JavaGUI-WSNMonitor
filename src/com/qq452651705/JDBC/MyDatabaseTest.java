package com.qq452651705.JDBC;

import com.qq452651705.JDBC.Exception.MySQLException;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MyDatabaseTest {
   @Test
    public void createDatabaseTest(){
      MyDatabase database=MyDatabase.getMyDatabase();
       try {
           database.createDatabase("wsn");
           System.out.println("created");
       } catch (MySQLException e) {
           System.out.println(e.print());
       }
   }
    @Test
    public void dropDatabaseTest(){
        MyDatabase database=MyDatabase.getMyDatabase();
        database.dropDatabase("wsn");
        System.out.println("droped");
    }

    @Test
    public void useDatabaseTest(){
        MyDatabase database=MyDatabase.getMyDatabase();
        try {
            database.openDatabase("wsn");
            System.out.println("opened");
        } catch (MySQLException e) {
            System.out.println(e.print());
        }
    }

    @Test
    public void createTableTest(){
        MyDatabase database=MyDatabase.getMyDatabase();
        try {
            Map<String,String> map=new HashMap<>();
            map.put("name",MyDatabase.TYPE_VARCHAR);
            map.put("phone_number",MyDatabase.TYPE_VARCHAR);
            map.put("time",MyDatabase.TYPE_TIMESTAMP);
            database.createTable("tourist_info",map,null,"");
            System.out.println("created");
        } catch (MySQLException e) {
            System.out.println(e.print());
        }
    }

    @Test
    public void dropTableTest(){
        MyDatabase database=MyDatabase.getMyDatabase();
        try {
            database.openDatabase("wsn");
            database.dropTable("tourist_info");
            System.out.println("deleted");
        } catch (MySQLException e) {
            System.out.println(e.print());
        }
    }

    @Test
    public void insertTest(){
        MyDatabase database=MyDatabase.getMyDatabase();
        try {
            database.openDatabase("wsn");
            Map<String,Object> map=new HashMap<>();
            map.put("name","王大锤");
            map.put("phone_number","13801691551");
            database.insertRow("tourist_info",map);
            System.out.println("inserted");
        } catch (MySQLException e) {
            System.out.println(e.print());
        }
    }

    @Test
    public void updateTest(){
        MyDatabase database=MyDatabase.getMyDatabase();
        try {
            database.openDatabase("wsn");
            Map<String,Object> map=new HashMap<>();
            map.put("phone_number","13081342342");
            database.updateRow("tourist_info",map,"name='王大锤'");
            System.out.println("updated");
        } catch (MySQLException e) {
            System.out.println(e.print());
        }
    }

    @Test
    public void deleteTest(){
        MyDatabase database=MyDatabase.getMyDatabase();
        try {
            database.openDatabase("wsn");
            database.deleteRow("tourist_info","name='王大锤'");
            System.out.println("deleted");
        } catch (MySQLException e) {
            System.out.println(e.print());
        }
    }


    @Test
    public void sort(){
       int[] a=new int[]{53,23,23,23,53,78,9,9,78};
        for (int i = 0; i < a.length-1 ; i++) {
            int minIndex=i;
            for (int j = i; j <= a.length-1 ; j++) {
                if(a[j]<a[minIndex])
                    minIndex=j;
            }
            if(minIndex!=i){
                int temp=a[minIndex];
                a[minIndex]=a[i];
                a[i]=temp;
            }
        }
        System.out.println(Arrays.toString(a));
    }

    @Test
    public void sortBubble(){
        int[] a=new int[]{53,23,23,23,53,78,9,9,78};
        for (int i = 0; i < a.length-1 ; i++) {
            Boolean flag=false;
            for(int j=a.length-1;j>i;j--){
                if(a[j]<a[j-1]){
                    int temp=a[j-1];
                    a[j-1]=a[j];
                    a[j]=temp;
                    flag=true;
                }
            }
            if(!flag)
                break;
        }
        System.out.println(Arrays.toString(a));
    }
}
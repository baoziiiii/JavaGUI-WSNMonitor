package com.qq452651705.DataMGM.Node;

import com.qq452651705.JDBC.Exception.MySQLException;
import com.qq452651705.JDBC.MyDatabase;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.List;

import com.qq452651705.DataMGM.Node.NodeTree.SinkNode;
import com.qq452651705.DataMGM.Node.NodeTree.Node;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;

/**
 * The type Node manager. 管理节点树与数据库的同步与交互。
 *
 */
public class NodeManager {

    private static MyDatabase myDatabase = MyDatabase.getMyDatabase();

    private static NodeManager nodeManager = new NodeManager();

    private NodeManager() {
        createTables();
    }

    /**
     * Gets node manager. Singleton.
     *
     * @return the node manager
     */
    public static NodeManager getNodeManager() {
        return nodeManager;
    }



    /**
     * The constant TABLE_SINK.
     * 表1: Sink节点表
     * -------主键-----------------------
     * 列:  Sink地址  Sink名称  Sink信息
     *
     */
    public final static String TABLE_SINK = "Sink";


    /**
     * The constant KEY_SINK_ADDRESS.
     */
    public final static String KEY_SINK_ADDRESS = "Sink地址";
    /**
     * The constant KEY_SINK_NAME.
     */
    public final static String KEY_SINK_NAME = "Sink名称";
    /**
     * The constant KEY_SINK_DETAIL.
     */
    public final static String KEY_SINK_DETAIL = "Sink信息";


    /**
     * The constant TABLE_NODE.
     * 表2: 传感器节点表
     * -------主键--------主键-------------------------
     * 列:  传感器地址  父Sink地址  传感器名称  监测值单位
     *
     */
    public final static String TABLE_NODE = "Node";

    /**
     * The constant KEY_NODE_ADDRESS.
     */
    public final static String KEY_NODE_ADDRESS = "传感器地址";
    /**
     * The constant KEY_NODE_PARENT.
     */
    public final static String KEY_NODE_PARENT = "父Sink地址";
    /**
     * The constant KEY_NODE_NAME.
     */
    public final static String KEY_NODE_NAME = "传感器名称";
    /**
     * The constant KEY_NODE_DETAIL.
     */
    public final static String KEY_NODE_DETAIL = "监测值单位";


    /**
     * The constant TABLE_DATA.
     * 表3: 传感器数据表
     * -------主键--------主键-------------------------
     * 列:  传感器地址  父Sink地址    时刻   值
     *
     */
    public final static String TABLE_DATA = "Data";

    /**
     * The constant KEY_DATA_TIME.
     */
    public final static String KEY_DATA_TIME = "时刻";
    /**
     * The constant KEY_DATA_VAL.
     */
    public final static String KEY_DATA_VAL = "值";



    /**
     * Create tables in database if not existed.
     */
    public void createTables(){
        Map<String, String> name_type_map = new HashMap<>();
        List<String> primaryKey = new ArrayList<>();
        try {
            name_type_map.put(KEY_SINK_NAME, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_SINK_ADDRESS, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_SINK_DETAIL, MyDatabase.TYPE_VARCHAR);
            primaryKey.add(KEY_SINK_ADDRESS);
            myDatabase.createTable(TABLE_SINK, name_type_map, primaryKey, null);
        }catch (MySQLException e){ //数据表已存在
            e.print();
        }
        try {
            name_type_map.clear();
            name_type_map.put(KEY_NODE_NAME, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_NODE_PARENT, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_NODE_DETAIL, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_NODE_ADDRESS,MyDatabase.TYPE_VARCHAR);
            primaryKey.clear();
            primaryKey.add(KEY_NODE_PARENT);
            primaryKey.add(KEY_NODE_ADDRESS);
            myDatabase.createTable(TABLE_NODE, name_type_map, primaryKey, null);
        }catch (MySQLException e){
            e.print();
        }

        try {
            name_type_map.clear();
            name_type_map.put(KEY_NODE_ADDRESS, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_NODE_PARENT, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_DATA_TIME, MyDatabase.TYPE_TIMESTAMP);
            name_type_map.put(KEY_DATA_VAL, MyDatabase.TYPE_FLOAT);
            primaryKey.clear();
            primaryKey.add(KEY_DATA_TIME);
            primaryKey.add(KEY_NODE_ADDRESS);
            primaryKey.add(KEY_NODE_PARENT);
            myDatabase.createTable(TABLE_DATA, name_type_map, primaryKey, null);
        }catch (MySQLException e) {
            e.print();
        }
    }

    public void dropTable(String tbName){
        myDatabase.dropTable(tbName);
    }

    /**
     * Sync by database. 从数据库同步节点树.
     *
     * @param nodeTree the node tree 节点树
     */
    public void syncByDatabase(NodeTree nodeTree){
        Boolean wrongTable = null;
        ResultSet resultSet = null;
        try {
            resultSet = myDatabase.query("SELECT * FROM " + TABLE_SINK,null);
            nodeTree.getSinkNodeList().clear();
            while (resultSet.next()) {
                Map<String,Object> map=new HashMap<>();
                map.put(KEY_SINK_NAME,resultSet.getString(KEY_SINK_NAME));
                map.put(KEY_SINK_ADDRESS,resultSet.getString(KEY_SINK_ADDRESS));
                map.put(KEY_SINK_DETAIL,resultSet.getString(KEY_SINK_DETAIL));
                nodeTree.getSinkNodeList().add(new SinkNode(map)); //添加Sink节点
            }
        } catch (Exception e) {
            wrongTable=true;
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
            if(wrongTable!=null&&wrongTable==true){
                dropTable(TABLE_SINK);
                wrongTable=false;
            }
        }
        try {
            resultSet = myDatabase.query("SELECT * FROM " + TABLE_NODE,null);
            while (resultSet.next()) {
                Map<String,Object> map=new HashMap<>();
                map.put(KEY_NODE_NAME,resultSet.getString(KEY_NODE_NAME));
                map.put(KEY_NODE_DETAIL,resultSet.getString(KEY_NODE_DETAIL));
                map.put(KEY_NODE_PARENT,resultSet.getString(KEY_NODE_PARENT));
                map.put(KEY_NODE_ADDRESS,resultSet.getString(KEY_NODE_ADDRESS));
                for(SinkNode sinkNode:nodeTree.getSinkNodeList()){
                    if(sinkNode.getAddress().equals(resultSet.getString(KEY_NODE_PARENT))) {
                        sinkNode.nodeList.add(new Node(map));  //在相应的sinknodelist下添加node
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            wrongTable=true;
        }finally {
            if (resultSet != null) {
                try {
                    nodeTree.generateJTree();
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            myDatabase.close();
            if(wrongTable!=null&&wrongTable==true){
                dropTable(TABLE_NODE);
                createTables();
                wrongTable=false;
            }
        }
        if(wrongTable!=null){
            createTables();
        }
    }

    /**
     * Add sink node. 向Sink数据表中添加新的Sink节点.
     *
     * @param sinkNode the sink node
     * @return the boolean  false:通常因为重复主键(同一地址的Sink节点已存在)
     */
    public Boolean addSinkNode(SinkNode sinkNode) {
        Map<String, Object> map = sinkNode.toTable();
        try {
            myDatabase.insertRow(TABLE_SINK, map);
            return true;
        } catch (MySQLException e) {
            e.print();
            return false;
        }
    }

    /**
     * Remove sink node. 从数据库中移除Sink节点(三张关联表).
     *
     * @param sinkNodeAddress the sink node address sink节点地址
     * @return the boolean
     */
    public Boolean removeSinkNode(String sinkNodeAddress) {
        String where = KEY_SINK_ADDRESS + " = \'" + sinkNodeAddress+"\'";
        String where2=KEY_NODE_PARENT + " = \'"+sinkNodeAddress+"\'";
        try {
            myDatabase.deleteRow(TABLE_SINK, where);
            myDatabase.deleteRow(TABLE_NODE, where2);
            myDatabase.deleteRow(TABLE_DATA, where2);
            return true;
        } catch (MySQLException e) {
            e.print();
            return false;
        }
    }


    /**
     * Add node.    添加传感器节点
     *
     * @param node the node
     * @return the boolean
     */
    public Boolean addNode(Node node) {
        Map<String, Object> map = node.toTable();
        try {
            myDatabase.insertRow(TABLE_NODE, map);
            return true;
        } catch (MySQLException e) {
            e.print();
            return false;
        }
    }

    /**
     * Remove node.  移除传感器节点
     *
     * @param parent the parent address 父Sink节点地址
     * @param nodeAddress  the node address  传感器节点地址
     * @return the boolean
     */
    public Boolean removeNode(String parent, String nodeAddress) {
        String where = KEY_NODE_ADDRESS + " = \'" + nodeAddress + "\' AND " + KEY_NODE_PARENT + " = \'" + parent+"\'";
        try {
            myDatabase.deleteRow(TABLE_NODE, where);
            myDatabase.deleteRow(TABLE_DATA, where);
            return true;
        } catch (MySQLException e) {
            e.print();
            return false;
        }
    }

    /**
     * Update sink node.  更新Sink节点信息
     *
     * @param oldSinkNode the old sink node  旧Sink节点
     * @param newSinkNode the new sink node  新Sink节点(地址必须一样)
     * @return the boolean  false:通常因为地址不一样或未找到旧Sink节点
     */
    public Boolean updateSinkNode(SinkNode oldSinkNode, SinkNode newSinkNode) {
        try {
            Map<String, Object> map = new HashMap<>();
            String where = KEY_SINK_ADDRESS + " = \'" + oldSinkNode.address + "\'";
            String where2 = KEY_NODE_PARENT + " = \'" + oldSinkNode.address + "\'";
            if(oldSinkNode.getAddress().equals(newSinkNode.getAddress())) {
            }else {
                map.put(KEY_NODE_PARENT,newSinkNode.address);
                myDatabase.updateRow(TABLE_NODE, map, where2);
                myDatabase.updateRow(TABLE_DATA, map, where2);
                map.remove(KEY_NODE_PARENT);
            }
            map.put(KEY_SINK_NAME, newSinkNode.name);
            map.put(KEY_SINK_DETAIL,newSinkNode.detail);
            map.put(KEY_SINK_ADDRESS,newSinkNode.address);
            myDatabase.updateRow(TABLE_SINK,map,where);
            return true;
        } catch (MySQLException e) {
            e.print();
            return false;
        }
    }

    /**
     * Update node.  更新传感器节点信息
     *
     * @param parent   the parent address  父Sink节点地址
     * @param oldnode  the oldnode    旧传感器节点
     * @param newnode  the newnode    新传感器节点(地址必须一样)
     * @return the boolean   false:通常因为地址不一样或地址错误
     */
    public Boolean updateNode(String parent, Node oldnode, Node newnode) {
        try {
            String where = KEY_NODE_ADDRESS + " = \'" + oldnode.address + "\' AND " + KEY_NODE_PARENT + " = \'" + parent+"\'";
            Map<String, Object> map = new HashMap<>();
            map.put(KEY_NODE_ADDRESS, newnode.address);
            map.put(KEY_NODE_PARENT,newnode.parent);
            myDatabase.updateRow(TABLE_DATA, map, where);
            map.put(KEY_NODE_DETAIL,newnode.detail);
            myDatabase.updateRow(TABLE_NODE, map, where);
            return true;
        } catch (MySQLException e) {
            e.print();
            return false;
        }
    }

    /**
     * Add new data.  新传感器数据.
     *
     * @param nodeAddress  the node address    传感器节点地址
     * @param parent       the parent address  父Sink节点地址
     * @param timestamp    the timestamp       时刻
     * @param value        the value           值
     * @return the boolean  false:通常因为地址错误
     */
    public Boolean addNewData(String nodeAddress,String parent, Timestamp timestamp, Float value){
        Map<String,Object> map=new HashMap<>();
        map.put(KEY_NODE_ADDRESS,nodeAddress);
        map.put(KEY_NODE_PARENT, parent);
        map.put(KEY_DATA_TIME, timestamp);
        map.put(KEY_DATA_VAL, value);
        try {
            myDatabase.insertRow(TABLE_DATA,map);
            return true;
        } catch (MySQLException e) {
            e.print();
            return false;
        }
    }

    /**
     * Clear all data.  清空所有传感数据.
     */
    public void clearAllData(){
        try {
            myDatabase.clearTable(TABLE_DATA);
        } catch (MySQLException e) {
            e.print();
        }
    }

    /**
     * Clear node data.  清除指定传感器的数据.
     *
     * @param node the node
     */
    public void clearNodeData(Node node){
        try {
            myDatabase.deleteRow(TABLE_DATA,KEY_NODE_ADDRESS+" = \'"+node.address+"\'"+" AND "+KEY_NODE_PARENT+" = \'"+node.parent+"\'");
        } catch (MySQLException e) {
            System.out.println(e.print());
        }
    }

    /**
     * Retrieve data from db timeseries.       从数据库中读取指定时间内的传感器数据.
     *
     * @param timeSeries    the time series    读取的数据将存入timeSeries中
     * @param parentAddress the parent address
     * @param nodeAddress   the node address
     * @param startdate     the startdate      时间上限
     * @param enddate       the enddate        时间下限
     * @return the time series
     */
    public TimeSeries retrieveDataFromDB(TimeSeries timeSeries,String parentAddress,String nodeAddress,  Date startdate, Date enddate){
        Timestamp start=new Timestamp(startdate.getTime());
        Timestamp end=new Timestamp(enddate.getTime());
        List<Object> holders=new ArrayList<>();
        holders.add(parentAddress);
        holders.add(nodeAddress);
        holders.add(start);
        holders.add(end);
        ResultSet resultSet=null;
        try {
            resultSet = myDatabase.query("SELECT "+KEY_DATA_TIME+","+KEY_DATA_VAL+" FROM " + TABLE_DATA + " WHERE "+KEY_NODE_PARENT+" = ? AND "+KEY_NODE_ADDRESS+" = ? AND "+KEY_DATA_TIME+" BETWEEN ? AND ? ",holders);
            timeSeries.clear();
                while (resultSet.next()) {
                    timeSeries.add(new Millisecond(resultSet.getTimestamp(KEY_DATA_TIME)),resultSet.getFloat(KEY_DATA_VAL));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            myDatabase.close();
        }
        return timeSeries;
    }

//    public Node searchNode(TreeNode parent, DefaultMutableTreeNode mutableTreeNode) {
//        Integer Level = mutableTreeNode.getLevel();
//        ResultSet resultSet = null;
//        if (Level == 1) {
//            List<Object> holders = new ArrayList<>();
//            holders.add(mutableTreeNode.toString());
//            try {
//                resultSet = myDatabase.query("SELECT * FROM " + TABLE_SINK +" WHERE " + KEY_SINK_NAME + " = ? ", holders);
//                if(resultSet.next()){
//                    return new SinkNode(resultSet.getString(KEY_SINK_NAME),resultSet.getString(KEY_SINK_ADDRESS),resultSet.getString(KEY_SINK_DETAIL));
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                if (resultSet != null) {
//                    try {
//                        resultSet.close();
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    }
//                }
//                myDatabase.close();
//            }
//        }else if(Level==2){
//            List<Object> holders = new ArrayList<>();
//            holders.add(mutableTreeNode.toString());
//            holders.add(parent.toString());
//            try {
//                resultSet = myDatabase.query("SELECT * FROM " + TABLE_SINK +" WHERE " + KEY_NODE_NAME + " = ? AND "+KEY_NODE_PARENT+" = ?", holders);
//                if(resultSet.next()){
//                    return new Node(resultSet.getString(KEY_NODE_PARENT),resultSet.getString(KEY_NODE_NAME),resultSet.getString(KEY_NODE_DETAIL),resultSet.getString(KEY_NODE_ADDRESS));
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                if (resultSet != null) {
//                    try {
//                        resultSet.close();
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    }
//                }
//                myDatabase.close();
//            }
//        }
//        return null;
//    }

    /**
     * Clear. 清空所有表.
     *
     * @return the boolean
     */
    public Boolean clear() {
        try {
            myDatabase.clearTable(TABLE_SINK);
            myDatabase.clearTable(TABLE_NODE);
            myDatabase.clearTable(TABLE_DATA);
        } catch (MySQLException e) {
            e.printStackTrace();
        }
        return true;
    }
}

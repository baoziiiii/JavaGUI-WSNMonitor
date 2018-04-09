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

public class NodeManager {

    private static NodeManager nodeManager = new NodeManager();

    private NodeManager() {
    }

    public static NodeManager getNodeManager() {
        return nodeManager;
    }

    private static MyDatabase myDatabase = MyDatabase.getMyDatabase();

    public final static String KEY_SINK_NAME = "SinkName";
    public final static String KEY_SINK_ADDRESS = "SinkAddress";
    public final static String KEY_SINK_DETAIL = "SinkDetail";
    public final static String KEY_NODE_NAME = "NodeName";
    public final static String KEY_NODE_DETAIL = "NodeDetail";
    public final static String KEY_NODE_PARENT = "NodeParent";
    public final static String KEY_DATA_TIME = "DataTime";
    public final static String KEY_DATA_VAL = "DataVal";


    public final static String TABLE_SINK = "Sink";
    public final static String TABLE_NODE = "Node";
    public final static String TABLE_DATA = "Data";

    static {
        Map<String, String> name_type_map = new HashMap<>();
        List<String> primaryKey = new ArrayList<>();
        try {
            name_type_map.put(KEY_SINK_NAME, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_SINK_ADDRESS, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_SINK_DETAIL, MyDatabase.TYPE_VARCHAR);
            primaryKey.add(KEY_SINK_NAME);
            myDatabase.createTable(TABLE_SINK, name_type_map, primaryKey, null);
        }catch (MySQLException e){

        }
        try {
            name_type_map.clear();
            name_type_map.put(KEY_NODE_NAME, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_NODE_PARENT, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_NODE_DETAIL, MyDatabase.TYPE_VARCHAR);
            primaryKey.clear();
            primaryKey.add(KEY_NODE_PARENT);
            primaryKey.add(KEY_NODE_NAME);
            myDatabase.createTable(TABLE_NODE, name_type_map, primaryKey, null);
        }catch (MySQLException e){
        }

        try {
            name_type_map.clear();
            name_type_map.put(KEY_NODE_NAME, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_NODE_PARENT, MyDatabase.TYPE_VARCHAR);
            name_type_map.put(KEY_DATA_TIME, MyDatabase.TYPE_TIMESTAMP);
            name_type_map.put(KEY_DATA_VAL, MyDatabase.TYPE_FLOAT);
            primaryKey.clear();
            primaryKey.add(KEY_DATA_TIME);
            primaryKey.add(KEY_NODE_NAME);
            primaryKey.add(KEY_NODE_PARENT);
            myDatabase.createTable(TABLE_DATA, name_type_map, primaryKey, null);
        }
          catch (MySQLException e) {
        }
    }

    public void syncByDatabase(NodeTree nodeTree){
        ResultSet resultSet = null;
        try {
            resultSet = myDatabase.query("SELECT * FROM " + TABLE_SINK,null);
            while (resultSet.next()) {
                Map<String,Object> map=new HashMap<>();
                map.put(KEY_SINK_NAME,resultSet.getString(KEY_SINK_NAME));
                map.put(KEY_SINK_ADDRESS,resultSet.getString(KEY_SINK_ADDRESS));
                map.put(KEY_SINK_DETAIL,resultSet.getString(KEY_SINK_DETAIL));
                nodeTree.getSinkNodeList().add(new SinkNode(map));
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
        try {
            resultSet = myDatabase.query("SELECT * FROM " + TABLE_NODE,null);
            while (resultSet.next()) {
                Map<String,Object> map=new HashMap<>();
                map.put(KEY_NODE_NAME,resultSet.getString(KEY_NODE_NAME));
                map.put(KEY_NODE_DETAIL,resultSet.getString(KEY_NODE_DETAIL));
                map.put(KEY_NODE_PARENT,resultSet.getString(KEY_NODE_PARENT));
                for(SinkNode sinkNode:nodeTree.getSinkNodeList()){
                    if(sinkNode.getName().equals(resultSet.getString(KEY_NODE_PARENT))) {
                        sinkNode.nodeList.add(new Node(map));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        }
    }

    public Boolean addSinkNode(SinkNode sinkNode) {
        Map<String, Object> map = sinkNode.toTable();
        try {
            myDatabase.insertRow(TABLE_SINK, map);
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean removeSinkNode(SinkNode sinkNode) {
        String where = KEY_SINK_NAME + " = \'" + sinkNode.name+"\'";
        String where2=KEY_NODE_PARENT + " = \'"+sinkNode.name+"\'";
        try {
            myDatabase.deleteRow(TABLE_SINK, where);
            myDatabase.deleteRow(TABLE_NODE, where2);
            myDatabase.deleteRow(TABLE_DATA, where2);
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public Boolean addNode(Node node) {
        Map<String, Object> map = node.toTable();
        try {
            myDatabase.insertRow(TABLE_NODE, map);
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean removeNode(SinkNode sinkNode, Node node) {
        String where = KEY_NODE_NAME + " = \'" + node.name + "\' AND " + KEY_NODE_PARENT + " = \'" + sinkNode.name+"\'";
        try {
            myDatabase.deleteRow(TABLE_NODE, where);
            myDatabase.deleteRow(TABLE_DATA, where);
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean updateSinkNode(SinkNode oldSinkNode, SinkNode newSinkNode) {
        try {
            Map<String, Object> map = new HashMap<>();
            String where = KEY_SINK_NAME + " = \'" + oldSinkNode.name + "\'";
            String where2 = KEY_NODE_PARENT + " = \'" + oldSinkNode.name + "\'";
            if(oldSinkNode.getName().equals(newSinkNode.getName())) {
            }else {
                map.put(KEY_NODE_PARENT,newSinkNode.name);
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
            e.printStackTrace();
            return false;
        }
    }

    public Boolean updateNode(SinkNode sinkNode, Node oldnode, Node newnode) {
        try {
            String where = KEY_NODE_NAME + " = \'" + oldnode.name + "\' AND " + KEY_NODE_PARENT + " = \'" + sinkNode.name+"\'";
            Map<String, Object> map = new HashMap<>();
            map.put(KEY_NODE_NAME, newnode.name);
            map.put(KEY_NODE_PARENT,newnode.parent);
            myDatabase.updateRow(TABLE_DATA, map, where);
            map.put(KEY_NODE_DETAIL,newnode.detail);
            myDatabase.updateRow(TABLE_NODE, map, where);
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean addNewData(String sinkNodeName,String nodeName, Timestamp timestamp, Float value){
        Map<String,Object> map=new HashMap<>();
        map.put(KEY_NODE_NAME,nodeName);
        map.put(KEY_NODE_PARENT, sinkNodeName);
        map.put(KEY_DATA_TIME, timestamp);
        map.put(KEY_DATA_VAL, value);
        try {
            myDatabase.insertRow(TABLE_DATA,map);
            return true;
        } catch (MySQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void clearAllData(){
        try {
            myDatabase.clearTable(TABLE_DATA);
        } catch (MySQLException e) {
            System.out.println(e.print());
        }
    }

    public void clearNodeData(Node node){
        try {
            myDatabase.deleteRow(TABLE_DATA,KEY_NODE_NAME+" = \'"+node.name+"\'");
        } catch (MySQLException e) {
            System.out.println(e.print());
        }
    }

    public TimeSeries retrieveDataFromDB(TimeSeries timeSeries,String sinkNodeName, String nodeName, Date startdate, Date enddate){
        Timestamp start=new Timestamp(startdate.getTime());
        Timestamp end=new Timestamp(enddate.getTime());
        List<Object> holders=new ArrayList<>();
        holders.add(sinkNodeName);
        holders.add(nodeName);
        holders.add(start);
        holders.add(end);
        ResultSet resultSet=null;
        try {
            resultSet = myDatabase.query("SELECT "+KEY_DATA_TIME+","+KEY_DATA_VAL+" FROM " + TABLE_DATA + " WHERE "+KEY_NODE_PARENT+" = ? AND "+KEY_NODE_NAME+" = ? AND "+KEY_DATA_TIME+" BETWEEN ? AND ? ",holders);
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

    public Node searchNode(TreeNode parent, DefaultMutableTreeNode mutableTreeNode) {
        Integer Level = mutableTreeNode.getLevel();
        ResultSet resultSet = null;
        if (Level == 1) {
            List<Object> holders = new ArrayList<>();
            holders.add(mutableTreeNode.toString());
            try {
                resultSet = myDatabase.query("SELECT * FROM " + TABLE_SINK +" WHERE " + KEY_SINK_NAME + " = ? ", holders);
                if(resultSet.next()){
                    return new SinkNode(resultSet.getString(KEY_SINK_NAME),resultSet.getString(KEY_SINK_ADDRESS),resultSet.getString(KEY_SINK_DETAIL));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (resultSet != null) {
                    try {
                        resultSet.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                myDatabase.close();
            }
        }else if(Level==2){
            List<Object> holders = new ArrayList<>();
            holders.add(mutableTreeNode.toString());
            holders.add(parent.toString());
            try {
                resultSet = myDatabase.query("SELECT * FROM " + TABLE_SINK +" WHERE " + KEY_NODE_NAME + " = ? AND "+KEY_NODE_PARENT+" = ?", holders);
                if(resultSet.next()){
                    return new Node(resultSet.getString(KEY_NODE_PARENT),resultSet.getString(KEY_NODE_NAME),resultSet.getString(KEY_NODE_DETAIL));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (resultSet != null) {
                    try {
                        resultSet.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                myDatabase.close();
            }
        }
        return null;
    }

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

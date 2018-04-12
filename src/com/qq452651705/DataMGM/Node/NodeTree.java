package com.qq452651705.DataMGM.Node;

import com.qq452651705.JDBC.MyDatabase;
import com.qq452651705.Utils.Bean;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static com.qq452651705.DataMGM.Node.NodeManager.*;

/**
 * The type Node jTree.  管理节点树相关操作.实现Swing的JTree组件及模型.
 */
public class NodeTree {


    private NodeManager nodeManager = NodeManager.getNodeManager();

    /**
     *  JTree Swing树组件
     */
    private JTree jTree;

    /**
     *  root JTree模型的
     */
    private DefaultMutableTreeNode root = new DefaultMutableTreeNode("Sink节点树");

    /**
     *  sinkNodeList 节点链表
     */
    private List<SinkNode> sinkNodeList = new ArrayList<>();



    /**
     * Instantiates a new NodeTree.
     *
     * @param jTree the jTree
     */
    public NodeTree(JTree jTree) {
        this.jTree = jTree;
    }

    /**
     * Generate jTree. 根据
     */
    public void generateJTree(){
        root.removeAllChildren();
        for (SinkNode sinkNode:sinkNodeList) {
            DefaultMutableTreeNode sink=new DefaultMutableTreeNode(sinkNode.address+":"+sinkNode.name);
            for (Node node:sinkNode.nodeList) {
               sink.add(new DefaultMutableTreeNode(node.address+":"+node.name));
            }
            root.add(sink);
        }
    }

    /**
     * Add sink node boolean.
     *
     * @param sinkNode the sink node
     * @return the boolean
     */
    public Boolean addSinkNode(SinkNode sinkNode){
        if(!containSinkNode(sinkNode)) {
            sinkNodeList.add(sinkNode);
            nodeManager.addSinkNode(sinkNode);
            root.add(new DefaultMutableTreeNode(sinkNode.address+":"+sinkNode.name));
            SwingUtilities.invokeLater(()-> jTree.updateUI());
            return true;
        }else{
            return false;
        }
    }

    /**
     * Add node boolean.
     *
     * @param node the node
     * @return the boolean
     */
    public Boolean addNode(Node node){
        SinkNode sinkNode=searchSinkNode(node.parent);
        if(sinkNode!=null){
            if(nodeManager.addNode(node)) {
                sinkNode.addNode(node);
                generateJTree();
                SwingUtilities.invokeLater(() -> jTree.updateUI());
                return true;
            }
        }
        return false;
    }

    /**
     * Insert node into node jTree.
     *
     * @param mutableTreeNode the mutable jTree node
     * @param node            the node
     */
    public void insertNodeIntoNodeTree(DefaultMutableTreeNode mutableTreeNode, Node node) {
        Integer Level = mutableTreeNode.getLevel();
        if (Level == 0) {
            SinkNode sinkNode = (SinkNode) node;
            root.add(new DefaultMutableTreeNode(sinkNode.address+":"+sinkNode.name));
            sinkNodeList.add(sinkNode);
            nodeManager.addSinkNode(sinkNode);
        } else if (Level == 1) {
            for (SinkNode sinkNode : sinkNodeList) {
                if (sinkNode.address.equals(mutableTreeNode.toString().split(":")[0])) {
                    node.parent=sinkNode.address;
                    sinkNode.addNode(node);
                    nodeManager.addNode(node);
                    mutableTreeNode.add(new DefaultMutableTreeNode(node.address+":"+node.name));
                }
            }
        }
    }

    

    /**
     * Remove node from node jTree.
     *
     * @param mutableTreeNode the mutable jTree node
     */
    public void removeNodeFromNodeTree(DefaultMutableTreeNode mutableTreeNode) {
        Integer Level = mutableTreeNode.getLevel();
        if (Level == 0) {
            mutableTreeNode.removeAllChildren();
            sinkNodeList.clear();
            nodeManager.clear();
            return;
        }
        for (SinkNode sinkNode : sinkNodeList) {
            if (Level == 1) {
                if (sinkNode.address.equals(mutableTreeNode.toString().split(":")[0])) {
                    sinkNodeList.remove(sinkNode);
                    mutableTreeNode.removeFromParent();
                    nodeManager.removeSinkNode(sinkNode.address);
                    break;
                }
            } else if (Level == 2) {
                if(sinkNode.address.equals(mutableTreeNode.getParent().toString().split(":")[0])) {
                    sinkNode.removeNode(mutableTreeNode.toString().split(":")[0]);
                    mutableTreeNode.removeFromParent();
                    nodeManager.removeNode(sinkNode.address, mutableTreeNode.toString().split(":")[0]);
                    break;
                }
            }
        }
    }

    /**
     * Update sink node.
     *
     * @param oldsinknode the oldsinknode
     * @param newsinknode the newsinknode
     */
    public void updateSinkNode(SinkNode oldsinknode,SinkNode newsinknode){
        nodeManager.updateSinkNode(oldsinknode,newsinknode);
        for (SinkNode sinkNode:sinkNodeList) {
             if(sinkNode.getAddress().equals(oldsinknode.getAddress())){
                 List<Node> nodeList=sinkNode.nodeList;
                 for (Node node:nodeList) {
                     node.setParent(newsinknode.getAddress());
                 }
                 newsinknode.setNodeList(nodeList);
                 sinkNodeList.remove(sinkNode);
                 sinkNodeList.add(newsinknode);
                 generateJTree();
                 break;
             }
        }
    }

    /**
     * Update node boolean.
     *
     * @param parent  the parent
     * @param oldnode the oldnode
     * @param newnode the newnode
     * @return the boolean
     */
    public Boolean updateNode(String parent,Node oldnode,Node newnode){
        if(!oldnode.parent.equals(newnode.parent)){
            Boolean isExist=false;
            for(SinkNode sinkNode:sinkNodeList){
                if(sinkNode.getAddress().equals(newnode.parent)){
                    isExist=true;
                    sinkNode.nodeList.add(newnode);
                    break;
                }
            }
            if(!isExist){
                return false;
            }else{
                for(SinkNode sinkNode:sinkNodeList){
                    if(sinkNode.getAddress().equals(oldnode.parent)){
                        sinkNode.nodeList.remove(oldnode);
                        generateJTree();
                        break;
                    }
                }
            }
        }else {
            for (SinkNode sinkNode : sinkNodeList) {
                if (sinkNode.getAddress().equals(parent)) {
                    for (Node node : sinkNode.nodeList) {
                        if (node.getAddress().equals(oldnode.address)) {
                            sinkNode.nodeList.remove(node);
                            sinkNode.nodeList.add(newnode);
                            generateJTree();
                            break;
                        }
                    }
                }
            }
        }
        nodeManager.updateNode(parent, oldnode, newnode);
        return true;
    }

    /**
     * Contain sink node boolean.
     *
     * @param sinkNode the sink node
     * @return the boolean
     */
    public Boolean containSinkNode(SinkNode sinkNode){
        for(SinkNode sinkNode1:sinkNodeList){
            if(sinkNode1.address.equals(sinkNode.address))
                return true;
        }
        return false;
    }

    /**
     * Search sink node sink node.
     *
     * @param sinkNodeAddress the sink node address
     * @return the sink node
     */
    public SinkNode searchSinkNode(String sinkNodeAddress){
        for(SinkNode sinkNode:sinkNodeList){
            if(sinkNode.address.equals(sinkNodeAddress)){
                return sinkNode;
            }
        }
        return null;
    }

    /**
     * Search node node.
     *
     * @param parent      the parent
     * @param nodeAddress the node address
     * @return the node
     */
    public Node searchNode(String parent,String nodeAddress){
        for(SinkNode sinkNode:sinkNodeList){
            if(sinkNode.address.equals(parent)) {
                Node node;
                if ((node = sinkNode.searchNode(nodeAddress)) != null) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Search node node.
     *
     * @param parent          the parent
     * @param mutableTreeNode the mutable jTree node
     * @return the node
     */
    public Node searchNode(TreeNode parent, DefaultMutableTreeNode mutableTreeNode) {
        Integer Level = mutableTreeNode.getLevel();
        for (SinkNode sinkNode : sinkNodeList) {
            if (Level == 1) {
                if (sinkNode.address.equals(mutableTreeNode.toString().split(":")[0])) {
                    return sinkNode;
                }
            } else if (Level == 2) {
                if (sinkNode.address.equals(parent.toString().split(":")[0])) {
                    return sinkNode.searchNode(mutableTreeNode.toString().split(":")[0]);
                }
            }
        }
        return null;
    }


    /**
     * Update ui.
     */
    public void updateUI(){
        javax.swing.SwingUtilities.invokeLater(()-> {
            jTree.updateUI();
        });
    }

    /**
     * Clear selection.
     */
    public void clearSelection(){
        javax.swing.SwingUtilities.invokeLater(()-> {
            if(jTree !=null) {
                jTree.updateUI();
                jTree.clearSelection();
            }
        });
    }

    /**
     * Gets root.
     *
     * @return the root
     */
    public DefaultTreeModel getModel() {

        return new DefaultTreeModel(root);
    }

    /**
     * Gets sink node list.
     *
     * @return the sink node list
     */
    public List<SinkNode> getSinkNodeList() {
        return sinkNodeList;
    }



    public static class TreeNodeModel{

    }
    /**
     * The type Node.
     */
    public static class Node implements Bean {

        /**
         * The Address.
         */
        String address;
        /**
         * The Name.
         */
        String name;
        /**
         * The Detail.
         */
        String detail;
        /**
         * The Parent.
         */
        String parent;
        /**
         * The Time series.
         */
        TimeSeries timeSeries;

        private Node() {}

        /**
         * Instantiates a new Node.
         *
         * @param map the map
         */
        public Node(Map<String,Object> map){
            setFields(map);
        }

        /**
         * Instantiates a new Node.
         *
         * @param address the address
         */
        public Node(String address) {
            this.address = address;
        }

        /**
         * Instantiates a new Node.
         *
         * @param parent  the parent
         * @param name    the name
         * @param detail  the detail
         * @param address the address
         */
        public Node(String parent, String name, String detail,String address) {
            this(address);
            this.name=name;
            this.detail = detail;
            this.parent = parent;
            this.address=address;
        }

        /**
         * Gets address.
         *
         * @return the address
         */
        public String getAddress() {
            return address;
        }

        /**
         * Gets name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets parent.
         *
         * @param parent the parent
         */
        public void setParent(String parent) {
            this.parent = parent;
        }

        /**
         * Sets time series.
         *
         * @param timeSeries the time series
         */
        public void setTimeSeries(TimeSeries timeSeries) {
            this.timeSeries = timeSeries;
            ResultSet resultSet=null;
            try {
                resultSet = MyDatabase.getMyDatabase().query("SELECT * FROM " + TABLE_DATA + " WHERE "+KEY_NODE_ADDRESS+" = "+address+" AND "+ KEY_NODE_PARENT+" = "+parent+" ORDER BY "+KEY_DATA_TIME+ " DESC LIMIT 100",null);
                while (resultSet.next()) {
                    try {
                        this.timeSeries.add(new Millisecond(new Date(resultSet.getTimestamp(KEY_DATA_TIME).getTime())), resultSet.getFloat(KEY_DATA_VAL));
                    }catch (SeriesException e){}
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
                MyDatabase.getMyDatabase().close();
            }
        }

        /**
         * Gets time series.
         *
         * @return the time series
         */
        public TimeSeries getTimeSeries() {
            return timeSeries;
        }

        /**
         * Gets parent.
         *
         * @return the parent
         */
        public String getParent() {
            return parent;
        }

        /**
         * Gets detail.
         *
         * @return the detail
         */
        public String getDetail() {
            return detail;
        }

        /**
         * Add new value.
         *
         * @param date  the date
         * @param value the value
         */
        public void addNewValue(java.util.Date date, Float value){
            Timestamp timestamp=new Timestamp(date.getTime());
            getNodeManager().addNewData(address,parent,timestamp,value);
            timeSeries.add(new Millisecond(date),value);
        }

        /**
         * Clear data.
         */
        public void clearData(){
            getNodeManager().clearNodeData(this);
        }

        @Override
        public Map<String, Object> toTable() {
            Map<String, Object> map = new HashMap<>();
            map.put(KEY_NODE_NAME, name);
            map.put(KEY_NODE_DETAIL, detail);
            map.put(KEY_NODE_PARENT, parent);
            map.put(KEY_NODE_ADDRESS, address);
            return map;
        }

        /**
         * Empty bean node.
         *
         * @return the node
         */
        public static Node emptyBean() {
            return new Node("","","","");
        }

        @Override
        public void setFields(Map<String, Object> map){
            name=(String) map.get(KEY_NODE_NAME);
            detail=(String)map.get(KEY_NODE_DETAIL);
            parent=(String)map.get(KEY_NODE_PARENT);
            address=(String)map.get(KEY_NODE_ADDRESS);
        }

        @Override
        public List<String> getFieldNames() {
            List<String> fields=new ArrayList<>();
            fields.add(KEY_NODE_PARENT);
            fields.add(KEY_NODE_ADDRESS);
            fields.add(KEY_NODE_NAME);
            fields.add(KEY_NODE_DETAIL);
            return fields;
        }

        @Override
        public Bean getCopy() {
            return new Node(this.toTable());
        }
    }

    /**
     * The type Sink node.
     */
    public static class SinkNode extends Node implements Bean {

        /**
         * The Node list.
         */
        List<Node> nodeList = new ArrayList<>();
        /**
         * The Name.
         */
        String name;
        /**
         * The Address.
         */
        String address;
        /**
         * The Detail.
         */
        String detail;

        /**
         * Instantiates a new Sink node.
         *
         * @param map the map
         */
        public SinkNode(Map<String,Object> map){
            setFields(map);
        }

        /**
         * Instantiates a new Sink node.
         *
         * @param address the address
         */
        public SinkNode(String address) {
            this.address = address;
        }

        /**
         * Instantiates a new Sink node.
         *
         * @param name    the name
         * @param address the address
         * @param detail  the detail
         */
        public SinkNode(String name, String address, String detail) {
            this(address);
            this.name = name;
            this.detail = detail;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        /**
         * Add node.
         *
         * @param node the node
         */
        public void addNode(Node node) {

            nodeList.add(node);
        }

        /**
         * Remove node.
         *
         * @param nodeAddress the node address
         */
        public void removeNode(String nodeAddress) {
            for (Node node1 : nodeList) {
                if (node1.address != null) {
                    if (node1.address.equals(nodeAddress)) {
                        nodeList.remove(node1);
                        break;
                    }
                }
            }
        }

        /**
         * Search node node.
         *
         * @param nodeAddress the node address
         * @return the node
         */
        public Node searchNode(String nodeAddress) {
            for (Node node : nodeList) {
                if (node.address != null) {
                    if (node.address.equals(nodeAddress)) {
                        return node;
                    }
                }
            }
            return null;
        }


        /**
         * Gets node list.
         *
         * @return the node list
         */
        public List<Node> getNodeList() {
            return nodeList;
        }

        /**
         * Sets node list.
         *
         * @param nodeList the node list
         */
        public void setNodeList(List<Node> nodeList) {
            this.nodeList = nodeList;
        }


        /**
         * Empty bean sink node.
         *
         * @return the sink node
         */
        public static SinkNode emptyBean() {
            return new SinkNode("","","");
        }

        @Override
        public Map<String, Object> toTable() {
            Map<String, Object> map = new HashMap<>();
            map.put(NodeManager.KEY_SINK_NAME, name);
            map.put(NodeManager.KEY_SINK_ADDRESS, address);
            map.put(NodeManager.KEY_SINK_DETAIL, detail);
            return map;
        }


        @Override
        public void setFields(Map<String,Object> map){
            address=(String) map.get(KEY_SINK_ADDRESS);
            name= (String) map.get(KEY_SINK_NAME);
            detail=(String) map.get(KEY_SINK_DETAIL);
        }

        @Override
        public List<String> getFieldNames() {
            List<String> fields=new ArrayList<>();
            fields.add(KEY_SINK_ADDRESS);
            fields.add(KEY_SINK_NAME);
            fields.add(KEY_SINK_DETAIL);
            return fields;
        }

        @Override
        public Bean getCopy() {
            return new SinkNode(this.toTable());
        }
    }
}






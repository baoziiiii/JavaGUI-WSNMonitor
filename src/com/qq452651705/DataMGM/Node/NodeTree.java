package com.qq452651705.DataMGM.Node;

import com.qq452651705.JDBC.MyDatabase;
import com.qq452651705.Utils.TBean;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
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
     *  root Sink节点树根节点
     */
    private CustomTreeNode root = new CustomTreeNode("Sink节点树");

    /**
     *  sinkNodeList 节点链表缓存
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
     * Gets root.  返回JTree根模型
     *
     * @return the root
     */
    public DefaultTreeModel getModel() {

        return new DefaultTreeModel(root);
    }

    /**
     * Generate jTree. 根据节点链表缓存sinkNodeList重新生成JTree树
     */
    public void generateJTree(){
        root.removeAllChildren();
        for (SinkNode sinkNode:sinkNodeList) {
            CustomTreeNode sink=new CustomTreeNode(sinkNode.address,sinkNode.name);
            for (Node node:sinkNode.nodeList) {
               sink.add(new CustomTreeNode(node.address,node.name));
            }
            root.add(sink);
        }
    }

    /**
     * Add sinknode. 同时向节点树,节点链表,数据库中添加新的Sink节点
     *
     * @param sinkNode the sink node
     * @return the boolean   false:地址已存在
     */
    public Boolean addSinkNode(SinkNode sinkNode){
        if(nodeManager.addSinkNode(sinkNode)) {
            sinkNodeList.add(sinkNode);
            root.add(new CustomTreeNode(sinkNode.address,sinkNode.name));
            SwingUtilities.invokeLater(()-> jTree.updateUI());
            return true;
        }else{
            return false;
        }
    }

    /**
     * Add node.  同时向节点树,节点链表,数据库中添加新的传感器节点
     *
     * @param node the node
     * @return the boolean  false:地址已存在.
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
     * Remove node from node jTree. 从节点树,节点链表,数据库中移除节点.
     *
     * @param treeNode 移除treeNode及其子节点.
     */
    public void removeNodeFromNodeTree(CustomTreeNode treeNode) {
        Integer Level = treeNode.getLevel();
        if (Level == 0) {       //如果是根节点,只移除子节点.
            treeNode.removeAllChildren();
            sinkNodeList.clear();
            nodeManager.clear();
            return;
        }
        for (SinkNode sinkNode : sinkNodeList) {
            if (Level == 1) {
                if (sinkNode.address.equals(treeNode.address)) {
                    sinkNodeList.remove(sinkNode);
                    treeNode.removeFromParent();
                    nodeManager.removeSinkNode(sinkNode.address);
                    break;
                }
            } else if (Level == 2) {
                CustomTreeNode parentTreeNode=(CustomTreeNode) treeNode.getParent();
                if(sinkNode.address.equals(parentTreeNode.address)){
                    sinkNode.removeNode(treeNode.address);
                    treeNode.removeFromParent();
                    nodeManager.removeNode(sinkNode.address, treeNode.address);
                    break;
                }
            }
        }
    }

    /**
     * Update sink node.   更新Sink节点信息.如果Sink节点地址改变，则将旧Sink节点下的所有传感器移动到新的Sink节点下，并移除旧Sink节点.
     *
     * @param oldsinknode the oldsinknode
     * @param newsinknode the newsinknode
     */
    public void updateSinkNode(SinkNode oldsinknode,SinkNode newsinknode){
        nodeManager.updateSinkNode(oldsinknode,newsinknode);
        for (SinkNode sinkNode:sinkNodeList) {
             if(sinkNode.getAddress().equals(oldsinknode.getAddress())){
                 List<Node> nodeList=sinkNode.nodeList;
                 for (Node node:nodeList)
                     node.setParent(newsinknode.getAddress());
                 newsinknode.setNodeList(nodeList);
                 sinkNodeList.remove(sinkNode);
                 sinkNodeList.add(newsinknode);
                 generateJTree();
                 break;
             }
        }
    }

    /**
     * Update node.     更新传感器节点信息.如果传感器父地址改变,则移动至新的父地址下.
     *
     * @param oldnode the oldnode
     * @param newnode the newnode
     * @return the boolean          false:没有找到新的父地址
     * @throws Exception the exception  地址重复
     */
    public Boolean updateNode(Node oldnode,Node newnode) throws Exception{

        for(SinkNode sinkNode:sinkNodeList){
            if(sinkNode.address.equals((newnode.parent))){
                for (Node node:sinkNode.nodeList) {
                    if(node.address.equals(newnode.address))
                        if(!(oldnode.parent.equals(newnode.parent)&&oldnode.address.equals(newnode.address))) {
                            throw new Exception(); //新父地址下有相同地址或原父地址下有相同的新地址
                        }
                }
                if(oldnode.parent.equals(newnode.parent)){  //父地址不变
                     sinkNode.nodeList.remove(oldnode);
                     sinkNode.nodeList.add(newnode);
                }else{        //父地址改变,寻找旧地址的父地址
                    sinkNode.nodeList.add(newnode);
                    for(SinkNode sinkNode1:sinkNodeList){
                        if(oldnode.parent.equals(sinkNode1.address)){
                            sinkNode1.nodeList.remove(oldnode);
                        }
                    }
                }
                nodeManager.updateNode(oldnode,newnode);
                generateJTree();
                return true;
            }
        }
        return false; //没有找到新节点的父地址
    }

    /**
     * Contain sink node.  查询Sink地址在节点链表中是否存在
     *
     * @param sinkNodeAddress
     * @return the boolean    true:地址存在 false:地址不存在
     */
    public Boolean containSinkNode(String sinkNodeAddress){
        for(SinkNode sinkNode:sinkNodeList){
            if(sinkNode.address.equals(sinkNodeAddress))
                return true;
        }
        return false;
    }

    /**
     * Search sink node.   获取指定Sink地址在节点链表中的SinkNode对象
     *
     * @param sinkNodeAddress the sink node address
     * @return the sink node   null:不存在
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
     * Search node.   获取指定传感器地址在节点链表中的Node对象
     *
     * @param parent      the parent
     * @param nodeAddress the node address
     * @return the node     null:不存在
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
     * Search node.    获取指定TreeNode对象在节点链表中对应的Node对象(SinkNode或Node)
     *
     * @param parent   the parent
     * @param treeNode the custom jTree node
     * @return the node    null:不存在
     */
    public Node searchNode(CustomTreeNode parent, CustomTreeNode treeNode) {
        Integer Level = treeNode.getLevel();
        for (SinkNode sinkNode : sinkNodeList) {
            if (Level == 1) {   //SinkNode
                if (sinkNode.address.equals(treeNode.getAddress())) {
                    return sinkNode;
                }
            } else if (Level == 2) { //Node
                if (sinkNode.address.equals(parent.getAddress())) {
                    return sinkNode.searchNode(treeNode.getAddress());
                }
            }
        }
        return null;
    }

    /**
     * Update ui. 更新jTree
     */
    public void updateUI(){
        javax.swing.SwingUtilities.invokeLater(()->jTree.updateUI());
    }

    /**
     * Clear selection.  清空jTree的所有选中状态
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
     * Gets sink node list.  获取节点链表
     *
     * @return the sink node list
     */
    public List<SinkNode> getSinkNodeList() {
        return sinkNodeList;
    }


    /**
     * The type Custom tree node. 自定义树节点.
     */
    public static class CustomTreeNode extends DefaultMutableTreeNode{
        /**
         * The Address.
         */
        String address;
        /**
         * The Name.
         */
        String name;

        /**
         * Instantiates a new Custom tree node. 用于树根初始化
         *
         * @param rootName the root name
         */
        public CustomTreeNode(String rootName){
            super(rootName);
        }

        /**
         * Instantiates a new Custom tree node.  用于节点初始化
         *
         * @param address the address   地址
         * @param name    the name      命名
         */
        public CustomTreeNode(String address,String name){
            super(address+":"+name);  //树节点上显示“地址：命名”
            this.address = address;
            this.name = name;
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

    }

    /**
     * The type Node.  传感器节点类.
     */
    public static class Node implements TBean {

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
         * Instantiates a new Node. 通过map初始化.map中的keys对应Node表的所有列名
         *
         * @param map the map
         */
        public Node(Map<String,Object> map){
            setFields(map);
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
            this.name=name;
            this.detail = detail;
            this.parent = parent;
            this.address=address;
        }

        /**
         * Sets name.
         *
         * @param name the name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Sets detail.
         *
         * @param detail the detail
         */
        public void setDetail(String detail) {
            this.detail = detail;
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
         * Sets time series.   从数据库中导入100条最近数据
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
         * Add new value.         添加新监测值
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


        /**
         * Empty bean node.  输出空传感器对象(用于接收添加的节点)
         *
         * @return the node
         */
        public static Node emptyNode() {
            return new Node("","","","");
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public void setFields(Map<String, Object> map){
            name=(String) map.get(KEY_NODE_NAME);
            detail=(String)map.get(KEY_NODE_DETAIL);
            parent=(String)map.get(KEY_NODE_PARENT);
            address=(String)map.get(KEY_NODE_ADDRESS);
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public List<String> getFieldNames() {
            List<String> fields=new ArrayList<>();
            fields.add(KEY_NODE_PARENT);
            fields.add(KEY_NODE_ADDRESS);
            fields.add(KEY_NODE_NAME);
            fields.add(KEY_NODE_DETAIL);
            return fields;
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
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
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public TBean getCopy() {
            return new Node(this.toTable());
        }
    }

    /**
     * The type Sinknode. Sink节点类.
     */
    public static class SinkNode extends Node implements TBean {

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
         * @param name    the name
         * @param address the address
         * @param detail  the detail
         */
        public SinkNode(String name, String address, String detail) {
            this.address=address;
            this.name = name;
            this.detail = detail;
        }

        /**
         * Add node.     添加传感节点
         *
         * @param node the node
         */
        public void addNode(Node node) {
            nodeList.add(node);
        }

        /**
         * Remove node.  移除传感节点
         *
         * @param nodeAddress the node address
         */
        public void removeNode(String nodeAddress) {
            for (Node node : nodeList) {
                if (node.address != null) {
                    if (node.address.equals(nodeAddress)) {
                        nodeList.remove(node);
                        break;
                    }
                }
            }
        }

        /**
         * Search node.  通过地址获取传感节点
         *
         * @param nodeAddress the node address
         * @return the node   null:不存在
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
         * Gets node list.  获取传感节点列表
         *
         * @return the node list
         */
        public List<Node> getNodeList() {
            return nodeList;
        }

        /**
         * Sets node list.  设置传感节点列表.
         *
         * @param nodeList the node list
         */
        public void setNodeList(List<Node> nodeList) {
            this.nodeList = nodeList;
        }

        /**
         * Empty bean sink node.  输出空Sink节点对象(用于接收添加的节点)
         *
         * @return the sink node
         */
        public static SinkNode emptySinkNode() {
            return new SinkNode("","","");
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public void setFields(Map<String,Object> map){
            address=(String) map.get(KEY_SINK_ADDRESS);
            name= (String) map.get(KEY_SINK_NAME);
            detail=(String) map.get(KEY_SINK_DETAIL);
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public List<String> getFieldNames() {
            List<String> fields=new ArrayList<>();
            fields.add(KEY_SINK_ADDRESS);
            fields.add(KEY_SINK_NAME);
            fields.add(KEY_SINK_DETAIL);
            return fields;
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public Map<String, Object> toTable() {
            Map<String, Object> map = new HashMap<>();
            map.put(NodeManager.KEY_SINK_NAME, name);
            map.put(NodeManager.KEY_SINK_ADDRESS, address);
            map.put(NodeManager.KEY_SINK_DETAIL, detail);
            return map;
        }

        /**
         * @See com.qq452651705.Utils.TBean
         */
        @Override
        public TBean getCopy() {
            return new SinkNode(this.toTable());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        @Override
        public String getDetail() {
            return detail;
        }

        @Override
        public void setDetail(String detail) {
            this.detail = detail;
        }
    }
}






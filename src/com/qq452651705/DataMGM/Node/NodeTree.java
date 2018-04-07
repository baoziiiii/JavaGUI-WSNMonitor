package com.qq452651705.DataMGM.Node;

import com.qq452651705.Utils.Bean;
import org.jfree.data.time.TimeSeries;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.sql.Timestamp;
import java.util.*;

import static com.qq452651705.DataMGM.Node.NodeManager.*;

public class NodeTree {

    private List<SinkNode> sinkNodeList = new ArrayList<>();
    private DefaultMutableTreeNode root = new DefaultMutableTreeNode("Sink节点树");

    private NodeManager nodeManager = NodeManager.getNodeManager();

    private JTree tree;


    public NodeTree(JTree tree) {
        this.tree=tree;
    }

    public void generateJTree(){
        root.removeAllChildren();
        for (SinkNode sinkNode:sinkNodeList) {
            DefaultMutableTreeNode sink=new DefaultMutableTreeNode(sinkNode.name);
            for (Node node:sinkNode.nodeList) {
               sink.add(new DefaultMutableTreeNode(node.name));
            }
            root.add(sink);
        }
    }

    public void addNode(DefaultMutableTreeNode mutableTreeNode, Node node) {
        Integer Level = mutableTreeNode.getLevel();
        if (Level == 0) {
            SinkNode sinkNode = (SinkNode) node;
            root.add(new DefaultMutableTreeNode(sinkNode.name));
            sinkNodeList.add(sinkNode);
            nodeManager.addSinkNode(sinkNode);
        } else if (Level == 1) {
            for (SinkNode sinkNode : sinkNodeList) {
                if (sinkNode.name.equals(mutableTreeNode.toString())) {
                    node.parent=sinkNode.name;
                    sinkNode.addNode(node);
                    nodeManager.addNode(node);
                    mutableTreeNode.add(new DefaultMutableTreeNode(node.name));
                }
            }
        }
    }

    public void removeNode(DefaultMutableTreeNode mutableTreeNode) {
        Integer Level = mutableTreeNode.getLevel();
        if (Level == 0) {
            mutableTreeNode.removeAllChildren();
            sinkNodeList.clear();
            nodeManager.clear();
            return;
        }
        for (SinkNode sinkNode : sinkNodeList) {
            if (Level == 1) {
                if (sinkNode.name.equals(mutableTreeNode.toString())) {
                    sinkNodeList.remove(sinkNode);
                    mutableTreeNode.removeFromParent();
                    nodeManager.removeSinkNode(sinkNode);
                    break;
                }
            } else if (Level == 2) {
                if(sinkNode.name.equals(mutableTreeNode.getParent().toString())) {
                    sinkNode.removeNode(mutableTreeNode.toString());
                    mutableTreeNode.removeFromParent();
                    nodeManager.removeNode(sinkNode, new Node(mutableTreeNode.toString()));
                    break;
                }
            }
        }
    }

    public void updateSinkNode(SinkNode oldsinknode,SinkNode newsinknode){
        nodeManager.updateSinkNode(oldsinknode,newsinknode);
        for (SinkNode sinkNode:sinkNodeList) {
             if(sinkNode.getName().equals(oldsinknode.getName())){
                 List<Node> nodeList=sinkNode.nodeList;
                 for (Node node:nodeList) {
                     node.setParent(newsinknode.getName());
                 }
                 newsinknode.setNodeList(nodeList);
                 sinkNodeList.remove(sinkNode);
                 sinkNodeList.add(newsinknode);
                 generateJTree();
                 break;
             }
        }
    }

    public Boolean updateNode(String parent,Node oldnode,Node newnode){
        if(!oldnode.parent.equals(newnode.parent)){
            Boolean isExist=false;
            for(SinkNode sinkNode:sinkNodeList){
                if(sinkNode.getName().equals(newnode.parent)){
                    isExist=true;
                    sinkNode.nodeList.add(newnode);
                    break;
                }
            }
            if(!isExist){
                return false;
            }else{
                for(SinkNode sinkNode:sinkNodeList){
                    if(sinkNode.getName().equals(oldnode.parent)){
                        sinkNode.nodeList.remove(oldnode);
                        generateJTree();
                        break;
                    }
                }
            }
        }else {
            for (SinkNode sinkNode : sinkNodeList) {
                if (sinkNode.getName().equals(parent)) {
                    for (Node node : sinkNode.nodeList) {
                        if (node.getName().equals(oldnode.name)) {
                            sinkNode.nodeList.remove(node);
                            sinkNode.nodeList.add(newnode);
                            generateJTree();
                            break;
                        }
                    }
                }
            }
        }
        nodeManager.updateNode(new SinkNode(parent), oldnode, newnode);
        return true;
    }

    public Boolean containSinkNode(String sinkNode){
        for(SinkNode sinkNode1:sinkNodeList){
            if(sinkNode1.getName().equals(sinkNode))
                return true;
        }
        return false;
    }

    public Node searchNode(String parent,String nodeName){
        for(SinkNode sinkNode:sinkNodeList){
            if(sinkNode.name.equals(parent)) {
                Node node;
                if ((node = sinkNode.searchNode(nodeName)) != null) {
                    return node;
                }
            }
        }
        return null;
    }

    public Node searchNode(TreeNode parent, DefaultMutableTreeNode mutableTreeNode) {
        Integer Level = mutableTreeNode.getLevel();
        for (SinkNode sinkNode : sinkNodeList) {
            if (Level == 1) {
                if (sinkNode.name.equals(mutableTreeNode.toString())) {
                    return sinkNode;
                }
            } else if (Level == 2) {
                if (sinkNode.name.equals(parent.toString())) {
                    return sinkNode.searchNode(mutableTreeNode.toString());
                }
            }
        }
        return null;
    }

    public void updateUI(){
        javax.swing.SwingUtilities.invokeLater(()-> {
            tree.updateUI();
        });
    }

    public void clearSelection(){
        javax.swing.SwingUtilities.invokeLater(()-> {
            if(tree!=null) {
                tree.updateUI();
                tree.clearSelection();
            }
        });
    }

    public DefaultMutableTreeNode getRoot() {
        return root;
    }

    public List<SinkNode> getSinkNodeList() {
        return sinkNodeList;
    }

    public static class Node implements Bean {

        String name;
        String detail;
        String parent;
        TimeSeries timeSeries;

        private Node() {}

        public Node(Map<String,Object> map){
            setFields(map);
        }

        public Node(String name) {
            this.name = name;
        }

        public Node(String parent, String name, String detail) {
            this(name);
            this.detail = detail;
            this.parent = parent;
        }


        public String getName() {
            return name;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }

        public void setTimeSeries(TimeSeries timeSeries) {
            this.timeSeries = timeSeries;
        }

        public TimeSeries getTimeSeries() {
            return timeSeries;
        }

        public void addNewValue(java.util.Date date, Float value){
            Timestamp timestamp=new Timestamp(date.getTime());
            getNodeManager().addNewData(parent,name,timestamp,value);
        }

        @Override
        public Map<String, Object> toTable() {
            Map<String, Object> map = new HashMap<>();
            map.put(KEY_NODE_NAME, name);
            map.put(KEY_NODE_DETAIL, detail);
            map.put(KEY_NODE_PARENT, parent);
            return map;
        }

        public static Node emptyBean() {
            return new Node("","","");
        }

        @Override
        public void setFields(Map<String, Object> map){
            name=(String) map.get(KEY_NODE_NAME);
            detail=(String)map.get(KEY_NODE_DETAIL);
            parent=(String)map.get(KEY_NODE_PARENT);
        }

        @Override
        public List<String> getFieldNames() {
            List<String> fields=new ArrayList<>();
            fields.add(KEY_NODE_NAME);
            fields.add(KEY_NODE_DETAIL);
            fields.add(KEY_NODE_PARENT);
            return fields;
        }
    }

    public static class SinkNode extends Node implements Bean {

        List<Node> nodeList = new ArrayList<>();
        String name;
        String address;
        String detail;

        public SinkNode(Map<String,Object> map){
            setFields(map);
        }

        public SinkNode(String name) {
            this.name = name;
        }

        public SinkNode(String name, String address, String detail) {
            this(name);
            this.address = address;
            this.detail = detail;
        }

        public String getName() {
            return name;
        }

        public void addNode(Node node) {
            nodeList.add(node);
        }

        public void removeNode(String nodeName) {
            for (Node node1 : nodeList) {
                if (node1.name != null) {
                    if (node1.name.equals(nodeName)) {
                        nodeList.remove(node1);
                        break;
                    }
                }
            }
        }

        public Node searchNode(String nodeName) {
            for (Node node : nodeList) {
                if (node.name != null) {
                    if (node.name.equals(nodeName)) {
                        return node;
                    }
                }
            }
            return null;
        }


        public List<Node> getNodeList() {
            return nodeList;
        }

        public void setNodeList(List<Node> nodeList) {
            this.nodeList = nodeList;
        }


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
            name= (String) map.get(KEY_SINK_NAME);
            detail=(String) map.get(KEY_SINK_DETAIL);
            address=(String) map.get(KEY_SINK_ADDRESS);
        }

        @Override
        public List<String> getFieldNames() {
            List<String> fields=new ArrayList<>();
            fields.add(KEY_SINK_NAME);
            fields.add(KEY_SINK_DETAIL);
            fields.add(KEY_SINK_ADDRESS);
            return fields;
        }
    }
}






package com.qq452651705.GUI;

import com.qq452651705.DataMGM.Tourists.TouristManager;
import com.qq452651705.JDBC.MyDatabase;
import com.qq452651705.SerialComm.CommHandler;
import com.qq452651705.SerialComm.CommHandler.Command;
import com.qq452651705.SerialComm.SerialComm;
import com.qq452651705.DataMGM.Account.AccountManager;
import com.qq452651705.DataMGM.Node.NodeManager;
import com.qq452651705.DataMGM.Node.NodeTree;
import com.qq452651705.Utils.TBean;
import com.qq452651705.Utils.ExcelUtils;
import com.qq452651705.Utils.TxtUtils;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import com.qq452651705.DataMGM.Node.NodeTree.SinkNode;
import com.qq452651705.DataMGM.Node.NodeTree.Node;
import com.qq452651705.DataMGM.Node.NodeTree.CustomTreeNode;

/**
 * The type Main activity. 主界面
 */

public class MainActivity {

    /*************************GUI*******************************/
    private JFrame frame;
    private JPanel mainActivity;
    private JTabbedPane tabbedPane1;
    private JTree jTree;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu configMenu;
    private JMenu accountMenu;
    private JMenu nodeMenu;
    private JMenuItem outputChartItem;
    private JMenuItem outputTouristItem;
    private JMenuItem autoGenerate;
    private JMenuItem autoSaveLogItem;
    private JMenuItem syncItem;
    //  private Boolean autoGenerateFlag = false;
    private Boolean enableSelectForSinkNodeCombo = false;
    private JComboBox SinkNodeCombo;
    private JComboBox NodeCombo;
    /*************************GUI*******************************/

    /**
     * 标签页:节点管理，监测管理，游客管理
     */
    private NodeTab nodeTab;
    private CommTab commTab;
    private TouristTab touristTab;

    /**
     * 一些管理服务类
     */
    private SerialPort comm;
    private AccountManager accountManager = AccountManager.getAccountManager();
    private NodeManager nodeManager = NodeManager.getNodeManager();
    private TouristManager touristManager = TouristManager.getTouristManager();

    /**
     * 节点树
     */
    private NodeTree nodeTree = new NodeTree(jTree);


    /**
     * (图表)所选的传感器父Sink地址
     */
    private String sinkNodeAddressForChart;
    /**
     * (图表)所选的传感器地址
     */
    private String nodeAddressForChart;

    /**
     * (图表)所选的传感器节点
     */
    private Node nodeForChart;

    /**
     * 向单片机轮询数据的间隔(毫秒)
     */
    private Integer REQUEST_FOR_DATA_INTERVAL = 5000;

    /**
     * Instantiates a new Main activity. 初始化主界面
     *
     * @param frame the frame
     */
    public MainActivity(JFrame frame) {

        this.frame = frame;
        frame.setContentPane(tabbedPane1);
        MainFrame.setframeSize(MainFrame.screenWidth * 2 / 3, MainFrame.screenHeight * 2 / 3, true);
        frame.setVisible(true);

        initMenuBar();

        nodeTab = new NodeTab();
        commTab = new CommTab();
        touristTab = new TouristTab();

        /**
         *  标签页切换触发
         *
         */
        tabbedPane1.addChangeListener((l) ->
        {
            Component tab = tabbedPane1.getSelectedComponent();

            //先清空所有独占菜单
            outputChartItem.setEnabled(false);
            autoSaveLogItem.setEnabled(false);
            outputTouristItem.setEnabled(false);

            if (tab.equals(CommJPanel)) { //选中监测控制,开启对应的独占菜单
                outputChartItem.setEnabled(true);
                autoSaveLogItem.setEnabled(true);
                SinkNodeCombo.removeAllItems();
                NodeCombo.removeAllItems();
                enableSelectForSinkNodeCombo = !(nodeTree.containSinkNode(sinkNodeAddressForChart));
                for (SinkNode sinkNode : nodeTree.getSinkNodeList()) {
                    SinkNodeCombo.addItem(sinkNode.getAddress() + ":" + sinkNode.getName());
                }
                enableSelectForSinkNodeCombo = true;
                if (sinkNodeAddressForChart != null && nodeAddressForChart != null) {
                    SinkNode s = nodeTree.searchSinkNode(sinkNodeAddressForChart);
                    Node n = nodeTree.searchNode(sinkNodeAddressForChart, nodeAddressForChart);
                    if (s != null && n != null) {
                        SinkNodeCombo.setSelectedItem(sinkNodeAddressForChart + ":" + s.getName());
                        NodeCombo.setSelectedItem(nodeAddressForChart + ":" + n.getName());
                    }
                } else {
                    JOptionPane.showConfirmDialog(frame, "未找到可监测的传感节点，请打开串口并前往节点管理，进行串口同步！", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                }
                commTab.startNewChart();
            } else if (tab.equals(TouristJPanel)) { //选中游客标签
                outputTouristItem.setEnabled(true);
            }
        });

    }

    /**
     * Instantiates a new Main activity. 初始化菜单栏
     */
    private void initMenuBar() {
        menuBar = frame.getJMenuBar();
        fileMenu = menuBar.getMenu(0);
        configMenu = menuBar.getMenu(1);
        accountMenu = menuBar.getMenu(2);

        nodeMenu = new JMenu("节点");
        syncItem = new JMenuItem("从串口同步节点");
        nodeMenu.add(syncItem);
        menuBar.add(nodeMenu);

        JMenu databaseMenu = new JMenu("数据库");
        JMenuItem cleardbMenu = new JMenuItem("清空数据库中的所有传感器数据");
        cleardbMenu.addActionListener((l) -> {
            int selection = JOptionPane.showConfirmDialog(frame, "确定要清空所有传感器数据吗？删除操作不可恢复！", "警告", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (selection == JOptionPane.OK_OPTION) {
                nodeManager.clearAllData();
                JOptionPane.showConfirmDialog(frame, "传感器数据已清空！", "成功", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
            }
        });
        databaseMenu.add(cleardbMenu);
        menuBar.add(databaseMenu);

        outputChartItem = new JMenuItem("导出图表(Excel)");
        outputChartItem.setEnabled(false);
        fileMenu.insert(outputChartItem, 0);

        outputTouristItem = new JMenuItem("导出游客信息(Excel)");
        outputTouristItem.setEnabled(false);
        fileMenu.insert(outputTouristItem, 0);


//        autoGenerate = new JMenuItem("开启自动添加子节点(演示)");
//        autoGenerate.addActionListener(e -> {
//            if (autoGenerateFlag) {
//                autoGenerateFlag = false;
//                autoGenerate.setText("开启自动添加子节点(演示)");
//                SwingUtilities.invokeLater(() -> {
//                    autoGenerate.updateUI();
//                    jTree.clearSelection();
//                });
//            } else {
//                autoGenerateFlag = true;
//                autoGenerate.setText("关闭自动添加子节点(演示)");
//                SwingUtilities.invokeLater(() -> {
//                    autoGenerate.updateUI();
//                    jTree.clearSelection();
//                });
//            }
//        });
//        configMenu.add(autoGenerate);

        autoSaveLogItem = new JMenuItem("自动缓存日志设置");
        configMenu.add(autoSaveLogItem);
        autoSaveLogItem.setEnabled(false);

        accountMenu.getMenuComponent(0).setEnabled(false); //注册选项禁止
        accountMenu.getMenuComponent(1).setEnabled(false); //找回密码选项禁止
        JMenuItem logout = new JMenuItem("退出账号");
        logout.addActionListener(e -> {
            LoginActivity loginActivity = new LoginActivity(frame);
            accountManager.setNowUserName(null);
        });
        JMenuItem changepwd = new JMenuItem("修改密码");
        changepwd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog dialog = new ChangePWDDialog(frame);
                dialog.setSize(MainFrame.screenWidth / 5, MainFrame.screenHeight / 5);
                dialog.setLocation(MainFrame.screenWidth / 2 - dialog.getWidth() / 2, MainFrame.screenHeight / 2 - dialog.getHeight() / 2);
                dialog.setVisible(true);
            }

            /**
             * Instantiates a change password dialog. 修改密码窗口
             */
            class ChangePWDDialog extends JDialog {
                public ChangePWDDialog(JFrame owner) {
                    super(owner, "修改密码", true);
                    JPanel panel = new JPanel();
                    panel.setLayout(new GridLayout(4, 2));
                    JLabel usernameLabel = new JLabel("当前账号：");
                    panel.add(usernameLabel);
                    JTextField usernameText = new JTextField(accountManager.getNowUsername());
                    usernameText.setEditable(false);
                    panel.add(usernameText);
                    JPasswordField passwordField = new JPasswordField();
                    JPasswordField passwordField2 = new JPasswordField();
                    panel.add(new JLabel("密码:"));
                    panel.add(passwordField);
                    panel.add(new JLabel("确认密码:"));
                    panel.add(passwordField2);

                    JButton button = new JButton("确认");
                    button.addActionListener(event -> {
                        String password = new String(passwordField.getPassword());
                        String confirmPassword = new String(passwordField2.getPassword());
                        String msg;
                        Boolean flag;
                        if (password.equals("") || confirmPassword.equals("")) {
                            msg = "密码不能为空！";
                            flag = false;
                        } else if (!(confirmPassword.equals(password))) {
                            msg = "两次密码不一致！";
                            flag = false;
                        } else {

                            Boolean result = accountManager.changePassword(accountManager.getNowUsername(), new String(password));
                            if (result) {
                                msg = "密码修改成功！";
                                flag = true;
                            } else {
                                msg = "密码修改失败！";
                                flag = false;
                            }
                        }
                        if (flag) {
                            JOptionPane.showConfirmDialog(frame, msg, "修改密码",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            dispose();
                        } else {
                            JOptionPane.showConfirmDialog(frame, msg, "修改密码",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        }
                    });
                    panel.add(button);
                    add(panel);
                    pack();
                }
            }

        });
        accountMenu.add(changepwd);
        accountMenu.add(logout);
    }

//    private Random random;


    /**
     * The type Node tab. 节点管理标签页
     */

    private JPanel nodeControl;    //节点管理按钮面板
    private JButton 查看节点信息Button;
    private JButton 添加子节点Button;
    private JButton 删除节点Button;
    private JButton 从串口同步节点结构Button;

    class NodeTab {

        /**
         * 节点树右键点击弹出菜单
         */
        private JMenuItem lookitem;
        private JMenuItem additem;
        private JMenuItem deleteitem;
        private JPopupMenu popupMenu;

        /**
         * 节点树当前选中节点
         */
        private CustomTreeNode selectedTreeNode;
        private String selectedNodeAddress;

        /**
         * Instantiates a new Node tab. 初始化节点标签页
         */
        NodeTab() {

            nodeManager.syncByDatabase(nodeTree);//从数据库同步节点树
            jTree.setModel(nodeTree.getModel());
            jTree.addTreeSelectionListener(e -> {
                        JTree tree = (JTree) e.getSource();
                        //利用JTree的getLastSelectedPathComponent()方法取得目前选取的节点.
                        selectedTreeNode =
                                (CustomTreeNode) tree.getLastSelectedPathComponent();
                        if (selectedTreeNode == null) return;
                        selectedNodeAddress = selectedTreeNode.getAddress();
//                        if (autoGenerateFlag) {
//                            random = new Random(System.currentTimeMillis());
//                        }
                        if (selectedTreeNode.getLevel() == 0) {
                            添加子节点Button.setText("添加Sink节点");
                            删除节点Button.setText("删除所有节点");
//                            if (autoGenerateFlag)
//                                nodeTree.insertNodeIntoNodeTree(selectedTreeNode, new SinkNode("sinknode", "sinknode" + Integer.toString(random.nextInt(50)), "detail"));
                        } else if (selectedTreeNode.getLevel() == 1) {
                            添加子节点Button.setText("添加传感器");
                            删除节点Button.setText("删除当前节点");
//                            if (autoGenerateFlag)
//                                nodeTree.insertNodeIntoNodeTree(selectedTreeNode, new Node(selectedTreeNode.toString().split(":")[0], "name", "detail","node" + Integer.toString(random.nextInt(50))));
                        }
                        nodeControl.setBorder(BorderFactory.createTitledBorder("当前选中节点：" + selectedNodeAddress));
                        SwingUtilities.invokeLater(() -> nodeControl.updateUI());
//                      myTable.addBean(nodeTree.searchNode(selectedTreeNode.getParent(),selectedTreeNode));
                        nodeTree.updateUI();
                    }
            );

            添加子节点Button.addActionListener(new NodeActionListener());

            查看节点信息Button.addActionListener(new NodeActionListener());

            删除节点Button.addActionListener(new NodeActionListener());

            从串口同步节点结构Button.addActionListener(new SyncNodeActionListener());
            syncItem.addActionListener(new SyncNodeActionListener());

            /**
             *  节点树点击事件
             */
            jTree.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (evt.getModifiers() == InputEvent.BUTTON1_MASK && evt.getClickCount() == 2) {
                        查看节点信息Button.doClick();
                    } else if (evt.getModifiers() == InputEvent.BUTTON3_MASK) {
                        popupMenu = new JPopupMenu();
                        JMenuItem syncitem2 = new JMenuItem("从串口同步节点");
                        syncitem2.addActionListener(new SyncNodeActionListener());
                        popupMenu.add(syncitem2);
                        lookitem = new JMenuItem("查看节点信息");
                        lookitem.addActionListener(new NodeActionListener());
                        popupMenu.add(lookitem);
                        deleteitem = new JMenuItem("删除当前节点");
                        deleteitem.addActionListener(new NodeActionListener());
                        popupMenu.add(deleteitem);
                        additem = new JMenuItem("添加子节点");
                        additem.addActionListener(new NodeActionListener());
                        popupMenu.add(additem);
                        popupMenu.show(jTree, evt.getX(), evt.getY());
                    }
                }
            });
        }

        /**
         * The type Node action listener.  节点操作触发,通过触发源区分
         */
        class NodeActionListener implements ActionListener {

            @Override
            public void actionPerformed(ActionEvent e) {
                Object source = e.getSource();
                if (source.equals(添加子节点Button) || source.equals(additem)) { //添加节点信息
                    JDialog dialog = new TableDialog(frame, selectedTreeNode, selectedNodeAddress);//创建添加节点窗口
                    dialog.setVisible(true);
                } else if (source.equals(查看节点信息Button) || source.equals(lookitem)) { //查看节点信息
                    if (selectedTreeNode == null) {
                        JOptionPane.showConfirmDialog(frame, "请选择一个节点！", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    CustomTreeNode parentNode = (CustomTreeNode) selectedTreeNode.getParent();
                    if (parentNode != null) {
                        JDialog dialog = new TableDialog(frame, nodeTree.searchNode((CustomTreeNode) selectedTreeNode.getParent(), selectedTreeNode), selectedNodeAddress);
                        dialog.setVisible(true);
                    } else {
                        if (selectedTreeNode.getChildCount() == 0)
                            JOptionPane.showConfirmDialog(frame, "当前没有任何Sink节点，请前往‘监测控制’打开串口，并同步节点！", "Sink节点树", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
                        else
                            JOptionPane.showConfirmDialog(frame, "我只是一棵树没什么好看的！", "Sink节点树", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
                    }
                } else if (source.equals(删除节点Button) || source.equals(deleteitem)) { //删除节点
                    int selection = JOptionPane.showConfirmDialog(frame, "你确定要删除" + selectedNodeAddress + "吗？删除操作不可恢复且所有子节点都会被删除！", "删除节点",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (selection == JOptionPane.OK_OPTION) {
                        nodeTree.removeNodeFromNodeTree(selectedTreeNode);
                        nodeTree.clearSelection();
                    }
                }
            }
        }

        /**
         * The type Sync node action listener. 从串口同步节点结构触发
         */
        class SyncNodeActionListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (CommHandler.commSwitch) {
                    CommHandler.requestSinkNodeList();  //向单片机请求节点列表
                    //重设图表的节点选择Combo
                    SinkNodeCombo.removeAllItems();
                    NodeCombo.removeAllItems();
                    for (SinkNode sinkNode : nodeTree.getSinkNodeList()) {
                        SinkNodeCombo.addItem(sinkNode.getAddress() + ":" + sinkNode.getName());
                    }
                    JOptionPane.showConfirmDialog(frame, "已发送同步命令至下位机！可多次进行同步以保证所有节点同步完毕。", "同步", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showConfirmDialog(frame, "请先打开串口！", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    tabbedPane1.setSelectedIndex(1);
                }
            }
        }

        /**
         * The type Table dialog.  表格信息弹窗
         */
        class TableDialog extends JDialog {

            /**
             * The Changed flag. true:表格内容已改变但未保存
             */
            Boolean changed_flag = false;

            /**
             * Instantiates a new Table dialog. 节点信息表格弹窗(可编辑)
             *
             * @param owner the owner  frame
             * @param node  the node   节点
             * @param title the title  标题
             */
            public TableDialog(JFrame owner, Node node, String title) {

                super(owner, title, true);
                setLayout(new FlowLayout());

                BeanTableModel table = new BeanTableModel(true);//可编辑表格
                table.addBean(node);
                JTable t = new JTable(table);
                t.setPreferredScrollableViewportSize(new Dimension(550, 100));
                t.getModel().addTableModelListener(e -> changed_flag = true);
                // 将表格加入到滚动条组件中
                JScrollPane scrollPane = new JScrollPane(t);
                add(scrollPane);

                /**
                 * update_button : 保存修改按键实现
                 */
                JButton update_button = new JButton("保存修改");
                update_button.addActionListener(e -> {

                    /**
                     *  封装表格内容成map
                     */
                    Map<String, Object> map = new HashMap<>();
                    for (int i = 0; i < table.getColumnCount(); i++) {
                        String colName = table.getColumnName(i);
                        Object o = t.getValueAt(0, i);
                        //提示用户所有地址不能为空
                        if (colName.equals(NodeManager.KEY_NODE_PARENT) || colName.equals(NodeManager.KEY_NODE_ADDRESS) || colName.equals(NodeManager.KEY_SINK_ADDRESS)) {
                            if (o == null || o.toString().isEmpty()) {
                                JOptionPane.showConfirmDialog(frame, "地址不能为空！", "错误",
                                        JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                        map.put(colName, o);
                    }

                    //判断节点是SinkNode还是Node
                    if (node instanceof SinkNode) {
                        SinkNode newsinknode = new SinkNode(map);
                        SinkNode oldsinkNode = (SinkNode) node;
                        nodeTree.updateSinkNode(oldsinkNode, newsinknode);
                        nodeTree.updateUI();
                        JOptionPane.showConfirmDialog(frame, "保存成功！", "查看节点信息",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        Node oldnode = node;
                        Node newnode = new Node(map);
                        try {
                            if (nodeTree.updateNode(oldnode, newnode)) {
                                nodeTree.updateUI();
                                JOptionPane.showConfirmDialog(frame, "保存成功！", "查看节点信息",
                                        JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showConfirmDialog(frame, "保存失败！找不到修改的父节点！", "查看节点信息",
                                        JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception duplicateNodeAddress) {
                            JOptionPane.showConfirmDialog(frame, "移动失败！目标父Sink节点下存在相同的传感器地址！", "错误",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    changed_flag = false;
                });
                add(update_button);

                /**
                 * cancel_button : 还原修改按键实现
                 */
                JButton cancel_button = new JButton("还原修改");
                cancel_button.addActionListener(e -> {
                    table.clearList();
                    //node保存着进入弹窗时的节点信息,所以可以用于还原
                    table.addBean(node);
                    javax.swing.SwingUtilities.invokeLater(() -> t.updateUI());
                    changed_flag = true;
                });

                /**
                 *  用户关闭窗口时检测是否进行过修改且修改是否已保存.
                 */
                addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        if (changed_flag) {
                            int selection = JOptionPane.showConfirmDialog(frame, "不保存修改立即退出？", "查看节点信息",
                                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            if (selection == JOptionPane.OK_OPTION) {
                                setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                                nodeControl.setBorder(BorderFactory.createTitledBorder("当前选中节点：未选中"));
                                SwingUtilities.invokeLater(() -> nodeControl.updateUI());
                            } else {
                                setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
                            }
                        }
                    }
                });
                add(cancel_button);
                pack();
            }

            /**
             * Instantiates a new Table dialog.    添加节点弹窗
             *
             * @param owner            the owner
             * @param treeNodeSelected the tree node selected
             * @param title            the title
             */
            public TableDialog(JFrame owner, CustomTreeNode treeNodeSelected, String title) {

                super(owner, title, true);
                setLayout(new FlowLayout());

                BeanTableModel<Node> table = new BeanTableModel(true);
                Integer Level = treeNodeSelected.getLevel();
                if (Level == 0) { //选中根节点，表格中加入Sink节点的表格信息
                    table.addBean(SinkNode.emptySinkNode());
                } else {
                    Node node = Node.emptyNode();//选中Sink节点或传感器节点，表格中加入传感器节点的表格信息
                    node.setParent(treeNodeSelected.getAddress());
                    table.addBean(node);
                }
                JTable t = new JTable(table);
                t.setPreferredScrollableViewportSize(new Dimension(550, 100));
                t.getModel().addTableModelListener(e -> changed_flag = true);
                JScrollPane scrollPane = new JScrollPane(t);
                add(scrollPane);

                /**
                 *  添加按钮实现
                 */
                JButton update_button = new JButton("添加");
                update_button.addActionListener(e -> {
                    Map<String, Object> map = new HashMap<>();
                    for (int i = 0; i < table.getColumnCount(); i++) {
                        String colName = table.getColumnName(i);
                        Object o = t.getValueAt(0, i);
                        if (colName.equals(NodeManager.KEY_NODE_PARENT) || colName.equals(NodeManager.KEY_NODE_ADDRESS) || colName.equals(NodeManager.KEY_SINK_ADDRESS)) {
                            if (o == null || o.toString().isEmpty()) {
                                JOptionPane.showConfirmDialog(frame, "地址不能为空！", "错误",
                                        JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                        map.put(colName, o);
                    }
                    if (Level == 0) {
                        if (nodeTree.addSinkNode(new SinkNode(map))) { //检查是否有重复Sink节点存在
                            JOptionPane.showConfirmDialog(frame, "添加成功！", "添加节点",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showConfirmDialog(frame, "添加失败！出现重复Sink节点地址！", "错误",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        }
                    } else if (Level == 1) {  //检查目标位置是否存在重复节点地址
                        Node newnode = new Node(map);
                        newnode.setParent(treeNodeSelected.getAddress());
                        if (nodeTree.addNode(newnode)) {
                            table.clearList();
                            table.addBean(newnode);
                            SwingUtilities.invokeLater(() -> t.updateUI());
                            JOptionPane.showConfirmDialog(frame, "添加成功！", "添加节点",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showConfirmDialog(frame, "添加失败！出现重复节点地址！", "错误",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    nodeTree.updateUI();
                    changed_flag = false;
                });
                add(update_button);
                pack();
            }
        }
    }


    /**
     * The type Comm tab. 监测管理标签页
     */

    private JPanel CommJPanel;
    private JComboBox portCombo;    //端口号选项框
    private JComboBox baudrateCombo; //波特率选项框
    private JButton 串口Button;        //打开/关闭串口按钮
    private JButton 高级配置Button;
    private JPanel jChartPanel;      //JChart 图表监测框总容器
    private JFormattedTextField startTimeText; //图表x轴起始是时刻文本框
    private JFormattedTextField endTimeText;  //图表x轴终止时刻文本框
    private JButton 清除该节点数据Button;
    private JButton 导出数据Button;
    private JButton 复位视图Button;
    private JTextArea commLogText;    //串口日志文本框
    private JButton 日志输出Button;
    private JTextArea sendCMDText;   //发送命令文本框
    private JButton 发送命令Button;
    private JButton 设置命令Button;
    private JButton 清空日志Button;
    private JButton 自动缓存Button;

    class CommTab {

        private ChartPanel chartPanel;       //图表容器
        private RealTimeChart realTimeChart;  //图表
        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //时间格式化
        private Integer MAX_LogLine = 1000;     //日志显示最大行数
        private Boolean logAutoSave = false;      //自动缓存设置
        private Integer autoSaveLineCount = 0;   //自动缓存默认值
        private StringBuffer autoSaveLineBuffer = new StringBuffer(); //自动缓存
        private String autoSavePath;             //自动缓存路径
        private TxtUtils autoSaveTxtUtils;     //自动缓存输出

        private String selectedPort;            //选中端口号
        private Integer selectedBaudrate = 9600; //选中波特率
        private Integer selectedStopbit = SerialPort.STOPBITS_1;  //选中停止位
        private Integer selectedDatabit = SerialPort.DATABITS_8;  //选中数据位
        private Integer selectedParity = SerialPort.PARITY_NONE;  //选中校验
        private Integer[] baudrateList = new Integer[]{1200, 2400, 4800, 9600, 14400, 19200, 38400, 43000, 57600, 76800, 115200, 128000, 230400, 256000, 460800, 921600, 1382400};

        private Date startTime = new Date(); //起始时间
        private Date endTime = new Date();   //终止时间

        /**
         * The Command list. 指令表
         */
        List<Command> commandList = new ArrayList<>();

        /**
         * The Serial comm. 串口实例
         */
        SerialComm serialComm;


        /**
         * Instantiates a new Comm tab. 初始化串口标签页
         */

        CommTab() {

            serialComm = SerialComm.getSerialComm();

            /**
             *  菜单选项触发初始化
             */
            outputChartItem.addActionListener(new ChartOutputAction());
            autoSaveLogItem.addActionListener(new LogAutoSaveAction());

            /**
             *  串口设置框初始化
             */
            portCombo.removeAllItems();
            baudrateCombo.removeAllItems();
            List<String> portList = serialComm.findPort();
            for (String portName : portList) {
                portCombo.addItem(portName);
            }
            try {
                selectedPort = portList.get(0);
            } catch (IndexOutOfBoundsException e) {
            }
            for (Integer baudrate : baudrateList) {
                baudrateCombo.addItem(baudrate);
            }
            baudrateCombo.addItem("自定义");
            baudrateCombo.setSelectedIndex(3); //默认选择波特率9600
            selectedBaudrate = baudrateList[3];
            //端口选择框添加点击事件,寻找当前可用端口并更新
            portCombo.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    portCombo.removeAllItems();
                    SerialComm serialComm = SerialComm.getSerialComm();
                    List<String> portList = serialComm.findPort();
                    for (String portName : portList) {
                        portCombo.addItem(portName);
                    }
                }
            });
            //端口选择框添加选中事件
            portCombo.addActionListener((l) ->
                    selectedPort = (String) portCombo.getSelectedItem());
            //波特率选择框添加选中事件
            baudrateCombo.addActionListener((l) -> {
                Object selectedItem = baudrateCombo.getSelectedItem();
                if (selectedItem instanceof String) { //选中自定义波特率
                    selectedBaudrate = null;
                    baudrateCombo.setEditable(true);
                } else {
                    selectedBaudrate = (Integer) selectedItem;
                    baudrateCombo.setEditable(false);
                }
            });

            //串口开关按钮触发事件
            串口Button.addActionListener((l) -> {
                if (串口Button.getText().equals("关闭串口")) {
                    CommHandler.commSwitch = false;
                    try {
                        if (logAutoSave) {  //如果开启自动缓存，关闭串口前对日志进行抢救保存
                            autoSaveTxtUtils.appendToTxt(autoSaveLineBuffer.toString());
                            autoSaveLineBuffer = new StringBuffer();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                    } finally {
                        串口Button.setText("打开串口");
                    }
                } else {
                    if (selectedPort == null) { //检测是否设置了串口号
                        JOptionPane.showConfirmDialog(frame, "无可用串口", "串口",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    串口Button.setEnabled(false);
                    if (selectedBaudrate == null) { //检测是否设置了波特率
                        String s = (String) baudrateCombo.getSelectedItem();
                        try {
                            selectedBaudrate = Integer.parseInt(s);
                        } catch (NumberFormatException e) { //检测是否设置了合法的自定义波特率
                            JOptionPane.showConfirmDialog(frame, "波特率设置无效", "波特率设置",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    CommHandler.commSwitch = true;
                    串口Button.setText("关闭串口");
                    Thread commThread = new SerialCommThread(selectedPort, selectedBaudrate, selectedDatabit, selectedStopbit, selectedParity);
                    commThread.start();//启动串口线程
                    串口Button.setEnabled(true);
                }
            });

            //高级配置弹窗
            高级配置Button.addActionListener((l) -> {
                JDialog dialog = new AdvancedPortConfig(frame);
                dialog.setSize(MainFrame.screenWidth / 5, MainFrame.screenHeight / 5);
                dialog.setLocation(MainFrame.screenWidth / 2 - dialog.getWidth() / 2, MainFrame.screenHeight / 2 - dialog.getHeight() / 2);
                dialog.setVisible(true);
            });

            //图表Sink节点选择框触发
            SinkNodeCombo.addActionListener((e) -> {
                if (SinkNodeCombo.getSelectedItem() == null || enableSelectForSinkNodeCombo == false)
                    return;
                String selectedItem = (String) (SinkNodeCombo.getSelectedItem());
                sinkNodeAddressForChart = selectedItem.split(":")[0];
                if (sinkNodeAddressForChart.equals("null"))
                    return;
                NodeCombo.removeAllItems();
                try {
                    for (SinkNode sinkNode : nodeTree.getSinkNodeList()) {
                        if (sinkNode.getAddress().equals(sinkNodeAddressForChart)) {
                            for (Node node : sinkNode.getNodeList()) {
                                //选中Sink节点后，自动将其所有传感器节点向传感器选择框(NodeCombo)中添加
                                NodeCombo.addItem(node.getAddress() + ":" + node.getName());
                            }
                        }
                    }
                } catch (ConcurrentModificationException cme) {
                }
            });

            //选中传感器节点,刷新图表
            NodeCombo.addActionListener((e) -> startNewChart());

            //图表x轴起始时刻文本框初始化
            startTimeText.setAction(new DateInputAction());
            startTimeText.addKeyListener(new DateInputKeyListener());
            startTimeText.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) { //失去焦点则完成输入
                    String s = startTimeText.getText();
                    startTime = new Date(); //默认时间为当前时间
                    try {
                        startTime = sdf.parse(s);
                    } catch (ParseException e1) { //输入时间格式不正确，则startTime默认为当前时间
                        JOptionPane.showConfirmDialog(frame, "错误的日期格式！", "起始时间错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        startTimeText.setText("");
                    }
                    if (startTime.getTime() >= endTime.getTime()) { //保证x轴范围最小为1秒
                        startTime = new Date(endTime.getTime() - 1000);
                    }
                    startTimeText.setText(sdf.format(startTime));
                    if (realTimeChart != null)
                        realTimeChart.setXRange(startTime.getTime(), endTime.getTime()); //设置图表
                    //从数据库中调取对应时间段的数据
                    nodeManager.retrieveDataFromDB(realTimeChart.getTimeSeries(), sinkNodeAddressForChart, nodeAddressForChart, startTime, endTime);
                }
            });

            //图表x轴终止时刻文本框初始化
            endTimeText.setAction(new DateInputAction());
            endTimeText.addKeyListener(new DateInputKeyListener());
            endTimeText.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    String s = endTimeText.getText();
                    endTime = new Date();
                    try {
                        endTime = sdf.parse(s);
                    } catch (ParseException e1) {
                        JOptionPane.showConfirmDialog(frame, "错误的日期格式！", "结束时间错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        endTimeText.setText("");
                    }
                    if (startTime.getTime() >= endTime.getTime()) {
                        endTime = new Date(startTime.getTime() + 1000);
                    }
                    endTimeText.setText(sdf.format(endTime));
                    realTimeChart.setXRange(startTime.getTime(), endTime.getTime());
                    nodeManager.retrieveDataFromDB(realTimeChart.getTimeSeries(), sinkNodeAddressForChart, nodeAddressForChart, startTime, endTime);
                }
            });

            清除该节点数据Button.addActionListener((l) -> {
                if (NodeCombo.getSelectedItem() == null || nodeForChart == null) {
                    JOptionPane.showConfirmDialog(frame, "请先选择一个传感器节点！", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int selection = JOptionPane.showConfirmDialog(frame, "确定要清除传感器" + nodeForChart.getAddress() + ":" + nodeForChart.getName() + "保存在数据库中的数据吗？该操作不可恢复！", "警告", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (selection == JOptionPane.OK_OPTION) {
                    nodeForChart.clearData();//清除该节点的监测数据
                    JOptionPane.showConfirmDialog(frame, "数据清除成功", "成功", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                }
            });

            导出数据Button.addActionListener(new ChartOutputAction());

            复位视图Button.addActionListener((l) -> {
                if (realTimeChart == null) {
                    JOptionPane.showConfirmDialog(frame, "请先选择一个节点！", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                realTimeChart.resetXRange();//复位视图
                startTimeText.setText("");
                endTimeText.setText("");
            });

            //日志输出按钮触发
            日志输出Button.addActionListener((l) -> {
                //创建 文件选择器
                JFileChooser jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);//只选择目录,文件名自动生成
                jfc.showSaveDialog(new JLabel());
                try {
                    //文件名路径格式化
                    String path = jfc.getSelectedFile().getAbsolutePath() + File.separator + sdf.format(new Date()).replaceAll(":", "-") + ".txt";
                    TxtUtils txtUtils = new TxtUtils(path);
                    //输出
                    txtUtils.writeToTxt(commLogText.getText());
                    JOptionPane.showConfirmDialog(frame, "成功保存至下列路径:\n" + path, "保存成功", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException a) {
                    JOptionPane.showConfirmDialog(frame, "保存失败！请保证正确路径或者检查磁盘状态", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                } catch (NullPointerException a) {
                }
            });

            清空日志Button.addActionListener((l) -> {
                commLogText.setText("");
                autoSaveLineBuffer = new StringBuffer();
            });

            自动缓存Button.addActionListener(new LogAutoSaveAction());

            发送命令Button.addActionListener((l) -> {
                if (!CommHandler.commSwitch) {
                    JOptionPane.showConfirmDialog(frame, "请先打开串口！", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                sendCommand(sendCMDText.getText());
                sendCMDText.setText("");
            });

            设置命令Button.addActionListener(new CommandConfigAction());
        }

        /**
         * Send command. 向串口发送命令,同时在日志中记录
         *
         * @param cmd
         */
        private void sendCommand(String cmd) {
            serialComm.sendToPort(comm, cmd.getBytes());
            String temp = sdf.format(new Date()) + "  " + comm.getName() + "【发送】" + "\r\n\t" + cmd + "\r\n";
            commLogText.append(temp);
            autoSaveLineBuffer.append(temp);
            autoSaveLineCount++;
        }

        /**
         * Start new chart. 重载新图表
         */
        private void startNewChart() {
            if (NodeCombo.getSelectedItem() == null || SinkNodeCombo.getSelectedItem() == null) return;
            String selectedSinkNodeComboItem = (String) SinkNodeCombo.getSelectedItem();
            String selectedNodeComboItem = (String) NodeCombo.getSelectedItem();
            if ((selectedNodeComboItem.split(":")[0].equals(nodeAddressForChart) && selectedSinkNodeComboItem.split(":")[0].equals(sinkNodeAddressForChart)))
                return;//重复选择忽略
            if (chartPanel != null)
                jChartPanel.remove(chartPanel);//删去旧图表

            //提取选中的传感器节点
            nodeAddressForChart = selectedNodeComboItem.split(":")[0];
            nodeForChart = nodeTree.searchNode(sinkNodeAddressForChart, nodeAddressForChart);
            //创建新图表
            realTimeChart = new RealTimeChart(" ", nodeAddressForChart + ":" + nodeForChart.getName(), nodeForChart.getDetail(), nodeForChart.getTimeSeries());
            //获取保存的数据集
            nodeForChart.setTimeSeries(realTimeChart.getTimeSeries());

            chartPanel = realTimeChart.getChartPanel();
            jChartPanel.add(chartPanel, BorderLayout.CENTER);
            SwingUtilities.invokeLater(() -> jChartPanel.updateUI());
            CommHandler.setChart(realTimeChart, nodeForChart);
        }


        /**
         * The type Command config action. 设置指令集
         */
        class CommandConfigAction extends AbstractAction {

            @Override
            public void actionPerformed(ActionEvent e) {

                JDialog cmdDialog = new CMDConfigDialog(frame);
                cmdDialog.setVisible(true);

            }

            /**
             * The type Cmd config dialog. 设置指令集弹窗
             */
            class CMDConfigDialog extends JDialog {
                /**
                 * Instantiates a new Cmd config dialog.
                 *
                 * @param owner the owner
                 */
                public CMDConfigDialog(JFrame owner) {
                    super(owner, "设置指令集", false);
                    setLayout(new FlowLayout());

                    //指令表格初始化
                    BeanTableModel cmd_table = new BeanTableModel(new Command().getFieldNames(), commandList, true);
                    JTable t = new JTable(cmd_table);
                    t.setPreferredScrollableViewportSize(new Dimension(550, 100));

                    cmd_table.addTableModelListener((e) -> {
                        int row = e.getFirstRow();
                        int col = e.getColumn();
                        Map<String, Object> map = new HashMap<>();
                        map.put(cmd_table.getColumnName(col), cmd_table.getValueAt(row, col));
                        commandList.get(row).setFields(map);
                    });
                    JScrollPane scrollPane = new JScrollPane(t);
                    add(scrollPane);

                    JButton preset_cmd = new JButton("载入预设指令集");
                    preset_cmd.addActionListener((l) -> {
                        CommHandler.presetCommand(commandList);//添加预设指令集
                        SwingUtilities.invokeLater(() -> t.updateUI());
                    });

                    JButton add_cmd = new JButton("添加新指令");
                    add_cmd.addActionListener((l) -> {
                        commandList.add(new Command("<>", ""));
                        SwingUtilities.invokeLater(() -> t.updateUI());
                    });

                    JButton del_cmd = new JButton("删除指令");
                    del_cmd.addActionListener((l) -> {
                        int[] rows = t.getSelectedRows();
                        for (int row : rows) {
                            commandList.remove(row);
                        }
                        SwingUtilities.invokeLater(() -> t.updateUI());
                    });

                    JButton send_cmd = new JButton("发送指令");
                    send_cmd.addActionListener((l) -> {
                        int[] rows = t.getSelectedRows(); //支持多选
                        for (int row : rows) {
                            String cmd = (String) cmd_table.getValueAt(row, cmd_table.getTitleCol(Command.KEY_COMMAND));
                            if (CommHandler.commSwitch) {
                                sendCommand(cmd);
                            } else {
                                JOptionPane.showConfirmDialog(frame, "请先打开串口！", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                                dispose();
                                break;
                            }
                        }
                    });
                    add(preset_cmd);
                    add(add_cmd);
                    add(del_cmd);
                    add(send_cmd);
                    pack();
                }
            }
        }

        /**
         * The type Log auto save action.  日志自动缓存. 支持用户选择一个文本文档输出路径,开启自动缓存后,串口日志每更新n行,
         * 将自动更新该文本文档. n=1~1000
         */
        class LogAutoSaveAction extends AbstractAction {

            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog jDialog = new AutoSaveDialog(frame);
                jDialog.setVisible(true);
            }

            /**
             * The type Auto save dialog.  自动缓存设置弹窗
             */
            class AutoSaveDialog extends JDialog {
                /**
                 * Instantiates a new Auto save dialog.
                 *
                 * @param owner the owner
                 */
                public AutoSaveDialog(JFrame owner) {

                    super(owner, "日志缓存", true);
                    JPanel panel = new JPanel();
                    panel.setLayout(new GridLayout(4, 2));

                    JButton selectPathButton = new JButton("缓存路径");
                    JTextField pathTextField = new JTextField();
                    pathTextField.setText(autoSavePath);
                    selectPathButton.addActionListener((l) -> {
                        if (logAutoSave) {
                            JOptionPane.showConfirmDialog(frame, "请先关闭自动缓存！", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        //打开文件选择器,选择缓存路径
                        JFileChooser jfc = new JFileChooser();
                        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        jfc.showSaveDialog(new JLabel());
                        autoSavePath = jfc.getSelectedFile().getAbsolutePath();
                        pathTextField.setText(autoSavePath);
                        autoSaveTxtUtils = new TxtUtils(autoSavePath + File.separator + sdf.format(new Date()).replaceAll(":", "-") + ".txt");
                    });

                    //日志每更新n行自动更新文本文档
                    JLabel jLabel = new JLabel("每n行自动缓存:");
                    JTextField nTextField = new JTextField();
                    nTextField.setText(MAX_LogLine.toString());
                    nTextField.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusLost(FocusEvent e) {
                            if (logAutoSave) {
                                JOptionPane.showConfirmDialog(frame, "请先关闭自动缓存！", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                                nTextField.setText(MAX_LogLine.toString());
                                return;
                            }
                            String s = nTextField.getText();
                            Integer i;
                            try {
                                i = Integer.parseInt(s);
                                if (i < 1 && i > 5000) {
                                    JOptionPane.showConfirmDialog(frame, "请输入1~1000的数！", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                            } catch (NumberFormatException nfe) { //输入非法格式
                                nTextField.setText(MAX_LogLine.toString());
                                return;
                            }
                            MAX_LogLine = i;
                        }
                    });
                    panel.add(selectPathButton);
                    panel.add(pathTextField);
                    panel.add(jLabel);
                    panel.add(nTextField);

                    //自动缓存开关
                    JButton commit = new JButton();
                    if (logAutoSave)
                        commit.setText("关闭自动缓存");
                    else
                        commit.setText("开启自动缓存");
                    commit.addActionListener((l) -> {
                        if (commit.getText().equals("开启自动缓存")) {
                            commit.setText("关闭自动缓存");
                            JOptionPane.showConfirmDialog(frame, "自动缓存已开启，每收到" + MAX_LogLine + "行会自动缓存。", "自动缓存", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            logAutoSave = true;
                            dispose();
                        } else {
                            commit.setText("开启自动缓存");
                            logAutoSave = false;
                        }
                    });
                    panel.add(commit);
                    add(panel);
                    pack();
                }
            }
        }

        /**
         * The type Chart output action. 图表导出Excel
         */
        class ChartOutputAction extends AbstractAction {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (realTimeChart == null) {
                    JOptionPane.showConfirmDialog(frame, "请先选择一个节点！", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JFileChooser jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                jfc.showDialog(new JLabel(), "选择目录");
                try {
                    String path = jfc.getSelectedFile().getAbsolutePath() + File.separator + sinkNodeAddressForChart + "-" + nodeAddressForChart + " " + sdf.format(startTime).replaceAll(":", "-") + " to " + sdf.format(endTime).replaceAll(":", "-") + ".xls";
                    ExcelUtils excelUtils = new ExcelUtils(path);
                    excelUtils.chartToExcel(realTimeChart);
                    JOptionPane.showConfirmDialog(frame, "成功保存至下列路径:\n" + path, "保存成功", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException a) {
                    JOptionPane.showConfirmDialog(frame, "保存失败！请保证正确路径或者检查磁盘状态", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                } catch (NullPointerException a) {
                }
            }
        }

        /**
         * The type Date input action. 图表起止时刻文本框格式化
         */
        class DateInputAction extends AbstractAction {
            /**
             * Instantiates a new Date input action.
             */
            public DateInputAction() {
                putValue(SHORT_DESCRIPTION, "日期格式: 2000-01-01 00:00:00");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
            }
        }

        /**
         * The type Date input key listener.  图表起止时刻文本框按键触发,支持按回车结束输入
         */
        class DateInputKeyListener implements KeyListener {

            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    导出数据Button.requestFocus();  //导出按钮获得焦点意味着文本框失去焦点,相当于结束输入
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        }


        /**
         * The type Advanced port config. 串口高级设置弹窗
         */
        class AdvancedPortConfig extends JDialog {

            /**
             * The Stopbits.
             */
            Integer[] stopbits = new Integer[]{SerialPort.STOPBITS_1, SerialPort.STOPBITS_1_5, SerialPort.STOPBITS_2};
            /**
             * The Databits.
             */
            Integer[] databits = new Integer[]{SerialPort.DATABITS_5, SerialPort.DATABITS_6, SerialPort.DATABITS_7, SerialPort.DATABITS_8};
            /**
             * The Parities.
             */
            String[] parities = new String[]{"无", "奇校验", "偶校验"};

            Float STOPBITS_1_5 = new Float(1.5);

            Boolean add_flag;

            /**
             * Instantiates a new Advanced port config. 初始化
             *
             * @param owner the owner
             */
            public AdvancedPortConfig(JFrame owner) {

                super(owner, "串口设置", true);
                JPanel panel = new JPanel();
                panel.setLayout(new GridLayout(5, 2));

                panel.add(new JLabel("数据位"));
                JComboBox databitsCombot = new JComboBox();
                databitsCombot.addActionListener((l) -> {
                    if (add_flag)
                        return;
                    selectedDatabit = (Integer) databitsCombot.getSelectedItem();
                });

                add_flag = true;  //添加选项会自动选中第一个选项,避免添加时触发选中覆盖selectedDatabit
                for (Integer databit : databits) {
                    databitsCombot.addItem(databit);
                }
                add_flag = false;
                if (selectedDatabit == null) {
                    databitsCombot.setSelectedItem(databits[3]); //默认8位数据位
                } else {
                    for (Integer integer : databits) {
                        if (integer == selectedDatabit)
                            databitsCombot.setSelectedItem(integer);
                    }
                }
                panel.add(databitsCombot);

                panel.add(new JLabel("停止位"));
                JComboBox stopbitsCombo = new JComboBox();
                stopbitsCombo.addActionListener((l) -> {
                    if (add_flag)
                        return;
                    Object stopbitItem = stopbitsCombo.getSelectedItem();
                    if (stopbitItem instanceof Float)
                        selectedStopbit = SerialPort.STOPBITS_1_5;
                    else {
                        selectedStopbit = (Integer) stopbitItem;
                    }
                });
                add_flag = true;
                for (Integer stopbit : stopbits) {
                    if (stopbit == SerialPort.STOPBITS_1_5)
                        stopbitsCombo.addItem(STOPBITS_1_5);
                    stopbitsCombo.addItem(stopbit);
                }
                add_flag = false;

                if (selectedStopbit != null) {
                    if (selectedStopbit == SerialPort.STOPBITS_1_5) {
                        stopbitsCombo.setSelectedItem(STOPBITS_1_5);
                    } else {
                        stopbitsCombo.setSelectedItem(selectedStopbit);
                    }
                }
                panel.add(stopbitsCombo);

                panel.add(new JLabel("奇偶检验"));
                JComboBox parityCombo = new JComboBox();
                parityCombo.addActionListener((l) -> {
                    if (add_flag)
                        return;
                    String parityItem = (String) parityCombo.getSelectedItem();
                    if (parityItem.equals(parities[0])) {
                        selectedParity = SerialPort.PARITY_NONE;
                    } else if (parityItem.equals(parities[1])) {
                        selectedParity = SerialPort.PARITY_ODD;
                    } else if (parityItem.equals(parities[2])) {
                        selectedParity = SerialPort.PARITY_EVEN;
                    }
                });
                add_flag = true;
                for (String parity : parities) {
                    parityCombo.addItem(parity);
                }
                add_flag = false;

                if (selectedParity != null) {
                    parityCombo.setSelectedItem(parities[selectedParity]);
                }
                panel.add(parityCombo);

                //向单片机请求数据的周期,支持小数输入
                panel.add(new JLabel("请求数据周期(秒)"));
                JTextField interval = new JTextField();
                Float f = REQUEST_FOR_DATA_INTERVAL / 1000f;
                interval.setText(f.toString());
                panel.add(interval);

                //确认按钮
                JButton button = new JButton("确认");
                button.addActionListener(event -> {
                    try {
                        Float interval_f = Float.parseFloat(interval.getText());
                        REQUEST_FOR_DATA_INTERVAL = (int) (interval_f * 1000);
                    } catch (NumberFormatException e) {
                        JOptionPane.showConfirmDialog(frame, "请求数据周期请输入数字!", "请求数据周期", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    dispose();
                });
                panel.add(button);
                add(panel);
                pack();
            }
        }


        /**
         * The type Serial comm thread.  串口通信线程.实现开启串口,监听串口,轮询串口等后台任务
         */
        class SerialCommThread extends Thread {

            private String portName;
            private Integer baudrate;
            private Integer databit;
            private Integer stopbit;
            private Integer parity;

            /**
             * Instantiates a new Serial comm thread. 串口设置
             *
             * @param portName the port name
             * @param baudrate the baudrate
             * @param databit  the databit
             * @param stopbit  the stopbit
             * @param parity   the parity
             */
            public SerialCommThread(String portName, Integer baudrate, Integer databit, Integer stopbit, Integer parity) {
                super();
                this.portName = portName;
                this.baudrate = baudrate;
                this.databit = databit;
                this.stopbit = stopbit;
                this.parity = parity;
            }

            /**
             * Run. 串口线程主函数
             */
            @Override
            public synchronized void run() {
                synchronized (SerialComm.class) {
                    SerialComm serialComm = SerialComm.getSerialComm();
                    ArrayList<String> arrayList = serialComm.findPort();
                    System.out.println(arrayList);
                    System.out.println(System.getProperty("java.library.path"));
                    try {
                        //打开串口
                        comm = serialComm.openPort(portName, baudrate, databit, stopbit, parity);
                        CommHandler.comm = comm;
                        JOptionPane.showConfirmDialog(frame, "串口打开成功！正在自动发送同步节点结构命令。", "串口", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    } catch (NoSuchPortException e) {
                        串口Button.doClick();
                        JOptionPane.showConfirmDialog(frame, "端口不存在", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        return;
                    } catch (PortInUseException e) {
                        串口Button.doClick();
                        JOptionPane.showConfirmDialog(frame, "端口已被占用", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    //添加串口监听器
                    serialComm.addListener(comm, (serialPortEvent) -> {
                                switch (serialPortEvent.getEventType()) {
                                    case SerialPortEvent.BI: // 10 通讯中断
                                        System.out.println("串口通信终端");
                                        break;

                                    case SerialPortEvent.OE: // 7 溢位（溢出）错误
                                        System.out.println("串口溢位（溢出）错误");
                                        break;

                                    case SerialPortEvent.FE: // 9 帧错误
                                        System.out.println("串口帧错误");
                                        break;

                                    case SerialPortEvent.PE: // 8 奇偶校验错误
                                        System.out.println("串口奇偶校验错误");
                                        break;

                                    case SerialPortEvent.CD: // 6 载波检测
                                        System.out.println("串口载波监测");
                                        break;

                                    case SerialPortEvent.CTS: // 3 清除待发送数据
                                        System.out.println("串口清楚待发送数据");
                                        break;

                                    case SerialPortEvent.DSR: // 4 待发送数据准备好了
                                        System.out.println("串口待发送数据准备好了");
                                        break;

                                    case SerialPortEvent.RI: // 5 振铃指示
                                        System.out.println("串口振铃指示");
                                        break;

                                    case SerialPortEvent.OUTPUT_BUFFER_EMPTY: // 2 输出缓冲区已清空
                                        System.out.println("输出缓冲区已清空");
                                        break;

                                    case SerialPortEvent.DATA_AVAILABLE: // 1 串口收到可用数据
                                    {
                                        byte[] bytes = null;
                                        try { //读取串口数据
                                            bytes = serialComm.readFromPort(comm);
                                        } catch (Exception e) {
                                            CommHandler.commSwitch = false;
                                            JOptionPane.showConfirmDialog(frame, "串口连接断开！", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                                            串口Button.doClick();
                                        }
                                        try {
                                            //处理串口数据
                                            if (CommHandler.parseRawData(bytes, comm, nodeTree));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        if (bytes != null) {
                                            //更新串口日志
                                            String temp = sdf.format(new Date()) + "  " + comm.getName() + "【接收】" + "\r\n\t" + new String(bytes) + "\r\n";
                                            autoSaveLineBuffer.append(temp);//自动缓存
                                            EventQueue.invokeLater(() ->commLogText.append(temp));
                                            if (commLogText.getLineCount() > MAX_LogLine) { //日志过多清空
                                                commLogText.setText("");
                                            }
                                            if (autoSaveLineCount++ > MAX_LogLine && logAutoSave) {
                                                //自动缓存更新文本文档
                                                new Thread(() -> {
                                                    try {
                                                        autoSaveTxtUtils.appendToTxt(autoSaveLineBuffer.toString());
                                                        autoSaveLineBuffer = new StringBuffer();
                                                    } catch (IOException e) {
                                                        logAutoSave = false;
                                                    }
                                                }).start();
                                                autoSaveLineCount = 0;
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                    );

                    //串口开启后自动发3次同步节点指令
                    for (int i = 0; i < 3; i++) {
                        CommHandler.requestSinkNodeList();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    while (CommHandler.commSwitch) {
                        //传感器监测值轮询
                        try {
                            Thread.sleep(REQUEST_FOR_DATA_INTERVAL);
                            if (nodeAddressForChart != null) {
                                CommHandler.requestSensorDataAll(sinkNodeAddressForChart);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //关闭串口
                    serialComm.closePort(comm);
                }
            }
        }
    }


    /**
     * The type Tourist tab. 游客管理标签页
     */

    private JPanel TouristJPanel;
    private JPanel touristButtonPanel;
    private JPanel touristTablePanel;
    private JButton 导出游客信息Button;
    private JButton 删除游客记录Button;
    private JButton 查看登陆记录Button;

    class TouristTab {
        /**
         * The Tourist table. 游客信息表格
         */
        BeanTableModel touristTable;
        /**
         * The T.
         */
        JTable t;

        /**
         * Instantiates a new Tourist tab. 游客标签页初始化
         */
        TouristTab() {

            touristTablePanel.setLayout(new BorderLayout());
            touristManager.syncByDatabase();
            t = touristManager.getJTable();
            touristTable = (BeanTableModel) t.getModel();
            t.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
                        JPopupMenu touristpopupMenu = new JPopupMenu();
                        JMenuItem lookItem = new JMenuItem("查看游客登陆记录");
                        lookItem.addActionListener(new LookTouristLogAction());
                        touristpopupMenu.add(lookItem);
                        JMenuItem deleteItem = new JMenuItem("删除游客信息");
                        deleteItem.addActionListener(new DeleteTouristAction());
                        touristpopupMenu.add(deleteItem);
                        touristpopupMenu.show(t, e.getX(), e.getY());
                    }
                }
            });
            JScrollPane scrollPane = new JScrollPane(t);
            touristTablePanel.add(scrollPane, BorderLayout.CENTER);

            删除游客记录Button.addActionListener(new DeleteTouristAction());
            查看登陆记录Button.addActionListener(new LookTouristLogAction());
            导出游客信息Button.addActionListener(new TouristOutputAction());
            outputTouristItem.addActionListener(new TouristOutputAction());
            touristButtonPanel.setBorder(BorderFactory.createTitledBorder("游客管理"));
        }

        /**
         * The type Delete tourist action. 删除游客信息触发
         */
        class DeleteTouristAction extends AbstractAction {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (t.getSelectedRowCount() != 0) {
                    int selection = JOptionPane.showConfirmDialog(frame, "删除不可恢复，确定？", "删除", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (selection == JOptionPane.OK_OPTION) {
                        String IMEI;
                        try {
                            int[] rows = t.getSelectedRows(); //支持多选
                            for (int row : rows) {
                                IMEI = (String) touristTable.getValueAt(row, touristTable.getTitleCol(TouristManager.KEY_TOURIST_IMEI));//获取选中行的IMEI
                                touristManager.removeTourist(IMEI);//移除游客
                                SwingUtilities.invokeLater(() -> t.updateUI());
                            }
                        } catch (ClassCastException cce) {
                            cce.printStackTrace();
                        }
                    }
                } else {
                    JOptionPane.showConfirmDialog(frame, "请先选中一条记录！", "删除", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        /**
         * The type Look tourist log action.  查看游客登陆记录表格
         */
        class LookTouristLogAction extends AbstractAction {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (t.getSelectedRowCount() == 0) {
                    JOptionPane.showConfirmDialog(frame, "请先选择一条记录!", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                } else {
                    try {
                        int row = t.getSelectedRow();
                        String IMEI = (String) touristTable.getValueAt(row, touristTable.getTitleCol(TouristManager.KEY_TOURIST_IMEI));
                        JDialog dialog = new LookTouristLogDialog(frame, IMEI);//弹出游客登陆记录
                        dialog.setVisible(true);
                    }catch (ClassCastException cce){
                        cce.printStackTrace();
                    }
                }
            }

            /**
             * The type Look tourist log dialog.  游客登陆记录表格弹窗
             */
            class LookTouristLogDialog extends JDialog {
                /**
                 * Instantiates a new Look tourist log dialog.
                 *
                 * @param owner the owner
                 * @param IMEI  the imei
                 */
                public LookTouristLogDialog(JFrame owner, String IMEI) {
                    super(owner, "找回密码", true);
                    List<TBean> logs = touristManager.lookConnectLog(IMEI);
                    BeanTableModel<TBean> logTable = new BeanTableModel<>(new TouristManager.ConnectLog().getFieldNames(), logs, false);
                    JTable t = new JTable(logTable);
                    t.setPreferredScrollableViewportSize(new Dimension(550, 100));
                    // 将表格加入到滚动条组件中
                    JScrollPane scrollPane = new JScrollPane(t);
                    add(scrollPane, BorderLayout.CENTER);
                    JButton refresh = new JButton("刷新");
                    refresh.addActionListener((l) -> SwingUtilities.invokeLater(() -> {
                        List<TBean> logs2 = touristManager.lookConnectLog(IMEI);
                        BeanTableModel<TBean> logTable2 = new BeanTableModel<>(new TouristManager.ConnectLog().getFieldNames(), logs2, false);
                        t.setModel(logTable2);
                        t.updateUI();
                    }));
                    add(refresh, BorderLayout.SOUTH);
                    pack();
                }
            }
        }

        /**
         * The type Tourist output action.游客信息导出（游客信息表和登陆记录表同时导出）
         */
        class TouristOutputAction extends AbstractAction {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (touristManager.getTouristList().size() == 0) {
                    JOptionPane.showConfirmDialog(frame, "没有信息可供导出", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                } else {
                    JFileChooser jfc = new JFileChooser();
                    jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    jfc.showDialog(new JLabel(), "选择目录");
                    try {
                        String path = jfc.getSelectedFile().getAbsolutePath() + File.separator + "TouristsInfo_" + MyDatabase.dateTimeFormmatter(new Date()).replaceAll(":", "-") + ".xls";
                        ExcelUtils<TBean> excelUtils = new ExcelUtils(path);
                        Map<String, List<TBean>> map = new HashMap<>();
                        map.put("游客基本信息", touristManager.getTouristList()); //sheet0
                        map.put("游客连接记录", touristManager.lookAllConnectLog());//sheet1
                        excelUtils.beansToExcel(map);
                        JOptionPane.showConfirmDialog(frame, "成功保存至下列路径:\n" + path, "保存成功", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException a) {
                        a.printStackTrace();
                        JOptionPane.showConfirmDialog(frame, "保存失败！请保证正确路径或者检查磁盘状态", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    } catch (NullPointerException a) {
                        a.printStackTrace();
                    }
                }
            }
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}


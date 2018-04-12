package com.qq452651705.GUI;

import com.qq452651705.DataMGM.Tourists.TouristManager;
import com.qq452651705.JDBC.MyDatabase;
import com.qq452651705.SerialComm.CommHandler;
import com.qq452651705.SerialComm.CommHandler.Command;
import com.qq452651705.SerialComm.SerialComm;
import com.qq452651705.DataMGM.Account.AccountManager;
import com.qq452651705.DataMGM.Node.NodeManager;
import com.qq452651705.DataMGM.Node.NodeTree;
import com.qq452651705.Utils.Bean;
import com.qq452651705.Utils.ExcelUtils;
import com.qq452651705.Utils.TxtUtils;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
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

public class MainActivity {

    private SerialPort comm;

    private JTabbedPane tabbedPane1;
    private NodeTab nodeTab;
    private CommTab commTab;
    private TouristTab touristTab;

    private JTree jTree;
    private JButton 查看节点信息Button;
    private JButton 添加子节点Button;
    private JButton 删除节点Button;
    private JPanel nodeControl;

    private JFrame frame;

    private AccountManager accountManager = AccountManager.getAccountManager();
    private NodeManager nodeManager = NodeManager.getNodeManager();
    private TouristManager touristManager = TouristManager.getTouristManager();
    private NodeTree nodeTree = new NodeTree(jTree);

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
    private String sinkNodeAddressForChart;
    private String nodeAddressForChart;
    private Node nodeForChart;

    private Integer REQUEST_FOR_DATA_INTERVAL=5000;

    public MainActivity(JFrame frame) {
        this.frame = frame;
        frame.setContentPane(tabbedPane1);
        frame.setVisible(true);

        menuBar = frame.getJMenuBar();
        fileMenu = menuBar.getMenu(0);
        configMenu = menuBar.getMenu(1);
        accountMenu = menuBar.getMenu(2);

        nodeMenu=new JMenu("节点");
        syncItem=new JMenuItem("从串口同步节点");
        nodeMenu.add(syncItem);
        menuBar.add(nodeMenu);

        JMenu databaseMenu = new JMenu("数据库");
        JMenuItem cleardbMenu=new JMenuItem("清空数据库中的所有传感器数据");
        cleardbMenu.addActionListener((l)->{
            int selection=JOptionPane.showConfirmDialog(frame,"确定要清空所有传感器数据吗？删除操作不可恢复！","警告",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
            if(selection==JOptionPane.OK_OPTION) {
                nodeManager.clearAllData();
                JOptionPane.showConfirmDialog(frame,"传感器数据已清空！","成功",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE);
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


        autoGenerate = new JMenuItem("开启自动添加子节点(演示)");
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

        accountMenu.getMenuComponent(0).setEnabled(false);
        accountMenu.getMenuComponent(1).setEnabled(false);
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

            class ChangePWDDialog extends JDialog {
                public ChangePWDDialog(JFrame owner) {
                    super(owner, "修改密码", true);
                    AccountManager actmgr = AccountManager.getAccountManager();
                    JPanel panel = new JPanel();
                    panel.setLayout(new GridLayout(4, 2));
                    JLabel usernameLabel = new JLabel("当前账号：");
                    panel.add(usernameLabel);
                    JTextField usernameText = new JTextField(actmgr.getNowUsername());
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
                        String msg = null;
                        Boolean flag = false;
                        if (password.equals("") || confirmPassword.equals("")) {
                            msg = "密码不能为空！";
                            flag = false;
                        } else if (!(confirmPassword.equals(password))) {
                            msg = "两次密码不一致！";
                            flag = false;
                        } else {

                            Boolean result = actmgr.changePassword(actmgr.getNowUsername(), new String(password));
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


        nodeTab = new NodeTab();
        commTab = new CommTab();
        touristTab = new TouristTab();

        tabbedPane1.addChangeListener((l) ->

        {
           Component tab= tabbedPane1.getSelectedComponent();
            outputChartItem.setEnabled(false);
            autoSaveLogItem.setEnabled(false);
            outputTouristItem.setEnabled(false);

            if (tab.equals(CommJPanel)) {
                outputChartItem.setEnabled(true);
                autoSaveLogItem.setEnabled(true);
                SinkNodeCombo.removeAllItems();
                NodeCombo.removeAllItems();
                enableSelectForSinkNodeCombo = !(nodeTree.containSinkNode(new SinkNode(sinkNodeAddressForChart)));
                for (SinkNode sinkNode : nodeTree.getSinkNodeList()) {
                    SinkNodeCombo.addItem(sinkNode.getAddress()+":"+sinkNode.getName());
                }
                enableSelectForSinkNodeCombo = true;
                if (sinkNodeAddressForChart != null&&nodeAddressForChart!=null) {
                    SinkNode s=nodeTree.searchSinkNode(sinkNodeAddressForChart);
                    Node n=nodeTree.searchNode(sinkNodeAddressForChart,nodeAddressForChart);
                    if(s!=null&&n!=null) {
                        SinkNodeCombo.setSelectedItem(sinkNodeAddressForChart + ":" + s.getName());
                        NodeCombo.setSelectedItem(nodeAddressForChart + ":" + n.getName());
                    }
                }else{
                    JOptionPane.showConfirmDialog(frame,"未找到可监测的传感节点，请打开串口并前往节点管理，进行串口同步！","错误",JOptionPane.DEFAULT_OPTION,JOptionPane.ERROR_MESSAGE);
                }
                commTab.startNewChart();
            } else if(tab.equals(TouristJPanel)){
                outputTouristItem.setEnabled(true);
             }
        });

    }


    private DefaultMutableTreeNode seletectedTreeNode;
    private String selectedNodeAddress;
//    private Random random;

    private JMenuItem lookitem;
    private JMenuItem additem;
    private JMenuItem deleteitem;
    private JPopupMenu popupMenu;

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    class NodeTab {
        NodeTab() {
            nodeManager.syncByDatabase(nodeTree);
            jTree.setModel(nodeTree.getModel());
            jTree.addTreeSelectionListener(e -> {
                        JTree tree = (JTree) e.getSource();
                        //利用JTree的getLastSelectedPathComponent()方法取得目前选取的节点.
                        seletectedTreeNode =
                                (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                        if (seletectedTreeNode == null) return;
                        selectedNodeAddress = seletectedTreeNode.toString().split(":")[0];
//                        if (autoGenerateFlag) {
//                            random = new Random(System.currentTimeMillis());
//                        }
                        if (seletectedTreeNode.getLevel() == 0) {
                            添加子节点Button.setText("添加Sink节点");
                            删除节点Button.setText("删除所有节点");
//                            if (autoGenerateFlag)
//                                nodeTree.insertNodeIntoNodeTree(seletectedTreeNode, new SinkNode("sinknode", "sinknode" + Integer.toString(random.nextInt(50)), "detail"));
                        } else if (seletectedTreeNode.getLevel() == 1) {
                            添加子节点Button.setText("添加传感器");
                            删除节点Button.setText("删除当前节点");
//                            if (autoGenerateFlag)
//                                nodeTree.insertNodeIntoNodeTree(seletectedTreeNode, new Node(seletectedTreeNode.toString().split(":")[0], "name", "detail","node" + Integer.toString(random.nextInt(50))));
                        }
                        nodeControl.setBorder(BorderFactory.createTitledBorder("当前选中节点：" + selectedNodeAddress));
                        SwingUtilities.invokeLater(() -> nodeControl.updateUI());
//                      myTable.addBean(nodeTree.searchNode(seletectedTreeNode.getParent(),seletectedTreeNode));
                        nodeTree.updateUI();
                    }
            );

            添加子节点Button.addActionListener(new NodeActionListener());

            查看节点信息Button.addActionListener(new NodeActionListener());

            删除节点Button.addActionListener(new NodeActionListener());

            从串口同步节点结构Button.addActionListener(new SyncNodeActionListener());
            syncItem.addActionListener(new SyncNodeActionListener());

            jTree.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (evt.getModifiers() == InputEvent.BUTTON1_MASK && evt.getClickCount() == 2) {
//                        JOptionPane.showConfirmDialog(frame, "双击"+selectedNodeAddress, "双击",
//                                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
                    } else if (evt.getModifiers() == InputEvent.BUTTON3_MASK) {
                        popupMenu = new JPopupMenu();
                        JMenuItem syncitem2=new JMenuItem("从串口同步节点");
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

        class SyncNodeActionListener implements ActionListener{
            @Override
            public void actionPerformed(ActionEvent e) {
                if(CommHandler.commSwitch) {
                    CommHandler.requestSinkNodeList();
                    SinkNodeCombo.removeAllItems();
                    NodeCombo.removeAllItems();
                    for (SinkNode sinkNode : nodeTree.getSinkNodeList()) {
                        SinkNodeCombo.addItem(sinkNode.getAddress()+":"+sinkNode.getName());
                    }
                    JOptionPane.showConfirmDialog(frame,"已发送同步命令至下位机！可多次进行同步以保证所有节点同步完毕。","同步",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE);
                }else{
                    JOptionPane.showConfirmDialog(frame,"请先打开串口！","Error",JOptionPane.DEFAULT_OPTION,JOptionPane.ERROR_MESSAGE);
                    tabbedPane1.setSelectedIndex(1);
                }
            }
        }

        class NodeActionListener implements ActionListener {

            @Override
            public void actionPerformed(ActionEvent e) {
                Object source = e.getSource();
                if (source.equals(添加子节点Button) || source.equals(additem)) {
                    JDialog dialog = new TableDialog(frame, seletectedTreeNode, selectedNodeAddress);
                    dialog.setVisible(true);
                } else if (source.equals(查看节点信息Button) || source.equals(lookitem)) {
                    TreeNode parentNode = seletectedTreeNode.getParent();
                    if (parentNode != null) {
                        JDialog dialog = new TableDialog(frame, parentNode.toString().split(":")[0], nodeTree.searchNode(seletectedTreeNode.getParent(), seletectedTreeNode), selectedNodeAddress);
                        dialog.setVisible(true);
                    } else {
                        if(seletectedTreeNode.getChildCount()==0)
                            JOptionPane.showConfirmDialog(frame,"当前没有任何Sink节点，请前往‘监测控制’打开串口，并同步节点！","Sink节点树",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE);
                        else
                            JOptionPane.showConfirmDialog(frame,"我只是一棵树没什么好看的！","Sink节点树",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE);
                    }

                } else if (source.equals(删除节点Button) || source.equals(deleteitem)) {
                    int selection = JOptionPane.showConfirmDialog(frame, "你确定要删除" + selectedNodeAddress + "吗？删除操作不可恢复且所有子节点都会被删除！", "删除节点",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (selection == JOptionPane.OK_OPTION) {
                        nodeTree.removeNodeFromNodeTree(seletectedTreeNode);
                        nodeTree.clearSelection();
                    }
                }

            }
        }

        class TableDialog extends JDialog {

            Boolean changed_flag = false;

            public TableDialog(JFrame owner, String parent, Node node, String title) {

                super(owner, title, true);
                setLayout(new FlowLayout());
                BeanTable table = new BeanTable();
                table.addBean(node);
                JTable t = new EditableJTable(table);
                t.setPreferredScrollableViewportSize(new Dimension(550, 100));
                t.getModel().addTableModelListener(e -> changed_flag = true);
                // 将表格加入到滚动条组件中
                JScrollPane scrollPane = new JScrollPane(t);
                add(scrollPane);

                JButton update_button = new JButton("保存修改");
                update_button.addActionListener(e -> {
                    Map<String, Object> map = new HashMap<>();
                    for (int i = 0; i < table.getColumnCount(); i++) {
                        map.put(table.getColumnName(i), t.getValueAt(0, i));
                    }

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
                        if (nodeTree.updateNode(parent, oldnode, newnode)) {
                            nodeTree.updateUI();
                            JOptionPane.showConfirmDialog(frame, "保存成功！", "查看节点信息",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showConfirmDialog(frame, "保存失败！找不到修改的父节点！", "查看节点信息",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    changed_flag = false;
                });
                add(update_button);


                JButton cancel_button = new JButton("还原修改");
                cancel_button.addActionListener(e -> {
                    table.clearList();
                    table.addBean(node);
                    javax.swing.SwingUtilities.invokeLater(() -> t.updateUI());
                    changed_flag = true;
                });
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

            public TableDialog(JFrame owner, DefaultMutableTreeNode selectionNode, String title) {
                super(owner, title, true);
                setLayout(new FlowLayout());
                BeanTable<Node> table = new BeanTable();
                Integer Level = selectionNode.getLevel();

                if (Level == 0) {
                    table.addBean(SinkNode.emptyBean());
                } else if (Level == 1) {
                    Node node = Node.emptyBean();
                    node.setParent(selectionNode.toString().split(":")[0]);
                    table.addBean(node);
                }
                JTable t = new EditableJTable(table);
                t.setPreferredScrollableViewportSize(new Dimension(550, 100));
                t.getModel().addTableModelListener(e -> changed_flag = true);
                // 将表格加入到滚动条组件中
                JScrollPane scrollPane = new JScrollPane(t);
                add(scrollPane);

                JButton update_button = new JButton("添加");
                update_button.addActionListener(e -> {
                    Map<String, Object> map = new HashMap<>();
                    for (int i = 0; i < table.getColumnCount(); i++) {
                        map.put(table.getColumnName(i), t.getValueAt(0, i));
                    }

                    if (Level == 0) {
                        nodeTree.insertNodeIntoNodeTree(selectionNode, new SinkNode(map));
                    } else if (Level == 1) {
                        Node newnode = new Node(map);
                        newnode.setParent(selectionNode.toString().split(":")[0]);
                        nodeTree.insertNodeIntoNodeTree(selectionNode, newnode);
                        table.clearList();
                        table.addBean(newnode);
                        SwingUtilities.invokeLater(() -> t.updateUI());
                    }
                    nodeTree.updateUI();
                    JOptionPane.showConfirmDialog(frame, "添加成功！", "添加节点",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    changed_flag = false;
                });
                add(update_button);
                pack();
            }



        }
    }

    private JComboBox portCombo;
    private JComboBox baudrateCombo;

    private JButton 串口Button;
    private JPanel jChartPanel;
    private JButton 高级配置Button;
    private JPanel CommJPanel;
    private JFormattedTextField startTimeText;
    private JFormattedTextField endTimeText;
    private JButton 复位视图Button;
    private JButton 导出数据Button;
    private JTextArea commLogText;
    private JButton 日志输出Button;
    private JTextArea sendCMDText;
    private JButton 发送命令Button;
    private JButton 清空日志Button;
    private JButton 自动缓存Button;
    private JPanel touristTablePanel;
    private JButton 导出游客信息Button;
    private JButton 删除游客记录Button;
    private JButton 查看登陆记录Button;
    private JPanel TouristJPanel;
    private JButton 从串口同步节点结构Button;
    private JButton 设置命令Button;
    private JPanel touristButtonPanel;
    private JPanel mainActivity;
    private JButton 清除该节点数据Button;
    private ChartPanel chartPanel;
    private RealTimeChart realTimeChart;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Integer MAX_LogLine = 1000;
    private Integer autoSaveLineCount = 0;
    private StringBuffer autoSaveLineBuffer = new StringBuffer();
    private Boolean logAutoSave = false;
    private String autoSavePath;
    private TxtUtils autoSaveTxtUtils;

    class CommTab {

        private String selectedPort;
        private Integer selectedBaudrate = 9600;
        private Integer selectedStopbit = SerialPort.STOPBITS_1;
        private Integer selectedDatabit = SerialPort.DATABITS_8;
        private Integer selectedParity = SerialPort.PARITY_NONE;
        private Integer[] baudrateList = new Integer[]{1200, 2400, 4800, 9600, 14400, 19200, 38400, 43000, 57600, 76800, 115200, 128000, 230400, 256000, 460800, 921600, 1382400};


        private Date startTime = new Date();
        private Date endTime = new Date();
        List<Command> commandList;
        SerialComm serialComm;


        CommTab() {
            outputChartItem.addActionListener(new ChartOutputAction());
            autoSaveLogItem.addActionListener(new LogAutoSaveAction());

            portCombo.removeAllItems();
            baudrateCombo.removeAllItems();
            serialComm = SerialComm.getSerialComm();
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
            baudrateCombo.setSelectedIndex(3);
            selectedBaudrate=baudrateList[3];

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

            portCombo.addActionListener((l) ->
                    selectedPort = (String) portCombo.getSelectedItem());
            baudrateCombo.addActionListener((l) -> {
                Object selectedItem = baudrateCombo.getSelectedItem();
                if (selectedItem instanceof String) {
                    selectedBaudrate = null;
                    baudrateCombo.setEditable(true);
                } else {
                    selectedBaudrate = (Integer) selectedItem;
                    baudrateCombo.setEditable(false);
                }
            });

            串口Button.addActionListener((l) -> {
                if (串口Button.getText().equals("关闭串口")) {
                    CommHandler.commSwitch = false;
                    try {
                        autoSaveTxtUtils.appendToTxt(autoSaveLineBuffer.toString());
                        autoSaveLineBuffer = new StringBuffer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                    } finally {
                        串口Button.setText("打开串口");
                    }
                } else {
                    if (selectedPort == null) {
                        JOptionPane.showConfirmDialog(frame, "无可用串口", "串口",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    串口Button.setEnabled(false);
                    if (selectedBaudrate == null) {
                        String s = (String) baudrateCombo.getSelectedItem();
                        try {
                            selectedBaudrate = Integer.parseInt(s);
                        } catch (NumberFormatException e) {
                            JOptionPane.showConfirmDialog(frame, "波特率设置无效", "波特率设置",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    CommHandler.commSwitch = true;
                    串口Button.setText("关闭串口");
                    Thread commThread = new SerialCommThread(selectedPort, selectedBaudrate, selectedDatabit, selectedStopbit, selectedParity);
                    commThread.start();
                    串口Button.setEnabled(true);
                }
            });

            高级配置Button.addActionListener((l) -> {
                JDialog dialog = new AdvancedPortConfig(frame);
                dialog.setSize(MainFrame.screenWidth / 5, MainFrame.screenHeight / 5);
                dialog.setLocation(MainFrame.screenWidth / 2 - dialog.getWidth() / 2, MainFrame.screenHeight / 2 - dialog.getHeight() / 2);
                dialog.setVisible(true);
            });

            SinkNodeCombo.addActionListener((e) -> {
                if (SinkNodeCombo.getSelectedItem() == null || enableSelectForSinkNodeCombo == false)
                    return;
                String selectedItem=(String)(SinkNodeCombo.getSelectedItem());
                sinkNodeAddressForChart=selectedItem.split(":")[0];
                NodeCombo.removeAllItems();
                try {
                    for (SinkNode sinkNode : nodeTree.getSinkNodeList()) {
                        if (sinkNode.getAddress().equals(sinkNodeAddressForChart)) {
                            for (Node node : sinkNode.getNodeList()) {
                                NodeCombo.addItem(node.getAddress() + ":" + node.getName());
                            }
                        }
                    }
                }catch (ConcurrentModificationException cme){}
            });

            NodeCombo.addActionListener((e) -> startNewChart());

            startTimeText.setAction(new DateInputAction());
            startTimeText.addKeyListener(new DateInputKeyListener());
            startTimeText.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    String s = startTimeText.getText();
                    startTime = new Date();
                    try {
                        startTime = sdf.parse(s);
                    } catch (ParseException e1) {
                        JOptionPane.showConfirmDialog(frame, "错误的日期格式！", "起始时间错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        startTimeText.setText("");
                    }
                    if (startTime.getTime() >= endTime.getTime()) {
                        startTime = new Date(endTime.getTime() - 1000);
                    }
                    startTimeText.setText(sdf.format(startTime));
                    if(realTimeChart!=null)
                          realTimeChart.setXRange(startTime.getTime(), endTime.getTime());
                    nodeManager.retrieveDataFromDB(realTimeChart.getTimeSeries(), sinkNodeAddressForChart, nodeAddressForChart, startTime, endTime);
                }
            });

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
                        endTime = startTime;
                        endTime = new Date(startTime.getTime() + 1000);
                    }
                    endTimeText.setText(sdf.format(endTime));
                    realTimeChart.setXRange(startTime.getTime(), endTime.getTime());
                    nodeManager.retrieveDataFromDB(realTimeChart.getTimeSeries(), sinkNodeAddressForChart, nodeAddressForChart, startTime, endTime);
                }
            });

            清除该节点数据Button.addActionListener((l)->{
                if(NodeCombo.getSelectedItem()==null||nodeForChart==null) {
                    JOptionPane.showConfirmDialog(frame, "请先选择一个传感器节点！", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int selection= JOptionPane.showConfirmDialog(frame,"确定要清除传感器"+nodeForChart.getAddress()+":"+nodeForChart.getName()+"保存在数据库中的数据吗？该操作不可恢复！","警告",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
                if(selection==JOptionPane.OK_OPTION){
                    nodeForChart.clearData();
                    JOptionPane.showConfirmDialog(frame,"数据清除成功","成功",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE);
                }
            });

            导出数据Button.addActionListener(new ChartOutputAction());

            复位视图Button.addActionListener((l) -> {
                if (realTimeChart == null) {
                    JOptionPane.showConfirmDialog(frame, "请先选择一个节点！", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                realTimeChart.resetXRange();
                startTimeText.setText("");
                endTimeText.setText("");
            });



            日志输出Button.addActionListener((l) -> {
                JFileChooser jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                jfc.showSaveDialog(new JLabel());
                try {
                    String path = jfc.getSelectedFile().getAbsolutePath() + File.separator + sdf.format(new Date()).replaceAll(":", "-") + ".txt";
                    TxtUtils txtUtils = new TxtUtils(path);
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

            commandList = CommHandler.presetCommand();
        }

        private void sendCommand(String cmd){
            serialComm.sendToPort(comm, cmd.getBytes());
            String temp = sdf.format(new Date()) + "  " + comm.getName() + "【发送】" + "\r\n\t"+ cmd +"\r\n";
            commLogText.append(temp);
            autoSaveLineBuffer.append(temp);
            autoSaveLineCount++;
        }

        private void startNewChart() {
            if(NodeCombo.getSelectedItem()==null||SinkNodeCombo.getSelectedItem()==null)return;
            String selectedSinkNode=(String)SinkNodeCombo.getSelectedItem();
            String selectedNode=(String)NodeCombo.getSelectedItem();
            if ((selectedNode.split(":")[0].equals(nodeAddressForChart) && selectedSinkNode.split(":")[0].equals(sinkNodeAddressForChart)))
                return;//重复选择忽略
            if (chartPanel != null)
                jChartPanel.remove(chartPanel);//删去旧图表

            nodeAddressForChart = selectedNode.split(":")[0];
            nodeForChart = nodeTree.searchNode(sinkNodeAddressForChart, nodeAddressForChart);
            realTimeChart = new RealTimeChart("chart", nodeAddressForChart+":"+nodeForChart.getName(), nodeForChart.getDetail(), nodeForChart.getTimeSeries());
            nodeForChart.setTimeSeries(realTimeChart.getTimeSeries());
            chartPanel = realTimeChart.getChartPanel();
            jChartPanel.add(chartPanel, BorderLayout.CENTER);
            SwingUtilities.invokeLater(() -> jChartPanel.updateUI());
            CommHandler.setChart(realTimeChart,nodeForChart);

        }


        class CommandConfigAction extends AbstractAction{

            @Override
            public void actionPerformed(ActionEvent e) {

                JDialog cmdDialog=new CMDConfigDialog(frame);
                cmdDialog.setVisible(true);

            }

            class CMDConfigDialog extends JDialog {
                public CMDConfigDialog(JFrame owner) {
                    super(owner, "设置指令集", false);
                    setLayout(new FlowLayout());
                    BeanTable cmd_table = new BeanTable(new Command().getFieldNames(),commandList);
                    JTable t = new EditableJTable(cmd_table);
                    t.setPreferredScrollableViewportSize(new Dimension(550, 100));

                    cmd_table.addTableModelListener((e)->{
                            int row=e.getFirstRow();
                            int col=e.getColumn();
                            Map<String,Object> map=new HashMap<>();
                            map.put(cmd_table.getColumnName(col),cmd_table.getValueAt(row,col));
                            commandList.get(row).setFields(map);
                        });
                    // 将表格加入到滚动条组件中
                    JScrollPane scrollPane = new JScrollPane(t);
                    add(scrollPane);

                    JButton add_cmd = new JButton("添加新指令");
                    add_cmd.addActionListener((l)->{
                        commandList.add(new Command("<>",""));
                        SwingUtilities.invokeLater(()->t.updateUI());
                    });

                    JButton del_cmd=new JButton("删除指令");
                    del_cmd.addActionListener((l)->{
                        int[] rows=t.getSelectedRows();
                        for (int row:rows) {
                            commandList.remove(row);
                        }
                        SwingUtilities.invokeLater(()->t.updateUI());
                    });

                    JButton send_cmd=new JButton("发送指令");
                    send_cmd.addActionListener((l)->{
                        int[] rows=t.getSelectedRows();
                        for (int row:rows) {
                            String cmd=(String)cmd_table.getValueAt(row,cmd_table.getTitleCol(Command.KEY_COMMAND));
                            if (CommHandler.commSwitch){
                                sendCommand(cmd);
                            }
                            else{
                                JOptionPane.showConfirmDialog(frame,"请先打开串口！","错误",JOptionPane.DEFAULT_OPTION,JOptionPane.ERROR_MESSAGE);
                                dispose();
                                break;
                            }
                        }
                    });
                    add(add_cmd);
                    add(del_cmd);
                    add(send_cmd);
                    pack();
                }
            }
        }

        class LogAutoSaveAction extends AbstractAction {

            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog jDialog = new AutoSaveDialog(frame);
                jDialog.setVisible(true);
            }

            class AutoSaveDialog extends JDialog {
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
                        JFileChooser jfc = new JFileChooser();
                        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        jfc.showSaveDialog(new JLabel());
                        autoSavePath = jfc.getSelectedFile().getAbsolutePath();
                        pathTextField.setText(autoSavePath);
                        autoSaveTxtUtils = new TxtUtils(autoSavePath + File.separator + sdf.format(new Date()).replaceAll(":", "-") + ".txt");
                    });

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
                            } catch (NumberFormatException nfe) {
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

        class DateInputAction extends AbstractAction {
            public DateInputAction() {
                putValue(SHORT_DESCRIPTION, "日期格式: 2000-01-01 00:00:00");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
            }
        }

        class DateInputKeyListener implements KeyListener {

            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    复位视图Button.requestFocus();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        }


        class AdvancedPortConfig extends JDialog {

            Integer[] stopbits = new Integer[]{SerialPort.STOPBITS_1, SerialPort.STOPBITS_1_5, SerialPort.STOPBITS_2};
            Integer[] databits = new Integer[]{SerialPort.DATABITS_5, SerialPort.DATABITS_6, SerialPort.DATABITS_7, SerialPort.DATABITS_8};
            String[] parities = new String[]{"无", "奇校验", "偶校验"};

            public AdvancedPortConfig(JFrame owner) {
                super(owner, "串口设置", true);
                JPanel panel = new JPanel();
                panel.setLayout(new GridLayout(5, 2));

                panel.add(new JLabel("数据位"));
                JComboBox databitsCombot = new JComboBox();
                for (Integer databit : databits) {
                    databitsCombot.addItem(databit);
                }
                databitsCombot.setSelectedItem(databits[3]);
                databitsCombot.addActionListener((l) -> {
                    selectedStopbit = (Integer) databitsCombot.getSelectedItem();
                });
                panel.add(databitsCombot);

                panel.add(new JLabel("停止位"));
                JComboBox stopbitsCombo = new JComboBox();
                for (Integer stopbit : stopbits) {
                    if (stopbit == SerialPort.STOPBITS_1_5)
                        stopbitsCombo.addItem(new Float(1.5));
                    stopbitsCombo.addItem(stopbit);
                }
                stopbitsCombo.addActionListener((l) -> {
                    Object stopbitItem = stopbitsCombo.getSelectedItem();
                    if (stopbitItem instanceof Float)
                        selectedStopbit = SerialPort.STOPBITS_1_5;
                    else {
                        selectedStopbit = (Integer) stopbitItem;
                    }
                });
                panel.add(stopbitsCombo);

                panel.add(new JLabel("奇偶检验"));
                JComboBox parityCombo = new JComboBox();
                for (String parity : parities) {
                    parityCombo.addItem(parity);
                }
                parityCombo.addActionListener((l) -> {
                    String parityItem = (String) parityCombo.getSelectedItem();
                    if (parityItem.equals(parities[0])) {
                        selectedParity = SerialPort.PARITY_NONE;
                    } else if (parityItem.equals(parities[1])) {
                        selectedParity = SerialPort.PARITY_ODD;
                    } else if (parityItem.equals(parities[2])) {
                        selectedParity = SerialPort.PARITY_EVEN;
                    }
                });
                panel.add(parityCombo);

                panel.add(new JLabel("请求数据周期(秒)"));
                JTextField interval=new JTextField();
                Float f=REQUEST_FOR_DATA_INTERVAL/1000f;
                interval.setText(f.toString());
                panel.add(interval);

                JButton button = new JButton("确认");
                button.addActionListener(event -> {
                    if (selectedBaudrate == null) {
                        String s = (String) baudrateCombo.getSelectedItem();
                        try {
                            selectedBaudrate = Integer.parseInt(s);
                        } catch (NumberFormatException e) {
                            JOptionPane.showConfirmDialog(frame, "波特率设置无效!", "波特率设置",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    try {
                        Float interval_f = Float.parseFloat(interval.getText());
                        REQUEST_FOR_DATA_INTERVAL=(int)(interval_f*1000);
                    }catch (NumberFormatException e){
                        JOptionPane.showConfirmDialog(frame,"请求数据周期请输入数字!","请求数据周期",JOptionPane.DEFAULT_OPTION,JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    dispose();
                });
                panel.add(button);
                add(panel);
                pack();
            }
        }


        class SerialCommThread extends Thread {

            private String portName;
            private Integer baudrate;
            private Integer databit;
            private Integer stopbit;
            private Integer parity;

            public SerialCommThread(String portName, Integer baudrate, Integer databit, Integer stopbit, Integer parity) {
                super();
                this.portName = portName;
                this.baudrate = baudrate;
                this.databit = databit;
                this.stopbit = stopbit;
                this.parity = parity;
            }

            @Override
            public synchronized void run() {
                synchronized (SerialComm.class) {
                    SerialComm serialComm = SerialComm.getSerialComm();
                    ArrayList<String> arrayList = serialComm.findPort();
                    System.out.println(arrayList);
                    System.out.println(System.getProperty("java.library.path"));
                    try {
                        comm = serialComm.openPort(portName, baudrate, databit, stopbit, parity);
                        CommHandler.comm=comm;
                        JOptionPane.showConfirmDialog(frame,"串口打开成功！正在自动发送同步节点结构命令。","串口",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE);
                    } catch (NoSuchPortException e) {
                        串口Button.doClick();
                        JOptionPane.showConfirmDialog(frame, "端口不存在", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        return;
                    } catch (PortInUseException e) {
                        串口Button.doClick();
                        JOptionPane.showConfirmDialog(frame, "端口已被占用", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        return;
                    }
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

                                    case SerialPortEvent.DATA_AVAILABLE: // 1 串口存在可用数据
                                        byte[] bytes = null;
                                        try {
                                            bytes = serialComm.readFromPort(comm);

                                        } catch (Exception e) {
                                            CommHandler.commSwitch = false;
                                            JOptionPane.showConfirmDialog(frame, "串口连接断开！", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                                            串口Button.doClick();
                                        }
                                        try {
                                            if (CommHandler.parseRawData(bytes, comm,nodeTree)) ;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        if (bytes != null) {
                                            String temp = sdf.format(new Date()) + "  " + comm.getName() + "【接收】" + "\r\n\t"+new String(bytes)+"\r\n";
                                            autoSaveLineBuffer.append(temp);
                                            EventQueue.invokeLater(() -> {
                                                commLogText.append(temp);
                                            });
                                            if (commLogText.getLineCount() > 1000) {
                                                commLogText.setText("");
                                            }
                                            if (autoSaveLineCount++ > MAX_LogLine && logAutoSave) {
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
                    );
                    for(int i=0;i<3;i++) {
                        CommHandler.requestSinkNodeList();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    while (CommHandler.commSwitch) {
                        try {
                            Thread.sleep(REQUEST_FOR_DATA_INTERVAL);
                            if(nodeAddressForChart!=null) {
                                CommHandler.requestSensorDataAll(sinkNodeAddressForChart);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    serialComm.closePort(comm);
                }
            }
        }
    }


    class TouristTab {
        BeanTable touristTable;
        JTable t;

        TouristTab() {
            touristTablePanel.setLayout(new BorderLayout());
            touristManager.syncByDatabase();
            t = touristManager.getJTable();
            touristTable = (BeanTable) t.getModel();
            t.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
                        JPopupMenu touristpopupMenu = new JPopupMenu();
                        JMenuItem lookItem=new JMenuItem("查看游客登陆记录");
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

        class DeleteTouristAction extends AbstractAction {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(t.getSelectedRowCount()!=0) {
                    int selection = JOptionPane.showConfirmDialog(frame, "删除不可恢复，确定？", "删除", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (selection == JOptionPane.OK_OPTION) {
                        String IMEI;
                        try {
                            int[] rows = t.getSelectedRows();
                            for (int row : rows) {
                                IMEI = (String) touristTable.getValueAt(row, touristTable.getTitleCol(TouristManager.KEY_TOURIST_IMEI));
                                touristManager.removeTourist(new TouristManager.Tourist(IMEI));
                                SwingUtilities.invokeLater(() -> t.updateUI());
                            }
                        } catch (ClassCastException cce) {
                            cce.printStackTrace();
                        }
                    }
                }else{
                    JOptionPane.showConfirmDialog(frame, "请先选中一条记录！", "删除", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        class LookTouristLogAction extends AbstractAction {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (t.getSelectedRowCount() == 0) {
                    JOptionPane.showConfirmDialog(frame, "请先选择一条记录!", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                } else {
                    int row = t.getSelectedRow();
                    String IMEI = (String) touristTable.getValueAt(row, touristTable.getTitleCol(TouristManager.KEY_TOURIST_IMEI));
                    JDialog dialog = new LookTouristLogDialog(frame, IMEI);
                    dialog.setVisible(true);
                }
            }

            class LookTouristLogDialog extends JDialog {
                public LookTouristLogDialog(JFrame owner, String IMEI) {
                    super(owner, "找回密码", true);
                    List<Bean> logs=touristManager.lookConnectLog(new TouristManager.Tourist(IMEI));
                    BeanTable<Bean> logTable = new BeanTable<>(new TouristManager.ConnectLog().getFieldNames(),logs);
                    JTable t = new JTable(logTable);
                    t.setPreferredScrollableViewportSize(new Dimension(550, 100));
                    // 将表格加入到滚动条组件中
                    JScrollPane scrollPane = new JScrollPane(t);
                    add(scrollPane, BorderLayout.CENTER);
                    JButton refresh=new JButton("刷新");
                    refresh.addActionListener((l)->SwingUtilities.invokeLater(()->{
                        List<Bean> logs2=touristManager.lookConnectLog(new TouristManager.Tourist(IMEI));
                        BeanTable<Bean> logTable2 = new BeanTable<>(new TouristManager.ConnectLog().getFieldNames(),logs2);
                        t.setModel(logTable2);
                        t.updateUI();
                    }));
                    add(refresh,BorderLayout.SOUTH);
                    pack();
                }
            }
        }
        class TouristOutputAction extends AbstractAction{

            @Override
            public void actionPerformed(ActionEvent e) {
                if(touristManager.getTouristList().size()==0){
                    JOptionPane.showConfirmDialog(frame, "没有信息可供导出", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                }else {
                    JFileChooser jfc = new JFileChooser();
                    jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    jfc.showDialog(new JLabel(), "选择目录");
                    try {
                        String path = jfc.getSelectedFile().getAbsolutePath() + File.separator + "TouristsInfo_" + MyDatabase.dateTimeFormmatter(new Date()).replaceAll(":","-") + ".xls";
                        ExcelUtils<Bean> excelUtils = new ExcelUtils(path);
                        Map<String,List<Bean>> map=new HashMap<>();
                        map.put("游客基本信息",touristManager.getTouristList());
                        map.put("游客连接记录",touristManager.lookAllConnectLog());
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
}


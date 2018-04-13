package com.qq452651705.GUI;

import com.qq452651705.DataMGM.Account.AccountManager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * The type Login activity.  登陆界面
 */
public class LoginActivity {


    /**
     * @param accountManager 账号管理类实例
     */
    private AccountManager accountManager = AccountManager.getAccountManager();


    /**************************GUI组件******************************/
    private JFrame frame;
    private JPanel panel1;
    private JTextField login_username;
    private JPasswordField login_pswd;
    private JButton 注册Button;
    private JButton 登陆Button;
    private JPanel loginPanel;
    private JTextField register_username;
    private JPasswordField register_pswd;
    private JPasswordField register_pswd_confirm;
    private JButton register_commit;
    /**************************GUI组件******************************/


    /**
     * Instantiates a new Login activity. 初始化登陆界面
     *
     * @param frame the frame    主窗口
     */
    public LoginActivity(JFrame frame) {
        this.frame = frame;
        frame.setContentPane(loginPanel);
        initButton();
        initMenuBar();
        frame.pack();
        SwingUtilities.invokeLater(()->loginPanel.updateUI());
    }

    /**
     * Instantiates menu bar. 初始化菜单栏
     */
    private void initMenuBar() {

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        /**
         * 文件菜单
         */
        JMenu fileMenu = new JMenu("文件");
        menuBar.add(fileMenu);
        JMenuItem closeItem = new JMenuItem("关闭");
        closeItem.addActionListener(e -> System.exit(0));
        fileMenu.add(closeItem);

        /**
         * 配置菜单
         */
        JMenu configMenu = new JMenu("配置");
        menuBar.add(configMenu);
        JMenu styleMenu = new JMenu("主题");
        UIManager.LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo info : infos) {
            JMenuItem styleItem = new JMenuItem(info.getName());
            if (info.getName().equals("Nimbus")) {
                try {
                    UIManager.setLookAndFeel(info.getClassName());
                    SwingUtilities.updateComponentTreeUI(frame);
                } catch (Exception e) {
                }
            }
            styleItem.addActionListener(event -> {
                try {
                    UIManager.setLookAndFeel(info.getClassName());
                    SwingUtilities.updateComponentTreeUI(frame);
                } catch (Exception e) {
                }
            });
            styleMenu.add(styleItem);
        }
        configMenu.add(styleMenu);

        /**
         * 账号菜单
         */
        JMenu accountMenu = new JMenu("账号");
        menuBar.add(accountMenu);
        JMenuItem registerItem = new JMenuItem("注册");
        JMenuItem forgetItem = new JMenuItem("找回密码");
        registerItem.addActionListener(new RegisterAction());
        forgetItem.addActionListener(new ForgetAction());
        accountMenu.add(registerItem);
        accountMenu.add(forgetItem);
    }


    /**
     * Instantiates menu bar. 初始化按键
     *
     */
    private void initButton() {
        注册Button.addActionListener(new RegisterAction());
        登陆Button.addActionListener(new LoginAction());
    }

    /**
     * The type Register action. 注册触发类.实现注册操作的响应
     */
    class RegisterAction extends AbstractAction {

        /**
         * Instantiates a new Register action.
         */
        public RegisterAction() {
            putValue(Action.NAME, "注册");
        }

        /**
         * Instantiates a new Register action. 弹出一个注册窗口框
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            JDialog dialog = new RegisterDialog(frame);
            dialog.setSize(MainFrame.screenWidth / 5, MainFrame.screenHeight / 5);
            dialog.setLocation(MainFrame.screenWidth / 2 - dialog.getWidth() / 2, MainFrame.screenHeight / 2 - dialog.getHeight() / 2);
            dialog.setVisible(true);
        }

        /**
         * The type Register dialog.  注册窗口框实现
         */
        class RegisterDialog extends JDialog {
            /**
             * Instantiates a new Register dialog. 初始化注册窗口框
             *
             * @param owner the owner   父窗口
             */
            public RegisterDialog(JFrame owner) {

                super(owner, "注册", true);
                JPanel panel = new JPanel();
                panel.setLayout(new GridLayout(4, 2));
                panel.add(new JLabel("用户名: "));
                register_username = new JTextField();
                register_pswd = new JPasswordField();
                register_pswd_confirm = new JPasswordField();
                panel.add(register_username);
                panel.add(new JLabel("密码:"));
                panel.add(register_pswd);
                panel.add(new JLabel("确认密码:"));
                panel.add(register_pswd_confirm);

                register_commit = new JButton("确认");
                register_commit.addActionListener(event -> {

                    /**
                     *   注册提交操作实现
                     */

                    String username = register_username.getText();
                    String password = new String(register_pswd.getPassword());
                    String confirmPassword = new String(register_pswd_confirm.getPassword());
                    String msg ;
                    Boolean flag ;
                    if (password.equals("") || confirmPassword.equals("")) {
                        msg = "密码不能为空！";
                        flag = false;
                    } else if (!(confirmPassword.equals(password))) {
                        msg = "两次密码不一致！";
                        flag = false;
                    } else {
                        AccountManager actmgr = AccountManager.getAccountManager();
                        Boolean result = actmgr.register(username, new String(password));
                        if (result) {
                            msg = "注册成功";
                            flag = true;
                        } else {
                            msg = "用户名已存在!";
                            flag = false;
                        }
                    }
                    if (flag) {
                        JOptionPane.showConfirmDialog(frame, msg, "注册成功",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        JOptionPane.showConfirmDialog(frame, msg, "注册失败",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    }
                });
                panel.add(register_commit);
                add(panel);
                pack();
            }
        }
    }


    /**
     * The type Login action. 登陆触发类.实现登陆操作的响应
     */
    class LoginAction extends AbstractAction {

        /**
         * Instantiates a new Login action.
         */
        public LoginAction() {
            putValue(Action.NAME, "登陆");
        }

        @Override
        public void actionPerformed(ActionEvent event) {

            String username = login_username.getText();
            char[] password = login_pswd.getPassword();

            /**
             *   登陆成功则加载主界面MainActivity
             */
            Boolean result = accountManager.login(username, new String(password));
            if (result) {
                javax.swing.SwingUtilities.invokeLater(()-> {
                    MainActivity mainActivity = new MainActivity(frame);
                });
            } else {
                int selection = JOptionPane.showConfirmDialog(frame, "登录失败！", "登陆",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                if (selection == JOptionPane.OK_OPTION) {
                }
            }
        }
    }

    /**
     * The type Forget action.  找回密码触发类
     */
    class ForgetAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent event) {
            JDialog dialog = new ForgetDialog(frame);
            dialog.setVisible(true);
        }

        /**
         * The type Forget dialog.  找回密码窗口.
         */
        class ForgetDialog extends JDialog {
            /**
             * Instantiates a new Forget dialog.
             *
             * @param owner the owner
             */
            public ForgetDialog(JFrame owner) {
                super(owner, "找回密码", true);
                JPanel panel = new JPanel();
                panel.setLayout(new GridLayout(2, 2));
                panel.add(new JLabel("输入管理员密钥: "));
                JTextField rootField = new JTextField();
                panel.add(rootField);
                JButton button = new JButton("确定");
                button.addActionListener(e -> {
                    if ("452651705".equals(rootField.getText())) {
                        dispose();
                        JDialog dialog = new AccountListDialog(frame);
                        dialog.setVisible(true);
                    } else {
                        JOptionPane.showConfirmDialog(frame, "密钥错误，请联系管理员。", "找回密码",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    }
                });
                panel.add(button);
                add(panel);
                pack();
            }
        }
    }

    /**
     * The type Account list dialog.   客户账号信息窗口
     */
    class AccountListDialog extends JDialog {

        /**
         * Instantiates a new Account list dialog.
         *
         * @param owner the owner
         */
        public AccountListDialog(JFrame owner) {

            super(owner, "找回密码", true);
            AccountTable at = new AccountTable();
            at.setList(accountManager.getAllAccounts());
            JTable t = new JTable(at);
            t.setPreferredScrollableViewportSize(new Dimension(550, 100));
            JScrollPane scrollPane = new JScrollPane(t);
            add(scrollPane, BorderLayout.CENTER);
            JButton deletebutton=new JButton("清空");
            deletebutton.addActionListener((l)->{
                int selection=JOptionPane.showConfirmDialog(frame,"是否清空所有用户？","账户管理",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
                if(selection==JOptionPane.OK_OPTION){
                    accountManager.deleteAllAccounts();
                    JOptionPane.showConfirmDialog(frame,"清空成功！","账号管理",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE);
                }
                dispose();
            });
            add(deletebutton,BorderLayout.SOUTH);
            pack();
        }

        /**
         * The type Account table. 账户信息表格
         */
        class AccountTable extends AbstractTableModel {

            private List<Object[]> list;

            String[] n = {"用户名", "密码"};

            /**
             * Sets list.
             *
             * @param list the list
             */
            public void setList(List<Object[]> list) {
                this.list = list;
            }

            @Override
            public int getRowCount() {
                return list.size();
            }

            @Override
            public int getColumnCount() {
                return n.length;
            }

            @Override
            public Object getValueAt(int row, int col) {
                return list.get(row)[col];
            }

            @Override
            public String getColumnName(int column) {
                return n[column];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            @Override
            public void setValueAt(Object value, int rowIndex, int columnIndex) {
                list.get(rowIndex)[columnIndex] = value;
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }
}


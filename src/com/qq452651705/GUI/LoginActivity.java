package com.qq452651705.GUI;

import com.qq452651705.DataMGM.Account.AccountManager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class LoginActivity {

    private JFrame frame;
    private AccountManager accountManager = AccountManager.getAccountManager();

    private JPanel panel1;
    private JTextField textField1;
    private JPasswordField passwordField1;
    private JButton 注册Button;
    private JButton 登陆Button;
    private JMenuBar menuBar;

    public LoginActivity(JFrame frame) {
        this.frame = frame;
        frame.setContentPane(panel1);
        initButton();
        initMenuBar();
        SwingUtilities.invokeLater(()->panel1.updateUI());
    }


    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("文件");
        menuBar.add(fileMenu);
        JMenuItem closeItem = new JMenuItem("关闭");
        closeItem.addActionListener(e -> System.exit(0));
        fileMenu.add(closeItem);

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

        JMenu accountMenu = new JMenu("账号");
        menuBar.add(accountMenu);
        JMenuItem registerItem = new JMenuItem("注册");
        JMenuItem forgetItem = new JMenuItem("找回密码");
        registerItem.addActionListener(new RegisterAction());
        forgetItem.addActionListener(new ForgetAction());

        accountMenu.add(registerItem);
        accountMenu.add(forgetItem);

    }


    private void initButton() {
        注册Button.addActionListener(new RegisterAction());
        登陆Button.addActionListener(new LoginAction());
    }

    class RegisterAction extends AbstractAction {

        public RegisterAction() {
            putValue(Action.NAME, "注册");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JDialog dialog = new RegisterDialog(frame);
            dialog.setSize(MainFrame.screenWidth / 5, MainFrame.screenHeight / 5);
            dialog.setLocation(MainFrame.screenWidth / 2 - dialog.getWidth() / 2, MainFrame.screenHeight / 2 - dialog.getHeight() / 2);
            dialog.setVisible(true);
        }

        class RegisterDialog extends JDialog {
            public RegisterDialog(JFrame owner) {
                super(owner, "注册", true);
                JPanel panel = new JPanel();
                panel.setLayout(new GridLayout(4, 2));
                panel.add(new JLabel("用户名: "));
                JTextField usernameField = new JTextField();
                panel.add(usernameField);
                JPasswordField passwordField = new JPasswordField();
                JPasswordField passwordField2 = new JPasswordField();
                panel.add(new JLabel("密码:"));
                panel.add(passwordField);
                panel.add(new JLabel("确认密码:"));
                panel.add(passwordField2);

                JButton button = new JButton("确认");
                button.addActionListener(event -> {
                    String username = usernameField.getText();
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
                panel.add(button);
                add(panel);
                pack();
            }
        }
    }


    class LoginAction extends AbstractAction {

        public LoginAction() {
            putValue(Action.NAME, "登陆");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            String username = textField1.getText();
            char[] password = passwordField1.getPassword();
            Boolean result = accountManager.login(username, new String(password));
            String msg;
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

    class AccountListDialog extends JDialog {
        public AccountListDialog(JFrame owner) {
            super(owner, "找回密码", true);
            AccountTable at = new AccountTable();
            at.setList(accountManager.getAllAccounts());
            JTable t = new JTable(at);
            t.setPreferredScrollableViewportSize(new Dimension(550, 100));
            // 将表格加入到滚动条组件中
            JScrollPane scrollPane = new JScrollPane(t);
            add(scrollPane, BorderLayout.CENTER);
            pack();
        }

        class AccountTable extends AbstractTableModel {
            private static final long serialVersionUID = 1L;

            private List<Object[]> list;

            String[] n = {"用户名", "密码"};

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

    class ForgetAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent event) {

            JDialog dialog = new ForgetDialog(frame);
            dialog.setVisible(true);
        }

        class ForgetDialog extends JDialog {
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
}


package src;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private JTextField nameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JButton cancelButton;
    private String result = null;
    private ChessDatabase database;

    public LoginDialog(Frame parent, ChessDatabase database) {
        super(parent, "中国象棋 - 玩家登录", true);
        this.database = database;

        setSize(500, 380);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));
        setResizable(false);

        /* ================= top title ================= */
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(139, 69, 19));
        JLabel titleLabel = new JLabel("中国象棋网络版");
        titleLabel.setFont(new Font("楷体", Font.BOLD, 28));
        titleLabel.setForeground(new Color(255, 215, 0));
        titlePanel.add(titleLabel);
        titlePanel.setPreferredSize(new Dimension(0, 70));

        /* ================= middle input ================= */
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        inputPanel.setBackground(new Color(245, 222, 179));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        /* user label */
        JLabel nameLabel = new JLabel("用户名：");
        nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        nameLabel.setForeground(Color.BLACK);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        inputPanel.add(nameLabel, gbc);

        /* user input */
        nameField = new JTextField();
        nameField.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        nameField.setForeground(Color.WHITE);
        nameField.setBackground(Color.WHITE);
        nameField.setOpaque(true);
        nameField.setPreferredSize(new Dimension(250, 40));
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(139, 69, 19), 2),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        inputPanel.add(nameField, gbc);

        /* key label */
        JLabel passwordLabel = new JLabel("密  码：");
        passwordLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        passwordLabel.setForeground(Color.BLACK);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        inputPanel.add(passwordLabel, gbc);

        /* key input */
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        passwordField.setForeground(Color.WHITE);
        passwordField.setBackground(Color.WHITE);
        passwordField.setOpaque(true);
        passwordField.setPreferredSize(new Dimension(250, 40));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(139, 69, 19), 2),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        inputPanel.add(passwordField, gbc);

        /* ================= top button ================= */
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(new Color(222, 184, 135));
        buttonPanel.setPreferredSize(new Dimension(0, 80));

        loginButton = new JButton("登 录");
        loginButton.setFont(new Font("微软雅黑", Font.BOLD, 16));
        loginButton.setBackground(new Color(139, 69, 19));
        loginButton.setForeground(Color.WHITE);
        loginButton.setPreferredSize(new Dimension(100, 45));
        loginButton.setFocusPainted(false);
        loginButton.setBorder(BorderFactory.createRaisedBevelBorder());
        loginButton.addActionListener(e -> handleLogin());

        registerButton = new JButton("注 册");
        registerButton.setFont(new Font("微软雅黑", Font.BOLD, 16));
        registerButton.setBackground(new Color(184, 134, 11));
        registerButton.setForeground(Color.WHITE);
        registerButton.setPreferredSize(new Dimension(100, 45));
        registerButton.setFocusPainted(false);
        registerButton.setBorder(BorderFactory.createRaisedBevelBorder());
        registerButton.addActionListener(e -> handleRegister());

        cancelButton = new JButton("取 消");
        cancelButton.setFont(new Font("微软雅黑", Font.BOLD, 16));
        cancelButton.setBackground(new Color(128, 128, 128));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setPreferredSize(new Dimension(100, 45));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);

        add(titlePanel, BorderLayout.NORTH);
        add(inputPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void handleLogin() {
        String name = nameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (name.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields");
            return;
        }

        PlayerInfo player = database.loginPlayer(name, password);
        if (player != null) {
            result = name;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password");
        }
    }

    private void handleRegister() {
        String name = nameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (name.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields");
            return;
        }

        if (database.registerPlayer(name, password)) {
            JOptionPane.showMessageDialog(this, "Register successful! Now please login.");
            nameField.setText("");
            passwordField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Username already exists or registration failed");
        }
    }

    public String getLoginResult() {
        setVisible(true);
        return result;
    }
}

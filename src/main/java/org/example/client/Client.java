package org.example.client;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

public class Client extends JFrame {
    private  final String SERVER_ADDR = "localhost";
    private  final int SERVER_PORT = 8888;
    private  Socket socket;
    private  DataInputStream in;
    private  DataOutputStream out;
    private  String login;
    private  String pass;
    private  JTextArea jTextArea;

    public Client() {
        prepareMainGui();
        start();
    }

    private void prepareMainGui(){
        setTitle("SelfChat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 400);
        setResizable(false);
        setLocationByPlatform(true);

        JMenuBar jMenuBar = new JMenuBar();
        JMenu menuFile = new JMenu("File");
        JMenu menuHelp = new JMenu("Help");
        jMenuBar.add(menuFile);
        jMenuBar.add(menuHelp);

        JMenuItem menuFileItem1 = new JMenuItem("Exit");
        JMenuItem menuFileItem2 = new JMenuItem("Authorization");
        JMenuItem menuFileItem3 = new JMenuItem("Disconnect");
        menuFile.add(menuFileItem1);
        menuFile.add(menuFileItem2);
        menuFile.add(menuFileItem3);
        menuFileItem1.addActionListener(e -> System.exit(0));
        menuFileItem2.addActionListener(e -> new prepareAuthGui());
        menuFileItem3.addActionListener(e -> {
            sendMessage("/EXIT");
            closeConnection();
        });
        JMenuItem menuHelpItem1 = new JMenuItem("About");
        menuHelp.add(menuHelpItem1);
        menuHelpItem1.addActionListener(e -> new prepareAboutGui());

        jTextArea = new JTextArea();
        jTextArea.setEditable(false);
        jTextArea.setLineWrap(true);
        jTextArea.setWrapStyleWord(true);
        JScrollPane jScrollPane = new JScrollPane(jTextArea);
        JTextField jTextField = new JTextField(25);

        ActionListener sendListener = e -> {
            String text = jTextField.getText().trim();
            if (!text.trim().isEmpty()){
                sendMessage(jTextField.getText());
                jTextField.setText(null);
            }
        };
        jTextField.addActionListener(sendListener);

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(sendListener);

        addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
                try {
                    super.windowClosing(e);
                    sendMessage("/EXIT");
                    closeConnection();
                } catch (Exception e1){
                    e1.printStackTrace();
                }
            }
        });

        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        flow.add(jTextField);
        flow.add(sendButton);
        getContentPane().add(flow, BorderLayout.SOUTH);
        getContentPane().add(BorderLayout.NORTH, jMenuBar);
        getContentPane().add(BorderLayout.CENTER, jScrollPane);
        setVisible(true);
        new prepareAuthGui();
    }

    class prepareAuthGui extends JFrame {
        public prepareAuthGui(){
            setTitle("Authorization");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(280, 150);
            setResizable(false);
            setLocationByPlatform(true);

            Container contentPane = getContentPane();
            SpringLayout layout = new SpringLayout();
            contentPane.setLayout(layout);

            JLabel labelLogin = new JLabel("Login");
            JTextField fieldLogin = new JTextField(15);
            JLabel labelPass = new JLabel("Password");
            JPasswordField fieldPass = new JPasswordField(15);
            JButton buttonOk = new JButton("OK");
            JButton buttonCancel = new JButton("Cancel");

            JPanel grid = new JPanel(new GridLayout(1, 2, 5, 0) );
            grid.add (buttonOk);
            grid.add (buttonCancel);
            JPanel flow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            flow.add(grid);

            contentPane.add(flow);
            contentPane.add(labelLogin);
            contentPane.add(fieldLogin);
            contentPane.add(labelPass);
            contentPane.add(fieldPass);

            layout.putConstraint(SpringLayout.WEST , labelLogin, 10,
                    SpringLayout.WEST , contentPane);
            layout.putConstraint(SpringLayout.NORTH, labelLogin, 25,
                    SpringLayout.NORTH, contentPane);
            layout.putConstraint(SpringLayout.NORTH, fieldLogin, 25,
                    SpringLayout.NORTH, contentPane);
            layout.putConstraint(SpringLayout.WEST , fieldLogin, 47,
                    SpringLayout.EAST , labelLogin);

            layout.putConstraint(SpringLayout.WEST , labelPass, 10,
                    SpringLayout.WEST , contentPane);
            layout.putConstraint(SpringLayout.NORTH, labelPass, 25,
                    SpringLayout.NORTH, labelLogin);
            layout.putConstraint(SpringLayout.NORTH, fieldPass, 25,
                    SpringLayout.NORTH, fieldLogin);
            layout.putConstraint(SpringLayout.WEST , fieldPass, 20,
                    SpringLayout.EAST , labelPass);

            layout.putConstraint(SpringLayout.EAST , flow, 5,
                    SpringLayout.EAST , fieldPass);
            layout.putConstraint(SpringLayout.SOUTH, flow, -10,
                    SpringLayout.SOUTH, contentPane);

            ActionListener sendListener = e -> {
                login = fieldLogin.getText();
                pass = new String(fieldPass.getPassword());
                if (!login.trim().isEmpty() && !pass.trim().isEmpty()){
                    onAuthClick();
                    fieldLogin.setText(null);
                    fieldPass.setText(null);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(null, "Login or password is empty!");
                }
            };
            buttonOk.addActionListener(sendListener);
            getRootPane().setDefaultButton(buttonOk);
            buttonCancel.addActionListener(e -> {
                fieldLogin.setText(null);
                fieldPass.setText(null);
                dispose();
            });
            setVisible(true);
        }
    }

    class prepareAboutGui extends JDialog{
        public prepareAboutGui(){
            setTitle("About");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(250, 100);
            setResizable(false);
            setLocationByPlatform(true);
            setModal(true);

            JTextPane jTextPane = new JTextPane();
            jTextPane.setText("Welcome to SelfChat");

            JButton okButton = new JButton("OK");
            okButton.addActionListener(e -> dispose());

            getContentPane().add(BorderLayout.CENTER, jTextPane);
            JPanel grid = new JPanel(new GridLayout(1, 2, 5, 0) );
            grid.add(okButton);
            JPanel flow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            flow.add(grid);
            getContentPane().add(flow, BorderLayout.SOUTH);

            setVisible(true);
        }
    }

    private void start(){
        try {
            socket = new Socket(SERVER_ADDR, SERVER_PORT);
            System.out.println("We have the connection...");
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String strFromServer = in.readUTF();
                        if (strFromServer.startsWith("/authok")) {
                            jTextArea.append("Authorization is ok" + "\n");
                            break;
                        }
                        jTextArea.append(strFromServer + "\n");
                    }
                    while (true) {
                        String strFromServer = in.readUTF();
                        jTextArea.append(strFromServer + "\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Connection to server is lost");
            e.printStackTrace();
        }
    }

    private void closeConnection(){
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        jTextArea.append("You are disconnected!" + "\n");
    }

    private void sendMessage(String msg){
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void onAuthClick() {
        try {
            out.writeUTF("/auth " + login + " " + pass);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

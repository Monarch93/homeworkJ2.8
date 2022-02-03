package org.example.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static java.lang.Thread.sleep;

public class ClientHandler {
    private MyServer myServer;
    private DataInputStream in;
    private DataOutputStream out;
    private Socket socket;
    private volatile boolean close = false;

    private int connect_timeout_sec = 120;

    private String name;

    private volatile boolean isAuthentication = false;

    public String getName(){
        return name;
    }

    public ClientHandler(Socket socket, MyServer server) {
        try {
            this.myServer = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.name = "";
            new Thread(() -> {
                try {
                    authentication();
                    if (!close) {
                        readMessages();
                    }
                } catch (IOException e){
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException("ClientHandler is failed");
        }
    }

    public void authentication() throws IOException{
        while (true){
            String str = in.readUTF();
            if (str.startsWith("/auth")){
                String[] parts = str.split("\\s");
                String nick = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2]);
                if (nick != null) {
                    if (!myServer.isNickBusy(nick)) {
                        sendMsg("/authok " + nick);
                        name = nick;
                        myServer.broadcastMsg(name + " is chatting");
                        myServer.subscribe(this);
                        isAuthentication = true;
                        return;
                    } else {
                        sendMsg("Nick is busy");
                    }
                } else {
                    sendMsg("Login or password are wrong");
                }
            }
            try {
                for (int i = 0 ; i < (connect_timeout_sec*4) ; i++) {
                    sleep(250);
                    if (isAuthentication){
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!isAuthentication) {
                System.out.println("Client does not connected");
                close = true;
                return;
            }
        }
    }


    private void readMessages() throws IOException {
        while (true){
            String strFromClient = in.readUTF();
            System.out.println("from " + name + ": " + strFromClient);
            if (strFromClient.startsWith("/")){
                if (strFromClient.equalsIgnoreCase("/end")) break;
                if (strFromClient.startsWith("/w")){
                    String[] parts = strFromClient.split("\\s");
                    String nick = parts[1];
                    try {
                        String msg = strFromClient.substring(4 + nick.length());
                        myServer.unicastMsg(this, nick, name + ": " + msg);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                continue;
            }
            myServer.broadcastMsg(name + ": " + strFromClient);
        }
    }

    public void sendMsg(String message) {
        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection(){
        myServer.broadcastMsg(name + " is leaving chat");
        myServer.unsubscribe(this);
        try {
            in.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}

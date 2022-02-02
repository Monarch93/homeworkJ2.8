package org.example.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private MyServer myServer;
    private DataInputStream in;
    private DataOutputStream out;
    private Socket socket;

    private String name;

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
                    readMessages();
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
                        return;
                    } else {
                        sendMsg("Nick is busy");
                    }
                } else {
                    sendMsg("Login or password are wrong");
                }
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

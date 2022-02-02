package org.example.server;

import org.example.server.interfaces.AuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MyServer {
    public static final int PORT = 8888;

    private List<ClientHandler> clients = new ArrayList<>();
    private AuthService authService;

    public AuthService getAuthService(){
        return authService;
    }

    public MyServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            authService = new BaseAuthService();
            authService.start();
            clients = new ArrayList<>();
            while (true) {
                System.out.println("Server is waiting for a connection...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("We have a new client....");
                new ClientHandler(clientSocket, this);
            }
        } catch (IOException e) {
            System.out.println("Server is failed");
        } finally {
            if (authService != null){
                authService.stop();
            }
        }
    }

    public synchronized boolean isNickBusy(String nick){
        for (ClientHandler o : clients) {
            if (o.getName().equals(nick)){
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastMsg(String message){
        for (ClientHandler o : clients) {
            o.sendMsg(message);
        }
    }

    public synchronized void unicastMsg(ClientHandler from, String nick, String message){
        for (ClientHandler o : clients) {
            if (o.getName().equals(nick)) {
                o.sendMsg(message);
                return;
            }
        }
        from.sendMsg(nick + " is not in chat!");
    }

    public synchronized void broadcastClientsList() {
        StringBuilder sb = new StringBuilder("/clients ");
        for (ClientHandler o : clients) {
            sb.append(o.getName() + " ");
        }
        broadcastMsg(sb.toString());
    }


    public synchronized void unsubscribe(ClientHandler o) {
        clients.remove(o);
        broadcastClientsList();
    }

    public synchronized void subscribe(ClientHandler o) {
        clients.add(o);
        broadcastClientsList();
    }
}

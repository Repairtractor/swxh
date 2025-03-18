package com.example.swxh;


import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class WebSocketPushServer extends WebSocketServer {

    public WebSocketPushServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Message received: " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server started successfully");
    }

    public static void main(String[] args) {
        WebSocketPushServer server = new WebSocketPushServer(new InetSocketAddress(8080));
        server.start();

        // Example: Simulate a push after 5 seconds
        new Thread(() -> {
            try {
                Thread.sleep(5000);  // Simulate some event after 5 seconds
                while (true) {
                    for (WebSocket client : server.getConnections()) {
                        client.send("Hello client, this is the server push message!");
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}

package Server;

import Shared.StateManager;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.DatagramSocket;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 27/06/2009
 * Time: 9:33:56 AM
 */
public class ServerCommsListenerThread extends Thread {
    ServerSocket serverSocket = null;
    Server server;

    public ServerCommsListenerThread(ServerSocket socket, Server server) {

        super("ServerCommsListenerThread");
        this.serverSocket = socket;
        this.server = server;

    }

    public void run() {
        for (; ;) {
            try {
                Socket socket = serverSocket.accept();
                if(socket.getInetAddress().isLoopbackAddress()) {
                    StateManager.setInputSocket(socket); //todo: use addinput in server, this is temporary
                    System.out.println("Got connection from media player"); //todo: delete this
                } else {
                    server.addClient(socket);
                }
            } catch (IOException e) {

            }
        }
    }
}

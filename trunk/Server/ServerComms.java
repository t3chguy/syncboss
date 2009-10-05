package Server;

import Shared.UDPListener;

import java.net.*;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 26/06/2009
 * Time: 3:39:35 AM
 */
public class ServerComms {
    ServerSocket dataSocket = null;

    static int dataPort=51923;
    static int syncPort=51924;
    ServerCommsListenerThread listener;
    Server server;

    public Server getServer() {
        return server;
    }

    public static int getDataPort() {
        return dataPort;
    }

    public static void setDataPort(int dataPort) {
        ServerComms.dataPort = dataPort;
    }

    public static int getSyncPort() {
        return syncPort;
    }

    public static void setSyncPort(int syncPort) {
        ServerComms.syncPort = syncPort;
    }

    /*public int getSyncPort() {
        return syncPort;
    }

    public int getDataPort() {
        return dataPort;
    }

    public void setDataPort(int portNumber) {
        this.dataPort = portNumber;
        closeDataSocket(); // in case one is already open
        try {
            dataSocket = new ServerSocket(dataPort);
        } catch (IOException e) {
            System.err.printf("Could not listen on port: %d.\n",dataPort);
            System.exit(1);
        }
    }

    public void setSyncPort(int portNumber) {
        this.syncPort = portNumber;
        closeSyncSocket(); // in case one is already open
        try {
            syncSocket = new DatagramSocket(dataPort);
        } catch (IOException e) {
            System.err.printf("Could not listen on port: %d.\n",syncPort);
            System.exit(1);
        }
    }*/

    public ServerComms(Server server) {
        this.server = server;
        try {
            dataSocket = new ServerSocket(dataPort);
        } catch (IOException e) {
            e.printStackTrace(); 
        }
    }

    /*private void closeDataSocket() {
        if(dataSocket != null) {
            try {
                dataSocket.close();
            } catch (Exception e) {

            }
        }
    }*/

    public void startListening() {
        listener = new ServerCommsListenerThread(dataSocket, server);
        listener.start();
    }

    public void stopListening() {
        listener.stop();
        listener = null;
    }


}

/*
Copyright (c) 2010, Jack Langman
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the copyright holder nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package Server;

import Shared.UDPListener;

import java.net.*;
import java.io.*;

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

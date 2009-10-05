package Server;

import Shared.UDPListenerHelper;

import java.net.*;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 27/06/2009
 * Time: 9:00:32 AM
 */
public class Server {

    ArrayList<Client> clients;
    int nextClientID=-1;
    UDPListenerHelper udpListener;

  /*  public Client getClients(int ID) {
        return clients.
    }*/

    public int getNextClientID() {
        nextClientID++;
        return nextClientID;
    }

    public ArrayList<Client> getClients() {
        return clients;
    }

    public int getClientCount() {
        return clients.size();
    }

    public Server(UDPListenerHelper udpListener) {
        clients = new ArrayList<Client>();
        this.udpListener = udpListener;
        // sync thread
        Thread sync = new Thread() {
            public void run() {
                try {
                    for(;;) {
                        for(Client c : getClients()) {
                            c.doSync();
                        }
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        sync.start();
    }

    public void addClient(Socket socket) {

        clients.add(new Client(socket, getNextClientID(), this));
        
    }

    /*public void addInput(Socket socket) { //todo: implement this for plugin input

    }*/

}

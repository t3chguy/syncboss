package Shared;

import java.net.DatagramSocket;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 27/06/2009
 * Time: 11:47:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class UDPListenerHelper {
    DatagramSocket socket;
    HashSet<UDPHandler> handlers = new HashSet<UDPHandler>();
    UDPListener listener;

    public UDPListenerHelper(int port) {
        listener = new UDPListener(port, this);
        listener.start();
    }

    public int getPort() {
        return socket.getPort();
    }

    public void registerHandler(UDPHandler handler) {
        handlers.add(handler);
    }

    public void unregisterHandler(UDPHandler handler) {
        handlers.remove(handler);
    }

    public DatagramSocket getSocket() {
        return socket;
    }
}

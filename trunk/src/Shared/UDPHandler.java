package Shared;

import java.net.*;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 27/06/2009
 * Time: 8:44:34 PM
 */
public interface UDPHandler {
    public void handleDatagram(DatagramPacket packet);
    public InetAddress getInetAddress();
    public int getPort();
}

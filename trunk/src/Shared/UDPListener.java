package Shared;
import java.net.*;
import java.io.IOException;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 27/06/2009
 * Time: 11:16:29 AM
 */
public class UDPListener extends Thread {
    UDPListenerHelper helper;
    DatagramSocket socket;

    public UDPListener(int port, UDPListenerHelper helper) {
        super("UDPListener");       
        this.helper = helper;
        do{
            try {
                socket = new DatagramSocket(port);
            } catch (SocketException e) {
                port++;
                //e.printStackTrace();
                //isUnique = false;
            }
        } while((socket == null) || !socket.isBound());
        helper.socket = socket;
        System.out.printf("Listening on port %d.\n",port);
    }


    public void run() {
        for(;;) {
            try {
                byte[] buf = new byte[256];

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                //do: remvoe this debug code
                //System.out.printf("udp packet: %s\n", new String(packet.getData(), packet.getOffset(), packet.getLength()));

                // send it to registered clients
                for (UDPHandler handler : helper.handlers) {
                    if (handler.getInetAddress().equals(packet.getAddress()) && (handler.getPort() == packet.getPort() || handler.getPort() == -1)) {
                        handler.handleDatagram(packet);
                    }
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
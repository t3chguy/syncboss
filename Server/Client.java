package Server;

import Shared.UDPHandler;
import Shared.TCPListener;
import Shared.TCPHandler;
import Shared.StateManager;

import java.net.*;
import java.io.IOException;
import java.io.DataOutputStream;
import java.util.Calendar;

import MediaPlayer.AbstractMediaPlayer;
import MediaPlayer.MediaTransmitter;

import javax.sound.sampled.AudioFormat;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 27/06/2009
 * Time: 8:48:00 AM
 */
public class Client implements UDPHandler, AbstractMediaPlayer, TCPHandler {
    long offset; //how many seconds ahead or behind server system time should the client play it
    long polls = 0; //how many times has the offset been polled?
    int ID; //identifying number for this Client
    InetAddress networkAddress = null; //IP address or whatever
    Socket dataSocket = null;
    DataOutputStream outputDataStream;
    TCPListener tcp;
    Server server;
    int targetUDPPort = -1;
    String ipString;

    public InetAddress getInetAddress() {
        return networkAddress;
    }

    public int getPort() {
        return this.targetUDPPort;
    }

    public Client(Socket socket, int ID, Server server) {
        this.ID = ID;
        this.dataSocket = socket;
        this.networkAddress = socket.getInetAddress();
        System.out.printf("%d- %s connected.\n", ID, networkAddress.toString());
        ipString = networkAddress.getHostAddress();
        tcp = new TCPListener(this, socket);
        tcp.start();
        try {
            outputDataStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.server = server;
        server.udpListener.registerHandler(this);
        Thread t = new Thread() {
            public void run() {
                try {
                    System.out.println("Starting client transmitter thread");
                    sendBuffer();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        //transmit tester packet. Todo: remove
        /*byte msg[] = {-127, -100, 0, 100, 127, -126, -125, -124, -123 ,-122};
        try {
            this.sendDataMessage("t" + new String(msg));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        System.out.println("Sent tester packet. Contents:");
        for(int i=0;i<msg.length;i++) {
            System.out.println(msg[i]);
        }*/

        //transmit format if something is already playing
        if(StateManager.isPlaying()) {
            this.setFormat(StateManager.getAudioStream().getFormat());
            this.play();
        }
        t.start();        
    }

    public void linkFailed() {
        System.out.printf("%d- %s disconnected.\n", ID, networkAddress.toString());
        server.udpListener.unregisterHandler(this);
        server.clients.remove(this);
        try {
            tcp.getSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleDatagram(DatagramPacket packet) {
        byte[] buf = packet.getData();
//        String bufstrx = new String(buf, 0, packet.getLength());
//        System.out.println(bufstrx);
        if (buf[0] == 'p') {
            this.targetUDPPort = packet.getPort();
        }
        if (this.targetUDPPort != -1) {
            if (buf[0] == 'r') {
                String bufstr = new String(buf, packet.getOffset(), packet.getLength());
                String args[];
                args = bufstr.split("\n");
                long timeServer = Long.parseLong(args[0].substring(1));
                long timeClient = Long.parseLong(args[1]);
                Calendar cal = Calendar.getInstance();
                long currentTime = cal.getTimeInMillis();
                long delay = (currentTime - timeServer) / 2;

                long offset = timeClient + delay - timeServer;
                if (polls == 0) {
                    this.offset = offset;
                } else {
                    this.offset = (this.offset * polls + offset) / (polls + 1); //moving average
                }
                if (polls < 5) polls++;
                System.out.printf("%d- sync time: %dms.\n", ID, this.offset);
            }
        }
    }

    public void handleDataMessage(byte[] msg, int len, char ident) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void doSync() {
        if (this.targetUDPPort != -1) {
            String msg = "r";
            Calendar cal = Calendar.getInstance();
            long currentTime = cal.getTimeInMillis();
            msg = msg + currentTime + "\n";
            sendDatagram(msg);
        }
    }

    public void sendDatagram(String smsg) {
        byte[] msg = smsg.getBytes();
        DatagramPacket p = new DatagramPacket(msg, msg.length, networkAddress, targetUDPPort);
        try {
            DatagramSocket ds = server.udpListener.getSocket();
            ds.send(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    byte[] outBuf = new byte[MediaTransmitter.getPacketSize() * 16];
    long outBufWrite = 0;
    long outBufHead = 0;

    public void sendDataMessage(final byte[] msg) throws Exception {

        if ((outBufHead + msg.length - outBufWrite) > outBuf.length) {
            throw new Exception("Buffer overflow in Server/Client.java for client "+ipString+", aborting send\n");
        }
        for (int i = 0; i < msg.length; i++) {
            outBuf[(int) (outBufHead % outBuf.length)] = msg[i];
            outBufHead++;
        }

    }

    private void sendBuffer() throws InterruptedException {
        for (; ;) { //todo: check stop ??
            while (outBufHead > outBufWrite) {
                int writehead = (int) (outBufWrite % outBuf.length);
                int outlen = Math.min(Math.min((int) (outBufHead - outBufWrite), outBuf.length - writehead), 8192);
                //System.out.println("out from " + writehead + " to " + (writehead+outlen));
                try {

                    outputDataStream.write(outBuf, writehead, outlen);
                } catch (IOException e) {
                    e.printStackTrace();
                    return; //client gone, terminate
                }
                outBufWrite += outlen;
            }
            Thread.sleep(10); //todo: how much should this be?
        }
    }

    public void sendDataMessage(String msg) throws Exception {
        sendDataMessage(msg.getBytes());
    }

    public void play() {
        try { //todo: retry?
            sendDataMessage("p\n");
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public void stop() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void queue(byte[] data, long time) {
        try {
            String s = "d" + (time + offset) + "\n";
            byte[] sbytes = s.getBytes();
            byte[] send = new byte[sbytes.length + data.length];
            System.arraycopy(sbytes,0,send,0,sbytes.length);
            System.arraycopy(data,0,send,sbytes.length,data.length);
            sendDataMessage(send);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setFormat(AudioFormat format) {
        try { //todo: retry?
            sendDataMessage(getFormatString(format));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public void forceResync() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    //init char 'f'
    public String getFormatString(AudioFormat format) {
        String output;
        output = String.format("f%s^%d^%f^%d^%f^%d^%b\n",
                format.getEncoding().toString(),
                format.getChannels(),
                format.getFrameRate(),
                format.getFrameSize(),
                format.getSampleRate(),
                format.getSampleSizeInBits(),
                format.isBigEndian());
        return output;
    }
}

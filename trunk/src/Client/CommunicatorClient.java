package Client;

import Shared.*;

import java.net.*;
import java.io.*;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;

import Server.ServerComms;

import javax.swing.*;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat;

import MediaPlayer.AbstractMediaPlayer;
import MediaPlayer.MediaTransmitter;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 26/06/2009
 * Time: 3:39:44 AM
 */
public class CommunicatorClient implements UDPHandler, TCPHandler {
    Socket cSocket = null;
    PrintWriter out = null;
    BufferedReader in = null;
    UDPListenerHelper udpListener;
    TCPListener tcp;
    AbstractMediaPlayer player;

    public CommunicatorClient(int portn, UDPListenerHelper udpListener, String addr, AbstractMediaPlayer amp) throws IOException {
        this.player = amp;
        this.udpListener = udpListener;
        try {
            cSocket = new Socket(addr, portn);
            out = new PrintWriter(cSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(null,"Can't find that address.");
            throw e;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,"Can't connect to that address.");
            throw e;
        }

        tcp = new TCPListener(this, cSocket);
        tcp.start();

        Timer sch = new Timer("sendpacket");
        ScheduleDatagram dg = new ScheduleDatagram("p");
        sch.schedule(dg, 1000);
    }

    public void handleDataMessage(byte[] msg, int len, char ident) {
        if(ident == 'p') {
            player.play();
        } else if(ident == 'f') {
            String s = new String(msg, 0, len);
            decodeFormatString(s);
        } else if(ident == 'd') {
            int newlinePos=0;
            for (int i=0;i<len;i++) {
                if(msg[i] == '\n') { newlinePos = i; break; }
            }
            long time = new Long(new String(msg, 0, newlinePos));
            byte[] data = new byte[MediaTransmitter.getPacketSize()];
            System.arraycopy(msg, newlinePos + 1, data, 0, MediaTransmitter.getPacketSize());
            player.queue(data, time);
        }
    }

    public void linkFailed() {
        System.out.println("Fatal(ish) error: server dc'd"); //todo: fix? this
    }

    //reference:
    //    output = String.format("f%s^%d^%f^%d^%f^%d^%b\n",
    // 0              format.getEncoding().toString(),
    // 1              format.getChannels(),
    // 2              format.getFrameRate(),
    // 3              format.getFrameSize(),
    // 4              format.getSampleRate(),
    // 5              format.getSampleSizeInBits(),
    // 6              format.isBigEndian());
    private void decodeFormatString(String s) {
        //System.out.println(s);
        s = s.substring(0,s.length() - 1);
        String[] p = s.split("\\^"); //split, trimming off the \n char

        for(int i=0;i<p.length;i++) {
            System.out.println(i + " " + p[i]);
        }
        float sampleRate = new Float(p[4]);
        int sampleSizeInBits = new Integer(p[5]);
        int channels = new Integer(p[1]);
        boolean isBigEndian = new Boolean(p[6]);


        this.player.setFormat(new AudioFormat(sampleRate,sampleSizeInBits,channels, true, isBigEndian)); //todo: urgent fix 'signed', currently assuming true (just check if encoding is PCM_SIGNED or PCM_UNSIGNED) -- easy fix
    }

    private class ScheduleDatagram extends TimerTask {
        String message;
        public ScheduleDatagram(String msg) {
            this.message = msg; 
        }
        public void run() {
            sendDatagram(message);
        }
    }

    public void listenForSync() {
        udpListener.registerHandler(this);
    }

    public void handleDatagram(DatagramPacket packet) {
        byte[] buf = packet.getData();
        if(buf[0] == 'r') { //sync request
            String bufstr = new String(buf, packet.getOffset(), packet.getLength());
            Calendar cal = Calendar.getInstance();
            long currentTime = cal.getTimeInMillis();
            sendDatagram(bufstr + currentTime);
        }
    }

    public InetAddress getInetAddress() {
        return this.cSocket.getInetAddress();
    }

    public int getPort() {
        return udpListener.getPort();
    }

    public void sendDatagram(String smsg) {
        byte[] msg = smsg.getBytes();
        DatagramPacket sMsg = new DatagramPacket(msg, msg.length, cSocket.getInetAddress(), ServerComms.getSyncPort());
        try {
            DatagramSocket ds = udpListener.getSocket();
            ds.send(sMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMediaPlayer(AbstractMediaPlayer player) {
        this.player = player;    
    }
}

package Shared;

import Server.*;
import Client.*;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

import MediaPlayer.SimpleMediaPlayer;
import MediaPlayer.MediaTransmitter;
import MediaPlayer.AbstractMediaPlayer;
import GUI.BaseForm;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 28/06/2009
 * Time: 11:02:03 AM
 */
public class StateManager {
    static boolean isClient = false;
    static boolean isServer = false;
    static public Socket pluginSocket;
    static public AudioInputStream audioInputStream = null;
    static public BaseForm form;
    static public boolean isTransmit = false;
    static MediaTransmitter mt;
    static public AbstractMediaPlayer player;

    static public boolean isPlaying() {
        return audioInputStream != null;
    }

    static public void setForm(BaseForm tform) {
        form = tform;
    }

    static public void setInputSocket(Socket socket) {
        pluginSocket = socket;
        try {
            //AudioSystem.getAudioInputStream(new URL("file:///c:/one.wav")).getFormat()
            audioInputStream = new AudioInputStream(pluginSocket.getInputStream(), new AudioFormat((float)44100.0,16,2,true,false), Long.MAX_VALUE); //todo: fetch this otherways
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(!isTransmit) {
            startTransmit();
        } else {
            mt.setInputStream(audioInputStream);
        }
    }

    static public void startTransmit() {
        ListObject sl = (ListObject)form.getOutputDeviceSelect().getSelectedItem();
        SimpleMediaPlayer lp = new SimpleMediaPlayer((Mixer.Info)sl.getValue(),form.getSelfVolumeRegistry(),form.getSelfOffsetRegistry());

        player = lp;

        mt = new MediaTransmitter(audioInputStream, getServer(), lp);
        mt.sendFormat();
        mt.play();
        isTransmit = true;
    }

    static public AudioInputStream getAudioStream() {
        return audioInputStream;
    }

    public static ServerComms getServerComms() {
        return serverComms;
    }

    public static Server getServer() {
        return serverComms.getServer();
    }

    public static void setServerComms(ServerComms serverComms) {
        StateManager.serverComms = serverComms;
    }

    public static CommunicatorClient getClient() {
        return client;
    }

    public static void setClient(CommunicatorClient client) {
        StateManager.client = client;
    }

    static ServerComms serverComms;
    static UDPListenerHelper udpListener = null;
    static CommunicatorClient client;


    public static boolean isClient() {
        return isClient;
    }

    public static boolean isServer() {
        return isServer;
    }

    private static void setupUDPListener() {
        if (udpListener == null) {
            udpListener = new UDPListenerHelper(ServerComms.getSyncPort());
        }
    }

    public static ServerComms setServerMode() {
        setupUDPListener();
        if(!isServer) {
            // todo: close old stuff
            System.out.println("Server mode selected");
            serverComms = new ServerComms(new Server(udpListener));
            serverComms.startListening();
            isServer=true;
            isClient=false;
            return serverComms;
        }
        return null;
    }

    public static CommunicatorClient setClientMode(String addr, AbstractMediaPlayer mp) {
        setupUDPListener();
        if(!isClient) {
            player = mp;
            // todo: close old stuff
            System.out.println("Client mode selected");
            try {
                client = new CommunicatorClient(51923, udpListener, addr, mp);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            client.listenForSync();
            isServer=false;
            isClient=true;
            return client;
        }
        return null;
    }


}

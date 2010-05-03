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

package Shared;

import Server.*;
import Client.*;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.prefs.Preferences;

import MediaPlayer.SimpleMediaPlayer;
import MediaPlayer.MediaTransmitter;
import MediaPlayer.AbstractMediaPlayer;
import GUI.BaseForm;

public class StateManager {
    static public Preferences prefs = Preferences.userNodeForPackage(StateManager.class);
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

package MediaPlayer;

import javax.sound.sampled.*;
import java.net.Socket;
import java.net.URL;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.io.IOException;

import Server.Server;
import Server.Client;
import Shared.StateManager;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 29/06/2009
 * Time: 1:16:28 PM
 */
public class MediaTransmitter {


    AudioInputStream input;
    Server server;
    AbstractMediaPlayer localPlayer;
    final static int packetSize = 16384; //todo: globalize this
    int packetTimeMilliseconds; //how long spent to play a packet?
    double packetTimeMillisecondsDouble; // more accurate
    final static int buffAhead = 1000; //how many milliseconds to buffer ahead? note.. changing this requires changing values in plugins

    public void setInputStream(AudioInputStream input) {
        this.input = input;
    }

    public MediaTransmitter(AudioInputStream input, Server server, AbstractMediaPlayer localPlayer) {
        this.input = input;
        this.localPlayer = localPlayer;
        this.server = server;
        packetTimeMilliseconds = (int) (((packetSize / input.getFormat().getFrameSize())) / input.getFormat().getFrameRate() * 1000.0);
        packetTimeMillisecondsDouble = ((double) packetSize / (double) input.getFormat().getFrameSize()) / (double) input.getFormat().getFrameRate() * 1000.0;
        System.out.println("Starting MediaTransmitter with fragment length of " + packetTimeMilliseconds + "ms.");
    }

    public static int getPacketSize() {
        return packetSize;
    }

    public void sendFormat() {
        localPlayer.setFormat(input.getFormat());

        for (Client c : server.getClients()) {
            c.setFormat(input.getFormat());
        }
    }

    byte[] inBuf = new byte[packetSize*4]; //input buffer
    long inBufHead=0;
    long inBufRead=0;
    private void buffer() {
        int bytesread = 0;
        byte[] buf=new byte[1024];
        for(;;) { //TODO: Check if stopped
            try {
                if((inBufHead - inBufRead) < (inBuf.length - buf.length) ) { // is there space for another KB?
                    /*this.input = new AudioInputStream(StateManager.pluginSocket.getInputStream(),AudioSystem.getAudioInputStream(new URL("file:///C:/one.wav")).getFormat(), Long.MAX_VALUE); *///todo: OO this code, handle winamp reconnects
                    bytesread = input.read(buf, 0, buf.length);
                    for(int i=0;i<bytesread;i++) {
                        inBuf[(int)(inBufHead%inBuf.length)] = buf[i];
                        inBufHead++;
                    }
                }
            } catch (Exception e) { //probably the input failed, wait for it to come back
                System.out.printf("Input stream seems to have failed. Retrying.\n");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    long startTime = -1;
    long fragmentsSent = 0;
    private void sendData() {
        if (startTime == -1) startTime = curTime();
        byte[] buf = new byte[packetSize]; //packet out
      
        if (inBufHead - inBufRead >= packetSize) { //check there's enough info in the buffer

            long checksum = 0;
            // First, read some bytes from the buffer.
            for(int i=0;i<packetSize;i++) {
                buf[i] = inBuf[(int)(inBufRead % inBuf.length)];
                checksum += buf[i];
                inBufRead++;
            }

            if(fragmentsSent%100==0) { //debugger message todo: remove
                System.out.printf("Debug message: send data message %d, checksum: %d\n",fragmentsSent,checksum);
            }

            // If there were no more bytes to read, we're done.
            //send local
            localPlayer.queue(buf, startTime + (long) (packetTimeMillisecondsDouble * (double) fragmentsSent) + buffAhead);
            //send remote
            for (Client c : server.getClients()) {
                c.queue(buf, startTime + (long) (packetTimeMillisecondsDouble * (double) fragmentsSent) + buffAhead);
            }
        } else {
            System.out.println("MediaTransmitter is ... STARVING!!");
        }

        fragmentsSent++; //regardless of whether we sent data, increment the fragment count so
                         //packets aren't queued behind time

        Timer sch = new Timer("datascheduler");
        ScheduleData sd = new ScheduleData();
        sch.schedule(sd, new Date(startTime + (int) (packetTimeMillisecondsDouble * (double) fragmentsSent)));
    }

    public class ScheduleData extends TimerTask {
        public void run() {
            sendData();
        }
    }

    public long curTime() {
        Calendar cal = Calendar.getInstance();
        return cal.getTimeInMillis();
    }

    public void play() {
        Thread t2 = new Thread() {
            public void run() {
                try {
                    buffer();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        };
        t2.start();
        try { //wait for buffer to load
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread t = new Thread() {
            public void run() {
                try {
                    sendData();
                    Thread.sleep(buffAhead / 2);
                    localPlayer.play();
                    //send remote
                    for (Client c : server.getClients()) {
                        c.play();
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        };
        t.start();
    }

}

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

package MediaPlayer;

import javax.sound.sampled.*;
import java.net.Socket;
import java.net.URL;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.io.IOException;
import java.io.InputStream;

import Server.Server;
import Server.Client;
import Shared.StateManager;

public class MediaTransmitter {


    InputStream input;
    AudioFormat inputFormat;
    Server server;
    AbstractMediaPlayer localPlayer;
    final static int packetSize = 16384; //todo: globalize this
    int packetTimeMilliseconds; //how long spent to play a packet?
    double packetTimeMillisecondsDouble; // more accurate
    final static int buffAhead = 1000; //how many milliseconds to buffer ahead? note.. changing this requires changing values in plugins

    public void setInputStream(InputStream input) {
        this.input = input;
    }

    public MediaTransmitter(InputStream input, Server server, AbstractMediaPlayer localPlayer) {
        this.input = input;
        this.localPlayer = localPlayer;
        this.server = server;
        System.out.println("Starting MediaTransmitter");// with fragment length of " + packetTimeMilliseconds + "ms.");
    }

    public static int getPacketSize() {
        return packetSize;
    }

    void setFormat(AudioFormat f) {
        inputFormat = f;
        packetTimeMilliseconds = (int) (((packetSize / f.getFrameSize())) / f.getFrameRate() * 1000.0);
        packetTimeMillisecondsDouble = ((double) packetSize / (double) f.getFrameSize()) / (double) f.getFrameRate() * 1000.0;
        sendFormat();
    }

    public AudioFormat getFormat() {
        return inputFormat;
    }

    public void sendFormat() {
        localPlayer.setFormat(inputFormat);

        for (Client c : server.getClients()) {
            c.setFormat(inputFormat);
        }
    }

    byte[] inBuf = new byte[packetSize*4]; //input buffer
    long inBufHead=0;
    long inBufRead=0;
    private void buffer() {
        int megadebugcount = 0;
        System.out.println("Input buffer init.");
        byte[] buf=new byte[1024];
        for(;;) { //TODO: Check if stopped
            try {
                int headerbyte = input.read();
                int data;
                //System.out.print(in+"."); //todo: remove debug
                if(headerbyte==-1) {
                    throw new Exception("Stream dead?");
                }
                if(headerbyte==1) { //0 implies no new format info, 1 implies new format info
                    //flush buffer

                    int[] header = new int[32];
                    for(int i=1;i<32;i++) { //total header size is 32, already read 1 byte, leave the first byte blank so i can read it easy
                        data=input.read(); //todo: remove debug
                        //System.out.print(in+".");
                        header[i] = data;
                    }
                    int sr; //samplerate
                    sr = (header[1] << 24) | (header[2] << 16) | (header[3] << 8) | (header[4]);
                    int numchans = header[5];
                    int bps = header[6];

                    //debug code
                    /*for(int i=0;i<32;i++) {
                        System.out.printf("%d.",header[i]);
                    } */


                    System.out.printf("sr: %d, ch: %d, bps: %d\n", sr, numchans, bps);

                    AudioFormat newFormat = new AudioFormat((float)sr,bps,numchans,true,false);

                    if(getFormat()==null || !newFormat.matches(getFormat())) {
                        inBufRead=inBufHead=0;
                        startTime = -1;
                        fragmentsSent = 0;
                        setFormat(newFormat);
                    }



                }
                int it = 0;
                megadebugcount++;
                while(it < INCOMING_PACKET_SIZE) {
                    if((inBufHead - inBufRead) < inBuf.length) {
                        byte d[] = new byte[1];
                        int incount = input.read(d,0,1);
                        if(incount > 0) {
                            if(it==0 || it==INCOMING_PACKET_SIZE-1) System.out.println(megadebugcount + "-" + it + ":" + (int)d[0]); //debug
                            //System.out.print((char)in);
                            //System.out.print(data);
                            inBuf[(int)(inBufHead%inBuf.length)] = d[0];
                            inBufHead++;
                            it++;
                        }
                    } else {
                        Thread.sleep(1);
                    }
                }
            } catch (Exception e) { //probably the input failed, wait for it to come back
                e.printStackTrace();
                System.out.printf("Input stream seems to have failed. Retrying.\n");                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }            
        }
    }

    long startTime = -1;
    long fragmentsSent = 0;
    final static int INCOMING_PACKET_SIZE = 1024; //needs to match winamp code
    boolean doPrintError = true;
    private void sendData() {
        if (startTime == -1) startTime = curTime();
        byte[] buf = new byte[packetSize]; //packet out

        if (inBufHead - inBufRead >= packetSize && getFormat() != null) { //check there's enough info in the buffer

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
            doPrintError = true;
        } else {
            if(doPrintError) {
                System.out.println("MediaTransmitter is ... STARVING!!");
                doPrintError = false;
            }

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
                    //Thread.sleep(buffAhead / 2);
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

package Shared;

import Server.Client;

import java.net.Socket;
import java.io.*;

import MediaPlayer.MediaTransmitter;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 27/06/2009
 * Time: 11:16:29 AM
 */
public class TCPListener extends Thread {
    Socket socket;
    TCPHandler handler;

    public TCPListener(TCPHandler handler, Socket socket) {
        super("TCPListener");
        this.socket = socket;
        this.handler = handler;
    }

    public Socket getSocket() {
        return this.socket;
    }

    long dcount = 0;

    public void run() {

        try {
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());


            byte buf[] = new byte[MediaTransmitter.getPacketSize() + 128];
            int ident;
            int byt;
            int len = 0;
            while ((ident = in.read()) != -1) {
                len = 0;
                if (ident == 'd') {
                    while ((byt = in.read()) != '\n') {
                        buf[len] = (byte) byt;
                        len++;
                    }
                    buf[len] = (byte) byt; //write the newline char in //todo fix this mess
                    len++;
                    //System.out.println(new String(buf,0, len));//todo: remove debug str
                    /*int x = 0;
                    do {
                        x = x + in.read(buf, len+x, MediaTransmitter.getPacketSize()-x);
                        if (x < MediaTransmitter.getPacketSize()) { //todo: make this code more efficient
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } while (x < MediaTransmitter.getPacketSize());
                    len=len+x;*/
                    for(int i=0;i<MediaTransmitter.getPacketSize();i++) {
                        int x = 0;
                        do {
                            x = in.read();
                            if (x == -1) { //todo: make this code more efficient
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            //System.out.print(x);
                        } while(x==-1);
                        buf[len] = (byte)(x);
                        len++;
                    }
                    //checksum.. todo: remove
                    int checksum = 0;

                    for(int i=0;i<MediaTransmitter.getPacketSize();i++) {
                        checksum += buf[len-MediaTransmitter.getPacketSize()+i];
                    }
                    /*if (dcount % 100 == 0) { //debugger message todo: remove
                        System.out.printf("Debug message: received data message %d, checksum: %d\n", dcount, checksum);
                    }*/
                    dcount++;
                    //end checksum
                    //todo: remove debug code
                } else if (ident == 'p') {
                    System.out.println("Got 'play' signal from server");
                    // discard newline char
                    in.read();
                } else if (ident == 'f') {
                    System.out.println("Got 'format' signal from server");
                    while ((byt = in.read()) != '\n') {
                        buf[len] = (byte) byt;
                        len++;
                    }
                    buf[len] = (byte) byt; //write the newline char in //todo fix this mess
                    len++;
                } else if(ident == 't') { //test packet, todo: remove
                    System.out.println("Tester packet received");
                    for(int i=0;i<10;i++) {
                        int x = 0;
                        do {
                            x = in.read();
                            if (x == -1) { //todo: make this code more efficient
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } while(x==-1);
                        System.out.println((byte)(x));
                    }
                } else {
                    System.out.println("Malformed input from server! Byte: " + ((char) ident));
                }
                handler.handleDataMessage(buf, len, (char) ident);
            }
        } catch (IOException e) {
            handler.linkFailed();
        }
    }

}

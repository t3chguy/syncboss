package MediaPlayer;

import GUI.VolumeRegistry;

import javax.sound.sampled.*;
import java.net.*;
import java.io.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 28/06/2009
 * Time: 3:26:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class tester {
    AudioInputStream audioInputStream = null;  // We read audio data from here
    SourceDataLine sourceDataLine = null;   // And write it here.
    boolean stop = false;
    FloatControl volCtrl = null;

    public void start(final Object mixerInfo, final VolumeRegistry volumeRegistry) {
        Thread t = new Thread() {
            public void run() {
                try {
                    streamSampledAudio(new URL("file:///C:/one.wav"), mixerInfo, volumeRegistry);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        };
        t.start();
        System.out.println("started");
    }

    public void stop() {
        stop = true;
        sourceDataLine.stop();
    }

    public void destroy() {
        stop = true;
        sourceDataLine.stop();
    }

    public static void getMix() {
        Mixer.Info[] mi = AudioSystem.getMixerInfo();
        for(Mixer.Info m : mi) {
            System.out.printf("%s\n",m.getName());         
        }

    }

    /**
     * Read sampled audio data from the specified URL and play it
     */
    public void streamSampledAudio(URL url, Object mixerInfo, VolumeRegistry volumeRegistry) throws IOException, UnsupportedAudioFileException, LineUnavailableException {

        try {
            // Get an audio input stream from the URL
            System.out.println("Getting input");

            System.out.println(AudioSystem.getAudioFileFormat(url));
            audioInputStream = AudioSystem.getAudioInputStream(url);

            // Get information about the format of the stream
            System.out.println("Getting format");
            AudioFormat format = audioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            // use correct mixer
            Mixer mixer = AudioSystem.getMixer((Mixer.Info)mixerInfo);

            // Open the line through which we'll play the streaming audio.
            sourceDataLine = (SourceDataLine) mixer.getLine(info);
            sourceDataLine.open(format);

            //volume control
            Control[] lineControls = sourceDataLine.getControls();
            for(Control ctrl : lineControls) {
                if (ctrl.getType().toString().equals("Master Gain")) {
                    volCtrl = (FloatControl)ctrl;
                    break;
                }
            }
            //register line volume control with form
            volumeRegistry.registerControl(volCtrl);
            volumeRegistry.updateVolumeControls();
            

            // Allocate a buffer for reading from the input stream and writing
            // to the line.  Make it large enough to hold 4k audio frames.
            // Note that the SourceDataLine also has its own internal buffer.
            int framesize = format.getFrameSize();
            byte[] buffer = new byte[4 * 1024 * framesize]; // the buffer
            int numbytes = 0;                               // how many bytes

            // We haven't started the line yet.
            boolean started = false;

            while (!stop) {  // We'll exit the loop when we reach the end of stream
                // First, read some bytes from the input stream.
                int bytesread = audioInputStream.read(buffer, numbytes, buffer.length-numbytes); //-numbytes
                // If there were no more bytes to read, we're done.
                if (bytesread == -1) break;
                numbytes += bytesread;

                // Now that we've got some audio data to write to the line,
                // start the line, so it will play that data as we write it.
                if (!started) {
                    sourceDataLine.start();
                    started = true;
                }

                // We must write bytes to the line in an integer multiple of
                // the framesize.  So figure out how many bytes we'll write.
                int bytestowrite = (numbytes / framesize) * framesize;

                // Now write the bytes. The line will buffer them and play
                // them. This call will block until all bytes are written.
                sourceDataLine.write(buffer, 0, bytestowrite);

                // If we didn't have an integer multiple of the frame size,
                // then copy the remaining bytes to the start of the buffer.
                int remaining = numbytes - bytestowrite;
                if (remaining > 0)
                    System.arraycopy(buffer, bytestowrite, buffer, 0, remaining);
                numbytes = remaining;
            }
            if (!stop) {
                // Now block until all buffered sound finishes playing.
                sourceDataLine.drain();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally { // Always relinquish the resources we use
            if (sourceDataLine != null) sourceDataLine.close();
            if (audioInputStream != null) audioInputStream.close();
            if (volCtrl != null) volumeRegistry.unregisterControl(volCtrl);
        }
    }
}


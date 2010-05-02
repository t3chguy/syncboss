package MediaPlayer;

import GUI.VolumeRegistry;
import GUI.OffsetRegistry;

import javax.sound.sampled.*;
import java.util.Calendar;
import java.util.prefs.Preferences;

import Shared.StateManager;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 29/06/2009
 * Time: 3:06:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleMediaPlayer implements AbstractMediaPlayer {
    AudioFormat format = null;
    SourceDataLine line = null;
    boolean stop = true;
    FloatControl volCtrl = null;
    OffsetObject playerOffset = new OffsetObject(0);
    Mixer.Info mixerInfo = null;
    VolumeRegistry volumeRegistry = null;
    OffsetRegistry offsetRegistry = null;
    //FloatControl sampleRate;
    //data storage
    static final int BUFSIZE = 1024;
    byte[][] soundFragments = new byte[BUFSIZE][MediaTransmitter.getPacketSize()]; //10meg odd data buffer
    long[] fragmentTimes = new long[BUFSIZE];//when to play the above fragments
    long[] fragmentIterator = new long[BUFSIZE];//what order did this packet arrive in
    long head = 0;
    long read = 0;
    byte[] silent = new byte[MediaTransmitter.getPacketSize()];
    Preferences prefs = Preferences.userNodeForPackage(getClass());

    public SimpleMediaPlayer(Mixer.Info mixerInfo, VolumeRegistry volumeRegistry, OffsetRegistry offsetRegistry) {
        this.mixerInfo = mixerInfo;
        this.volumeRegistry = volumeRegistry;
        this.offsetRegistry = offsetRegistry;
        for (int i = 0; i < MediaTransmitter.getPacketSize(); i++) { // full on silence
            silent[i] = 0;
        }


        seekOffset = prefs.getDouble("seek_offset", 0.0);
        System.out.printf("Loaded seek_offset: %f\n", seekOffset);
        seekOffsetCount = prefs.getLong("seek_count", 0);
        System.out.printf("Loaded seek_count: %d\n", seekOffsetCount);
        driftMultiplier = prefs.getDouble("respeed_coefficient", 1.0);
        System.out.printf("Loaded respeed_coefficient: %f\n", driftMultiplier);

    }

    public void play() {
        if (stop) {
            stop = false;
            Thread t = new Thread() {
                public void run() {
                    try {
                        buffer();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            t.start();

        }
    }

    public void stop() {
        stop = true;
        line.stop();
    }

    public void queue(byte[] data, long time) {
        soundFragments[(int) (head % BUFSIZE)] = data;
        fragmentTimes[(int) (head % BUFSIZE)] = time;
        head++;
        if ((int) (head % BUFSIZE) == (int) (read % BUFSIZE)) {
            System.out.println("Erorr: Buffer overflow");
            System.exit(1);
        }

    }

    public void setFormat(AudioFormat format) {
        this.format = format;

        // Get information about the format of the stream
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        // use correct mixer
        Mixer mixer = AudioSystem.getMixer((Mixer.Info) mixerInfo);

        // Open the line through which we'll play the streaming audio.
        try {
            line = (SourceDataLine) mixer.getLine(info);
            line.open(format);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return;
        }

        //sampleRate = (FloatControl)line.getControl(FloatControl.Type.SAMPLE_RATE);

        //volume control
        Control[] lineControls = line.getControls();
        for (Control ctrl : lineControls) {
            if (ctrl.getType().toString().equals("Master Gain")) {
                volCtrl = (FloatControl) ctrl;
                break;
            }
        }
        //register line volume control with form
        volumeRegistry.registerControl(volCtrl);
        volumeRegistry.updateVolumeControls();
        oldVol = volCtrl.getValue();

        //register sound offset with form
        offsetRegistry.registerOffsetObject(playerOffset);
        offsetRegistry.updateOffsets();
    }

    boolean forceResync = false;
    public void forceResync() {
        this.forceResync = true;
        seekOffset = 0.0;
        seekOffsetCount = 0;
        driftMultiplier = 1.0;
        prefs.putLong("seek_count", seekOffsetCount);
        prefs.putDouble("respeed_coefficient", driftMultiplier);
        prefs.putDouble("seek_offset", seekOffset);

        System.out.println("Reset sync vars.");                
    }

    /*
    public void doPlay() {
        byte[] spill = new byte[64];
        int spillLength = 0;
        double factor = 1;
        double oldFactor = 1;
        while (!stop) {  // We'll exit the loop when we reach the end of stream

            while (head == read) { //wait till there's some data in the buffer (buffer underrun)
                try {
                    System.out.println("Buffer underrun");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            int size = MediaTransmitter.getPacketSize();

            // Now that we've got some audio data to write to the line,
            // start the line, so it will play that data as we write it.
            long time = curTime();
            if (!started) {
                line.start();
                this.startTime = curTime();
                started = true;
                System.out.println(startTime);
            }

            // what time was this fragment meant to play?
            long projectedTime = 0;
            for (int i = 0; i < BUFSIZE; i++) {// todo: make this loop walk the buffer more efficiently
                if (fragmentIterator[i] == fragmentsPlayed) {
                    projectedTime = fragmentTimes[i];
                    break;
                }
            }
            //seconds ahead/behind
            long dif = (projectedTime) - (time + playerOffset.getOffset());

            //debug todo:delete this debug code
            System.out.printf("dif: %d\n", dif);

            //how much to slow/speed. Dupe/delte a frame every x frames


            double magicNumber = 30;
            if (dif < -magicNumber * 4) {
                line.flush();
            } else if (dif > magicNumber * 4) {
                try {
                    Thread.sleep(dif);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (dif < -magicNumber / 2) {
                factor = 1.000; //fast
            } else if (dif > magicNumber / 2) {
                factor = 1.000; //slow
            }

            System.out.printf("factor %f", factor);
            //now, speedup/slowdown the music:
            byte[] buf = new byte[MediaTransmitter.getPacketSize() * 5];
            if (spillLength > 0) {
                System.arraycopy(spill, 0, buf, 0, spillLength);
            }
            int spliceSize = this.format.getFrameSize();
            if (factor != 1) {
                double invFactor = 1 / factor;
                size = (int) (invFactor * size);
                for (int i = 0; i < size / spliceSize; i++) {
                    //buf[i] = soundFragments[read][(int)(invFactor * i)];
                    System.arraycopy(soundFragments[read], (int) (factor * i) * spliceSize, buf, spillLength + i * spliceSize, spliceSize);
                }
            } else {
                System.arraycopy(soundFragments[read], 0, buf, 0+spillLength, size);
            }


            // Now write the bytes. The line will buffer them and play
            // them. This call will block until all bytes are written.
            line.write(buf, 0, (size / spliceSize) * spliceSize);
            spillLength = size % spliceSize;
            if (spillLength > 0) {
                System.arraycopy(buf, (size / spliceSize) * spliceSize, spill, 0, spillLength);
                spillLength = spillLength;
            }

            oldFactor = factor;

            fragmentsPlayed++;
            if (read < BUFSIZE - 1) read++;
            else read = 0;
        }
    }*/

    double effectiveDriftMultiplier=-1; //consider the limited resolution given number of frames, can only remove/add 1 at a time
    double historicalDriftMultiplier=-1;
    long respeedCount=0;
    public byte[] respeed(byte[] buf, int off, int len) {
        if(effectiveDriftMultiplier == -1) {
            effectiveDriftMultiplier = driftMultiplier;
        }
        byte[] outbuf;
        double invFactor = 2 - effectiveDriftMultiplier; //high multplier means we want less frames, and vica versa
        int size = (int) (invFactor * len);
        outbuf = new byte[size];

        for (int i = 0; i < size / format.getFrameSize(); i++) {
            System.arraycopy(buf, (int) (effectiveDriftMultiplier * i) * format.getFrameSize(), outbuf, i * format.getFrameSize(), format.getFrameSize());
        }

        double minunit = 1.0 / (((double)MediaTransmitter.getPacketSize() / (double)format.getFrameSize())); //todo: dont execute this each time lol       
        double actualDriftMultiplier = 2.0-(double)((double)size / (double)len);

        historicalDriftMultiplier = ((historicalDriftMultiplier * respeedCount) + (actualDriftMultiplier)) / (respeedCount+1);

        if(historicalDriftMultiplier < driftMultiplier) {
            effectiveDriftMultiplier = effectiveDriftMultiplier + minunit;
        } else if (historicalDriftMultiplier > driftMultiplier) {
            effectiveDriftMultiplier = effectiveDriftMultiplier - minunit;
        }

        if (respeedCount % 100 == 0) {
            System.out.printf("Respeed info: target: %f, historical: %f, effective: %f, actual(current): %f, min unit: %f\n", driftMultiplier, historicalDriftMultiplier, effectiveDriftMultiplier, actualDriftMultiplier,minunit);
        }

        respeedCount++;
        return outbuf;
    }

    /**
     * Method handles buffer
     */
    boolean doBuffer=false;
    public void buffer() {
        //line.start();
        int size = MediaTransmitter.getPacketSize();
        int frameSize = format.getFrameSize();
        byte[] buf = new byte[MediaTransmitter.getPacketSize()+32]; //temp buffer to manipulate data
        byte[] spill = new byte[64]; //handle un-even frame amounts when writing to the data line
        int spillLength = 0;

        //start line sync thread
        //todo: move this to a more sensible location
        Thread t = new Thread() {
            public void run() {
                try {
                    syncLine();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();

        for(;;) { //loop until stopped

            while (head == read) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                correctDrift = false; // buffer underrun, dont make bad drift corrections
            } //wait till there's some data in the buffer (buffer underrun)
            while (!doBuffer) {
                try {
                    Thread.sleep(10);//wait till started again
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //if leftover from last loop, try add it to current data
            if (spillLength > 0) {
                System.arraycopy(spill, 0, buf, 0, spillLength);
            }

            /*if (fragmentTimes[(int) (read % BUFSIZE)] - curTime() < 1) { //if timecode is very close or gone, skip
                read++;
                continue;
            } else if (fragmentTimes[(int) (read % BUFSIZE)] - curTime() > 50) { //wait before buffering sound that's off in the future
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }*/

            System.arraycopy(soundFragments[(int) (read % BUFSIZE)], 0, buf, 0 + spillLength, size);

            byte[] resizedBuf = respeed(buf, 0, size);

            // Now write the bytes. The line will buffer them and play
            // them. This call will block until all bytes are written.
            int evenFrames = (resizedBuf.length / frameSize) * frameSize;

            //prebuffer code
            while(line.available() < evenFrames) {
                if(!doBuffer) break;
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            line.write(resizedBuf, 0, evenFrames);
            spillLength = size % frameSize;
            if (spillLength > 0) {
                System.arraycopy(resizedBuf, evenFrames, spill, 0, spillLength);
            }

            // Increment buffer reading head
            read++;

        }
    }

    double avgDif;
    long avgDifCount;
    boolean correctDrift = true;
    public void syncLine() {
        //some synchronisation vars:
        double millisecondsPerFrame = 1.0 / (double) format.getFrameRate() * 1000.0;
        //long startTime = curTime();
        boolean playing = true;

        try {
            seekFragment(0); //play the first fragment when it's due
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long lastPrintedFragmentPos = 0;

       while (!stop) {
           if(!seeking) {
                //check our position
                int size = getFragmentSize();
                int framesPerFragment = size / format.getFrameSize();
                long thisFramePos = getNewFramesPlayed(); //how many frames processed?
                long thisFragmentPos = thisFramePos / framesPerFragment + getStartFragment(); //how many whole fragments processed?
                long thisCurrentFragmentAmount = thisFramePos % framesPerFragment;//how many frames processed in the current fragment?
                long thisStartedFragmentAt = curTime() + playerOffset.getOffset() - (long) ((double) thisCurrentFragmentAmount * millisecondsPerFrame); //guess the time when this player really started playing thisFragmentPos

                //check master
                long masterStartedFragmentAt = fragmentTimes[(int) (thisFragmentPos % BUFSIZE)];

                //how far ahead/behind. -ve is behind master, +ve is ahead.
                long dif = masterStartedFragmentAt - thisStartedFragmentAt;

                //todo: remove debug cod ehere
                //System.out.printf("d%d,p%d|", dif, thisFragmentPos);

                //ignore weird soundcard output
                if((justSeeked && (dif<-100 || dif>100)) || (dif<-100000 || dif > 100000)) {
                    System.out.println("Seek time was unusual... " + dif + "ms");
                } else {
                   //difference moving average
                   avgDif = (avgDif*(double)avgDifCount + (double)dif)/(double)(avgDifCount + 1.0);
                   if(avgDifCount < 200) {
                       avgDifCount++;
                   }
                }
                justSeeked = false;

               //debug message todo: remove (if you want, it's pretty cool)
               if((thisFragmentPos % 50)==0 && thisFragmentPos > 0 && thisFragmentPos != lastPrintedFragmentPos) {
                   lastPrintedFragmentPos = thisFragmentPos;
                   System.out.printf("Sync: %+.3fms offset from the server.",avgDif);
                   if(lastSeekTime != 0) {
                       System.out.printf(" Last seek was %.1fs ago",((double)curTime() - (double)lastSeekTime)/1000.0);
                   }
                   System.out.printf("\n");
                   StateManager.form.getSyncMonitor().setValue((int)avgDif);
               }

                //once sync has been polled 50 times exactly, update sync offset
                //this measures how good the seek was
                if(avgDifCount == 100 /* && thisFragmentPos > 50*/ && correctDrift) { //adjust seekOffset, average over all seeks... TODO: save this PER DEVICE, per computer ><
                    seekOffset = ((seekOffset*(double)seekOffsetCount) - (avgDif-seekOffset))/((double)seekOffsetCount+1.0);
                    seekOffsetCount++;
                    lastSeekOffset = avgDif;
                    prefs.putDouble("seek_offset", seekOffset);
                    prefs.putLong("seek_count", Math.min(seekOffsetCount, 50));
                    System.out.println("Seek was out by "+avgDif+" ms.. seek offset adjusted to: " + seekOffset);
                }

                //do seek if req'd
                if ((avgDif < -20 || avgDif > 20 || forceResync) && avgDifCount >= 50 && curTime() - lastSeekTime > 5000) {
                    forceResync = false;
                    lastDriftOffset = avgDif;
                    lastDriftTime = curTime();
                    //pick a fragment (1)? second in advance of now
                    for(long i=read;i<head;i++) {
                        if(fragmentTimes[(int)(i % BUFSIZE)] > curTime() + playerOffset.getOffset() + 100 + (int)seekOffset) {
                            try {
                                seekFragment((int) (i));
                                break;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    boolean seeking = false;
    boolean justSeeked = false;
    double seekOffset = 0; //how long does it take for line.start() to take effect when buffer is loaded
    long seekOffsetCount = 0;
    long lastSeekTime=0; //when was the last seek done
    long lastDriftTime=0; //when did it last drift
    double lastSeekOffset=0; // what was the offset last time it drifted
    double lastDriftOffset=0; // what was the offset last time it drifted out of sync
    double driftMultiplier=1.0; // speed up or slow down the track
    /** seek fragment x, blocks until seek is complete
     *
     * @param fragment what fragment to seek
     * @throws InterruptedException whaaat
     */
    void seekFragment(int fragment) throws InterruptedException {
        seeking = true;
        System.out.println("Seek fragment " + fragment);

        //reset difference moving averages
        avgDifCount = 0;
        avgDif = 0;

        if(fragment > head) {
            System.out.println("seekFragment error: don't have requested fragment yet");
            return;
        }

        doBuffer = false;
        if(line.isRunning()) {
            //fadeOut(100);
            //Thread.sleep(100);
            line.stop();
            Thread.sleep(10); //let buffer fizzle
            line.flush();
        }

        adjustSpeed();

        setNewStartPoint(fragment);
        read=fragment;
        doBuffer = true;

        while((double)curTime() + (double)playerOffset.getOffset() + seekOffset < (double)fragmentTimes[fragment%BUFSIZE]) {//wait till it's time to play it
            Thread.sleep(1);
        }
        //volCtrl.setValue(volCtrl.getMinimum());
        lastSeekTime = curTime();
        line.start();
        stop=false;
        //fadeIn(100);
        seeking = false;
        justSeeked = true;
        correctDrift = true;
    }
    
    void adjustSpeed() {
        if (seekOffsetCount > 0 && lastSeekTime != 0 && lastDriftTime != 0 && correctDrift && startFragment != 0) { //todo: are all these checks needed? think abt your code
            /*double driftAmount = lastDriftOffset - lastSeekOffset; //how much did it actually drift
            long driftTime = lastDriftTime - lastSeekTime;
            driftAmount -= driftTime * (driftMultiplier - 1); //virtual drift (if the existing multiplier wasn't there)
            double mult = ( driftAmount / (double)driftTime )*-1.0 + 1.0;*/

            //determine speedup/slowdown
            //g(t) = f(t)*(2-oldMultiplier) is total playback frames amount
            //driftTime = t2 - t1 : lastDriftTime - lastSeekTime
            //driftAmount = f(lastDriftTime
            long t2 = lastDriftTime;
            long t1 = lastSeekTime;
            long t=t2-t1;/*
            double oldMultiplier = driftMultiplier;
            double newMultiplier;
            double gt2 = lastDriftOffset;
            double gt1 = lastSeekOffset;
            double error = gt2-gt1;
            double gt2a = gt2/(2-oldMultiplier); //adjusted
            double error = (gt2 - gt1)/(2-oldMultiplier);
            double diffperms = diff / (t2-t1); //difference per millisecond traversed.. i.e. rate of divergance*/

            //damp measurements made over short periods to account for oscillation
            double damp  = 1.0;
            if (t < 60000)  { //100s
                damp =  (double)t/(double)60000;
            }

            double mult = (2-driftMultiplier);

            double x2 = lastDriftOffset;
            double x2a = x2 + -t*(1-mult);
            double x1 = lastSeekOffset;
            double error = (x2a - x1);
            // in operation, x2 = x1 + errorpersecond * time * mult
            double mult2 = error / t+1;
            double newMultiplier = 2 - mult2;


            effectiveDriftMultiplier=-1; //consider the limited resolution given number of frames, can only remove/add 1 at a time
            historicalDriftMultiplier=-1;
            respeedCount=0;


            if(newMultiplier < 1.1 && newMultiplier > 0.9) {
                double newMultiplierDamped = damp * (newMultiplier-1) + (1-damp) * (driftMultiplier - 1) + 1;
                System.out.printf("Over %dms, drifted %fms under multiplier of %f (otherwise would have been %fms). Drift correction set from: %f to: %f (undampened: %f)\n",t2-t1,x2-x1,driftMultiplier,x2a-x1,driftMultiplier,newMultiplierDamped, newMultiplier);
                driftMultiplier = newMultiplierDamped;
                prefs.putDouble("respeed_coefficient", driftMultiplier);
            }

        }
    }

    // frames played since fragment x, getStartFragment
    long startFragment = 0;
    long oldFramesPlayed = 0;
    long getNewFramesPlayed() {
        return line.getLongFramePosition() - oldFramesPlayed;
    }

    long getTotalFramesPlayed() {
        return getNewFramesPlayed() + startFragment * (getFragmentSize() / format.getFrameSize());
    }

    double getTotalFragmentsPlayed() {
        return (double)getTotalFramesPlayed() / (double)getFragmentSize();
    }

    void setNewStartPoint(long fragment) {
        startFragment = fragment;
        oldFramesPlayed = line.getLongFramePosition();
    }

    int getFragmentSize() { //size in bytes
        return (int)((double)MediaTransmitter.getPacketSize() * (2.0-driftMultiplier));
    }

    double getFragmentLengthInMs() {
        return ((((double)getFragmentSize() / (double)format.getFrameSize())) / (double)format.getFrameRate() * 1000.0);
    }

    long getStartFragment() {
        return startFragment;
    }

    float oldVol;
   // boolean isFaded = false;
    private void fadeOut(final int ms) {
           /*Thread t = new Thread() {
                public void run() {
                    try {
                        oldVol = volCtrl.getValue();
                        float range = oldVol - volCtrl.getMinimum();
                        float inc = range/(float)ms;
                        for(int i=0;i<ms;i++) {
                            volCtrl.setValue(Math.max(oldVol - inc*i,volCtrl.getMinimum()));
                            Thread.sleep(1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            t.start();*/
    }

    private void fadeIn(final int ms) {
           /*
           Thread t = new Thread() {
                public void run() {
                    try {
                        float range = oldVol - volCtrl.getMinimum();
                        float inc = range/(float)ms;
                        for(int i=0;i<ms;i++) {
                            volCtrl.setValue(Math.min(volCtrl.getMinimum() + inc*i,oldVol));
                            System.out.println("VOLLLL" + volCtrl.getValue()); //todo: remove
                            Thread.sleep(1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            t.start();*/
    }

    public long curTime() {
        Calendar cal = Calendar.getInstance();
        return cal.getTimeInMillis();
    }

}

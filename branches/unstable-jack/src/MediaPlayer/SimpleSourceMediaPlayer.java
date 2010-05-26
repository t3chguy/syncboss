package MediaPlayer;

import org.testng.annotations.Test;
import org.testng.Assert;

import javax.sound.sampled.AudioFormat;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Arrays;/*
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

public class SimpleSourceMediaPlayer implements SourceMediaPlayer {

    InputStream in;
    SourceMediaPlayerHandler[] h;

    public SimpleSourceMediaPlayer(InputStream is) {
        in = is;
    }

    public SimpleSourceMediaPlayer() {

    }

    /** fetch some bytes from the media player (winamp or other)
     * silently catches header information and passes it to readHeader()
     * header information won't be counted in return number of bytes read or anything like that
     * BLOCKS!
     * @return number of bytes
     */
    int bytesSinceHeader = 0;
    byte header[];
    private int getBytes(byte[] out, int max) throws Exception {
        Exception e = new Exception("End of stream");
        if(bytesSinceHeader == 1024) { //todo: configuration
            bytesSinceHeader = 0;
        }
        if(bytesSinceHeader == 0) { //check for header
            int signature = in.read();
            if(signature == -1) {
                bytesSinceHeader=0;
                throw e;
            } else if(signature == 1) { //incoming header

            } else if(signature == 0) { //no incoming header

            } else {
                throw new Exception("Malformed input from media player - please restart it?");
            }
        }
        return 0;
    }

    /** read an audio format from a 32 byte header
     * header[0] : always 1, header[1] - header[4] : samplerate, header[5] : numchannels, header[6] : bitspersample, header[7]-header[32] : unused
     * @param h header bytes (32)
     * @return AudioFormat audio format associated with this header
     */
    private AudioFormat readHeader(byte[] h) {
        assert(h[0] == 1);
        int sr; //samplerate
        sr = ((h[1]&0xff) << 24) | ((h[2]&0xff) << 16) | ((h[3]&0xff) << 8) | (h[4]&0xff);
        int numchans = h[5];
        int bps = h[6];
        return new AudioFormat(sr, bps, numchans, true, false);
    }

    @Test
    public void testReadHeader() {

        // one: 44100 hz, 16 bps, 2 ch
        // 44100 is AC44
        byte[] one = new byte[32];
        Arrays.fill(one, (byte)0x00);
        one[0] = 0x01;
        one[1] = 0x00;
        one[2] = 0x00;
        one[3] = (byte)0xac;
        System.out.print(one[3]);
        one[4] = 0x44;
        one[5] = 0x04;
        one[6] = 0x20;

        //simulate incoming data
        InputStream isone = new ByteArrayInputStream(one);

        AudioFormat referenceone = new AudioFormat(44100, 32, 4, true, false);

        Assert.assertTrue(readHeader(one).matches(referenceone));

    }
}

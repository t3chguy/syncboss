package MediaPlayer;

import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Arrays;
import java.io.InputStream;
import java.io.ByteArrayInputStream;/*
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

public class SimpleSourceMediaPlayerTest {



    @Test
    public void testGetBytes() throws Exception {

        byte[] one = new byte[69];
        Arrays.fill(one, (byte)0);
        one[0] = 0x00;
        one[43] = 0x69;

        byte[] target = new byte[68];

        SourceMediaPlayer testplayer = new SimpleSourceMediaPlayer(new ByteArrayInputStream(one));
        SourceMediaPlayer testplayer2 = new SimpleSourceMediaPlayer(new ByteArrayInputStream(one));

        Assert.assertEquals(testplayer.getBytes(target, 0, 1000), 68, "Correct number of bytes read 1");
        Assert.assertEquals(target[42], 0x69, "Copied to buffer without error"); //was copy without error
        Assert.assertEquals(testplayer2.getBytes(target, 1, 50), 50, "Correct number of bytes read 2");
        Assert.assertEquals(testplayer.getBytes(target, 0, 1000), 0, "Correct number of bytes read 3");
        Assert.assertEquals(testplayer2.getBytes(target, 0, 21), 18, "Correct number of bytes read 4");


    }
}

package MediaPlayer;

import GUI.VolumeRegistry;
import GUI.OffsetRegistry;

import javax.sound.sampled.AudioFormat;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 29/06/2009
 * Time: 1:47:22 PM
 * To change this template use File | Settings | File Templates.
 */
public interface AbstractMediaPlayer {
    //public AbstractMediaPlayer(VolumeRegistry volumeRegistry, OffsetRegistry offsetRegistry, Object mixerInfo);
    public void play();
    public void stop();
    public void queue(byte[] data, long time);
    public void setFormat(AudioFormat format);
    public void forceResync();
}

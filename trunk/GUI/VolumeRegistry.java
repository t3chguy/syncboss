package GUI;

import javax.sound.sampled.Control;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 29/06/2009
 * Time: 11:55:43 AM
 * To change this template use File | Settings | File Templates.
 */
public interface VolumeRegistry {
    public void registerControl(Control ctrl);
    public void unregisterControl(Control ctrl);
    public void updateVolumeControls();
}

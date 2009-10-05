package GUI;

import MediaPlayer.OffsetObject;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 29/06/2009
 * Time: 11:55:43 AM
 * To change this template use File | Settings | File Templates.
 */
public interface OffsetRegistry {
    public void registerOffsetObject(OffsetObject offs);
    public void unregisterOffsetObject(OffsetObject offs);
    public void updateOffsets();
}
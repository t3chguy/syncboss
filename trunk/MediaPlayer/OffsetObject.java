package MediaPlayer;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 29/06/2009
 * Time: 1:51:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class OffsetObject {
    int offsetMilliseconds;
    public OffsetObject(int offsetMilliseconds) {
        this.offsetMilliseconds = offsetMilliseconds;
    }
    public void setOffset(int offsetMilliseconds) {
        this.offsetMilliseconds = offsetMilliseconds;
    }
    public int getOffset() {
        return this.offsetMilliseconds;
    }
}

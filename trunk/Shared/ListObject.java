package Shared;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 29/06/2009
 * Time: 9:36:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class ListObject {
    String disp;
    Object val;

    public ListObject(final String display, final Object value) {
        disp = display;
        val = value;
    }

    public String toString() {
        return disp;
    }

    public Object getValue() {
        return val;
    }
}

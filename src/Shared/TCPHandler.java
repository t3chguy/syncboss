package Shared;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 01/07/2009
 * Time: 2:24:42 PM
 * To change this template use File | Settings | File Templates.
 */
public interface TCPHandler {
    public void handleDataMessage(byte[] msg, int len, char ident);
    public void linkFailed();
}

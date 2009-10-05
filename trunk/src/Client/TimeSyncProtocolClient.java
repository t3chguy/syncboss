package Client;

import java.util.Calendar;

/**
 * Created by IntelliJ IDEA.
 * User: Jack
 * Date: 27/06/2009
 * Time: 7:49:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class TimeSyncProtocolClient {
        public String processInput(String theInput) {
        String theOutput = "";
        if(theInput.charAt(0) == 'r') {
            Calendar cal = Calendar.getInstance();
            long currentTime = cal.getTimeInMillis();
            theOutput = theInput + "\n" + currentTime;
        }
        return theOutput;
    }


}

package Server;

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.SecureRandom;

/**
 * Created by Razvan Tache on 12/5/2015.
 */
public class Session {
    public String id;
    public String filePlaying;
    public int counter;
    public String state;
    public int imageCount = 0;
    public InetAddress clientIpAddress;

    private static SecureRandom random = new SecureRandom();

    public Session() {
        this.id = generateSessionId();
    }

    public static String generateSessionId() {
        return new BigInteger(130, random).toString(32);
    }
}

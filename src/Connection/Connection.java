package Connection;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by Razvan Tache on 12/5/2015.
 */
public class Connection {
    private final static String CRLF = System.lineSeparator();

    public static void writeMessage(BufferedWriter writer, String[] lines) {
        try {
            for (String line:lines) {
                writeLine(writer, line + CRLF);
            }
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeLine(BufferedWriter writer, String message) throws IOException {
        writer.write(message);
        System.out.println("Writing: " + message);
    }
}

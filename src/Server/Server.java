package Server;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class Server implements ActionListener {

    DatagramPacket datagramPacket;
    DatagramSocket datagramSocket;

    InetAddress ClientIpAddress;
    int RTP_dest_port = 0;

    byte[] buf;

    Socket socket;

    private final static String CRLF = System.lineSeparator();

    static BufferedReader reader;
    static BufferedWriter writer;

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    private void parseRequest() {
        try {
            String line = readLine();
            line = readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readLine() throws IOException {
        String line = reader.readLine();
        System.out.println("Read: " + line);
        return line;
    }

    private void sendResponse() {
        try {
            writeLine("Handshake" + CRLF);
            writeLine(1 + CRLF);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeLine(String message) throws IOException {
        writer.write(message);
        System.out.println("Writing: " + message);
    }
}

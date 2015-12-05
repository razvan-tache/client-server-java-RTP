package Server;

import java.io.*;
import java.net.*;

public class Server {
    Socket socket;

    private final static String CRLF = System.lineSeparator();

    public BufferedReader reader;
    public BufferedWriter writer;

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

    public static void main(String[] args) throws Exception {
        Server server = new Server();

        int port = Integer.parseInt(args[0]);

        ServerSocket socket = new ServerSocket(port);
        server.socket = socket.accept();
        socket.close();

        server.reader = new BufferedReader(new InputStreamReader(server.socket.getInputStream()));
        server.writer = new BufferedWriter(new OutputStreamWriter(server.socket.getOutputStream()));

        boolean done = false;

        while (!done) {
            server.parseRequest();
            server.sendResponse();
            done = true;
        }
    }
}

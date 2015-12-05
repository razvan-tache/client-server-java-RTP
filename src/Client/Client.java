package Client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    private Socket socket;

    private BufferedReader reader;
    private BufferedWriter writer;

    private final static String CRLF = System.lineSeparator();

    private void sendRequest() {
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

    private void parseResponse() {
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

    public static void main(String[] args) throws Exception {
        Client client = new Client();

        // Parsing the arguments: 0 - server host ; 1 - server port
        int serverPort = Integer.parseInt(args[1]);
        String serverHost = args[0];

        InetAddress ServerIPAddr = InetAddress.getByName(serverHost);

        // Establish a connection  (TCP) with the server
        client.socket = new Socket(ServerIPAddr, serverPort);

        // Initialize input and output buffers
        client.reader = new BufferedReader(new InputStreamReader(client.socket.getInputStream()));
        client.writer = new BufferedWriter(new OutputStreamWriter(client.socket.getOutputStream()));

        client.sendRequest();
        client.parseResponse();
    }
}

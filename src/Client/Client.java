package Client;

import Connection.Connection;
import Connection.State;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class Client {
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;
    public static int UDP_RECEIVER_PORT;

    public String sessionID = null;
    private Socket socket;

    private BufferedReader reader;
    private BufferedWriter writer;

    private final static String CRLF = System.lineSeparator();

    private int counter = 0;

    private String[] buildRequest(String action)
    {
        String[] lines = new String[3];

        counter++;
        switch (action) {
            case State.INIT: {
                lines[0] = "INIT some_file RTSP/1.0";
                lines[2] = "Transport: RTP/UDP; client_port= 25000";
                break;
            }
            default: {
                lines[0] = action + "";
                lines[2] = "Session: " + sessionID;
            }
        }

        lines[1] = "Counter: " + counter;

        return lines;
    }

    private void sendRequest(String action) {
        Connection.writeMessage(writer, buildRequest(action));
    }

    private void parseResponse() {
        try {
            String responseCodeLine = readLine();
            StringTokenizer tokens = new StringTokenizer(responseCodeLine);
            tokens.nextToken();
            int responseCode = Integer.parseInt(tokens.nextToken());
            String responseStatus = tokens.nextToken();

            if (responseCode >= 200 && responseCode < 300 && responseStatus.toLowerCase().equals("ok")) {
                String counterLine = readLine();
                tokens = new StringTokenizer(counterLine);
                tokens.nextToken();
                int counter = Integer.parseInt(tokens.nextToken());

                String sessionLine = readLine();
                if (counter == this.counter + 1) {
                    this.counter++;
                    if (sessionID == null) {
                        tokens = new StringTokenizer(sessionLine);
                        tokens.nextToken();
                        sessionID = tokens.nextToken();
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readLine() throws IOException {
        String line = reader.readLine();
        System.out.println("Read: " + line);
        return line;
    }

    private void initDatagram() throws SocketException {
        datagramSocket = new DatagramSocket(UDP_RECEIVER_PORT);
        datagramSocket.setSoTimeout(5);

        sendRequest(State.INIT);
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

        client.sendRequest(State.INIT);
        client.parseResponse();
        client.sendRequest(State.PLAY);
        client.parseResponse();
    }
}

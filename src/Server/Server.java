package Server;

import Connection.Connection;
import Connection.State;

import java.io.*;
import java.net.*;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Server {
    Socket socket;

    private final static String CRLF = System.lineSeparator();

    public BufferedReader reader;
    public BufferedWriter writer;

    public HashMap<String, Session> sessionTable;

    public Server() {
        sessionTable = new HashMap<String, Session>();
    }

    private String parseRequest() {

        try {
            String requestDescLine = readLine();
            StringTokenizer tokens = new StringTokenizer(requestDescLine);
            String request_type = tokens.nextToken();

            switch (request_type) {
                case State.INIT : {
                    String filename = tokens.nextToken();

                    Session session = new Session();
                    session.filePlaying = filename;

                    sessionTable.put(session.id, session);

                    String counterLine = readLine();
                    tokens = new StringTokenizer(counterLine);
                    tokens.nextToken();
                    int counter = Integer.parseInt(tokens.nextToken());
                    if (session.counter != counter - 1) {
                        throw new InvalidParameterException("Counter doesn't match");
                    }

                    session.counter = counter;
                    session.state = request_type;
                    String line = readLine();

                    return session.id;
                }
                default: {
                    // TODO: Add check for filename
                    String counterLine = readLine();
                    tokens = new StringTokenizer(counterLine);
                    tokens.nextToken();
                    int counter = Integer.parseInt(tokens.nextToken());

                    String sessionLine = readLine();
                    tokens = new StringTokenizer(sessionLine);
                    tokens.nextToken();
                    String sessionID = tokens.nextToken();
                    Session session = sessionTable.get(sessionID);

                    if (session == null) {
                        return null;
                    }

                    if (session.counter != counter - 1) {
                        throw new InvalidParameterException("Counter doesn't match");
                    }

                    session.counter = counter;
                    session.state = request_type;

                    return sessionID;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String readLine() throws IOException {
        String line = reader.readLine();
        System.out.println("Read: " + line);
        return line;
    }

    private void sendResponse(Session session) {
        Connection.writeMessage(writer, buildOKResponse(session));
    }

    private String[] buildOKResponse(Session session)
    {
        session.counter++;

        String[] lines = new String[3];
        lines[0] = "RTSP/1.0 200 OK";
        lines[1] = "Counter: " + session.counter;
        lines[2] = "Session: " + session.id;

        return lines;
    }

    // TODO: This should do the whole while(done) logic
    private void listen()
    {
        parseRequest();
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();

        int port = Integer.parseInt(args[0]);

        ServerSocket socket = new ServerSocket(port);
        server.socket = socket.accept();
        socket.close();

        server.reader = new BufferedReader(new InputStreamReader(server.socket.getInputStream()));
        server.writer = new BufferedWriter(new OutputStreamWriter(server.socket.getOutputStream()));

        int done = 2;

        while (done != 0) {
            String sessionID = server.parseRequest();
            Session session = server.sessionTable.get(sessionID);
            server.sendResponse(session);
            done --;
        }
    }
}

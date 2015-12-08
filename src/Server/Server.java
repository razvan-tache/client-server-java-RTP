package Server;

import Connection.Connection;
import Connection.State;
import Connection.RTPpacket;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Server implements ActionListener {
    Socket socket;

    private final static String CRLF = System.lineSeparator();

    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;

    public BufferedReader reader;
    public BufferedWriter writer;

    public HashMap<String, Session> sessionTable;

    public Timer timer;

    public byte[] buf;

    VideoStream video;

    private String sess;
    public Server() {
        sessionTable = new HashMap<String, Session>();

        timer = new Timer(100, this);
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        //allocate memory for the sending buffer
        buf = new byte[15000];
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
                    session.clientIpAddress = socket.getInetAddress();
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
            try {
                String sessionID = server.parseRequest();
                Session session = server.sessionTable.get(sessionID);

                switch (session.state) {
                    case State.INIT: {
                        System.out.println("New RTSP state: READY");

                        //Send response
                        server.sendResponse(session);

                        //init the VideoStream object:
                        server.video = new VideoStream(session.filePlaying);

                        //init RTP socket
                        server.datagramSocket = new DatagramSocket();
                        server.sess = sessionID;
                        break;
                    }
                    case State.PLAY: {
                        server.sendResponse(session);
                        server.timer.start();
                        System.out.println("PLAYING");
                        break;
                    }
                    case State.PAUSE: {
                        server.sendResponse(session);

                        server.timer.stop();
                        System.out.println("New RTSP state: READY(after stop)");
                        break;
                    }
                    case State.STOP: {
                        server.sendResponse(session);

                        server.timer.stop();
                        server.socket.close();
                        server.datagramSocket.close();

                        System.exit(0);
                    }
                }

            } catch (Exception e) {
                System.out.println("Connection closed");
                break;
            }

        }
    }

    public void actionPerformed(ActionEvent e) {

        Session session = sessionTable.get(sess);
        //if the current image nb is less than the length of the video
        if (session.imageCount < 500)
        {
            //update current imagenb
            session.imageCount++;

            try {
                //get next frame to send from the video, as well as its size
                int image_length = video.getNextFrame(buf);

                //Builds an RTPpacket object containing the frame
                RTPpacket rtp_packet = new RTPpacket(26, session.imageCount, session.imageCount*100, buf, image_length);

                //get to total length of the full rtp packet to send
                int packet_length = rtp_packet.getlength();

                //retrieve the packet bitstream and store it in an array of bytes
                byte[] packet_bits = new byte[packet_length];
                rtp_packet.getpacket(packet_bits);

                //send the packet as a DatagramPacket over the UDP socket
                datagramPacket = new DatagramPacket(packet_bits, packet_length, session.clientIpAddress, 25000);
                datagramSocket.send(datagramPacket);

                //System.out.println("Send frame #"+imagenb);
                //print the header bitstream
                rtp_packet.printheader();

                //update GUI
                System.out.println("Send frame #" + session.imageCount);
            }
            catch(Exception ex)
            {
                System.out.println("Exception caught: "+ex);
                ex.printStackTrace();

                System.exit(0);
            }
        }
        else
        {
            //if we have reached the end of the video file, stop the timer
            timer.stop();
        }
    }
}

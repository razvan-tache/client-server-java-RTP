package Client;

import Connection.Connection;
import Connection.State;
import Connection.RTPpacket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.ArrayDeque;
import java.util.StringTokenizer;
import javax.swing.Timer;

public class Client {

    //GUI
    //----
    JFrame f = new JFrame("Client");
    JButton initButton = new JButton("Start");
    JButton playButton = new JButton("Play");
    JButton pauseButton = new JButton("Pause");
    JButton tearButton = new JButton("Close");
    JPanel mainPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JLabel iconLabel = new JLabel();
    ImageIcon icon;

    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;
    public static int UDP_RECEIVER_PORT = 25000;

    public String sessionID = null;
    private Socket socket;

    private BufferedReader reader;
    private BufferedWriter writer;

    private final static String CRLF = System.lineSeparator();

    private int counter = 0;
    public byte[] buf;
    private boolean isPlaying = false;
    public Timer timer;

    public String fileName;
    public FrameSynchronizer fsynch;
    public Client() {
        //--------------------------

        //Frame
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        //Buttons
        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(initButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);
        initButton.addActionListener(new setupButtonListener(this));
        playButton.addActionListener(new playButtonListener(this));
        pauseButton.addActionListener(new pauseButtonListener(this));
        tearButton.addActionListener(new closeButtonListener(this));

        //Image display label
        iconLabel.setIcon(null);

        //frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0,0,380,280);
        buttonPanel.setBounds(0,280,380,50);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(380,420));
        f.setVisible(true);

        //init timer
        //--------------------------
        timer = new Timer(20, new timerListener(this));
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        fsynch = new FrameSynchronizer(100);

        //allocate enough memory for the buffer used to receive data from the server
        buf = new byte[15000];
    }
    private String[] buildRequest(String action)
    {
        String[] lines = new String[3];

        counter++;
        switch (action) {
            case State.INIT: {
                lines[0] = "INIT " + fileName + " RTSP/1.0";
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

    private int parseResponse() {
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

            return responseCode;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private String readLine() throws IOException {
        String line = reader.readLine();
        System.out.println("Read: " + line);
        return line;
    }

    private void initDatagram() throws SocketException {
        datagramSocket = new DatagramSocket(UDP_RECEIVER_PORT);
        datagramSocket.setSoTimeout(5);
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client();

        // Parsing the arguments: 0 - server host ; 1 - server port
        client.fileName = args[2];
        int serverPort = Integer.parseInt(args[1]);
        String serverHost = args[0];

        InetAddress ServerIPAddr = InetAddress.getByName(serverHost);

        // Establish a connection  (TCP) with the server
        client.socket = new Socket(ServerIPAddr, serverPort);

        // Initialize input and output buffers
        client.reader = new BufferedReader(new InputStreamReader(client.socket.getInputStream()));
        client.writer = new BufferedWriter(new OutputStreamWriter(client.socket.getOutputStream()));

//        client.sendRequest(State.INIT);
//        client.parseResponse();
//        client.sendRequest(State.PLAY);
//        client.parseResponse();
    }

    class setupButtonListener implements ActionListener {

        public Client client;
        public setupButtonListener (Client client) {
            super();
            this.client = client;
        }

        public void actionPerformed(ActionEvent e){

            System.out.println("Init Button pressed !");
            if (sessionID == null) { // if nothing is initialised
                //Init non-blocking RTPsocket that will be used to receive data
                try {
                    //construct a new DatagramSocket to receive RTP packets from the server, on port RTP_RCV_PORT
                    client.initDatagram();
                }
                catch (SocketException se)
                {
                    System.out.println("Socket exception: "+se);
                    System.exit(0);
                }

                //Send SETUP message to the server
                client.sendRequest(State.INIT);

                //Wait for the response
                if (client.parseResponse() != 200)
                    System.out.println("Invalid Server Response");
                else {
                    System.out.println("New RTSP state: READY");
                }
            }
        }
    }

    class playButtonListener implements ActionListener {
        public Client client;
        public playButtonListener (Client client) {
            super();
            this.client = client;
        }

        public void actionPerformed(ActionEvent e){

            System.out.println("Play Button pressed !");
            if (client.sessionID != null && !client.isPlaying) { // if nothing is initialised
                //Init non-blocking RTPsocket that will be used to receive data

                //Send SETUP message to the server
                client.sendRequest(State.PLAY);

                //Wait for the response
                if (client.parseResponse() != 200)
                    System.out.println("Invalid Server Response");
                else {
                    System.out.println("New RTSP state: PLAYING");
                    client.isPlaying = true;

                    timer.start();
                }
            }
        }
    }

    class pauseButtonListener implements ActionListener {
        public Client client;
        public pauseButtonListener (Client client) {
            super();
            this.client = client;
        }

        public void actionPerformed(ActionEvent e){

            System.out.println("Play Button pressed !");
            if (client.sessionID != null && client.isPlaying) { // if nothing is initialised
                //Init non-blocking RTPsocket that will be used to receive data

                //Send SETUP message to the server
                client.sendRequest(State.PAUSE);

                //Wait for the response
                if (client.parseResponse() != 200)
                    System.out.println("Invalid Server Response");
                else {
                    System.out.println("New RTSP state: STOPED");
                    client.isPlaying = false;

                    // stop timer ()
                    timer.stop();
                }
            }
        }
    }

    class closeButtonListener implements ActionListener {
        public Client client;
        public closeButtonListener (Client client) {
            super();
            this.client = client;
        }

        public void actionPerformed(ActionEvent e){

            System.out.println("Play Button pressed !");
            client.sendRequest(State.STOP);

            //Wait for the response
            if (client.parseResponse() != 200)
                System.out.println("Invalid Server Response");
            else {
                System.out.println("New RTSP state: CLOSED CONNECTION");
                client.isPlaying = false;

                if (timer.isRunning()) {
                    timer.stop();
                }

                System.exit(0);
            }
        }
    }

    class timerListener implements ActionListener {
        public Client client;
        public timerListener (Client client) {
            super();
            this.client = client;
        }

        public void actionPerformed(ActionEvent e) {

            //Construct a DatagramPacket to receive data from the UDP socket
            client.datagramPacket = new DatagramPacket(buf, buf.length);

            try{
                //receive the DP from the socket:
                client.datagramSocket.receive(client.datagramPacket);

                //create an RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(client.datagramPacket.getData(), client.datagramPacket.getLength());

                //print important header fields of the RTP packet received:
                System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());

                //print header bitstream:
                rtp_packet.printheader();

                //get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                //get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
//                fsynch.addFrame(toolkit.createImage(payload, 0, payload_length), rtp_packet.getsequencenumber());
//                Image image = toolkit.createImage(payload, 0, payload_length);

                //display the image as an ImageIcon object
                icon = new ImageIcon(toolkit.createImage(payload, 0, payload_length));
                iconLabel.setIcon(icon);
            }
            catch (InterruptedIOException iioe){
                //System.out.println("Nothing to read");
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
            }
        }
    }

    class FrameSynchronizer {

        private ArrayDeque<Image> queue;
        private int bufSize;
        private int curSeqNb;
        private Image lastImage;

        public FrameSynchronizer(int bsize) {
            curSeqNb = 1;
            bufSize = bsize;
            queue = new ArrayDeque<Image>(bufSize);
        }

        //synchronize frames based on their sequence number
        public void addFrame(Image image, int seqNum) {
            if (seqNum < curSeqNb) {
                queue.add(lastImage);
            }
            else if (seqNum > curSeqNb) {
                for (int i = curSeqNb; i < seqNum; i++) {
                    queue.add(lastImage);
                }
                queue.add(image);
            }
            else {
                queue.add(image);
            }
        }

        //get the next synchronized frame
        public Image nextFrame() {
            curSeqNb++;
            lastImage = queue.peekLast();
            return queue.remove();
        }
    }
}

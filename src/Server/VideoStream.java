package Server;

import java.io.*;

public class VideoStream {

    private FileInputStream fileInputStream;
    public int frameNumber; //current frame nb


    public VideoStream(String filename) throws Exception{
        fileInputStream = new FileInputStream(filename);
        frameNumber = 0;
    }

    //-----------------------------------
    // getNextFrame
    //returns the next frame as an array of byte and the size of the frame
    //-----------------------------------
    public int getNextFrame(byte[] frame) throws Exception
    {
        int length = 0;
        String length_string;
        byte[] frame_length = new byte[5];

        //read current frame length
        fileInputStream.read(frame_length,0,5);

        //transform frame_length to integer
        length_string = new String(frame_length);
        length = Integer.parseInt(length_string);

        return(fileInputStream.read(frame,0,length));
    }
}
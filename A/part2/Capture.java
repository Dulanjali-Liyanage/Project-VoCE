//package iteration2;
import java.net.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Scanner;


public class Capture extends Main {

    private final int packetsize = 1000;
    private final int port = 55000;
    private InetAddress host = null;
    private MulticastSocket socket = null;
    private byte tempBuffer[] = new byte[this.packetsize];
    private boolean stopCapture = true;

    private void captureAndSend() {
        this.stopCapture = true;
        try {
            int readCount;
            while (true) {
                if (!this.stopCapture) {
                    readCount = getTargetDataLine().read(this.tempBuffer, 0, this.tempBuffer.length);  //capture sound into tempBuffer

                    if (readCount > 0) {

                        // Construct the datagram packet
                        DatagramPacket packet = new DatagramPacket(this.tempBuffer, this.tempBuffer.length, this.host, 55001);

                        // Send the packet
                        this.socket.send(packet);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void stopCapture(){
        this.stopCapture = true;
    }
    
    public void startCapture(){
        this.stopCapture = false;
    }

    public void run() {
        try {
            this.socket = new MulticastSocket();
            this.socket.joinGroup(this.host);
            this.captureAudio();
            this.captureAndSend();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.socket.close();
        }
    }

    public Capture(InetAddress host) {
        this.host = host;
    }

    public Capture() {
        super();
    }

    public static void main(String[] args) {

        // Check the whether the arguments are given
        if (args.length != 1) {
            System.out.println("DatagramClient host ");
            return;
        }
        
        Capture cap = null;
        Play ply = null;

        try {

            cap = new Capture(InetAddress.getByName(args[0]));
            cap.start();

            ply = new Play(InetAddress.getByName(args[0]));
            ply.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        Scanner in = new Scanner(System.in);
        boolean state = true; // playing
        
        while(true && (cap != null) && (ply != null)){
            in.nextLine();
            if(state) {
                cap.stopCapture();
                ply.startPlay();
                System.out.println("Playing...");
                state = false;
            } else {
                ply.stopPlay();
                cap.startCapture();
                System.out.println("Capturing...");
                state = true;
            }
        } 
    }
}
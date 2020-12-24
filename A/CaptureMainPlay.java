import java.net.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

class Main {

    private AudioFormat audioFormat;
    private TargetDataLine targetDataLine;
    private AudioInputStream audioInputStream;
    private SourceDataLine sourceDataLine;

    public AudioFormat getAudioFormat() {
        float sampleRate = 16000.0F;
        int sampleSizeInBits = 16;
        int channels = 2;
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public synchronized SourceDataLine getSourceDataLine() {
        return this.sourceDataLine;
    }

    public synchronized TargetDataLine getTargetDataLine() {
        return this.targetDataLine;
    }

    public void playAudio() {
        try {
            audioFormat = getAudioFormat();     //get the audio format

            DataLine.Info dataLineInfo1 = new DataLine.Info(SourceDataLine.class, audioFormat);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo1);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();

//            //Setting the maximum volume
            FloatControl control = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            control.setValue(control.getMaximum()/2);

        } catch (LineUnavailableException e) {
            e.printStackTrace();
            System.exit(0);
        }

    }

    public synchronized void  captureAudio() {
        try {
            Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();    //get available mixers
            System.out.println("Available mixers:");
            Mixer mixer = null;
            for (int cnt = 0; cnt < mixerInfo.length; cnt++) {
                System.out.println(cnt + " " + mixerInfo[cnt].getName());
                mixer = AudioSystem.getMixer(mixerInfo[cnt]);

                Line.Info[] lineInfos = mixer.getTargetLineInfo();
                if (lineInfos.length >= 1 && lineInfos[0].getLineClass().equals(TargetDataLine.class)) {
                    System.out.println(cnt + " Mic is supported!");
                    break;
                }
            }

            audioFormat = getAudioFormat();     //get the audio format
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

            targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

        } catch (LineUnavailableException e) {
            e.printStackTrace(); //********************
            System.exit(0);
        }
    }
}

class Play extends Main implements Runnable {

    private final int packetsize = 100;
    private final int port = 55001;

    public void run() {

        try {

            // Construct the socket
            DatagramSocket socket = new DatagramSocket(this.port);
            System.out.println("The server is ready");

            // Create a packet
            DatagramPacket packet = new DatagramPacket(new byte[this.packetsize], (this.packetsize));
            this.playAudio();


            for (;;) {

                try {

                    // Receive a packet (blocking)
                    socket.receive(packet);

                    // Print the packet
                    this.getSourceDataLine().write(packet.getData(), 0, this.packetsize); //playing the audio

                } catch (Exception e) {

                    e.printStackTrace();

                }

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

    }


}

public class CaptureMainPlay extends Main implements Runnable {

    private final int packetsize = 100;
    private final int port = 5200;
    private InetAddress host = null;
    private DatagramSocket socket = null;
    private ByteArrayOutputStream byteArrayOutputStream = null;
    private byte tempBuffer[] = new byte[this.packetsize];
    private boolean stopCapture = false;

    private void captureAndSend() {
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.stopCapture = false;
        try {
            int readCount;
            while (!this.stopCapture) {
                readCount = getTargetDataLine().read(this.tempBuffer, 0, this.tempBuffer.length);  //capture sound into tempBuffer

                if (readCount > 0) {
                    this.byteArrayOutputStream.write(this.tempBuffer, 0, readCount);

                    // Construct the datagram packet
                    DatagramPacket packet = new DatagramPacket(this.tempBuffer, this.tempBuffer.length, this.host,55001);

                    // Send the packet
                    this.socket.send(packet);
                }
            }
            this.byteArrayOutputStream.close();
        } catch (IOException e) {

            e.printStackTrace();
            //System.exit(0);
        }
    }

    public void run() {
        try {
            this.socket = new DatagramSocket(this.port);
            this.captureAudio();
            this.captureAndSend();

        } catch (Exception e) {

            e.printStackTrace();

        // } finally {
            this.socket.close();
        }
    }

    public CaptureMainPlay(InetAddress host) {
        this.host = host;
    }

    public CaptureMainPlay() {
        super();
    }

    public static void main(String[] args) {

        // Check the whether the arguments are given
        if (args.length != 1) {
            System.out.println("Enter : java CaptureMainPlay <ip_address> ");
            return;
        }

        try {

            Thread cap = new Thread(new CaptureMainPlay(InetAddress.getByName(args[0])));
            cap.start();

            Thread ply = new Thread(new Play());
            ply.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

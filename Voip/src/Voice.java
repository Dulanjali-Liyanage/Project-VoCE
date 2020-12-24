import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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

public class Voice extends RecordPlayback  implements Runnable {

    private final int packetsize = 100;
    private final int port = 55000;
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

        } finally {
            this.socket.close();
        }
    }

    public Voice(InetAddress host) {
        this.host = host;
    }

    public Voice() {
        super();
    }

    public static void main(String[] args) {

        // Check the whether the arguments are given
        if (args.length != 1) {
            System.out.println("DatagramClient host ");
            return;
        }

        try {

            Thread cap = new Thread(new Voice(InetAddress.getByName(args[0])));
            cap.start();

            Thread ply = new Thread(new Record());
            ply.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
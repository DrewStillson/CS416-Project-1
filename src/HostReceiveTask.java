import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

public class HostReceiveTask implements Runnable{
    private DatagramSocket socket;
    private String host_name;

    public HostReceiveTask(DatagramSocket socket, String host_name) {
        this.socket = socket;
        this.host_name = host_name;
    }

    public void run() {
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                socket.receive(packet);

                byte[] content = Arrays.copyOf(packet.getData(), packet.getLength());
                String frame_data = new String(content);

                String[] parts = frame_data.split(":", 3);

                String source = parts[0];
                String destination = parts[1];
                String message = parts[2];

                if (destination.equals(host_name)) {
                    System.out.println("\nMessage from " + source + ": " + message);
                    System.out.print("Enter the name of the host you would like to send to: ");
                } else {
                    System.out.println("\nFlooded Frame");
                    System.out.print("Enter the name of the host you would like to send to: ");
                }
            } catch (IOException e) {
                System.out.println("Receiver Stopped");
            }
        }
    }
}

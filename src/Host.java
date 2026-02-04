import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Host {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Host Name Needed");
            return;
        }

        Scanner reader = new Scanner(System.in);

        String host_name = args[0];

        Config config = Config.load("config.txt");

        InetSocketAddress host_address = config.addresses.get(host_name);

        if (host_address ==  null) {
            System.out.println("Error host name is not found in config file");
            return;
        }

        List<String> neighbors = config.links.get(host_name);

        if (neighbors == null || neighbors.isEmpty()) {
            System.out.println("Error host has no links");
            return;
        }

        String host_switchID = neighbors.get(0);
        InetSocketAddress switch_address = config.addresses.get(host_switchID);

        if (switch_address == null) {
            System.out.println("Error neighbor has no info attached");
            return;
        }

        DatagramSocket socket = new DatagramSocket(host_address.getPort());
        ExecutorService es = Executors.newFixedThreadPool(2);
        es.submit(new HostReceiveTask(socket, host_name));

        while (true) {

            System.out.print("Enter the name of the host you would like to send to: ");
            String receiver = reader.nextLine();
            System.out.print("What is the message you would like to send to " + receiver + ": ");
            String message = reader.nextLine();
            System.out.println();

            String frame = host_name + ":" + receiver + ":" + message;

            DatagramPacket request = new DatagramPacket(frame.getBytes(), frame.getBytes().length, switch_address.getAddress(), switch_address.getPort());

            socket.send(request);

        }
    }
}

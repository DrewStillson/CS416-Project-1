import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Switch {

    private static String getIncomingSwitchPort(DatagramPacket frame,
                                             List<String> neighbors,
                                             Config config) {

        int source_UDP_port = frame.getPort();

        for (String neighborID: neighbors) {
            InetSocketAddress neighbor_address = config.addresses.get(neighborID);
            if (neighbor_address != null && neighbor_address.getPort() == source_UDP_port) {
                return neighborID;
            }
        }
        return null;
    }

    private static String addressToPortString(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    private static void sendToNeighbor(String frame_data,
                                       String neighborID,
                                       Config config,
                                       DatagramSocket socket) throws IOException {

        InetSocketAddress address = config.addresses.get(neighborID);

        byte[] data = frame_data.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address.getAddress(), address.getPort());
        socket.send(packet);

    }

    private static String neighborForPortString(String portString,
                                                Map<String, String> neighborToPortString) {
        for (Map.Entry<String, String> entry : neighborToPortString.entrySet()) {
            if (entry.getValue().equals(portString)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("Switch Name Needed");
            return;
        }

        Map<String, String> switch_table = new HashMap<>();

        String switch_name = args[0];

        Config config = Config.load("config.txt");

        InetSocketAddress switch_address = config.addresses.get(switch_name);

        if (switch_address ==  null) {
            System.out.println("Error host name is not found in config file");
            return;
        }

        List<String> neighbors = config.links.get(switch_name);

        if (neighbors == null || neighbors.isEmpty()) {
            System.out.println("Error switch has no links");
            return;
        }

        Map<String, String> neighborToPortString = new HashMap<>();

        for (String neighborID : neighbors) {
            InetSocketAddress address = config.addresses.get(neighborID);
            if (address == null) {
                System.out.println("Error neighbor has no address info");
                return;
            }
            neighborToPortString.put(neighborID, addressToPortString(address));
        }

        DatagramSocket socket = new DatagramSocket(switch_address.getPort());

        while (true) {
            DatagramPacket frame = new DatagramPacket(new byte[1024], 1024);
            socket.receive(frame);

            byte[] content = Arrays.copyOf(frame.getData(), frame.getLength());
            String frame_data = new String(content);

            String[] parts = frame_data.split(":", 3);

            String source_Mac = parts[0];
            String destination_Mac = parts[1];

            String inNeighbor = getIncomingSwitchPort(frame, neighbors, config);
            String inPortString = neighborToPortString.get(inNeighbor);

            boolean tableChanged = (!switch_table.containsKey(source_Mac) || (!switch_table.get(source_Mac).equals(inPortString)));
            switch_table.put(source_Mac, inPortString);

            if (tableChanged) {
                System.out.println("Switch Table: " + switch_table);
            }

            if (switch_table.containsKey(destination_Mac)) {
                String outPortString = switch_table.get(destination_Mac);
                if (!outPortString.equals(inPortString)) {
                    String outNeighbor = neighborForPortString(outPortString, neighborToPortString);

                    if (outNeighbor != null) {
                        sendToNeighbor(frame_data, outNeighbor, config, socket);
                    }
                }
            } else {
                flood(frame_data, inPortString, neighborToPortString, neighbors, config, socket);
            }

            System.out.println();

        }
    }

    public static void flood(String frame_data,
                             String inPortString,
                             Map<String, String> neighborToPortString,
                             List<String> neighbors,
                             Config config,
                             DatagramSocket socket) throws IOException {

        for (String neighborID : neighbors) {
            String neighborPortString = neighborToPortString.get(neighborID);
            if (neighborPortString != null && neighborPortString.equals(inPortString)) {
                continue;
            }
            sendToNeighbor(frame_data, neighborID, config, socket);
        }
    }
}

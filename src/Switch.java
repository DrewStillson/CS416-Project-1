import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Switch {

    private static int getIncomingSwitchPort(DatagramPacket frame,
                                             List<String> neighbors,
                                             Config config,
                                             Map<String, Integer> neighborToSwitchPort) {

        int source_UDP_port = frame.getPort();

        for (String neighborID: neighbors) {
            InetSocketAddress neighbor_address = config.addresses.get(neighborID);
            if (neighbor_address != null && neighbor_address.getPort() == source_UDP_port) {
                return neighborToSwitchPort.get(neighborID);
            }
        }
        return -1;
    }

    private static void sendOutPort(String frame_data,
                                    int out_port,
                                    Map<Integer, String> switchPortToNeighbor,
                                    Config config,
                                    DatagramSocket socket) throws IOException {

        String neighborID = switchPortToNeighbor.get(out_port);

        if (neighborID == null) {
            return;
        }

        InetSocketAddress address = config.addresses.get(neighborID);

        if (address == null) {
            return;
        }

        byte[] content = frame_data.getBytes();
        DatagramPacket packet = new DatagramPacket(content, content.length, address.getAddress(), address.getPort());
        socket.send(packet);

    }

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("Switch Name Needed");
            return;
        }

        Map<String, Integer> switch_table = new HashMap<>();

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

        Map<String, Integer> neighborToSwitchPort = new HashMap<>();
        Map<Integer, String> switchPortToNeighbor = new HashMap<>();

        for (int i = 0; i < neighbors.size(); i++) {
            int port = i + 1;
            String neighborID = neighbors.get(i);

            neighborToSwitchPort.put(neighborID, port);
            switchPortToNeighbor.put(port, neighborID);
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

            int inPort = getIncomingSwitchPort(frame, neighbors, config, neighborToSwitchPort);

            boolean tableChanged = (!switch_table.containsKey(source_Mac) || (switch_table.get(source_Mac) != inPort));
            switch_table.put(source_Mac, inPort);

            if (tableChanged) {
                System.out.println("Switch Table: " + switch_table);
            }

            if (switch_table.containsKey(destination_Mac)) {
                int outPort = switch_table.get(destination_Mac);
                if (outPort != inPort) {
                    sendOutPort(frame_data, outPort, switchPortToNeighbor, config, socket);
                }
            } else {
                flood(frame_data, inPort, switchPortToNeighbor, config, socket);
            }

            System.out.println();

        }
    }

    public static void flood(String frame_data,
                             int inPort,
                             Map<Integer, String> switchPortToNeighbor,
                             Config config,
                             DatagramSocket socket) throws IOException {

        byte[] data = frame_data.getBytes();

        for (Map.Entry<Integer, String> entry : switchPortToNeighbor.entrySet()) {
            int port = entry.getKey();

            if (port == inPort) {
                continue;
            }

            String neighborID = entry.getValue();
            InetSocketAddress address = config.addresses.get(neighborID);

            if (address == null) {
                continue;
            }

            DatagramPacket packet = new DatagramPacket(data, data.length, address.getAddress(), address.getPort());
            socket.send(packet);

        }
    }
}

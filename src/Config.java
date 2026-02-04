import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    Map<String, InetSocketAddress> addresses = new HashMap<>();
    Map<String, List<String>> links = new HashMap<>();

    public static Config load(String filename) throws IOException {
        Config config = new Config();
        List<String> lines = Files.readAllLines(Path.of(filename));

        boolean readingLinks = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equals("LINKS")) {
                readingLinks = true;
                continue;
            }
            else if (line.equals("DEVICES")) {
                continue;
            }

            String[] parts = line.split("\\s+");

            if (!readingLinks) {
                String id = parts[0];
                String ip = parts[1];
                int port = Integer.parseInt(parts[2]);

                config.addresses.put(id, new InetSocketAddress(ip, port));

                config.links.putIfAbsent(id, new ArrayList<>());

            } else {
                String l1 = parts[0];
                String l2 = parts[1];

                config.links.get(l1).add(l2);
                config.links.get(l2).add(l1);
            }
        }

        return config;

    }
}

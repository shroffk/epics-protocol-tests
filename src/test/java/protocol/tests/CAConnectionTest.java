package protocol.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.epics.ca.Channel;
import org.epics.ca.ConnectionState;
import org.epics.ca.Context;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CAConnectionTest {

    private static final Logger log = Logger.getLogger(CAConnectionTest.class.getName());

    @BeforeClass
    public static void setup() {
        log.info("Creating the context");
        log.info(System.getProperty("java.version"));

        // Start the test server
        InMemoryCAServer.initializeServerInstance();
    }

    @AfterClass
    public static void tearDown() {
        cleanup();
    }

    static AtomicBoolean success = new AtomicBoolean(true);

    @Test
    public void syncSingleThreadedClientConnections() {

        try (Context context = new Context();) {
            List<Channel> channels = new ArrayList<Channel>();
            try {
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < 10000; i++) {
                    channels.add(context.createChannel("test_int_" + i, Integer.class));
//                    channels.add(context.createChannel("test_long_" + i, Long.class));
                    channels.add(context.createChannel("test_double_" + i, Double.class));
                    channels.add(context.createChannel("test_String_" + i, String.class));
                }
                channels.stream().forEach(channel -> {
                    channel.connect();
                    if (channel.getConnectionState() != ConnectionState.CONNECTED) {
                        success.set(false);
                        log.info(channel.getName() + " " + channel.getConnectionState());
                    }
                });
                long completedChannelCreation = System.currentTimeMillis() - startTime;
                if (success.get()) {
                    log.info("Created 30000 channel connections using a single threaded client "
                            + completedChannelCreation + " ms");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cleanup() {
        log.info("cleanup");
        InMemoryCAServer.closeServerInstance();
    }

}

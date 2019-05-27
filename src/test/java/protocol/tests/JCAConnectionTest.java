package protocol.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cosylab.epics.caj.CAJContext;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;

/**
 * Create connections to pv's using JCA
 * 
 * @author Kunal Shroff
 *
 */
public class JCAConnectionTest {

    private static final Logger log = Logger.getLogger(JCAConnectionTest.class.getName());
    /**
     * Context to be tested.
     */
    private static CAJContext context;

    @BeforeClass
    public static void setup() {
        log.info("Creating the context");
        context = new CAJContext();
        log.info(System.getProperty("java.version"));

        // Start the test server
        InMemoryCAServer.initializeServerInstance();
    }

    @AfterClass
    public static void tearDown() {
        cleanup();
    }

    static AtomicBoolean success = new AtomicBoolean(true);

    /**
     * use the context in a single thread and synchronously create all the channels
     */
    @Test
    public void syncSingleThreadedClientConnections() {
        success.set(true);
        List<Channel> channels = new ArrayList<Channel>();
        try {
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                channels.add(context.createChannel("test_int_" + i));
                channels.add(context.createChannel("test_long_" + i));
                channels.add(context.createChannel("test_double_" + i));
                channels.add(context.createChannel("test_String_" + i));
            }
            context.pendIO(10);
            long completedChannelCreation = System.currentTimeMillis() - startTime;
            channels.stream().forEach(channel -> {
                if (channel.getConnectionState() != Channel.CONNECTED) {
                    success.set(false);
                    log.info(channel.getName() + " " + channel.getConnectionState());
                }
            });
            if (success.get()) {
                log.info("Created 40000 channel connections using a single threaded client " + completedChannelCreation + " ms");
            }
        } catch (IllegalArgumentException | IllegalStateException | CAException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * use the context in a multi threaded client
     */
    @Test
    public void syncMultiThreadedClientConnections() {
        success.set(true);
        Collection<Future<Channel>> channels = new ConcurrentLinkedQueue<Future<Channel>>();
        ExecutorService executorService = Executors.newCachedThreadPool();
        try {
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                channels.add(executorService.submit(new CreateChannelJob("test_int_" + i, context)));
                channels.add(executorService.submit(new CreateChannelJob("test_long_" + i, context)));
                channels.add(executorService.submit(new CreateChannelJob("test_double_" + i, context)));
                channels.add(executorService.submit(new CreateChannelJob("test_String_" + i, context)));
            }
            context.pendIO(10);
            long completedChannelCreation = System.currentTimeMillis() - startTime;
            channels.stream().forEach(channel -> {
                try {
                    if (channel.get().getConnectionState() != Channel.CONNECTED) {
                        success.set(false);
                        log.info(channel.get().getName() + " " + channel.get().getConnectionState());
                    }
                } catch (IllegalStateException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
            if (success.get()) {
                log.info("Created 40000 channel connections using a multi threaded client " + completedChannelCreation + " ms");
            }
        } catch (IllegalArgumentException | IllegalStateException | CAException | TimeoutException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    private static class CreateChannelJob implements Callable<Channel> {

        private Channel channel;

        public CreateChannelJob(String channelName, Context context) {
            try {
                channel = context.createChannel(channelName);
            } catch (IllegalArgumentException | IllegalStateException | CAException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Channel call() throws Exception {
            return channel;
        }

    }

    private static void asyncConnections() {

    }

    /**
     * 
     */
    private static void cleanup() {
        log.info("cleaning up the context and channels");
        try {
            context.destroy();
            InMemoryCAServer.closeServerInstance();
        } catch (IllegalStateException | CAException e) {
            e.printStackTrace();
        }
    }

    private static class JCAConnectionListener implements ConnectionListener {

        private final String name;

        JCAConnectionListener(String name) {
            this.name = name;
        }

        public void connectionChanged(ConnectionEvent ev) {
            System.out.println("channel:" + name + " Connected: " + ev.isConnected() + " " + ev.toString());
        }

    }

}

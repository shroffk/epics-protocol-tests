package protocol.tests;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public class CAMultiplexingTest {
    private static final Logger log = Logger.getLogger(CAMultiplexingTest.class.getName());

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

    private static void cleanup() {
        log.info("cleanup");
        InMemoryCAServer.closeServerInstance();
    }
    
    
}

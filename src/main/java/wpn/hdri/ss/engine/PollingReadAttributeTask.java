package wpn.hdri.ss.engine;

import org.slf4j.Logger;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.Value;
import wpn.hdri.ss.data.attribute.Attribute;

import java.util.Map;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 17.06.13
 */
public class PollingReadAttributeTask implements Runnable {
    private final Attribute<?> attribute;
    private final Client devClient;

    private final Logger logger;

    private final long delay;
    private final boolean append;

    /**
     * After each failed read attempt this will be multiplied by delay to get a delay before next attempt
     */
    private volatile byte tries = 0;
    /**
     * Defines a number of tries this read task will attempt before throw an exception
     */
    public final static byte MAX_TRIES = 10;


    public PollingReadAttributeTask(Attribute<?> attribute, Client devClient, long delay, boolean append, Logger logger) {
        this.attribute = attribute;
        this.devClient = devClient;
        this.delay = delay;
        this.append = append;
        this.logger = logger;
    }

    /**
     * Performs poll task.
     */
    @Override
    public void run() {
        try {
            Map.Entry<Object, Timestamp> result = devClient.readAttribute(attribute.getName().getName());
            Object data = result.getKey();
            //uncomment this will produce a huge number of Strings. So it is not recommended in production
            //logger.info("Read attribute " + attribute.getFullName() + ": " + data);

            if (append)
                attribute.addValue(Timestamp.now(), Value.getInstance(data), result.getValue());
            else
                attribute.replaceValue(Timestamp.now(), Value.getInstance(data), result.getValue());

            if (tries != 0)
                tries = 0;
        } catch (Exception e) {
            if (++tries < MAX_TRIES) {
                logger.warn("An attempt to read attribute " + attribute.getFullName() + " has failed. Tries left: " + (MAX_TRIES - tries), e);
            } else {
                logger.warn("All attempts to read attribute " + attribute.getFullName() + " failed. Writing null.", e);

                Timestamp now = Timestamp.now();
                attribute.addValue(now, Value.NULL, now);
                tries = 0;
            }
        }
    }

    public long getDelay() {
        return delay;
    }

    public Attribute<?> getAttribute() {
        return attribute;
    }

    public Client getDevClient() {
        return devClient;
    }
}

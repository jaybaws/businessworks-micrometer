package com.transavia.integration.workers;
import com.transavia.integration.MetricBridge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetProcessCountWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MetricBridge.class.getName());

    private MBeanServerConnection mbsc;
    private ObjectName objectName;

    private AtomicInteger processCount = Metrics.gauge("bwengine.process.count", Arrays.asList(Tag.of("method", "GetProcessCount")), new AtomicInteger(-1));

    public GetProcessCountWorker(MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");

        try {
            Integer valProcessCount = (Integer) mbsc.invoke(objectName, "GetProcessCount", null, null);

            if (valProcessCount != null) {
                processCount.set(valProcessCount);

                LOGGER.info(
                        String.format(
                                "[GetProcessCount] count=%d.",
                                valProcessCount
                        )
                );
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Exception invoking 'GetProcessCount'...", t);
        }

        LOGGER.exiting(this.getClass().getCanonicalName(), "run");
    }
}
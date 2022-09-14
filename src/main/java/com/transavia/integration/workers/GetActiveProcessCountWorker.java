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

public class GetActiveProcessCountWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MetricBridge.class.getName());

    private MBeanServerConnection mbsc;
    private ObjectName objectName;

    private AtomicInteger activeProcessCount = Metrics.gauge("bwengine.activeprocess.count", Arrays.asList(Tag.of("method", "GetActiveProcessCount")), new AtomicInteger(-1));

    public GetActiveProcessCountWorker(MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");

        try {
            Integer valActiveProcessCount = (Integer) mbsc.invoke(objectName, "GetActiveProcessCount", null, null);

            if (valActiveProcessCount != null) {
                activeProcessCount.set(valActiveProcessCount);

                LOGGER.info(
                        String.format(
                                "[GetActiveProcessCount] count=%d.",
                                valActiveProcessCount
                        )
                );
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Exception invoking 'GetActiveProcessCount'...", t);
        }

        LOGGER.exiting(this.getClass().getCanonicalName(), "run");
    }
}
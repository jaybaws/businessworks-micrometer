package com.transavia.integration;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetProcessCountWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MetricBridge.class.getName());

    private MBeanServerConnection mbsc;
    private ObjectName objectName;

    public GetProcessCountWorker(MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");

        try {
            Integer result = (Integer) mbsc.invoke(objectName, "GetProcessCount", null, null);

            if (result != null) {
                // @TODO: verify

                Metrics.gauge("SampleMetric", toTags("method=GetProcessCount"), result);
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Exception invoking 'GetProcessCount'...", t);
        }

        LOGGER.exiting(this.getClass().getCanonicalName(), "run");
    }

    private static Iterable<Tag> toTags(String... tags) {
        List<Tag> out = new ArrayList<Tag>();
        for (String t : tags) {
            out.add(Tag.of(t.split("=")[0], t.split("=")[1]));
        }
        return out;
    }

}
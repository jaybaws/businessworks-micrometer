package com.transavia.integration;
import io.micrometer.core.instrument.MeterRegistry;
import javax.management.*;
import javax.management.openmbean.TabularDataSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetActivitiesWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MetricBridge.class.getName());

    private ObjectName objectName;
    private MeterRegistry registry;
    private MBeanServerConnection mbsc;

    public GetActivitiesWorker(MBeanServerConnection mbsc, ObjectName objectName, MeterRegistry registry) {
        this.mbsc = mbsc;
        this.objectName = objectName;
        this.registry = registry;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");

        try {
            TabularDataSupport result = (TabularDataSupport) mbsc.invoke(objectName, "GetActivities", new Object[] { null }, new String[] { String.class.getName() });
            if (result != null) {
                // @TODO: process!

            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Exception invoking 'GetActivities'...", t);
        }

        LOGGER.exiting(this.getClass().getCanonicalName(), "run");
    }

}
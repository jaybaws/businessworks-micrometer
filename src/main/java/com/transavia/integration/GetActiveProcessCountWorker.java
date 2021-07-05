package com.transavia.integration;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetActiveProcessCountWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MetricBridge.class.getName());

    private MBeanServerConnection mbsc;
    private ObjectName objectName;

    public GetActiveProcessCountWorker(MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");
        try {
            Integer result = (Integer) mbsc.invoke(objectName, "GetActiveProcessCount", null, null);

            if (result != null) {
                // @TODO: process

            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Exception invoking 'GetActiveProcessCount'...", t);
        }

        LOGGER.exiting(this.getClass().getCanonicalName(), "run");
    }

}
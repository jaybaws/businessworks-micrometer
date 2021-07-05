package com.transavia.integration;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetMemoryUsageWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MetricBridge.class.getName());

    private MBeanServerConnection mbsc;
    private ObjectName objectName;

    public GetMemoryUsageWorker(MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");

        try {
            CompositeDataSupport result = (CompositeDataSupport) mbsc.invoke(objectName, "GetMemoryUsage", null, null);

            if (result != null) {
                // @TODO: process

            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Exception invoking 'GetMemoryUsage'...", t);
        }

        LOGGER.exiting(this.getClass().getCanonicalName(), "run");
    }

}

/*

javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(name=GetMemoryUsage,items=((itemName=FreeBytes,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=PercentUsed,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=TotalBytes,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=UsedBytes,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)))),contents={FreeBytes=50141632, PercentUsed=83, TotalBytes=308805632, UsedBytes=258664000})

 */
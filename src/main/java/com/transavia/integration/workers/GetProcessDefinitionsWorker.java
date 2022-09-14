package com.transavia.integration.workers;
import com.transavia.integration.MetricBridge;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularDataSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetProcessDefinitionsWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MetricBridge.class.getName());

    private MBeanServerConnection mbsc;
    private ObjectName objectName;

    public GetProcessDefinitionsWorker(MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");

        try {
            TabularDataSupport result = (TabularDataSupport) mbsc.invoke(objectName, "GetProcessDefinitions", null, null);

            if (result != null) {
                // @TODO: process

                System.out.println("GetProcessDefinitions: "+ result.toString());

            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Exception invoking 'GetProcessDefinitions'...", t);
        }

        LOGGER.exiting(this.getClass().getCanonicalName(), "run");
    }

}
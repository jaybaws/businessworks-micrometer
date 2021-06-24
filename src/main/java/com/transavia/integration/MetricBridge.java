package com.transavia.integration;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.graphite.GraphiteConfig;
import io.micrometer.graphite.GraphiteMeterRegistry;
import javax.management.*;
import javax.management.relation.MBeanServerNotificationFilter;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

public class MetricBridge implements NotificationListener {

    private static final int c_executorService_corePoolSize = 10;
    private static final String c_jvm_arg_graphiteHost = MetricBridge.class.getCanonicalName() + ".graphiteHost";
    private static final String c_jvm_arg_jmx = "Jmx.Enabled";

    private static final Logger LOGGER = Logger.getLogger(MetricBridge.class.getName());

    private final MBeanServerConnection server;
    private ObjectName engineHandle;
    private ScheduledExecutorService executorService;
    private MeterRegistry registry;

    @SuppressWarnings("unused")
    public static void premain(String agentArgs) {
        MetricBridge bridge = new MetricBridge();
    }

    public MetricBridge() {
        server = ManagementFactory.getPlatformMBeanServer();

        GraphiteConfig graphiteConfig = new GraphiteConfig() {
            @Override
            public String host() {
                return System.getProperty(c_jvm_arg_graphiteHost, "localhost");
            }

            @Override
            public String get(String k) {
                return null; // accept the rest of the defaults
            }
        };

        registry = new GraphiteMeterRegistry(graphiteConfig, Clock.SYSTEM, HierarchicalNameMapper.DEFAULT);

        MBeanServerNotificationFilter filter = new MBeanServerNotificationFilter();
        filter.enableAllObjectNames();

        try {
            // Register a listener for MBeans so we can pick up the bwengine's HMA
            server.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this, filter, null);

            // Force the bwengine to enable JMX, otherwise our plan dies in vain...
            System.setProperty(c_jvm_arg_jmx, "true");

        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Unable to register as an MBeanServer notification listener!", t);
        }
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        MBeanServerNotification mbs = (MBeanServerNotification) notification;

        if (mbs.getMBeanName().getDomain().equals("com.tibco.bw")) {
            if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(mbs.getType())) {
                LOGGER.info("Caught the bwengine's HMA MBean [" + mbs.getMBeanName() + "]");
                engineHandle = mbs.getMBeanName();

                executorService = Executors.newScheduledThreadPool(c_executorService_corePoolSize);

                executorService.scheduleWithFixedDelay(new GetActivitiesWorker(server, engineHandle, registry), 5, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetProcessStartersWorker(server, engineHandle, registry), 10, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetProcessDefinitionsWorker(server, engineHandle, registry), 15, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetMemoryUsageWorker(server, engineHandle, registry), 20, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetActiveProcessCountWorker(server, engineHandle, registry), 25, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetProcessCountWorker(server, engineHandle, registry), 30, 60, TimeUnit.SECONDS);

            } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(mbs.getType())) {
                LOGGER.info("Lost the bwengine's HMA MBean [" + mbs.getMBeanName() + "]");
                if (mbs.getMBeanName() == engineHandle) {
                    engineHandle = null;
                    executorService.shutdown();
                }
            }
        }
        // https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-in-process-agent#send-custom-metrics-using-micrometer
        // https://micrometer.io/docs/concepts#_global_registry
        // https://micrometer.io/docs/concepts#_gauges

    }
}
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
    private static final String c_jvm_arg_graphite_prefix = "com.transavia.integration";
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
        /**
         * Get the MBeanServer where the bwengine's MBean will be hosted. It will be in the default one (platformMBeanServer)
         */
        server = ManagementFactory.getPlatformMBeanServer();

        /**
         * Define the configuration to our Graphite back-end. We read from a JVM argument, but default to
         * 'localhost' if it is absent.
         */
        GraphiteConfig graphiteConfig = new GraphiteConfig() {

            @Override
            public String[] tagsAsPrefix() {
                return new String[] {
                        "store",
                        "environment",
                        "application",
                        "instance",
                        "method"
                };
            }

            @Override
            public String get(String k) {
                String key = c_jvm_arg_graphite_prefix + "." + k;
                String value = System.getProperty(key);
                LOGGER.info(String.format("GraphiteConfig queried property '%s'. Got value '%s'.", key, value));
                return value;
            }
        };

        /**
         * Construct the MicroMeter metric registry, based on the Graphite configuration.
         */
        registry = new GraphiteMeterRegistry(graphiteConfig, Clock.SYSTEM);

        registry.config().commonTags("store", "integration", "environment", "TST", "application", "Website_Adapter", "instance", "LB1");

        /**
         * Listen to new MBean's being registered.
         *
         * The assumption here is that our javaagent runs *BEFORE* the bwengine actually loads.
         * This means, that at the time of this execution, the MBean we wish to consume is not yet loaded.
         *
         * In order to 'catch' this bwengine MBean, we register a listener on the MBeanServer, and finalize.
         * This will cause the static 'premain()' method to complete, and the JVM-startup cycle will proceed.
         * Either, more javaagents will be loaded, and finally the BWEngine (PEMain) will start.
         *
         * One final note: this javaagent can only function if the BWEngine (PEMain) actually enables JMX.
         * To assure this is the case, we inject the 'Jmx.Enabled=true' JVM argument.
         *
         */
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

        /**
         * We only want to react to the BWEngine (PEMain) MBean, not anything else (like Tomcat MBeans, etc.).
         */
        if (mbs.getMBeanName().getDomain().equals("com.tibco.bw")) {
            if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(mbs.getType())) {
                LOGGER.info("Caught the bwengine's HMA MBean [" + mbs.getMBeanName() + "]");
                engineHandle = mbs.getMBeanName();

                /**
                 * Only construct the ScheduledExecutorService when needed. Also, we may need to recreate it if
                 * the MBean has been lost and (re)found, since it will be destroyed (shut-down) when it's lost.
                 */
                executorService = Executors.newScheduledThreadPool(c_executorService_corePoolSize);

                /**
                 * Schedule all our workers!
                 *
                 * Spread the CPU load by setting progressively increasing initlayDelay values.
                 *
                 */
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
    }
}

/*
 * Some more good reads:
 * https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-in-process-agent#send-custom-metrics-using-micrometer
 * https://micrometer.io/docs/concepts#_global_registry
 * https://micrometer.io/docs/concepts#_gauges
 */
package com.transavia.integration;
import com.transavia.integration.util.BWUtils;
import com.transavia.integration.workers.*;
import io.micrometer.azuremonitor.AzureMonitorConfig;
import io.micrometer.azuremonitor.AzureMonitorMeterRegistry;
import io.micrometer.core.instrument.*;
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

    private static final String c_jvm_arg_prefix = "com.transavia.integration";

    private static final String c_jvm_arg_registries = c_jvm_arg_prefix + ".registries";

    private static final String c_jvm_arg_logLevel = c_jvm_arg_prefix + ".logLevel";

    private static final String c_jvm_arg_bwengine_domain = c_jvm_arg_prefix + ".bwengine.domain";
    private static final String c_jvm_arg_bwengine_application = c_jvm_arg_prefix + ".bwengine.application";
    private static final String c_jvm_arg_bwengine_instance = c_jvm_arg_prefix + ".bwengine.instance";

    private static final String c_jvm_arg_graphite_metricPattern = c_jvm_arg_prefix + ".graphite.metricPattern";

    private static final String c_jvm_arg_graphite_metricPrefix = c_jvm_arg_prefix + ".graphite.prefix";
    private static final String c_jvm_arg_graphite_metricPrefix_defaultValue = "integration";

    private static final String c_jvm_arg_graphite_metricPattern_defaultValue = "prefix,domain,application,instance,method";

    private static final String c_jvm_arg_jmx = "Jmx.Enabled";

    private static final Logger LOGGER = Logger.getLogger(MetricBridge.class.getName());

    private final MBeanServerConnection server;
    private ObjectName engineHandle;
    private ScheduledExecutorService executorService;

    private static boolean isBusinessWorksEngine() {
        return true; // @TODO
    }

    private static boolean addGraphiteMeterRegistry() {
        return System.getProperty(c_jvm_arg_registries, "").contains("Graphite");
    }

    private static boolean addAzureMonitorMeterRegistry() {
        return System.getProperty(c_jvm_arg_registries, "").contains("AzureMonitor");
    }

    @SuppressWarnings("unused")
    public static void premain(String agentArgs) {
        if (isBusinessWorksEngine()) {
            LOGGER.info("Looks like a BusinessWorks engine, so instrumenting!");
            MetricBridge bridge = new MetricBridge();
        } else {
            LOGGER.info("Not a BusinessWorks engine. Exiting and not instrumenting...");
        }
    }

    public MetricBridge() {
        String level = System.getProperty(c_jvm_arg_logLevel, "INFO");
        LOGGER.setLevel(Level.parse(level));
        LOGGER.config(String.format("Set logLevel to '%s'.", level));

        /**
         * Get the MBeanServer where the bwengine's MBean will be hosted. It will be in the default one (platformMBeanServer)
         */
        server = ManagementFactory.getPlatformMBeanServer();

        /**
         * Determine the <prefix> from the JVM properties, and determine <domain>, <application>
         * and <instance> by reading the BWEngine's .tra file.
         */
        String engineAMIDisplayName = BWUtils.getAMIDisplayName();
        String[] instanceNameParts = engineAMIDisplayName.split("\\.");
        String domain = System.getProperty(c_jvm_arg_bwengine_domain, instanceNameParts[4]);
        String application = System.getProperty(c_jvm_arg_bwengine_application, instanceNameParts[5]);
        String instance = System.getProperty(c_jvm_arg_bwengine_instance, instanceNameParts[6]);

        LOGGER.info(String.format("BWEngine '%s' parsed as domain:%s, application:%s, instance:%s.", engineAMIDisplayName, domain, application, instance));

        /**
         * Construct the MicroMeter metric registry, based on the Graphite configuration.
         * Also, pass on the determined <prefix>, <domain>, <application> and <instance>.
         */


        /**
         * Set up the global (composite) Metric registry. Provide the global config generically.
         */
        Metrics.globalRegistry.config().commonTags(
                "domain", domain,
                "application", application,
                "instance", instance
        );

        // Metrics.addRegistry(new SimpleMeterRegistry());

        /**
         * Set up a GraphiteMeterRegistry, and add it to ghe global (composite) registry
         */
        if (addGraphiteMeterRegistry()) {
            /**
             * Define the configuration to our Graphite back-end. We read from a JVM argument, but default to
             * 'localhost' if it is absent.
             */
            GraphiteConfig graphiteConfig = new GraphiteConfig() {

                @Override
                public String[] tagsAsPrefix() {
                    String prop = System.getProperty(c_jvm_arg_graphite_metricPattern, c_jvm_arg_graphite_metricPattern_defaultValue);

                    LOGGER.config(String.format("tagsAsPrefix property set to '%s'.", prop));

                    return prop.split(",");
                }

                @Override
                public String get(String k) {
                    String key = c_jvm_arg_prefix + "." + k;
                    String value = System.getProperty(key);

                    LOGGER.config(String.format("GraphiteConfig queried for property '%s'. Returned value '%s'.", key, value));

                    return value;
                }
            };

            String prefix = System.getProperty(c_jvm_arg_graphite_metricPrefix, c_jvm_arg_graphite_metricPrefix_defaultValue);

            MeterRegistry graphiteRegistry = new GraphiteMeterRegistry(graphiteConfig, Clock.SYSTEM);
            graphiteRegistry.config().commonTags("prefix", prefix);
            Metrics.addRegistry(graphiteRegistry);
        }

        if (addAzureMonitorMeterRegistry()) {
            AzureMonitorConfig azureMonitorConfig = new AzureMonitorConfig() {
                @Override
                public String get(String k) {
                    String key = c_jvm_arg_prefix + "." + k;
                    String value = System.getProperty(key);

                    LOGGER.info(String.format("AzureMonitorConfig queried for property '%s'. Returned value '%s'.", key, value));

                    return value;
                }
            };

            MeterRegistry azureMonitorRegistry = new AzureMonitorMeterRegistry(azureMonitorConfig, Clock.SYSTEM);
            Metrics.addRegistry(azureMonitorRegistry);
        }

        LOGGER.config("Constructed and registered the metric registry");

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
            LOGGER.config("Programmatically enabled JMX on the BWEngine that's about to start.");

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
                executorService.scheduleWithFixedDelay(new GetMemoryUsageWorker(server, engineHandle), 5, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetProcessStartersWorker(server, engineHandle), 10, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetProcessDefinitionsWorker(server, engineHandle), 15, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetActivitiesWorker(server, engineHandle), 20, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetActiveProcessCountWorker(server, engineHandle), 25, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetProcessCountWorker(server, engineHandle), 30, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetExecInfoWorker(server, engineHandle), 35, 60, TimeUnit.SECONDS);
                LOGGER.info("Scheduled the workers!");

            } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(mbs.getType())) {
                LOGGER.warning("Lost the bwengine's HMA MBean [" + mbs.getMBeanName() + "]");
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
 *
 * https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-in-process-agent#send-custom-metrics-using-micrometer
 * https://micrometer.io/docs/concepts#_global_registry
 * https://micrometer.io/docs/concepts#_gauges
 *
 */
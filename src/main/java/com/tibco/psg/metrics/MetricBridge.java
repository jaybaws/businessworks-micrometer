package com.tibco.psg.metrics;
import com.tibco.psg.metrics.bridge.azure.logging.AzureLoggerBridge;
import com.tibco.psg.metrics.bridge.azure.metrics.workers.*;
import com.tibco.psg.metrics.util.BWUtils;
import io.micrometer.core.instrument.*;
import javax.management.*;
import javax.management.relation.MBeanServerNotificationFilter;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

public class MetricBridge implements NotificationListener {

    private static final String c_defaultLogLevel = "INFO";
    private static final String c_jvm_arg_jmx = "Jmx.Enabled";
    private static final int c_executorService_corePoolSize = 10;

    private static final String c_jvm_arg_prefix = MetricBridge.class.getPackage().getName();

    private static final String c_jvm_arg_logLevel = c_jvm_arg_prefix + ".logLevel";

    private static final String c_jvm_arg_bridgelogs = c_jvm_arg_prefix + ".bridgelogs";

    private static final String c_jvm_arg_bwengine_domain = c_jvm_arg_prefix + ".bwengine.domain";
    private static final String c_jvm_arg_bwengine_application = c_jvm_arg_prefix + ".bwengine.application";
    private static final String c_jvm_arg_bwengine_instance = c_jvm_arg_prefix + ".bwengine.instance";

    private static final Logger LOGGER = Logger.getLogger(MetricBridge.class.getName());

    private final MBeanServerConnection server;
    private ObjectName engineHandle;
    private ScheduledExecutorService executorService;

    private static boolean isBusinessWorksEngine() {
        return true; // @TODO
    }

    private static boolean doBridgeLogs() {
        String prop = System.getProperty(c_jvm_arg_bridgelogs, "false");
        boolean doIt = Boolean.valueOf(prop);
        LOGGER.info(String.format("doBridgeLogs (%s) is: %s.", c_jvm_arg_bridgelogs, prop));
        return doIt;
    }

    @SuppressWarnings("unused")
    public static void premain(String agentArgs) {
        if (isBusinessWorksEngine()) {
            LOGGER.info("Looks like a BusinessWorks engine, so instrumenting!");
            MetricBridge bridge = new MetricBridge();
            LOGGER.info("End of instrumentation!");
        } else {
            LOGGER.warning("Not a BusinessWorks engine. Exiting and not instrumenting...");
        }
    }

    public MetricBridge() {
        String level = System.getProperty(c_jvm_arg_logLevel, c_defaultLogLevel);
        LOGGER.setLevel(Level.parse(level));
        LOGGER.info(String.format("Set logLevel to '%s'.", level));

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
         * Set up the global (composite) Metric registry. Provide the global config generically.
         */
        Metrics.globalRegistry.config().commonTags(
                "domain", domain,
                "application", application,
                "instance", instance
        );

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
            LOGGER.info("Programmatically enabled JMX on the BWEngine that's about to start.");

        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Unable to register as an MBeanServer notification listener!", t);
        }

        if (doBridgeLogs()) {
            AzureLoggerBridge.instrument();
            LOGGER.info("Programmatically enabled bridging of logs.");
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
                 * Schedule all our workers! -- Spread the CPU load by setting progressively increasing initDelay values.
                 */
                executorService.scheduleWithFixedDelay(new GetExecInfoWorker(server, engineHandle), 5, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetMemoryUsageWorker(server, engineHandle), 10, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetProcessCountWorker(server, engineHandle), 15, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetActiveProcessCountWorker(server, engineHandle), 20, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetProcessStartersWorker(server, engineHandle), 25, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetProcessDefinitionsWorker(server, engineHandle), 30, 60, TimeUnit.SECONDS);
                executorService.scheduleWithFixedDelay(new GetActivitiesWorker(server, engineHandle), 35, 60, TimeUnit.SECONDS);

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
package com.tibco.psg.metrics.bridge.azure.logging;

public class AzureLoggerBridge {

    private static boolean isInstrumented = false;

    public static void instrument() {
        if (!isInstrumented) {
            doInstrument();
        }
    }

    private static void doInstrument() {
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("bw.logger");
        if (logger != null) {
            String appenderName = AzureLoggerBridge.class.getCanonicalName();
            if (logger.getAppender(appenderName) == null) {
                logger.addAppender(new BWLoggerAzureAppender());
                isInstrumented = true;
            }
        }

    }

}

package com.tibco.psg.metrics.bridge.azure.logging;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.util.logging.Logger;

public class BWLoggerAzureAppender extends ConsoleAppender {

    private static final Logger LOGGER = Logger.getLogger(BWLoggerAzureAppender.class.getName());

    @Override
    public void doAppend(LoggingEvent event) {
        System.out.println(event.toString()); // @TODO!
    }

}

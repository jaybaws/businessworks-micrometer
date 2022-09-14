package com.transavia.integration.util;
import java.io.FileInputStream;
import java.util.Properties;

public class BWUtils {

    private static final String cWrapperProp = "wrapper.tra.file";
    private static final String cDisplayNameProp = "Hawk.AMI.DisplayName";
    private static final String cDefaultDisplayName = "SampleDomain.SampleApplication.SampleInstance";

    public static String getAMIDisplayName() {
        try {
            String traFile = System.getProperty(cWrapperProp);
            Properties traProps = new Properties();
            traProps.load(new FileInputStream(traFile));
            String instanceName = traProps.getProperty(cDisplayNameProp);

            return instanceName;
        } catch (Throwable t) {
            return cDefaultDisplayName;
        }
    }
}
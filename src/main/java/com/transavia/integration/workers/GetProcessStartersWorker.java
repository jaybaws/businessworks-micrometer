package com.transavia.integration.workers;
import com.transavia.integration.MetricBridge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularDataSupport;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetProcessStartersWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MetricBridge.class.getName());

    private MBeanServerConnection mbsc;
    private ObjectName objectName;
    private Map<String, AtomicLong> metrics = new HashMap<String, AtomicLong>();

    public GetProcessStartersWorker(MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;
    }

    private AtomicLong metric(String processDefinitionName, String starterName, String metricName) {
        String uniqueId = processDefinitionName + "/" + starterName + "/" + metricName;

        AtomicLong m = metrics.get(uniqueId);
        if (m == null) {
            m = Metrics.gauge(
                    metricName,
                    Arrays.asList(
                            Tag.of("method", "GetProcessStarters"),
                            Tag.of("process", processDefinitionName),
                            Tag.of("activity", starterName)
                    ),
                    new AtomicLong(-1)
            );
            metrics.put(uniqueId, m);
        }

        return m;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");

        try {
            TabularDataSupport result = (TabularDataSupport) mbsc.invoke(objectName, "GetProcessStarters", null, null);

            if (result != null) {
                for (Object value : result.values()) {
                    CompositeDataSupport resultItem = (CompositeDataSupport) value;

                    String processDefinition = (String) resultItem.get("ProcessDef");

                    String starterName = (String) resultItem.get("Name");
                    String status = (String) resultItem.get("Status");

                    long valCompleted = (Integer) resultItem.get("Completed");
                    metric(processDefinition, starterName, "bwengine.starters.completed").set(valCompleted);

                    long valCreated = (Integer) resultItem.get("Created");
                    metric(processDefinition, starterName, "bwengine.starters.created").set(valCreated);

                    long valCreationRate = (Integer) resultItem.get("CreationRate");
                    metric(processDefinition, starterName, "bwengine.starters.creationrate").set(valCreationRate);

                    long valDuration = (Long) resultItem.get("Duration");
                    metric(processDefinition, starterName, "bwengine.starters.duration").set(valDuration);

                    long valRunning = (Integer) resultItem.get("Running");
                    metric(processDefinition, starterName, "bwengine.starters.running").set(valRunning);

                    long valStatus;
                    switch (status) {
                        /* @TODO: flow controlled? Are these values correct?
                           source: https://docs.tibco.com/pub/activematrix_businessworks/5.14.1/doc/pdf/TIB_BW_5.14.1_administration.pdf?id=4
                         */
                        case "INACTIVE":
                            valStatus = 0;
                            break;
                        case "READY":
                            valStatus = 1;
                            break;
                        case "ACTIVE":
                            valStatus = 2;
                            break;
                        default:
                            valStatus = -1;
                            break;
                    }
                    metric(processDefinition, starterName, "bwengine.starters.status").set(valStatus);

                    LOGGER.fine(
                            String.format(
                                    "[GetProcessStarters] completed=%d, created=%d, rate=%d, duration=%d, running=%d.",
                                    valCompleted,
                                    valCreated,
                                    valCreationRate,
                                    valDuration,
                                    valRunning
                            )
                    );
                }

            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Exception invoking 'GetProcessStarters'...", t);
        }

        LOGGER.exiting(this.getClass().getCanonicalName(), "run");
    }

}

/*

[AdapterCommon/Processes/Retrieve Resources.process] = javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(name=GetProcessStarters,items=((itemName=Checkpointed Start,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)),(itemName=Completed,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Created,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=CreationRate,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Duration,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=Name,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=ProcessDef,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Running,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Start time,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Status,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Tracing,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)))),contents={Checkpointed Start=false, Completed=0, Created=0, CreationRate=0, Duration=11034, Name=HTTP Receiver, ProcessDef=AdapterCommon/Processes/Retrieve Resources.process, Running=0, Start time=Jun 24, 2021 10:53:19 PM, Status=ACTIVE, Tracing=false})

[Services/FlyingBlue/FlyingBlue.V1.0.serviceagent] = javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(name=GetProcessStarters,items=((itemName=Checkpointed Start,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)),(itemName=Completed,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Created,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=CreationRate,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Duration,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=Name,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=ProcessDef,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Running,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Start time,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Status,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Tracing,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)))),contents={Checkpointed Start=false, Completed=0, Created=0, CreationRate=0, Duration=12492, Name=, ProcessDef=Services/FlyingBlue/FlyingBlue.V1.0.serviceagent, Running=0, Start time=Jun 24, 2021 10:53:17 PM, Status=ACTIVE, Tracing=false})

[Services/FlightMapper/FlightMapper.V1.0.serviceagent] = javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(name=GetProcessStarters,items=((itemName=Checkpointed Start,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)),(itemName=Completed,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Created,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=CreationRate,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Duration,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=Name,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=ProcessDef,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Running,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Start time,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Status,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Tracing,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)))),contents={Checkpointed Start=false, Completed=0, Created=0, CreationRate=0, Duration=12488, Name=, ProcessDef=Services/FlightMapper/FlightMapper.V1.0.serviceagent, Running=0, Start time=Jun 24, 2021 10:53:17 PM, Status=ACTIVE, Tracing=false})

[Services/KLMLogon/KLMLogon.serviceagent] = javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(name=GetProcessStarters,items=((itemName=Checkpointed Start,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)),(itemName=Completed,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Created,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=CreationRate,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Duration,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=Name,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=ProcessDef,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Running,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Start time,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Status,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Tracing,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)))),contents={Checkpointed Start=false, Completed=0, Created=0, CreationRate=0, Duration=12499, Name=, ProcessDef=Services/KLMLogon/KLMLogon.serviceagent, Running=0, Start time=Jun 24, 2021 10:53:17 PM, Status=ACTIVE, Tracing=false})

[builtinResource.serviceagent] = javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(name=GetProcessStarters,items=((itemName=Checkpointed Start,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)),(itemName=Completed,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Created,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=CreationRate,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Duration,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=Name,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=ProcessDef,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Running,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Start time,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Status,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Tracing,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)))),contents={Checkpointed Start=false, Completed=0, Created=0, CreationRate=0, Duration=12467, Name=, ProcessDef=builtinResource.serviceagent, Running=0, Start time=Jun 24, 2021 10:53:17 PM, Status=ACTIVE, Tracing=false})




 */
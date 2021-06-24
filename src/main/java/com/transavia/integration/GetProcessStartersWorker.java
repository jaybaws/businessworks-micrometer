package com.transavia.integration;
import io.micrometer.core.instrument.MeterRegistry;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.TabularDataSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetProcessStartersWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MetricBridge.class.getName());

    private MBeanServerConnection mbsc;
    private ObjectName objectName;
    private MeterRegistry registry;

    public GetProcessStartersWorker(MBeanServerConnection mbsc, ObjectName objectName, MeterRegistry registry) {
        this.mbsc = mbsc;
        this.objectName = objectName;
        this.registry = registry;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");

        try {
            TabularDataSupport result = (TabularDataSupport) mbsc.invoke(objectName, "GetProcessStarters", null, null);

            if (result != null) {
                // @TODO: process


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
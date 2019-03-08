# Spring Boot and Spring Integration with JMS

This is an example project that demonstrates utilisation of Spring Boot and Spring Integration frameworks to send, receive and process JMS messages with out-of-the-box features of both. This means that there is minimal code written and any written code is only for implementing business logic. This avoids writing mass code for technical logic such as infrastructure configuration, workflow orchestration, object lifecycles and relationship behaviour.

## Overview

This implementation is a Spring Boot application that boots up an in-memory Apache MQ message broker with 4 topics. This implementation can be altered or extended to integrate with an external message bus.   
Spring Integration is used as a layer to send and receive JMS messages and pass receiving messages' payload to specific channels for processing. Each channel has a dedicated queue of size 100 and a dedicated single executor (worker thread) to process items in the queue as they arrive. Exception handling is put into practice by adding the current item being processed back into the same queue for processing again later by a gateway. This application is therefore a multi-threaded application that operates asynchronously in an out of order fashion, adhering to the multi-producer single-consumer paradigm. 

With this approach, one can dynamically scale a process' throughput/performance/throttling with minimal configuration changes only, minimal resource impact and no coding or paradigm changes. Specific examples of this:

* Increment the number of executor threads of a channel (e.g. 1 to 5)
* Change the size of a queue (e.g. 100 to 200)
* Change the route of the payload 
* Introduce batching of messages and the size of the batch that goes through the workflow (e.g. from 1 to 1000 in one go)

## Diagram of application workflow

![Application Workflow Diagram](application-workflow.png)

## Walkthrough of application workflow and setup

The application workflow itself is simple. The application sends 100 messages to the 4 topics on the message bus and prints out the message once received. The 4 topics are:

1. `bond.jms.topic`
2. `future.jms.topic`
3. `option.jms.topic`
4. `swap.jms.topic`

The message generated, sent and printed out is a String that identifies what type of data it is appended by an index number. 

There are several files and packages in the source tree under the src/main/ root folder:

* **JmsAndSpringIntegrationApplication.java** : The main application class that will start Spring Boot, load the Spring Integration Framework configuration and send a message through the gateway.
* **messaging-configuration.xml** : The Spring Integration context file that defines all messaging aspects: JMS topics; messaging workflow and lifecycle of the application (sending, receiving and routing JMS messages to internal queues); executors and queue sizes for processing data; the definition of the components and relationship with each other; component activation and tying it all into place.
* **MessageDataProcessor.java** : The interface for processing the payload of the incoming JMS message
* **BondDataProcessor.java**, **OptionDataProcessor.java**, **FutureDataProcessor.java**, **SwapDataProcessor.java** : implementations of the **MessageDataProcessor** interface that line up with the respective topic names.
* **Gateway.java** : The interface representing the gateway that have methods to retry processing data for each respective instrument type (option, bond, future, swap). This is configured in the **messaging-configuration.xml** to add the data back the the processing queue of the processing channel.

### Instrument type configuration

Each instrument type has the following setup (in **messaging-configuration.xml**) :
1. A JMS topic in the format `<instrument_type>.jms.topic`
2. A JMS outbound adapter to send messages (`<jms:outbound-channel-adapter>`) and a JMS inbound adapter (`<jms:message-driven-channel-adapter>`) to listen for messages for the topic, extract its payload and send it to the relevant Spring Integration channel's queue.
3. A Spring Integration channel named in the format `send<instrument_type>Channel`, which binds to the respective `<jms:outbound-channel-adapter>` that acts as a an abstraction and facade, which will send a JMS message on the message bus with the respective instrument's topic name (as explained in point numbers 1,2)
3. A Spring Integration channel named in the format `<instrument_type>DataChannel` that the payload is passed to. The channel has a complementing executor named in the format `<instrument_type>DataExecutor` configured to have a `queue-size` of 100 and a `pool-size` of 1 (which is the number of threads for the executor)
4. A `<int:service-activator>` for each channel that will invoke the `processData` method of the instrument type's processor class (named in the format `<instrument_type>DataProcessor`) when an item appears in the channel's queue.
5. A Gateway method named in the format `retryProcessing<instrument_type>Data` that will pass the data to the back of the queue of that instrument type's Spring Integration channel (as explained in point number 3)

## Running the application
Run the `JmsAndSpringIntegrationApplication` class as a Java application. It will print out Spring Boot startup log messages and then print out the message that has been sent to the message bus, received by the listener, routed to the Spring Integration channel's queue and being processed by the executor. The printed output of the entire application run should turn out like the following:

```

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::       (v1.5.17.RELEASE)

2018-11-22 17:49:41 -INFO- [main] c.e.j.JmsAndSpringIntegrationApplication Starting JmsAndSpringIntegrationApplication on UKLONVD824791 with PID 31816 (C:\workspace\spring-boot-and-spring-integration-with-jms\target\classes started by ozbahak in C:\workspace\spring-boot-and-spring-integration-with-jms)
2018-11-22 17:49:41 -INFO- [main] c.e.j.JmsAndSpringIntegrationApplication No active profile set, falling back to default profiles: default
2018-11-22 17:49:41 -INFO- [main] o.s.c.a.AnnotationConfigApplicationContext Refreshing org.springframework.context.annotation.AnnotationConfigApplicationContext@516be40f: startup date [Thu Nov 22 17:49:41 GMT 2018]; root of context hierarchy
2018-11-22 17:49:41 -INFO- [main] o.s.b.f.x.XmlBeanDefinitionReader Loading XML bean definitions from URL [file:/C:/workspace/spring-boot-and-spring-integration-with-jms/target/classes/messaging-configuration.xml]
2018-11-22 17:49:41 -INFO- [main] o.s.i.c.IntegrationRegistrar No bean named 'integrationHeaderChannelRegistry' has been explicitly defined. Therefore, a default DefaultHeaderChannelRegistry will be created.
2018-11-22 17:49:41 -INFO- [main] o.s.i.c.DefaultConfiguringBeanFactoryPostProcessor No bean named 'errorChannel' has been explicitly defined. Therefore, a default PublishSubscribeChannel will be created.
2018-11-22 17:49:41 -INFO- [main] o.s.i.c.DefaultConfiguringBeanFactoryPostProcessor No bean named 'taskScheduler' has been explicitly defined. Therefore, a default ThreadPoolTaskScheduler will be created.
2018-11-22 17:49:42 -INFO- [main] o.s.c.s.PostProcessorRegistrationDelegate$BeanPostProcessorChecker Bean 'integrationGlobalProperties' of type [org.springframework.beans.factory.config.PropertiesFactoryBean] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)
2018-11-22 17:49:42 -INFO- [main] o.s.c.s.PostProcessorRegistrationDelegate$BeanPostProcessorChecker Bean 'integrationGlobalProperties' of type [java.util.Properties] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)
2018-11-22 17:49:42 -INFO- [main] o.s.s.c.ThreadPoolTaskScheduler Initializing ExecutorService 'taskScheduler'
2018-11-22 17:49:42 -INFO- [main] o.s.s.c.ThreadPoolTaskExecutor Initializing ExecutorService
2018-11-22 17:49:42 -INFO- [main] o.s.s.c.ThreadPoolTaskExecutor Initializing ExecutorService
2018-11-22 17:49:42 -INFO- [main] o.s.s.c.ThreadPoolTaskExecutor Initializing ExecutorService
2018-11-22 17:49:42 -INFO- [main] o.s.s.c.ThreadPoolTaskExecutor Initializing ExecutorService
2018-11-22 17:49:43 -INFO- [main] o.s.j.e.a.AnnotationMBeanExporter Registering beans for JMX exposure on startup
2018-11-22 17:49:43 -INFO- [main] o.s.c.s.DefaultLifecycleProcessor Starting beans in phase 0
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer Adding {jms:outbound-channel-adapter:optionJMSOutboundAdapter} as a subscriber to the 'sendOptionChannel' channel
2018-11-22 17:49:43 -INFO- [main] o.s.i.c.DirectChannel Channel 'application.sendOptionChannel' has 1 subscriber(s).
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer started optionJMSOutboundAdapter
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer Adding {jms:outbound-channel-adapter:bondJMSOutboundAdapter} as a subscriber to the 'sendBondChannel' channel
2018-11-22 17:49:43 -INFO- [main] o.s.i.c.DirectChannel Channel 'application.sendBondChannel' has 1 subscriber(s).
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer started bondJMSOutboundAdapter
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer Adding {jms:outbound-channel-adapter:futureJMSOutboundAdapter} as a subscriber to the 'sendFutureChannel' channel
2018-11-22 17:49:43 -INFO- [main] o.s.i.c.DirectChannel Channel 'application.sendFutureChannel' has 1 subscriber(s).
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer started futureJMSOutboundAdapter
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer Adding {jms:outbound-channel-adapter:swapJMSOutboundAdapter} as a subscriber to the 'sendSwapChannel' channel
2018-11-22 17:49:43 -INFO- [main] o.s.i.c.DirectChannel Channel 'application.sendSwapChannel' has 1 subscriber(s).
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer started swapJMSOutboundAdapter
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer Adding {service-activator} as a subscriber to the 'optionDataChannel' channel
2018-11-22 17:49:43 -INFO- [main] o.s.i.c.ExecutorChannel Channel 'application.optionDataChannel' has 1 subscriber(s).
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer started org.springframework.integration.config.ConsumerEndpointFactoryBean#0
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer Adding {service-activator} as a subscriber to the 'bondDataChannel' channel
2018-11-22 17:49:43 -INFO- [main] o.s.i.c.ExecutorChannel Channel 'application.bondDataChannel' has 1 subscriber(s).
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer started org.springframework.integration.config.ConsumerEndpointFactoryBean#1
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer Adding {service-activator} as a subscriber to the 'futureDataChannel' channel
2018-11-22 17:49:43 -INFO- [main] o.s.i.c.ExecutorChannel Channel 'application.futureDataChannel' has 1 subscriber(s).
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer started org.springframework.integration.config.ConsumerEndpointFactoryBean#2
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer Adding {service-activator} as a subscriber to the 'swapDataChannel' channel
2018-11-22 17:49:43 -INFO- [main] o.s.i.c.ExecutorChannel Channel 'application.swapDataChannel' has 1 subscriber(s).
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer started org.springframework.integration.config.ConsumerEndpointFactoryBean#3
2018-11-22 17:49:43 -INFO- [main] o.s.i.g.GatewayProxyFactoryBean$MethodInvocationGateway started gateway
2018-11-22 17:49:43 -INFO- [main] o.s.i.g.GatewayProxyFactoryBean$MethodInvocationGateway started gateway
2018-11-22 17:49:43 -INFO- [main] o.s.i.g.GatewayProxyFactoryBean$MethodInvocationGateway started gateway
2018-11-22 17:49:43 -INFO- [main] o.s.i.g.GatewayProxyFactoryBean$MethodInvocationGateway started gateway
2018-11-22 17:49:43 -INFO- [main] o.s.i.g.GatewayCompletableFutureProxyFactoryBean started gateway
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer Adding {logging-channel-adapter:_org.springframework.integration.errorLogger} as a subscriber to the 'errorChannel' channel
2018-11-22 17:49:43 -INFO- [main] o.s.i.c.PublishSubscribeChannel Channel 'application.errorChannel' has 1 subscriber(s).
2018-11-22 17:49:43 -INFO- [main] o.s.i.e.EventDrivenConsumer started _org.springframework.integration.errorLogger
2018-11-22 17:49:43 -INFO- [main] o.s.c.s.DefaultLifecycleProcessor Starting beans in phase 1073741823
2018-11-22 17:49:43 -INFO- [main] o.s.i.j.ChannelPublishingJmsMessageListener$GatewayDelegate started org.springframework.integration.jms.ChannelPublishingJmsMessageListener$GatewayDelegate@350ec41e
2018-11-22 17:49:43 -INFO- [main] o.a.a.b.BrokerService Using Persistence Adapter: MemoryPersistenceAdapter
2018-11-22 17:49:43 -INFO- [JMX connector] o.a.a.b.j.ManagementContext JMX consoles can connect to service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi
2018-11-22 17:49:43 -INFO- [main] o.a.a.b.BrokerService Apache ActiveMQ 5.14.5 (localhost, ID:UKLONVD824791-50657-1542908983134-0:1) is starting
2018-11-22 17:49:43 -INFO- [main] o.a.a.b.BrokerService Apache ActiveMQ 5.14.5 (localhost, ID:UKLONVD824791-50657-1542908983134-0:1) started
2018-11-22 17:49:43 -INFO- [main] o.a.a.b.BrokerService For help or more information please see: http://activemq.apache.org
2018-11-22 17:49:43 -WARN- [main] o.a.a.b.BrokerService Temporary Store limit is 51200 mb (current store usage is 0 mb). The data directory: C:\workspace\spring-boot-and-spring-integration-with-jms only has 12338 mb of usable space. - resetting to maximum available disk space: 12338 mb
2018-11-22 17:49:43 -INFO- [main] o.a.a.b.TransportConnector Connector vm://localhost started
2018-11-22 17:49:43 -INFO- [main] o.s.i.j.JmsMessageDrivenEndpoint started optionJMSInboundAdapter
2018-11-22 17:49:43 -INFO- [main] o.s.i.j.ChannelPublishingJmsMessageListener$GatewayDelegate started org.springframework.integration.jms.ChannelPublishingJmsMessageListener$GatewayDelegate@70fab835
2018-11-22 17:49:43 -INFO- [main] o.s.i.j.JmsMessageDrivenEndpoint started bondJMSInboundAdapter
2018-11-22 17:49:43 -INFO- [main] o.s.i.j.ChannelPublishingJmsMessageListener$GatewayDelegate started org.springframework.integration.jms.ChannelPublishingJmsMessageListener$GatewayDelegate@62417a16
2018-11-22 17:49:43 -INFO- [main] o.s.i.j.JmsMessageDrivenEndpoint started futureJMSInboundAdapter
2018-11-22 17:49:43 -INFO- [main] o.s.i.j.ChannelPublishingJmsMessageListener$GatewayDelegate started org.springframework.integration.jms.ChannelPublishingJmsMessageListener$GatewayDelegate@26be6ca7
2018-11-22 17:49:43 -INFO- [main] o.s.i.j.JmsMessageDrivenEndpoint started swapJMSInboundAdapter
2018-11-22 17:49:43 -INFO- [main] o.s.c.s.DefaultLifecycleProcessor Starting beans in phase 2147483647
2018-11-22 17:49:43 -INFO- [main] c.e.j.JmsAndSpringIntegrationApplication Started JmsAndSpringIntegrationApplication in 2.661 seconds (JVM running for 3.573)
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 1
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 1
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 1
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 1
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 2
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 2
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 2
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 2
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 3
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 3
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 3
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 3
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 4
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 4
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 4
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 4
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 5
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 5
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 5
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 5
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 6
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 6
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 6
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 6
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 7
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 7
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 7
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 7
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 8
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 8
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 8
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 8
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 9
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 9
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 9
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 9
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 10
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 10
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 10
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 10
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 11
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 11
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 11
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 11
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 12
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 12
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 12
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 12
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 13
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 13
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 13
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 13
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 14
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 14
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 14
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 14
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 15
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 15
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 15
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 15
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 16
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 16
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 16
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 16
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 17
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 17
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 17
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 17
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 18
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 18
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 18
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 18
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 19
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 19
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 19
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 19
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 20
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 20
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 20
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 20
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 21
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 21
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 21
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 21
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 22
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 22
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 22
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 22
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 23
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 23
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 23
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 23
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 24
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 24
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 24
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 24
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 25
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 25
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 25
2018-11-22 17:49:43 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 25
2018-11-22 17:49:43 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 26
2018-11-22 17:49:43 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 26
2018-11-22 17:49:43 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 26
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 26
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 27
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 27
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 27
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 27
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 28
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 28
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 28
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 28
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 29
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 29
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 29
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 29
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 30
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 30
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 30
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 30
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 31
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 31
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 31
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 31
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 32
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 32
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 32
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 32
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 33
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 33
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 33
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 33
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 34
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 34
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 34
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 34
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 35
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 35
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 35
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 35
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 36
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 36
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 36
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 36
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 37
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 37
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 37
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 37
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 38
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 38
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 38
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 38
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 39
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 39
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 39
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 39
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 40
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 40
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 40
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 40
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 41
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 41
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 41
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 41
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 42
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 42
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 42
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 42
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 43
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 43
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 43
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 43
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 44
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 44
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 44
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 44
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 45
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 45
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 45
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 45
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 46
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 46
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 46
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 46
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 47
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 47
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 47
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 47
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 48
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 48
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 48
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 48
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 49
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 49
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 49
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 49
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 50
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 50
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 50
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 50
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 51
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 51
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 51
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 51
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 52
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 52
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 52
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 52
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 53
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 53
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 53
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 53
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 54
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 54
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 54
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 54
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 55
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 55
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 55
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 55
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 56
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 56
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 56
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 56
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 57
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 57
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 57
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 57
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 58
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 58
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 58
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 58
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 59
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 59
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 59
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 59
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 60
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 60
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 60
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 60
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 61
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 61
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 61
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 61
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 62
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 62
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 62
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 62
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 63
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 63
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 63
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 63
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 64
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 64
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 64
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 64
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 65
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 65
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 65
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 65
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 66
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 66
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 66
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 66
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 67
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 67
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 67
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 67
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 68
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 68
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 68
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 68
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 69
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 69
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 69
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 69
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 70
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 70
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 70
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 70
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 71
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 71
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 71
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 71
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 72
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 72
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 72
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 72
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 73
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 73
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 73
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 73
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 74
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 74
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 74
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 74
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 75
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 75
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 75
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 75
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 76
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 76
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 76
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 76
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 77
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 77
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 77
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 77
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 78
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 78
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 78
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 78
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 79
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 79
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 79
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 79
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 80
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 80
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 80
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 80
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 81
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 81
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 81
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 81
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 82
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 82
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 82
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 82
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 83
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 83
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 83
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 83
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 84
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 84
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 84
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 84
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 85
2018-11-22 17:49:44 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 85
2018-11-22 17:49:44 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 85
2018-11-22 17:49:44 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 85
2018-11-22 17:49:44 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 86
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 86
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 86
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 86
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 87
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 87
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 87
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 87
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 88
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 88
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 88
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 88
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 89
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 89
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 89
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 89
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 90
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 90
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 90
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 90
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 91
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 91
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 91
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 91
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 92
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 92
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 92
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 92
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 93
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 93
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 93
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 93
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 94
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 94
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 94
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 94
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 95
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 95
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 95
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 95
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 96
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 96
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 96
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 96
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 97
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 97
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 97
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 97
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 98
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 98
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 98
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 98
2018-11-22 17:49:45 -INFO- [optionDataExecutor-1] c.e.j.m.c.OptionDataProcessor Retreived Message: OPTION DATA 99
2018-11-22 17:49:45 -INFO- [bondDataExecutor-1] c.e.j.m.c.BondDataProcessor Retreived Message: BOND DATA 99
2018-11-22 17:49:45 -INFO- [futureDataExecutor-1] c.e.j.m.c.FutureDataProcessor Retreived Message: FUTURE DATA 99
2018-11-22 17:49:45 -INFO- [swapDataExecutor-1] c.e.j.m.c.SwapDataProcessor Retreived Message: SWAP DATA 99
```



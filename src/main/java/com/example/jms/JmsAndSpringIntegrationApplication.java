package com.example.jms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;

@SpringBootApplication
@ImportResource("classpath*:messaging-configuration.xml")
@ComponentScan("com.example.jms")
public class JmsAndSpringIntegrationApplication {

	public static void main(String[] args){
		ConfigurableApplicationContext context = SpringApplication.run(JmsAndSpringIntegrationApplication.class, args);
		
		//MESSAGE BUS ACTIVITY BY SENDING MESSAGES TO ALL TOPICS
		MessageChannel greenChannel = context.getBean("sendGreenChannel", MessageChannel.class);
		MessageChannel redChannel = context.getBean("sendRedChannel", MessageChannel.class);
		MessageChannel blueChannel = context.getBean("sendBlueChannel", MessageChannel.class);
		MessageChannel blackChannel = context.getBean("sendBlackChannel", MessageChannel.class);
		
		for (int i= 1; i < 100; i ++)
		{
			greenChannel.send(MessageBuilder.withPayload("GREEN DATA " + i).build());
			redChannel.send(MessageBuilder.withPayload("RED DATA " + i).build());
			blueChannel.send(MessageBuilder.withPayload("BLUE DATA " + i).build());
			blackChannel.send(MessageBuilder.withPayload("BLACK DATA " + i).build());
		}
		
		context.stop();
		context.close();
		
	}
}

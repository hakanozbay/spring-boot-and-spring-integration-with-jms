package com.example.jms;

import javax.jms.JMSException;

import org.springframework.beans.BeansException;
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

	public static void main(String[] args) throws BeansException, JMSException {
		ConfigurableApplicationContext context = SpringApplication.run(JmsAndSpringIntegrationApplication.class, args);
		
		//MESSAGE BUS ACTIVITY BY SENDING MESSAGES TO ALL TOPICS
		MessageChannel optionChannel = context.getBean("sendOptionChannel", MessageChannel.class);
		MessageChannel bondChannel = context.getBean("sendBondChannel", MessageChannel.class);
		MessageChannel futureChannel = context.getBean("sendFutureChannel", MessageChannel.class);
		MessageChannel swapChannel = context.getBean("sendSwapChannel", MessageChannel.class);
		
		for (int i= 1; i < 100; i ++)
		{
			optionChannel.send(MessageBuilder.withPayload("OPTION DATA " + i).build());
			bondChannel.send(MessageBuilder.withPayload("BOND DATA " + i).build());
			futureChannel.send(MessageBuilder.withPayload("FUTURE DATA " + i).build());
			swapChannel.send(MessageBuilder.withPayload("SWAP DATA " + i).build());
		}
		
	}
}

package com.example.jms.message.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.jms.message.gateway.Gateway;

@Component
public class RedDataProcessor implements MessageDataProcessor {

	@Autowired
	Gateway gateway;

	private static final Logger log = LoggerFactory.getLogger(RedDataProcessor.class);

	public void processData (String data)
	{
		try
		{
			log.info("Retrieved Message: {}",data);
		}
		catch (Exception e) 
		{
			log.error("An error has occured while processing data. Will retry", e);
			gateway.retryProcessingRedData(data);
		}
	}
}

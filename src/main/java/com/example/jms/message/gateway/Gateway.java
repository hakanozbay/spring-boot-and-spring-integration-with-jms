package com.example.jms.message.gateway;

public interface Gateway {

	public void retryProcessingGreenData(String data);
	public void retryProcessingRedData(String data);
	public void retryProcessingBlueData(String data);
	public void retryProcessingBlackData(String data);
}

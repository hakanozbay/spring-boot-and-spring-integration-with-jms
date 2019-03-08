package com.example.jms.message.gateway;

public interface Gateway {

	public void retryProcessingOptionData(String data);
	public void retryProcessingBondData(String data);
	public void retryProcessingFutureData(String data);
	public void retryProcessingSwapData(String data);
}

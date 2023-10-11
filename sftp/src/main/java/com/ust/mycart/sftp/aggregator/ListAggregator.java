package com.ust.mycart.sftp.aggregator;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import com.fasterxml.jackson.databind.JsonNode;

public class ListAggregator implements AggregationStrategy{

	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		
		if(oldExchange == null) {
			List<JsonNode> response = new ArrayList<>();
			JsonNode jsonResponse = newExchange.getProperty("messagebody",JsonNode.class);
			if(jsonResponse != null) {
				response.add(jsonResponse);
			}
			newExchange.setProperty("filteredlist", response);
			newExchange.getIn().setBody(response);
			return newExchange;
		}
		
		@SuppressWarnings("unchecked")
		List<JsonNode> response = oldExchange.getProperty("filteredlist",List.class);
		JsonNode jsonResponse = newExchange.getProperty("messagebody",JsonNode.class);
		if(jsonResponse != null) {
			response.add(jsonResponse);
		}
		oldExchange.setProperty("filteredlist", response);
		oldExchange.getIn().setBody(response);
		return oldExchange;
	
	}

}

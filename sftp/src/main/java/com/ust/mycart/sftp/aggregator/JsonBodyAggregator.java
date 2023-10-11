package com.ust.mycart.sftp.aggregator;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import com.ust.mycart.sftp.entity.JsonResponse;

public class JsonBodyAggregator implements AggregationStrategy{

	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		// TODO Auto-generated method stub
		
		if(oldExchange == null) {
			List<JsonResponse> response = new ArrayList<>();
			JsonResponse jsonMessage = newExchange.getProperty("jsonmessage",JsonResponse.class);
			response.add(jsonMessage);
			newExchange.setProperty("listjsonbody", response);
			newExchange.getIn().setBody(response);
			return newExchange;
		}
		
		@SuppressWarnings("unchecked")
		List<JsonResponse> response = oldExchange.getProperty("listjsonbody",List.class);
		JsonResponse jsonMessage = newExchange.getProperty("jsonmessage",JsonResponse.class);
		response.add(jsonMessage);
		oldExchange.setProperty("listjsonbody",response);
		oldExchange.getIn().setBody(response);
		
		return oldExchange;
		
	}

}

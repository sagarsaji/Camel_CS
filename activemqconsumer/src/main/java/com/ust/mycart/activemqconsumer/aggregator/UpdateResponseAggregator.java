package com.ust.mycart.activemqconsumer.aggregator;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

public class UpdateResponseAggregator implements AggregationStrategy {

	@SuppressWarnings("unchecked")
	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

		List<String> workingIds;
		String response;

		if (oldExchange == null) {

			workingIds = new ArrayList<>();
			String id = newExchange.getProperty("workingId", String.class);

			if (id != null)
				workingIds.add(id);

			response = "Details updated for id : " + workingIds;
			newExchange.setProperty("response", response);
			newExchange.setProperty("ids", workingIds);
			newExchange.getIn().setBody(response);
			return newExchange;

		}

		workingIds = oldExchange.getProperty("ids", List.class);
		response = oldExchange.getProperty("response", String.class);
		String id = newExchange.getProperty("workingId", String.class);

		if (id != null)
			workingIds.add(id);

		response = "Details updated for id : " + workingIds;
		oldExchange.setProperty("response", response);
		oldExchange.setProperty("ids", workingIds);
		oldExchange.getIn().setBody(response);
		return oldExchange;

	}

}

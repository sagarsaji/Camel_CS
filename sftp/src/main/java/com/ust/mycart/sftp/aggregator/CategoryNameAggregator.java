package com.ust.mycart.sftp.aggregator;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

public class CategoryNameAggregator implements AggregationStrategy {

	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

		if (oldExchange == null) {

			Map<String, String> categoryMap = new HashMap<>();
			String categoryId = newExchange.getProperty("categoryid", String.class);
			String categoryName = newExchange.getProperty("categoryname", String.class);
			categoryMap.put(categoryId, categoryName);
			newExchange.setProperty("categoryMap", categoryMap);
			return newExchange;

		}

		@SuppressWarnings("unchecked")
		Map<String, String> categoryMap = oldExchange.getProperty("categoryMap", Map.class);
		String categoryId = newExchange.getProperty("categoryid", String.class);
		String categoryName = newExchange.getProperty("categoryname", String.class);
		categoryMap.put(categoryId, categoryName);
		oldExchange.setProperty("categoryMap", categoryMap);
		return oldExchange;

	}

}

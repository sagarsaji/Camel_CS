package com.ust.mycart.sftp.aggregator;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import com.ust.mycart.sftp.review.ItemReview;
import com.ust.mycart.sftp.review.Reviews;

public class ReviewXmlAggregator implements AggregationStrategy {

	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		// TODO Auto-generated method stub

		Reviews review;

		if (oldExchange == null) {
			review = new Reviews();
			List<ItemReview> items = new ArrayList<>();
			ItemReview item = newExchange.getProperty("items", ItemReview.class);
			items.add(item);
			review.setItems(items);
			newExchange.setProperty("reviews", review);
			newExchange.getIn().setBody(review);
			return newExchange;
		}

		review = oldExchange.getProperty("reviews", Reviews.class);
		ItemReview item = newExchange.getProperty("items", ItemReview.class);
		review.getItems().add(item);
		oldExchange.setProperty("reviews", review);
		oldExchange.getIn().setBody(review);
		return oldExchange;

	}

}

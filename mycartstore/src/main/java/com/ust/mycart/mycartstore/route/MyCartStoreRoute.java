package com.ust.mycart.mycartstore.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class MyCartStoreRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		// Handled exceptions here
		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.setBody(simple("{\"message\":\"${exception.message}\"} "));

		// REST Entry points
		rest()
				// API to fetch item by item id from previous GET service
				.get("/item/{_id}").to("direct:getByItemId")

				// API to fetch item by category id from previous GET service
				.get("/category/{category_id}").to("direct:getByCategoryId");

		// Route to fetch item by item id from previous GET service
		from("direct:getByItemId")
				.toD("{{camel.getByItemId}}/${header._id}?bridgeEndpoint=true&throwExceptionOnFailure=false")
				.log(LoggingLevel.DEBUG, "Message received : ${body}").log(LoggingLevel.INFO, "item fetched");

		// Route to fetch item by category id from previous GET service
		from("direct:getByCategoryId").toD(
				"{{camel.getByCategoryId}}/${header.category_id}?bridgeEndpoint=true&throwExceptionOnFailure=false")
				.log(LoggingLevel.DEBUG, "Message received : ${body}").log(LoggingLevel.INFO, "item fetched");

	}

}

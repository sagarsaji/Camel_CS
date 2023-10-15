package com.ust.mycart.activemqproducer.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ProducerRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		// Handled exception here
		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.setBody(constant("{\"message\":\"{{server.internalServerError}}\"}"));

		// REST Entry Point
		rest()
				// API to update item and send to activeMQ
				.put("/item").to("direct:updateItem");

		// Route that sends message to the activeMQ
		from("direct:updateItem").routeId("producerRoute").log(LoggingLevel.DEBUG, "Received message : ${body}")
				.log(LoggingLevel.INFO, "Message sending to activeMQ").unmarshal().json()
				.to("activemq:queue:updateItemQueue").setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

	}

}

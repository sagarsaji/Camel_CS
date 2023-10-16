package com.ust.mycart.activemqproducer.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ProducerRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		/**
		 * Global Exception Throwable.class handled here
		 */
		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.setBody(constant("{\"message\":\"{{server.internalServerError}}\"}"));

		/**
		 * REST Entry Point
		 */
		rest()
				/**
				 * API to send the message to activeMQ and then updates the item
				 */
				.put("/item").to("direct:updateItem");

		/**
		 * Req 2: API Route that sends message to activeMQ and then updates the item
		 */
		from("direct:updateItem").routeId("producerRoute").log(LoggingLevel.DEBUG, "Received message : ${body}")
				.log(LoggingLevel.INFO, "Message sending to activeMQ").unmarshal().json()
				.to("activemq:queue:updateItemQueue?requestTimeout=60000")
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
				.log(LoggingLevel.INFO, "Message send successfully...");

	}

}

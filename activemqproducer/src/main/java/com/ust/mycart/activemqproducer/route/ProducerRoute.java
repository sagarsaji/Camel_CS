package com.ust.mycart.activemqproducer.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.ust.mycart.activemqproducer.constant.ApplicationConstant;
import com.ust.mycart.activemqproducer.constant.ConstantClass;

@Component
public class ProducerRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		/**
		 * Global Exception Throwable.class handled here
		 */
		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.setBody(constant("{\"message\":\"{{server.internalServerError}}\"}"))
				.log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * REST Entry Point
		 */
		rest()
				/**
				 * API to send the message to activeMQ and then updates the item
				 */
				.put("/item").to(ApplicationConstant.UPDATE_ITEM);

		/**
		 * Req 2: API Route that sends message to activeMQ and then updates the item
		 */
		from(ApplicationConstant.UPDATE_ITEM).routeId(ConstantClass.PRODUCER_ROUTE)
				.log(LoggingLevel.DEBUG, "Received message : ${body}")
				.log(LoggingLevel.INFO, "Message sending to activeMQ").unmarshal().json()
				.to("activemq:queue:updateItemQueue?requestTimeout=60000")
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
				.setBody(constant("{\"message\":\"{{producerRoute.response}}\"}"));

	}

}

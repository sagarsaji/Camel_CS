package com.ust.mycart.activemqconsumer.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ust.mycart.activemqconsumer.aggregator.UpdateResponseAggregator;
import com.ust.mycart.activemqconsumer.headers.HeaderClass;
import com.ust.mycart.activemqconsumer.processor.StockUpdationProcessor;

@Component
public class ConsumerRoute extends RouteBuilder {

	@Value("${camel.mongodb.database}")
	private String database;

	@Value("${camel.mongodb.collection1}")
	private String item;

	@Override
	public void configure() throws Exception {

		// Handled exception here
		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setBody(constant("{{server.internalServerError}}"));

		from("direct:updateItemPropertyAdding").setProperty("soldout", simple("${body[stockDetails][soldOut]}"))
				.setProperty("damaged", simple("${body[stockDetails][damaged]}"));

		from("direct:findByItemId").setBody(header("itemid"))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=findById").choice()
				.when(body().isNull()).log(LoggingLevel.INFO, "Item not found for id : ${header.itemid}").otherwise()
				.log(LoggingLevel.INFO, "Item found for id : ${header.itemid}").end();

		from("direct:updateItemInDb")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=save");

		// Route that consumes message from activeMQ and does the update operation
		from("activemq:queue:updateItemQueue").log(LoggingLevel.INFO, "Message received from ActiveMQ")
				.split(simple("${body[items]}"), new UpdateResponseAggregator()).to("direct:updateItemPropertyAdding")
				.setHeader("itemid", simple("${body[_id]}")).to("direct:findByItemId")
				.setProperty("availablestock", simple("${body[stockDetails][availableStock]}"))
				.process(new StockUpdationProcessor()).to("direct:updateItemInDb")
				.setProperty("workingId", simple("${header.itemid}")).end()
				.setHeader(HeaderClass.CAMEL_HTTP_RESPONSE_CODE, constant(200));

	}

}

package com.ust.mycart.activemqconsumer.route;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mongodb.CamelMongoDbException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ust.mycart.activemqconsumer.aggregator.UpdateResponseAggregator;
import com.ust.mycart.activemqconsumer.processor.StockUpdationProcessor;

@Component
public class ConsumerRoute extends RouteBuilder {

	@Value("${camel.mongodb.database}")
	private String database;

	@Value("${camel.mongodb.collection1}")
	private String item;

	@Value("${camel.maximumRedeliveries}")
	private int maximumRedeliveries;

	@Value("${camel.redeliveryDelay}")
	private int redeliveryDelay;

	@Value("${camel.backOffMultiplier}")
	private int backOffMultiplier;

	@Override
	public void configure() throws Exception {

		/**
		 * Req 6: MongoDbException handled here: time =
		 * redeliveryDelay*(backOffMultiplier^(retryAttempt - 1))
		 */
		onException(CamelMongoDbException.class).routeId("CamelMongoDbException")
				.maximumRedeliveries(maximumRedeliveries).redeliveryDelay(redeliveryDelay)
				.backOffMultiplier(backOffMultiplier).useExponentialBackOff().handled(true)
				.log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * Global Exception Throwable.class handled here
		 */
		onException(Throwable.class).handled(true).log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * Route that consumes message from activeMQ and does the update operation
		 */
		from("activemq:queue:updateItemQueue").log(LoggingLevel.INFO, "Message received from ActiveMQ")
				.split(simple("${body[items]}"), new UpdateResponseAggregator()).to("direct:updateItemPropertyAdding")
				.setHeader("itemid", simple("${body[_id]}")).to("direct:findByItemId")
				.setProperty("availablestock", simple("${body[stockDetails][availableStock]}"))
				.process(new StockUpdationProcessor()).to("direct:updateItemInDb")
				.setProperty("workingId", header("itemid")).end();

		/**
		 * Route to assign properties for soldout and damaged
		 */
		from("direct:updateItemPropertyAdding").setProperty("soldout", simple("${body[stockDetails][soldOut]}"))
				.setProperty("damaged", simple("${body[stockDetails][damaged]}"));

		/**
		 * Route to check if the entered item id is present in the item collection or
		 * not
		 */
		from("direct:findByItemId").setBody(header("itemid"))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=findById").choice()
				.when(body().isNull()).log(LoggingLevel.INFO, "Item not found for id : ${header.itemid}").otherwise()
				.log(LoggingLevel.INFO, "Item found for id : ${header.itemid}").end();

		/**
		 * Route which invokes MongoDB operation to update the item in the item
		 * collection
		 */
		from("direct:updateItemInDb")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=save");

	}

}

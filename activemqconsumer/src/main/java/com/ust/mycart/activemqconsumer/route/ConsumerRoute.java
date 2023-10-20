package com.ust.mycart.activemqconsumer.route;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mongodb.CamelMongoDbException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ust.mycart.activemqconsumer.constant.ApplicationConstant;
import com.ust.mycart.activemqconsumer.constant.ConstantClass;
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
		 * Req 2: Route that consumes message from activeMQ and does the update
		 * operation
		 */
		from("activemq:queue:updateItemQueue?jmsMessageType=text").routeId(ConstantClass.CONSUMER_ROUTE)
				.log(LoggingLevel.DEBUG, "Message received from ActiveMQ : ${body}").split(simple("${body[items]}"))
				.to(ApplicationConstant.UPDATE_ITEM_PROPERTY_ASSIGNING)
				.setHeader(ConstantClass.ITEM_ID, simple("${body[_id]}")).to(ApplicationConstant.FIND_BY_ITEM_ID)
				.setProperty(ConstantClass.AVAILABLE_STOCK, simple("${body[stockDetails][availableStock]}"))
				.process(new StockUpdationProcessor()).to(ApplicationConstant.UPDATE_ITEM_IN_DB).end();

		/**
		 * Route to assign properties for soldout and damaged
		 */
		from(ApplicationConstant.UPDATE_ITEM_PROPERTY_ASSIGNING)
				.routeId(ApplicationConstant.UPDATE_ITEM_PROPERTY_ASSIGNING)
				.setProperty(ConstantClass.SOLDOUT, simple("${body[stockDetails][soldOut]}"))
				.setProperty(ConstantClass.DAMAGED, simple("${body[stockDetails][damaged]}"));

		/**
		 * Route to check if the entered item id is present in the item collection or
		 * not
		 */
		from(ApplicationConstant.FIND_BY_ITEM_ID).routeId(ApplicationConstant.FIND_BY_ITEM_ID)
				.setBody(header(ConstantClass.ITEM_ID))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=findById").choice()
				.when(body().isNull()).log(LoggingLevel.ERROR, "Item not found for id : ${header.itemid}").stop()
				.otherwise().log(LoggingLevel.INFO, "Item found for id : ${header.itemid}").end();

		/**
		 * Route which invokes MongoDB operation to update the item in the item
		 * collection
		 */
		from(ApplicationConstant.UPDATE_ITEM_IN_DB).routeId(ApplicationConstant.UPDATE_ITEM_IN_DB)
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=save");

	}

}

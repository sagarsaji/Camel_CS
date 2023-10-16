package com.ust.mycart.item.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mongodb.CamelMongoDbException;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ust.mycart.item.aggregator.UpdateResponseAggregator;
import com.ust.mycart.item.bean.ItemBean;
import com.ust.mycart.item.entity.Item;

import com.ust.mycart.item.exception.ItemException;

@Component
public class ItemRoute extends RouteBuilder {

	@Value("${camel.maximumRedeliveries}")
	private int maximumRedeliveries;

	@Value("${camel.redeliveryDelay}")
	private int redeliveryDelay;

	@Value("${camel.backOffMultiplier}")
	private int backOffMultiplier;

	@Value("${camel.mongodb.database}")
	private String database;

	@Value("${camel.mongodb.collection1}")
	private String item;

	@Value("${camel.mongodb.collection2}")
	private String category;

	@Autowired
	private ItemBean itemBean;

	@Override
	public void configure() throws Exception {

		/**
		 * All Exceptions handled here
		 */
		onException(ItemException.class).handled(true).setHeader(Exchange.CONTENT_TYPE, constant("application/json"));

		/**
		 * Req 6: MongoDbException handled here: time =
		 * redeliveryDelay*(backOffMultiplier^(retryAttempt - 1))
		 */
//		onException(CamelMongoDbException.class).log(LoggingLevel.INFO, "Mongo Retry...").routeId("CamelMongoDbException")
//				.maximumRedeliveries(maximumRedeliveries).redeliveryDelay(redeliveryDelay)
//				.backOffMultiplier(backOffMultiplier).useExponentialBackOff()
//				.handled(true).log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * Global Exception Throwable.class handled here
		 */
		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.setBody(simple("{\"message\":\"${exception.message}\"}"));

		/**
		 * REST entry points
		 */
		rest()
				/**
				 * API to access details by item id
				 */
				.get("/items/{_id}").to("direct:getItemsById")

				/**
				 * API to access details by category id and also accepts a filter named
				 * includeSpecial
				 */
				.get("/category/{category_id}").to("direct:getByCategoryId")

				/**
				 * API to add an item
				 */
				.post("/item").to("direct:addItems")

				/**
				 * API to update an item
				 */
				.put("/item").to("direct:updateItems");

		/**
		 * Req 1 sub 1: API Route to access details by item id
		 */
		from("direct:getItemsById").routeId("getItemsById").to("direct:findByItemId").marshal().json().unmarshal()
				.json(JsonLibrary.Jackson, Item.class).setProperty("messagebody", body())
				.setHeader("category_id", simple("${body.categoryId}")).to("direct:findByCategoryId")
				.bean(itemBean, "postResponse").marshal().json().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
				.log(LoggingLevel.INFO, "Item fetched from database");

		/**
		 * Route to check if the entered item id is present in the item collection or
		 * not
		 */
		from("direct:findByItemId").setBody(header("_id"))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=findById").choice()
				.when(body().isNull()).log(LoggingLevel.INFO, "Item not found for id : ${header._id}")
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
				.setBody(constant("{\"message\":\"{{error.itemNotFound}}\"}"))
				.throwException(new ItemException("not found")).otherwise()
				.log(LoggingLevel.INFO, "Item found for id : ${header._id}").end();

		/**
		 * Route to check if the entered category id is present in the category
		 * collection or not
		 */
		from("direct:findByCategoryId").setBody(header("category_id"))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + category + "&operation=findById")
				.choice().when(body().isNull()).log(LoggingLevel.INFO, "Category ${header.category_id} not found")
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
				.setBody(constant("{\"message\":\"{{error.categoryNotFound}}\"}"))
				.throwException(new ItemException("not found")).otherwise()
				.setProperty("categoryname", simple("${body[categoryName]}"))
				.setProperty("categorydept", simple("${body[categoryDep]}")).end();

		/**
		 * Req 1 sub 2: API Route to access details by category id and also accepts a
		 * filter named includeSpecial
		 */
		from("direct:getByCategoryId").routeId("getByCategoryId").to("direct:findByCategoryId")
				.to("direct:includeSpecial").to("direct:findAllItems").bean(itemBean, "categoryResponse").marshal()
				.json().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
				.log(LoggingLevel.INFO, "Details fetched from database");

		/**
		 * Route to check includeSpecial query parameter condition
		 */
		from("direct:includeSpecial").choice().when(header("includeSpecial").isEqualTo("false"))
				.setHeader(MongoDbConstants.CRITERIA,
						simple("{\"categoryId\": '${header.category_id}',\"specialProduct\": false}"))
				.otherwise().setHeader(MongoDbConstants.CRITERIA, simple("{\"categoryId\": '${header.category_id}'}"))
				.end();

		/**
		 * Route which invokes MongoDB operation to fetch all the items from the item
		 * collection
		 */
		from("direct:findAllItems")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=findAll");

		/**
		 * Req 1 sub 3: API Route to add an item
		 */
		from("direct:addItems").routeId("addItems").unmarshal().json(JsonLibrary.Jackson, Item.class)
				.to("bean-validator:validate").to("direct:addItemPropertyAssigning").to("direct:findByCategoryId")
				.bean(itemBean, "dateAdding").to("direct:insertItemIntoDb").bean(itemBean, "postResponse").marshal()
				.json().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
				.log(LoggingLevel.INFO, "Item inserted into database");

		/**
		 * Route to assign properties for Base Price, Selling Price and the list data
		 */
		from("direct:addItemPropertyAssigning").setHeader("category_id", simple("${body.categoryId}"))
				.setProperty("baseprice", simple("${body.itemPrice.basePrice}"))
				.setProperty("sellingprice", simple("${body.itemPrice.sellingPrice}"))
				.setProperty("messagebody", body());

		/**
		 * Route which invokes MongoDB operation to insert a new item into the item
		 * collection
		 */
		from("direct:insertItemIntoDb")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=insert");

		/**
		 * Req 1 sub 4: API Route to update an item
		 */
		from("direct:updateItems").routeId("updateItems")
				.split(simple("${body[items]}"), new UpdateResponseAggregator())
				.to("direct:updateItemPropertyAssigning").setHeader("_id", simple("${body[_id]}"))
				.to("direct:findByItemId")
				.setProperty("availablestock", simple("${body[stockDetails][availableStock]}"))
				.bean(itemBean, "stockUpdation").to("direct:updateItemInDb").setProperty("workingId", header("_id"))
				.end().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

		/**
		 * Route to assign properties for soldout and damaged
		 */
		from("direct:updateItemPropertyAssigning").setProperty("soldout", simple("${body[stockDetails][soldOut]}"))
				.setProperty("damaged", simple("${body[stockDetails][damaged]}"));

		/**
		 * Route which invokes MongoDB operation to update the item in the item
		 * collection
		 */
		from("direct:updateItemInDb")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=save");

	}

}

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

		// Handled exception here
		onException(ItemException.class).handled(true).setHeader(Exchange.CONTENT_TYPE, constant("application/json"));

		// Req 6 MongoDbException handled
		onException(CamelMongoDbException.class).log(LoggingLevel.INFO, "Mongo Retry...")
				.maximumRedeliveries(maximumRedeliveries).redeliveryDelay(redeliveryDelay)
				.backOffMultiplier(backOffMultiplier).handled(true)
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.setBody(constant("{\"message\":\"{{error.camelMongoDbException}}\"}"));

		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.setBody(simple("{\"message\":\"${exception.message}\"}"));

		// REST entry points
		rest()
				// API to access item by item id
				.get("/items/{_id}").to("direct:getItemsById")

				// API to access item by category id and include a filter
				.get("/category/{category_id}").to("direct:getByCategoryId")

				// API to add an item
				.post("/item").to("direct:addItems")

				// API to update an item
				.put("/item").to("direct:updateItems");

		from("direct:findByItemId").setBody(header("_id"))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=findById").choice()
				.when(body().isNull()).log(LoggingLevel.INFO, "Item not found for id : ${header._id}")
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
				.setBody(constant("{\"message\":\"{{error.itemNotFound}}\"}"))
				.throwException(new ItemException("not found")).otherwise()
				.log(LoggingLevel.INFO, "Item found for id : ${header._id}").end();

		from("direct:findByCategoryId").setBody(header("category_id"))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + category + "&operation=findById")
				.choice().when(body().isNull()).log(LoggingLevel.INFO, "Category ${header.category_id} not found")
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
				.setBody(constant("{\"message\":\"{{error.categoryNotFound}}\"}"))
				.throwException(new ItemException("not found")).otherwise()
				.setProperty("categoryname", simple("${body[categoryName]}"))
				.setProperty("categorydept", simple("${body[categoryDep]}")).end();

		from("direct:includeSpecial").choice().when(header("includeSpecial").isEqualTo("false"))
				.setHeader(MongoDbConstants.CRITERIA,
						simple("{\"categoryId\": '${header.category_id}',\"specialProduct\": false}"))
				.otherwise().setHeader(MongoDbConstants.CRITERIA, simple("{\"categoryId\": '${header.category_id}'}"))
				.end();

		from("direct:findAllItems")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=findAll");

		from("direct:addItemPropertyAssigning").setHeader("category_id", simple("${body.categoryId}"))
				.setProperty("baseprice", simple("${body.itemPrice.basePrice}"))
				.setProperty("sellingprice", simple("${body.itemPrice.sellingPrice}"))
				.setProperty("messagebody", body());

		from("direct:insertItemIntoDb")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=insert");

		from("direct:updateItemInDb")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=save");

		from("direct:updateItemPropertyAssigning").setProperty("soldout", simple("${body[stockDetails][soldOut]}"))
				.setProperty("damaged", simple("${body[stockDetails][damaged]}"));

		// Route to access item by item id
		from("direct:getItemsById").routeId("getItemsById").to("direct:findByItemId").marshal().json().unmarshal()
				.json(JsonLibrary.Jackson, Item.class).setProperty("messagebody", body())
				.setHeader("category_id", simple("${body.categoryId}")).to("direct:findByCategoryId")
				.bean(itemBean, "postResponse").marshal().json().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
				.log(LoggingLevel.INFO, "Item fetched from database");

		// Route to access item by category id and include a filter
		from("direct:getByCategoryId").routeId("getByCategoryId").to("direct:findByCategoryId")
				.to("direct:includeSpecial").to("direct:findAllItems").bean(itemBean, "categoryResponse").marshal()
				.json().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
				.log(LoggingLevel.INFO, "Details fetched from database");

		// Route to add an item
		from("direct:addItems").routeId("addItems").unmarshal().json(JsonLibrary.Jackson, Item.class)
				.to("bean-validator:validate").to("direct:addItemPropertyAssigning").to("direct:findByCategoryId")
				.bean(itemBean, "dateAdding").to("direct:insertItemIntoDb").bean(itemBean, "postResponse").marshal()
				.json().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
				.log(LoggingLevel.INFO, "Item inserted into database");

		// Route to update an item
		from("direct:updateItems").routeId("updateItems")
				.split(simple("${body[items]}"), new UpdateResponseAggregator())
				.to("direct:updateItemPropertyAssigning").setHeader("_id", simple("${body[_id]}"))
				.to("direct:findByItemId")
				.setProperty("availablestock", simple("${body[stockDetails][availableStock]}"))
				.bean(itemBean, "stockUpdation").to("direct:updateItemInDb")
				.setProperty("workingId", simple("${header._id}")).end()
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));
	}

}

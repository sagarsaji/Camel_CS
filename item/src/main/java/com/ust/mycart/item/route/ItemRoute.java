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
import com.ust.mycart.item.constants.ApplicationConstant;
import com.ust.mycart.item.constants.ConstantClass;
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
		onException(ItemException.class).handled(true).setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * Req 6: MongoDbException handled here: time =
		 * redeliveryDelay*(backOffMultiplier^(retryAttempt - 1))
		 */
		onException(CamelMongoDbException.class).log(LoggingLevel.INFO, "Mongo Retry...")
				.routeId("CamelMongoDbException").maximumRedeliveries(maximumRedeliveries)
				.redeliveryDelay(redeliveryDelay).backOffMultiplier(backOffMultiplier).useExponentialBackOff()
				.handled(true).log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * Global Exception Throwable.class handled here
		 */
		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.setBody(simple("{\"message\":\"${exception.message}\"}"))
				.log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * REST entry points
		 */
		rest()
				/**
				 * API to access details by item id
				 */
				.get("/items/{_id}").to(ApplicationConstant.GET_ITEMS_BY_ID)

				/**
				 * API to access details by category id and also accepts a filter named
				 * includeSpecial
				 */
				.get("/category/{category_id}").to(ApplicationConstant.GET_BY_CATEGORY_ID)

				/**
				 * API to add an item
				 */
				.post("/item").to(ApplicationConstant.ADD_ITEMS)

				/**
				 * API to update an item
				 */
				.put("/item").to(ApplicationConstant.UPDATE_ITEMS);

		/**
		 * Req 1 sub 1: API Route to access details by item id
		 */
		from(ApplicationConstant.GET_ITEMS_BY_ID).routeId(ConstantClass.GET_ITEMS_BY_ID)
				.to(ApplicationConstant.FIND_BY_ITEM_ID).marshal().json().unmarshal()
				.json(JsonLibrary.Jackson, Item.class).setProperty(ConstantClass.MESSAGE_BODY, body())
				.setHeader(ConstantClass.CATEGORY_ID, simple("${body.categoryId}"))
				.to(ApplicationConstant.FIND_BY_CATEGORY_ID).bean(itemBean, "postResponse").marshal().json()
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
				.log(LoggingLevel.INFO, "Item fetched from database");

		/**
		 * Route to check if the entered item id is present in the item collection or
		 * not
		 */
		from(ApplicationConstant.FIND_BY_ITEM_ID).routeId(ConstantClass.FIND_BY_ITEM_ID)
				.setBody(header(ConstantClass.ID))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=findById").choice()
				.when(body().isNull()).log(LoggingLevel.INFO, "Item not found for id : ${header._id}")
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
				.setBody(constant("{\"message\":\"{{error.itemNotFound}}\"}"))
				.throwException(new ItemException("Item not found")).otherwise()
				.log(LoggingLevel.INFO, "Item found for id : ${header._id}").end();

		/**
		 * Route to check if the entered category id is present in the category
		 * collection or not
		 */
		from(ApplicationConstant.FIND_BY_CATEGORY_ID).routeId(ConstantClass.FIND_BY_CATEGORY_ID)
				.setBody(header(ConstantClass.CATEGORY_ID))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + category + "&operation=findById")
				.choice().when(body().isNull()).log(LoggingLevel.INFO, "Category ${header.category_id} not found")
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
				.setBody(constant("{\"message\":\"{{error.categoryNotFound}}\"}"))
				.throwException(new ItemException("Category not found")).otherwise()
				.setProperty(ConstantClass.CATEGORY_NAME, simple("${body[categoryName]}"))
				.setProperty(ConstantClass.CATEGORY_DEPT, simple("${body[categoryDep]}")).end();

		/**
		 * Req 1 sub 2: API Route to access details by category id and also accepts a
		 * filter named includeSpecial
		 */
		from(ApplicationConstant.GET_BY_CATEGORY_ID).routeId(ConstantClass.GET_BY_CATEGORY_ID)
				.to(ApplicationConstant.FIND_BY_CATEGORY_ID).to(ApplicationConstant.INCLUDE_SPECIAL)
				.to(ApplicationConstant.FIND_ALL_ITEMS).bean(itemBean, "categoryResponse").marshal().json()
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
				.log(LoggingLevel.INFO, "Details fetched from database");

		/**
		 * Route to check includeSpecial query parameter condition
		 */
		from(ApplicationConstant.INCLUDE_SPECIAL).routeId(ConstantClass.INCLUDE_SPECIAL).choice()
				.when(header(ConstantClass.INCLUDE_SPECIAL).isEqualTo("false"))
				.setHeader(MongoDbConstants.CRITERIA,
						simple("{\"categoryId\": '${header.category_id}',\"specialProduct\": false}"))
				.otherwise().setHeader(MongoDbConstants.CRITERIA, simple("{\"categoryId\": '${header.category_id}'}"))
				.end();

		/**
		 * Route which invokes MongoDB operation to fetch all the items from the item
		 * collection
		 */
		from(ApplicationConstant.FIND_ALL_ITEMS)
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=findAll");

		/**
		 * Req 1 sub 3: API Route to add an item
		 */
		from(ApplicationConstant.ADD_ITEMS).routeId(ConstantClass.ADD_ITEMS).unmarshal()
				.json(JsonLibrary.Jackson, Item.class).to(ApplicationConstant.BEAN_VALIDATOR)
				.to(ApplicationConstant.ADD_ITEM_PROPERTY_ASSIGNING).to(ApplicationConstant.FIND_BY_CATEGORY_ID)
				.bean(itemBean, "dateAdding").to(ApplicationConstant.INSERT_ITEM_INTO_DB).bean(itemBean, "postResponse")
				.marshal().json().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
				.log(LoggingLevel.INFO, "Item inserted into database");

		/**
		 * Route to assign property for the message body and setting header for category
		 * id
		 */
		from(ApplicationConstant.ADD_ITEM_PROPERTY_ASSIGNING)
				.setHeader(ConstantClass.CATEGORY_ID, simple("${body.categoryId}"))
				.setProperty(ConstantClass.MESSAGE_BODY, body());

		/**
		 * Route which invokes MongoDB operation to insert a new item into the item
		 * collection
		 */
		from(ApplicationConstant.INSERT_ITEM_INTO_DB)
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=insert");

		/**
		 * Req 1 sub 4: API Route to update an item
		 */
		from(ApplicationConstant.UPDATE_ITEMS).routeId(ConstantClass.UPDATE_ITEMS)
				.split(simple("${body[items]}"), new UpdateResponseAggregator())
				.to(ApplicationConstant.UPDATE_ITEM_PROPERTY_ASSIGNING)
				.setHeader(ConstantClass.ID, simple("${body[_id]}")).to(ApplicationConstant.FIND_BY_ITEM_ID)
				.setProperty(ConstantClass.AVAILABLE_STOCK, simple("${body[stockDetails][availableStock]}"))
				.bean(itemBean, "stockUpdation").to(ApplicationConstant.UPDATE_ITEM_IN_DB)
				.setProperty(ConstantClass.WORKING_ID, header(ConstantClass.ID)).end()
				.setBody(simple("{\"message\":\"${body}\"}")).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

		/**
		 * Route to assign properties for soldout and damaged
		 */
		from(ApplicationConstant.UPDATE_ITEM_PROPERTY_ASSIGNING)
				.setProperty(ConstantClass.SOLDOUT, simple("${body[stockDetails][soldOut]}"))
				.setProperty(ConstantClass.DAMAGED, simple("${body[stockDetails][damaged]}"));

		/**
		 * Route which invokes MongoDB operation to update the item in the item
		 * collection
		 */
		from(ApplicationConstant.UPDATE_ITEM_IN_DB)
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=save");

	}

}

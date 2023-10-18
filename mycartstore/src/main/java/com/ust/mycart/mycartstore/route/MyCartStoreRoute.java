package com.ust.mycart.mycartstore.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.ust.mycart.mycartstore.constant.ApplicationConstant;
import com.ust.mycart.mycartstore.constant.ConstantClass;

@Component
public class MyCartStoreRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		/**
		 * Global Exception Throwable.class handled here
		 */
		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.setBody(simple("{\"message\":\"${exception.message}\"} "))
				.log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * REST Entry points
		 */
		rest()
				/**
				 * API to fetch details by item id from GET service of Req 1 sub 1
				 */
				.get("/item/{_id}").to(ApplicationConstant.GET_BY_ITEM_ID)

				/**
				 * API to fetch details by category id from GET service of Req 1 sub 2
				 */
				.get("/category/{category_id}").to(ApplicationConstant.GET_BY_CATEGORY_ID);

		/**
		 * API Route to fetch details by item id from GET service of Req 1 sub 1
		 */
		from(ApplicationConstant.GET_BY_ITEM_ID).routeId(ConstantClass.FIND_BY_ID)
				.toD("{{camel.getByItemId}}/${header._id}?bridgeEndpoint=true")
				.log(LoggingLevel.DEBUG, "Message received : ${body}").log(LoggingLevel.INFO, "item fetched");

		/**
		 * API to fetch details by category id from GET service of Req 1 sub 2
		 */
		from(ApplicationConstant.GET_BY_CATEGORY_ID).routeId(ConstantClass.FIND_BY_CATEGORY_ID)
				.toD("{{camel.getByCategoryId}}/${header.category_id}?bridgeEndpoint=true")
				.log(LoggingLevel.DEBUG, "Message received : ${body}").log(LoggingLevel.INFO, "item fetched");

	}

}

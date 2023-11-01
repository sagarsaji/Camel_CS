package com.ust.mycart.mycartstoreprod.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ust.mycart.mycartstoreprod.constant.ApplicationConstant;
import com.ust.mycart.mycartstoreprod.constant.ConstantClass;
import com.ust.mycart.mycartstoreprod.exception.MyCartStoreException;
import com.ust.mycart.mycartstoreprod.processor.MyCartStoreProcessor;
import com.ust.mycart.mycartstoreprod.response.JsonBody;

@Component
public class MyCartStoreProdRoute extends RouteBuilder {

	@Autowired
	private ProducerTemplate template;

	@Override
	public void configure() throws Exception {

		/**
		 * Exception related to item not found handled here
		 */
		onException(MyCartStoreException.class).handled(true).log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * Global Exception Throwable.class handled here
		 */
		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.setBody(simple("{\"message\":\"${exception.message}\"} "))
				.log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * API Route to fetch details by item id from GET service of Req 1 sub 1
		 */
		from(ApplicationConstant.GET_BY_ITEM_ID).routeId(ConstantClass.FIND_BY_ID).toD(
				"http://localhost:9090/mycart/items/${header._id}?bridgeEndpoint=true&throwExceptionOnFailure=false")
				.choice().when(body().isEqualTo("{\"message\":\"Item Not Found\"}"))
				.throwException(new MyCartStoreException("Item Not Found")).otherwise()
				.log(LoggingLevel.DEBUG, "Message received : ${body}").unmarshal()
				.json(JsonLibrary.Jackson, JsonBody.class).process(new MyCartStoreProcessor()).marshal().json()
				.log(LoggingLevel.INFO, "Processed message : ${body}").end();

	}

	public void sendHeader() {
		template.sendBodyAndHeader(ApplicationConstant.GET_BY_ITEM_ID, null, "_id", "item5");

	}

}

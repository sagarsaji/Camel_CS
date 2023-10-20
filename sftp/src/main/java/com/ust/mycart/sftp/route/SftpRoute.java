package com.ust.mycart.sftp.route;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mongodb.CamelMongoDbException;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ust.mycart.sftp.aggregator.CategoryNameAggregator;
import com.ust.mycart.sftp.aggregator.ItemTrendAggregator;
import com.ust.mycart.sftp.aggregator.JsonBodyAggregator;

import com.ust.mycart.sftp.aggregator.ReviewXmlAggregator;
import com.ust.mycart.sftp.bean.SftpBean;
import com.ust.mycart.sftp.constant.ApplicationConstant;
import com.ust.mycart.sftp.constant.ConstantClass;
import com.ust.mycart.sftp.entity.JsonBody;

@Component
public class SftpRoute extends RouteBuilder {

	@Autowired
	private SftpBean sftpBean;

	@Value("${camel.mongodb.database}")
	private String database;

	@Value("${camel.mongodb.collection1}")
	private String item;

	@Value("${camel.mongodb.collection2}")
	private String category;

	@Value("${camel.mongodb.collection3}")
	private String controlRef;

	@Value("${camel.sftp.maximumMessageCount}")
	private int maximumMessageCount;

	@Value("${camel.sftp.timePeriod}")
	private int timePeriod;

	@Value("${camel.sftp.password}")
	private String password;

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
		onException(CamelMongoDbException.class).routeId(ConstantClass.CAMEL_MONGODB_EXCEPTION)
				.maximumRedeliveries(maximumRedeliveries).redeliveryDelay(redeliveryDelay)
				.backOffMultiplier(backOffMultiplier).useExponentialBackOff().handled(true)
				.log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * Global Exception Throwable.class handled here
		 */
		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * Route that filters the data based on lastUpdateDate > lastProcessDate and
		 * then multicast to the three routes via cron
		 */
		from("cron:myData?schedule=0/10 * * * * *").routeId(ConstantClass.CRON_SERVICE)
				.to(ApplicationConstant.FETCHING_CONTROLREF_DATE).to(ApplicationConstant.FETCH_ITEMS_BASED_ON_DATE)
				.marshal().json().to(ApplicationConstant.UNMARSHAL_BODY_TO_LIST).choice()
				.when(simple("${body.size()} == 0")).log(LoggingLevel.INFO, "No record processed").otherwise()
				.multicast().parallelProcessing().to(ApplicationConstant.ITEM_TREND_ANALYZER,
						ApplicationConstant.REVIEW_DUMP, ApplicationConstant.STORE_FRONT_APP)
				.end().to(ApplicationConstant.CONTROL_REF_UPDATING).end();

		/**
		 * Route to fetch the controlRef date and store into some property
		 */
		from(ApplicationConstant.FETCHING_CONTROLREF_DATE).routeId(ApplicationConstant.FETCHING_CONTROLREF_DATE)
				.setBody(constant(ConstantClass.CONTROL_REF))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + controlRef + "&operation=findById")
				.setProperty(ConstantClass.CONTROL_REF_DATE, simple("${body[date]}"));

		/**
		 * Route to fetch all details from the item collection based on lastUpdateDate >
		 * lastProcessDate
		 */
		from(ApplicationConstant.FETCH_ITEMS_BASED_ON_DATE).routeId(ApplicationConstant.FETCH_ITEMS_BASED_ON_DATE)
				.setHeader(MongoDbConstants.CRITERIA,
						simple("{ \"lastUpdateDate\": { \"$gt\": \"${exchangeProperty.contref}\" } }"))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=findAll");

		/**
		 * Route to unmarshal body to type List.class
		 */
		from(ApplicationConstant.UNMARSHAL_BODY_TO_LIST).routeId(ApplicationConstant.UNMARSHAL_BODY_TO_LIST).unmarshal()
				.json(JsonLibrary.Jackson, List.class).setProperty(ConstantClass.LIST, body());

		/**
		 * Route to update lastProcessDate in controlRef collection
		 */
		from(ApplicationConstant.CONTROL_REF_UPDATING).routeId(ConstantClass.LAST_PROCESS_DATE)
				.bean(sftpBean, "controlRefDateUpdation")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + controlRef + "&operation=update")
				.log(LoggingLevel.INFO, "Date updated");

		/**
		 * Req 3 sub 1: itemTrendAnalyzer.xml
		 */
		from(ApplicationConstant.ITEM_TREND_ANALYZER).routeId(ConstantClass.ITEM_TREND_ANALYZER)
				.setHeader(ConstantClass.ROUTE_ID, simple("${routeId}")).marshal().json()
				.to(ApplicationConstant.UNMARSHAL_BODY_TO_LIST).split(body(), new CategoryNameAggregator())
				.setProperty(ConstantClass.CATEGORY_ID, simple("${body[categoryId]}"))
				.to(ApplicationConstant.FIND_BY_CATEGORY_ID).end().setBody(exchangeProperty(ConstantClass.LIST))
				.split(body(), new ItemTrendAggregator()).to(ApplicationConstant.UNMARSHAL_BODY_TO_JSONBODY)
				.bean(sftpBean, "itemTrendAnalyzer").end().marshal().jaxb(true).throttle(maximumMessageCount)
				.timePeriodMillis(timePeriod).log(LoggingLevel.INFO, "Converted to XML")
				.to(ApplicationConstant.SAVE_FILE);

		/**
		 * Route which invokes MongoDB findById operation to fetch details from category
		 * collection and assign property for category name
		 */
		from(ApplicationConstant.FIND_BY_CATEGORY_ID).routeId(ApplicationConstant.FIND_BY_CATEGORY_ID)
				.setBody(exchangeProperty(ConstantClass.CATEGORY_ID))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + category + "&operation=findById")
				.setProperty(ConstantClass.CATEGORY_NAME, simple("${body[categoryName]}"));

		/**
		 * Route to unmarshal body to type JsonBody.class
		 */
		from(ApplicationConstant.UNMARSHAL_BODY_TO_JSONBODY).routeId(ApplicationConstant.UNMARSHAL_BODY_TO_JSONBODY)
				.marshal().json().unmarshal().json(JsonLibrary.Jackson, JsonBody.class);

		/**
		 * Route to save file into SFTP folder and update lastProcessDate in controlRef
		 * collection
		 */
		from(ApplicationConstant.SAVE_FILE).routeId(ApplicationConstant.SAVE_FILE)
				.setHeader(Exchange.FILE_NAME, simple("${header.routeid}_${date:now:yyyyMMdd_HHmmss}"))
				.toD("ftp://{{camel.sftp.link}}/${header.routeid}?password=" + password
						+ "&fileName=${header.CamelFileName}");

		/**
		 * Req 3 sub 2: reviewDump.xml
		 */
		from(ApplicationConstant.REVIEW_DUMP).routeId(ConstantClass.REVIEW_DUMP)
				.setHeader(ConstantClass.ROUTE_ID, simple("${routeId}")).split(body(), new ReviewXmlAggregator())
				.to(ApplicationConstant.UNMARSHAL_BODY_TO_JSONBODY).bean(sftpBean, "reviewDump").end().marshal()
				.jaxb(true).throttle(maximumMessageCount).timePeriodMillis(timePeriod)
				.log(LoggingLevel.INFO, "Converted to XML").to(ApplicationConstant.SAVE_FILE);

		/**
		 * Req 3 sub 3: storeFrontApp.json
		 */
		from(ApplicationConstant.STORE_FRONT_APP).routeId(ConstantClass.STORE_FRONT_APP)
				.setHeader(ConstantClass.ROUTE_ID, simple("${routeId}")).split(body(), new JsonBodyAggregator())
				.marshal().json().unmarshal().json(JsonLibrary.Jackson, JsonBody.class)
				.setProperty(ConstantClass.MESSAGE_BODY, body())
				.setProperty(ConstantClass.CATEGORY_ID, simple("${body.categoryId}"))
				.to(ApplicationConstant.FIND_BY_CATEGORY_ID).bean(sftpBean, "jsonResponse").end().marshal()
				.json(JsonLibrary.Jackson, true).throttle(maximumMessageCount).timePeriodMillis(timePeriod)
				.log(LoggingLevel.INFO, "Converted to JSON").to(ApplicationConstant.SAVE_FILE);

	}
}

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
import com.ust.mycart.sftp.aggregator.ListAggregator;
import com.ust.mycart.sftp.aggregator.ReviewXmlAggregator;
import com.ust.mycart.sftp.bean.SftpBean;
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
		onException(CamelMongoDbException.class).routeId("CamelMongoDbException")
				.maximumRedeliveries(maximumRedeliveries).redeliveryDelay(redeliveryDelay)
				.backOffMultiplier(backOffMultiplier).useExponentialBackOff().handled(true)
				.log(LoggingLevel.ERROR, "${exception.message}");

		/**
		 * Global Exception Throwable.class handled here
		 */
		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json")).log("${exception.message}");

		/**
		 * Route that multicast's data to the three routes using parallel processing via
		 * cron which sends data every 10 seconds
		 */
		from("cron:myData?schedule=0/10 * * * * *")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=findAll").marshal()
				.json().multicast().parallelProcessing()
				.to("direct:itemTrendBody", "direct:reviewBody", "direct:storeFrontBody").end();

		/**
		 * Handling the filtering of data based on lastUpdateDate > lastProcessDate for
		 * itemTrendAnalyzer.xml
		 */
		from("direct:itemTrendBody").to("direct:unmarshalBody").split(body(), new ListAggregator())
				.to("direct:recentDateProperty")
				.setHeader(MongoDbConstants.CRITERIA, simple("{\"_id\" : \"itemTrendAnalyzer\"}"))
				.to("direct:controlRefFindByQuery").end().to("direct:itemTrendAnalyzer");

		/**
		 * Route to fetch the lastUpdateDate from the body and convert it into Date type
		 */
		from("direct:recentDateProperty").setProperty("listbody", body())
				.setProperty("recentDate", simple("${body[lastUpdateDate]}")).bean(sftpBean, "recentDate");

		/**
		 * Fetching lastProcessDate from controlRef collection and checking if
		 * lastUpdateDate > lastProcessDate
		 */
		from("direct:controlRefFindByQuery")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + controlRef + "&operation=findOneByQuery")
				.setProperty("controlrefdate", simple("${body[date]}")).choice()
				.when(exchangeProperty("recentDateNew").isGreaterThan(exchangeProperty("controlrefdate")))
				.setProperty("messagebody", exchangeProperty("listbody")).end();

		/**
		 * Req 3 sub 1: itemTrendAnalyzer.xml
		 */
		from("direct:itemTrendAnalyzer").routeId("itemTrendAnalyzer").setHeader("routeid", simple("${routeId}"))
				.choice().when(simple("${body.size()} == 0")).log(LoggingLevel.INFO, "No record processed").otherwise()
				.marshal().json().to("direct:unmarshalBody").split(body(), new CategoryNameAggregator())
				.to("direct:itemTrendPropertyAssigning").to("direct:findByCategoryId").end()
				.setBody(exchangeProperty("list")).split(body(), new ItemTrendAggregator())
				.to("direct:unmarshalToJsonBody").bean(sftpBean, "itemTrendAnalyzer").end().to("direct:marshalToJaxb")
				.log(LoggingLevel.INFO, "Converted to XML").to("direct:saveFileAndUpdateDate").end();

		/**
		 * Route to unmarshal body to type List.class
		 */
		from("direct:unmarshalBody").unmarshal().json(JsonLibrary.Jackson, List.class).setProperty("list", body());

		/**
		 * Route to assign properties for lastUpdateDate , each body from the list,
		 * category id from the message
		 */
		from("direct:itemTrendPropertyAssigning").setProperty("recentdate", simple("${body[lastUpdateDate]}"))
				.setProperty("messagebody", body()).setProperty("categoryid", simple("${body[categoryId]}"));

		/**
		 * Route which invokes MongoDB findById operation to fetch details from category
		 * collection and assign property for category name
		 */
		from("direct:findByCategoryId").setBody(exchangeProperty("categoryid"))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + category + "&operation=findById")
				.setProperty("categoryname", simple("${body[categoryName]}"));

		/**
		 * Route to unmarshal body to type JsonBody.class
		 */
		from("direct:unmarshalToJsonBody").marshal().json().unmarshal().json(JsonLibrary.Jackson, JsonBody.class);

		/**
		 * Req 5: Route to send specified number of messages in a specified time period
		 */
		from("direct:throttle").throttle(maximumMessageCount).timePeriodMillis(timePeriod);

		/**
		 * Marshal to jaxb and do requirement 5
		 */
		from("direct:marshalToJaxb").marshal().jaxb(true).to("direct:throttle");

		/**
		 * Route to update lastProcessDate in controlRef collection
		 */
		from("direct:controlRefUpdating").routeId("lastProcessDate").bean(sftpBean, "controlRefDateUpdation")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + controlRef + "&operation=save")
				.log(LoggingLevel.INFO, "Date updated");

		/**
		 * Route to save file into SFTP folder and update lastProcessDate in controlRef
		 * collection
		 */
		from("direct:saveFileAndUpdateDate")
				.setHeader(Exchange.FILE_NAME, simple("${header.routeid}_${date:now:yyyyMMdd_HHmmss}"))
				.toD("ftp://{{camel.sftp.link}}/${header.routeid}?password=" + password
						+ "&fileName=${header.CamelFileName}")
				.setHeader("controlId", header("routeid")).to("direct:controlRefUpdating");

		/**
		 * Handling the filtering of data based on lastUpdateDate > lastProcessDate for
		 * reviewDump.xml
		 */
		from("direct:reviewBody").to("direct:unmarshalBody").split(body(), new ListAggregator())
				.to("direct:recentDateProperty")
				.setHeader(MongoDbConstants.CRITERIA, simple("{\"_id\" : \"reviewDump\"}"))
				.to("direct:controlRefFindByQuery").end().to("direct:reviewDump");

		/**
		 * Req 3 sub 2: reviewDump.xml
		 */
		from("direct:reviewDump").routeId("reviewDump").setHeader("routeid", simple("${routeId}")).choice()
				.when(simple("${body.size()} == 0")).log(LoggingLevel.INFO, "No record processed").otherwise()
				.split(body(), new ReviewXmlAggregator()).to("direct:unmarshalToJsonBody").bean(sftpBean, "reviewDump")
				.end().to("direct:marshalToJaxb").log(LoggingLevel.INFO, "Converted to XML")
				.to("direct:saveFileAndUpdateDate").end();

		/**
		 * Handling the filtering of data based on lastUpdateDate > lastProcessDate for
		 * storeFrontApp.json
		 */
		from("direct:storeFrontBody").to("direct:unmarshalBody").split(body(), new ListAggregator())
				.to("direct:recentDateProperty")
				.setHeader(MongoDbConstants.CRITERIA, simple("{\"_id\" : \"storeFrontApp\"}"))
				.to("direct:controlRefFindByQuery").end().to("direct:storeFrontApp");

		/**
		 * Req 3 sub 3: storeFrontApp.json
		 */
		from("direct:storeFrontApp").routeId("storeFrontApp").setHeader("routeid", simple("${routeId}")).choice()
				.when(simple("${body.size()} == 0")).log(LoggingLevel.INFO, "No record processed").otherwise()
				.split(body(), new JsonBodyAggregator()).unmarshal().json(JsonLibrary.Jackson, JsonBody.class)
				.setProperty("messagebody", body()).setProperty("categoryid", simple("${body.categoryId}"))
				.to("direct:findByCategoryId").bean(sftpBean, "jsonResponse").end().marshal()
				.json(JsonLibrary.Jackson, true).to("direct:throttle").log(LoggingLevel.INFO, "Converted to JSON")
				.to("direct:saveFileAndUpdateDate").end();

	}
}

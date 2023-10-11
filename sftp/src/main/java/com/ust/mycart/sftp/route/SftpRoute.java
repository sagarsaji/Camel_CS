package com.ust.mycart.sftp.route;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
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
import com.ust.mycart.sftp.headers.HeaderClass;

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

	@Override
	public void configure() throws Exception {

		// Handled exceptions here
		onException(Throwable.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json")).log("${exception.message}")
				.setBody(constant("{\"message\":\"{{error.internalServerError}}\"}"));

		from("direct:recentDateProperty").setProperty("listbody", body())
				.setProperty("recentDate", simple("${body[lastUpdateDate]}")).bean(sftpBean, "recentDate");

		from("direct:unmarshalBody").unmarshal().json(JsonLibrary.Jackson, List.class).setProperty("list", body());

		from("direct:controlRefFindByQuery")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + controlRef + "&operation=findOneByQuery")
				.setProperty("controlrefdate", simple("${body[date]}")).choice()
				.when(exchangeProperty("recentDateNew").isGreaterThan(exchangeProperty("controlrefdate")))
				.setProperty("messagebody", exchangeProperty("listbody")).end();

		from("direct:findByCategoryId").setBody(exchangeProperty("categoryid"))
				.to("mongodb:mycartdb?database=" + database + "&collection=" + category + "&operation=findById")
				.setProperty("categoryname", simple("${body[categoryName]}"));

		from("direct:itemTrendPropertyAssigning").setProperty("recentdate", simple("${body[lastUpdateDate]}"))
				.setProperty("messagebody", body()).setProperty("categoryid", simple("${body[categoryId]}"));

		// Req 5
		from("direct:throttle").throttle(maximumMessageCount).timePeriodMillis(timePeriod);

		from("direct:controlRefUpdating").bean(sftpBean, "controlRefDateUpdation")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + controlRef + "&operation=save")
				.log(LoggingLevel.INFO, "Date updated");

		// Multicasting data to the three routes via cron
		from("cron:myData?schedule=0/10 * * * * *")
				.to("mongodb:mycartdb?database=" + database + "&collection=" + item + "&operation=findAll").marshal()
				.json().multicast().parallelProcessing()
				.to("direct:itemTrendBody", "direct:reviewBody", "direct:storeFrontBody").end();

		// Handling the filtering of data based on lastUpdateDate > lastProcessDate
		from("direct:itemTrendBody").to("direct:unmarshalBody").split(body(), new ListAggregator())
				.to("direct:recentDateProperty")
				.setHeader(MongoDbConstants.CRITERIA, simple("{\"_id\" : \"itemTrendAnalyzer\"}"))
				.to("direct:controlRefFindByQuery").end().to("direct:itemTrendAnalyzer");

		from("direct:reviewBody").to("direct:unmarshalBody").split(body(), new ListAggregator())
				.to("direct:recentDateProperty")
				.setHeader(MongoDbConstants.CRITERIA, simple("{\"_id\" : \"reviewDump\"}"))
				.to("direct:controlRefFindByQuery").end().to("direct:reviewDump");

		from("direct:storeFrontBody").to("direct:unmarshalBody").split(body(), new ListAggregator())
				.to("direct:recentDateProperty")
				.setHeader(MongoDbConstants.CRITERIA, simple("{\"_id\" : \"storeFrontApp\"}"))
				.to("direct:controlRefFindByQuery").end().to("direct:storeFrontApp");

		// Req 3 sub 1: itemTrendAnalyzer.xml
		from("direct:itemTrendAnalyzer").routeId("itemTrendAnalyzer").choice().when(simple("${body.size()} == 0"))
				.log(LoggingLevel.INFO, "empty").otherwise().marshal().json().to("direct:unmarshalBody")
				.split(body(), new CategoryNameAggregator()).to("direct:itemTrendPropertyAssigning")
				.to("direct:findByCategoryId").end().setBody(exchangeProperty("list"))
				.split(body(), new ItemTrendAggregator()).marshal().json().unmarshal()
				.json(JsonLibrary.Jackson, JsonBody.class).bean(sftpBean, "itemTrendAnalyzer").end().marshal()
				.jaxb(true).to("direct:throttle").log(LoggingLevel.INFO, "Converted to XML")
				.setHeader(Exchange.FILE_NAME, simple("itemTrendAnalyzer_${date:now:yyyyMMdd_HHmmss}.xml"))
				.to("ftp://{{camel.sftp.link}}/itemTrendAnalyzer?password=" + password
						+ "&fileName=${header.CamelFileName}")
				.setHeader(HeaderClass.CONTROL_ID, constant("itemTrendAnalyzer")).to("direct:controlRefUpdating").end();

		// Req 3 sub 2: reviewDump.xml
		from("direct:reviewDump").routeId("reviewDump").choice().when(simple("${body.size()} == 0"))
				.log(LoggingLevel.INFO, "empty").otherwise().split(body(), new ReviewXmlAggregator()).marshal().json()
				.unmarshal().json(JsonLibrary.Jackson, JsonBody.class).bean(sftpBean, "reviewDump").end().marshal()
				.jaxb(true).to("direct:throttle").log(LoggingLevel.INFO, "Converted to XML")
				.setHeader(Exchange.FILE_NAME, simple("reviewDump_${date:now:yyyyMMdd_HHmmss}.xml"))
				.to("ftp://{{camel.sftp.link}}/reviewDump?password=" + password + "&fileName=${header.CamelFileName}")
				.setHeader(HeaderClass.CONTROL_ID, constant("reviewDump")).to("direct:controlRefUpdating").end();

		// Req 3 sub 3: storeFrontApp.json
		from("direct:storeFrontApp").routeId("storeFrontApp").choice().when(simple("${body.size()} == 0"))
				.log(LoggingLevel.INFO, "empty").otherwise().split(body(), new JsonBodyAggregator()).unmarshal()
				.json(JsonLibrary.Jackson, JsonBody.class).setProperty("messagebody", body())
				.setProperty("categoryid", simple("${body.categoryId}")).to("direct:findByCategoryId")
				.bean(sftpBean, "jsonResponse").end().marshal().json(JsonLibrary.Jackson, true).to("direct:throttle")
				.log(LoggingLevel.INFO, "Converted to JSON")
				.setHeader(Exchange.FILE_NAME, simple("storeFrontApp_${date:now:yyyyMMdd_HHmmss}.json"))
				.to("ftp://{{camel.sftp.link}}/storeFrontApp?password=" + password
						+ "&fileName=${header.CamelFileName}")
				.setHeader(HeaderClass.CONTROL_ID, constant("storeFrontApp")).to("direct:controlRefUpdating").end();

	}
}

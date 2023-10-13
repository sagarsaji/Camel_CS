package com.ust.mycart.activemqconsumer.processor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;

public class StockUpdationProcessor implements Processor {

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static LocalDateTime currentDateTime;

	@Override
	public void process(Exchange exchange) throws Exception {
		// TODO Auto-generated method stub

		Document item = exchange.getIn().getBody(Document.class);
		Document stockdetails = (Document) item.get("stockDetails");
		String lastupdatedate = (String) item.get("lastUpdateDate");

		int soldout = exchange.getProperty("soldout", Integer.class);
		int damage = exchange.getProperty("damaged", Integer.class);
		int available = exchange.getProperty("availablestock", Integer.class);

		currentDateTime = LocalDateTime.now();

		lastupdatedate = currentDateTime.format(formatter);

		available = available - soldout - damage;

		stockdetails.put("availableStock", available);
		item.put("stockDetails", stockdetails);
		item.put("lastUpdateDate", lastupdatedate);

		exchange.getIn().setBody(item);

	}

}

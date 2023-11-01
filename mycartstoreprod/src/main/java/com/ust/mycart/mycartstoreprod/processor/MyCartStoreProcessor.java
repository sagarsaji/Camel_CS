package com.ust.mycart.mycartstoreprod.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.ust.mycart.mycartstoreprod.response.ItemResponse;
import com.ust.mycart.mycartstoreprod.response.JsonBody;

public class MyCartStoreProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {

		JsonBody response = exchange.getIn().getBody(JsonBody.class);
		ItemResponse itemResponse = new ItemResponse();
		itemResponse.set_id(response.get_id());
		itemResponse.setItemName(response.getItemName());
		itemResponse.setCategoryName(response.getCategoryName());
		itemResponse.setStockDetails(response.getStockDetails());

		exchange.getIn().setBody(itemResponse);

	}

}

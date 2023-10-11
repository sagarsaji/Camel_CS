package com.ust.mycart.item.bean;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangeProperty;
import org.bson.Document;
import org.springframework.stereotype.Component;

import com.ust.mycart.item.entity.CategoryResponse;
import com.ust.mycart.item.entity.Item;
import com.ust.mycart.item.entity.Response;

@Component
public class ItemBean {

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final LocalDateTime currentDateTime = LocalDateTime.now();

	public void postResponse(Exchange exchange, @ExchangeProperty("categoryname") String categoryname,
			@ExchangeProperty("messagebody") Item item) {

		Response response = new Response();

		response.set_id(item.get_id());
		response.setItemName(item.getItemName());
		response.setCategoryName(categoryname);
		response.setItemPrice(item.getItemPrice());
		response.setStockDetails(item.getStockDetails());
		response.setSpecialProduct(item.getSpecialProduct());

		exchange.getIn().setBody(response);

	}

	public void categoryResponse(Exchange exchange, @ExchangeProperty("categoryname") String categoryname,
			@ExchangeProperty("categorydept") String categorydept, @Body List<Item> item) {

		CategoryResponse response = new CategoryResponse();

		response.setCategoryName(categoryname);
		response.setCategoryDept(categorydept);

		response.setItems(item);
		exchange.getIn().setBody(response);
	}

	public void dateAdding(Exchange exchange, @ExchangeProperty("messagebody") Item item) {

		String date = currentDateTime.format(formatter);
		item.setLastUpdateDate(date);

		exchange.getIn().setBody(item);
	}

	public void stockUpdation(Exchange exchange, @ExchangeProperty("soldout") int soldout,
			@ExchangeProperty("damaged") int damage, @ExchangeProperty("availablestock") int available,
			@Body Document item) {

		Document stockdetails = (Document) item.get("stockDetails");
		String lastupdatedate = (String) item.get("lastUpdateDate");

		lastupdatedate = currentDateTime.format(formatter);

		available = available - soldout - damage;

		stockdetails.put("availableStock", available);
		item.put("stockDetails", stockdetails);
		item.put("lastUpdateDate", lastupdatedate);

		exchange.getIn().setBody(item);
	}

}

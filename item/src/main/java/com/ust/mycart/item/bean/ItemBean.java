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
import com.ust.mycart.item.entity.ItemRequest;
import com.ust.mycart.item.entity.Response;

@Component
public class ItemBean {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static LocalDateTime CURRENT_DATE_TIME;

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

	public void mappingToItemEntity(Exchange exchange, @ExchangeProperty("messagebody") ItemRequest itemRequest) {

		CURRENT_DATE_TIME = LocalDateTime.now();
		String date = CURRENT_DATE_TIME.format(FORMATTER);

		Item item = new Item();
		item.set_id(itemRequest.get_id());
		item.setItemName(itemRequest.getItemName());
		item.setCategoryId(itemRequest.getCategoryId());
		item.setLastUpdateDate(date);
		item.setItemPrice(itemRequest.getItemPrice());
		item.setStockDetails(itemRequest.getStockDetails());
		item.setSpecialProduct(itemRequest.getSpecialProduct());
		item.setReview(itemRequest.getReview());

		exchange.setProperty("messagebody", item);
		exchange.getIn().setBody(item);
	}

	public void stockUpdation(Exchange exchange, @ExchangeProperty("soldout") int soldout,
			@ExchangeProperty("damaged") int damage, @ExchangeProperty("availablestock") int available,
			@Body Document item) {

		Document stockdetails = (Document) item.get("stockDetails");
		String lastupdatedate = (String) item.get("lastUpdateDate");

		CURRENT_DATE_TIME = LocalDateTime.now();

		lastupdatedate = CURRENT_DATE_TIME.format(FORMATTER);

		available = available - soldout - damage;

		stockdetails.put("availableStock", available);
		item.put("stockDetails", stockdetails);
		item.put("lastUpdateDate", lastupdatedate);

		exchange.getIn().setBody(item);
	}

}

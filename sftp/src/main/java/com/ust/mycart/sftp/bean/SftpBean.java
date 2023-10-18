package com.ust.mycart.sftp.bean;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.springframework.stereotype.Component;

import com.ust.mycart.sftp.entity.JsonBody;
import com.ust.mycart.sftp.entity.JsonResponse;
import com.ust.mycart.sftp.inventory.Category;
import com.ust.mycart.sftp.inventory.Item;
import com.ust.mycart.sftp.review.ItemReview;
import com.ust.mycart.sftp.review.Review;

@Component
public class SftpBean {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static LocalDateTime CURRENT_DATE_TIME;

	public void controlRefDateUpdation(Exchange exchange) {

		CURRENT_DATE_TIME = LocalDateTime.now();
		String date = CURRENT_DATE_TIME.format(FORMATTER);

		Map<String, Object> filter = new HashMap<>();
		filter.put("_id", "controlRef");

		Map<String, Object> updateFields = new HashMap<>();
		updateFields.put("$set", Collections.singletonMap("date", date));

		exchange.getMessage().setHeader(MongoDbConstants.CRITERIA, filter);
		exchange.getMessage().setHeader(MongoDbConstants.MULTIUPDATE, false);
		exchange.getMessage().setBody(updateFields);
	}

	public void itemTrendAnalyzer(Exchange exchange, @ExchangeProperty("categoryMap") Map<String, String> categoryMap,
			@Body JsonBody response) {

		Item item = new Item();
		item.setItemId(response.get_id());
		item.setCategoryId(response.getCategoryId());
		item.setAvailableStock(response.getStockDetails().getAvailableStock());
		item.setSellingPrice(response.getItemPrice().getSellingPrice());

		exchange.setProperty("items", item);

		Category category = new Category();
		category.setId(response.getCategoryId());
		category.setName(categoryMap.get(response.getCategoryId()));

		exchange.setProperty("category", category);
	}

	public void reviewDump(Exchange exchange, @Body JsonBody response) {

		ItemReview item = new ItemReview();

		item.setId(response.get_id());

		if (response.getReview() == null) {
			List<Review> responselist = new ArrayList<>();
			Review reviews = new Review();
			reviews.setRating("null");
			reviews.setComment("null");
			responselist.add(reviews);
			item.setReview(responselist);
		} else {
			item.setReview(response.getReview());
		}

		exchange.setProperty("items", item);

	}

	public void jsonResponse(Exchange exchange, @ExchangeProperty("messagebody") JsonBody jsonResponse,
			@ExchangeProperty("categoryname") String categoryname) {

		JsonResponse response = new JsonResponse();

		response.set_id(jsonResponse.get_id());
		response.setItemName(jsonResponse.getItemName());
		response.setCategoryName(categoryname);
		response.setItemPrice(jsonResponse.getItemPrice());
		response.setStockDetails(jsonResponse.getStockDetails());
		response.setSpecialProduct(jsonResponse.getSpecialProduct());

		exchange.setProperty("jsonmessage", response);

		exchange.getIn().setBody(response);
	}

}

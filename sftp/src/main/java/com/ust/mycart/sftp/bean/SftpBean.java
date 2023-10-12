package com.ust.mycart.sftp.bean;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.Header;
import org.springframework.stereotype.Component;

import com.ust.mycart.sftp.entity.JsonBody;
import com.ust.mycart.sftp.entity.JsonResponse;
import com.ust.mycart.sftp.inventory.Category;
import com.ust.mycart.sftp.inventory.Item;
import com.ust.mycart.sftp.review.ItemReview;
import com.ust.mycart.sftp.review.Reviews;

@Component
public class SftpBean {

	public void recentDate(Exchange exchange, @ExchangeProperty("recentDate") String lastdate) throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date lastupdatedate = dateFormat.parse(lastdate);
		exchange.setProperty("recentDateNew", lastupdatedate);
	}

	public void controlRefDateUpdation(Exchange exchange, @Header("controlId") String id) {

		Map<String, Object> map = new HashMap<>();
		map.put("_id", id);
		map.put("date", new Date());
		exchange.getIn().setBody(map);
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

	public void reviewDump(Exchange exchange, @Body JsonBody response, @Body Reviews review) {

		if (review == null) {
			review = new Reviews();
			review.setItems(new ArrayList<>());
		}

		ItemReview item = new ItemReview();

		item.setId(response.get_id());
		item.setReview(response.getReview());

		exchange.setProperty("items", item);

		review.getItems().add(item);

		exchange.setProperty("reviewxml", review);
		exchange.getIn().setBody(review);
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

package com.ust.mycart.sftp.aggregator;

import java.util.ArrayList;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import com.ust.mycart.sftp.inventory.Category;
import com.ust.mycart.sftp.inventory.Inventory;
import com.ust.mycart.sftp.inventory.Item;

public class ItemTrendAggregator implements AggregationStrategy {

	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		// TODO Auto-generated method stub

		Inventory inventory;

		if (oldExchange == null) {
			inventory = new Inventory();
			Category category = newExchange.getProperty("category", Category.class);
			Item item = newExchange.getProperty("items", Item.class);
			category.setItems(new ArrayList<>());
			category.getItems().add(item);
			inventory.getCategories().add(category);
			newExchange.setProperty("inventory", inventory);
			newExchange.getIn().setBody(inventory);
			return newExchange;
		}

		inventory = oldExchange.getProperty("inventory", Inventory.class);
		Category category = newExchange.getProperty("category", Category.class);
		Item item = newExchange.getProperty("items", Item.class);

		// Find the matching category in the existing inventory
		Category matchingCategory = null;
		for (Category existingCategory : inventory.getCategories()) {
			if (existingCategory.getId().equals(category.getId())) {
				matchingCategory = existingCategory;
				break;
			}
		}

		if (matchingCategory != null) {
			// If a matching category is found, add the item to its itemList
			matchingCategory.getItems().add(item);
		} else {
			// If no matching category is found, add the category with the item to the
			// inventory
			category.setItems(new ArrayList<>());
			category.getItems().add(item);
			inventory.getCategories().add(category);
		}

		// Update the inventory in the oldExchange
		oldExchange.setProperty("inventory", inventory);
		oldExchange.getIn().setBody(inventory);
		return oldExchange;

	}
}

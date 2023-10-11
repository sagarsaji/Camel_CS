package com.ust.mycart.item.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CategoryResponse {

	@JsonProperty("categoryName")
	private String categoryName;
	@JsonProperty("categoryDept")
	private String categoryDept;
	@JsonProperty("items")
	private List<Item> items;

	public CategoryResponse(String categoryName, String categoryDept, List<Item> items) {
		super();
		this.categoryName = categoryName;
		this.categoryDept = categoryDept;
		this.items = items;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getCategoryDept() {
		return categoryDept;
	}

	public void setCategoryDept(String categoryDept) {
		this.categoryDept = categoryDept;
	}

	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	public CategoryResponse() {
		super();
	}

}

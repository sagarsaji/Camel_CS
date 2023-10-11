package com.ust.mycart.item.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Category {

	@JsonProperty("_id")
	private String _id;
	@JsonProperty("categoryName")
	private String categoryName;
	@JsonProperty("categoryDep")
	private String categoryDep;
	@JsonProperty("categoryTax")
	private String categoryTax;

	public Category(String _id, String categoryName, String categoryDep, String categoryTax) {
		super();
		this._id = _id;
		this.categoryName = categoryName;
		this.categoryDep = categoryDep;
		this.categoryTax = categoryTax;
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getCategoryDep() {
		return categoryDep;
	}

	public void setCategoryDep(String categoryDep) {
		this.categoryDep = categoryDep;
	}

	public String getCategoryTax() {
		return categoryTax;
	}

	public void setCategoryTax(String categoryTax) {
		this.categoryTax = categoryTax;
	}

	public Category() {
		super();
	}

}

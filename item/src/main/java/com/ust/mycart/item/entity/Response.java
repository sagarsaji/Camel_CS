package com.ust.mycart.item.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Response {

	@JsonProperty("_id")
	private String _id;
	@JsonProperty("itemName")
	private String itemName;
	@JsonProperty("categoryName")
	private String categoryName;
	@JsonProperty("itemPrice")
	private ItemPrice itemPrice;
	@JsonProperty("stockDetails")
	private StockDetails stockDetails;
	@JsonProperty("specialProduct")
	private Boolean specialProduct;

	public Response(String _id, String itemName, String categoryName, ItemPrice itemPrice, StockDetails stockDetails,
			Boolean specialProduct) {
		super();
		this._id = _id;
		this.itemName = itemName;
		this.categoryName = categoryName;
		this.itemPrice = itemPrice;
		this.stockDetails = stockDetails;
		this.specialProduct = specialProduct;
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public ItemPrice getItemPrice() {
		return itemPrice;
	}

	public void setItemPrice(ItemPrice itemPrice) {
		this.itemPrice = itemPrice;
	}

	public StockDetails getStockDetails() {
		return stockDetails;
	}

	public void setStockDetails(StockDetails stockDetails) {
		this.stockDetails = stockDetails;
	}

	public Boolean getSpecialProduct() {
		return specialProduct;
	}

	public void setSpecialProduct(Boolean specialProduct) {
		this.specialProduct = specialProduct;
	}

	public Response() {
		super();
	}

}

package com.ust.mycart.item.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Item {

	@JsonProperty("_id")
	private String _id;
	@JsonProperty("itemName")
	private String itemName;
	@JsonProperty("categoryId")
	private String categoryId;
	@JsonProperty("lastUpdateDate")
	private String lastUpdateDate;
	@JsonProperty("itemPrice")
	private ItemPrice itemPrice;
	@JsonProperty("stockDetails")
	private StockDetails stockDetails;
	@JsonProperty("specialProduct")
	private Boolean specialProduct;
	@JsonProperty("review")
	private List<Review> review;

	public Item(String _id, String itemName, String categoryId, String lastUpdateDate, ItemPrice itemPrice,
			StockDetails stockDetails, Boolean specialProduct, List<Review> review) {
		super();
		this._id = _id;
		this.itemName = itemName;
		this.categoryId = categoryId;
		this.lastUpdateDate = lastUpdateDate;
		this.itemPrice = itemPrice;
		this.stockDetails = stockDetails;
		this.specialProduct = specialProduct;
		this.review = review;
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

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(String lastUpdateDate) {
		this.lastUpdateDate = lastUpdateDate;
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

	public List<Review> getReview() {
		return review;
	}

	public void setReview(List<Review> review) {
		this.review = review;
	}

	public Item() {
		super();
	}

}

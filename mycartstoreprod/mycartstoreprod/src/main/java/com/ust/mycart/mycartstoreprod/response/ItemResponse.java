package com.ust.mycart.mycartstoreprod.response;

public class ItemResponse {

	private String _id;
	private String itemName;
	private String categoryName;
	private StockDetails stockDetails;

	public ItemResponse(String _id, String itemName, String categoryName, StockDetails stockDetails) {
		super();
		this._id = _id;
		this.itemName = itemName;
		this.categoryName = categoryName;
		this.stockDetails = stockDetails;
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

	public StockDetails getStockDetails() {
		return stockDetails;
	}

	public void setStockDetails(StockDetails stockDetails) {
		this.stockDetails = stockDetails;
	}

	public ItemResponse() {
		super();
	}

}

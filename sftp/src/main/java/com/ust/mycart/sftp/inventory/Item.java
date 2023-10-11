package com.ust.mycart.sftp.inventory;

import jakarta.xml.bind.annotation.XmlElement;

public class Item {
	
	private String itemId;
	private String categoryId;
	private int availableStock;
	private int sellingPrice;

	@XmlElement
	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	@XmlElement
	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	@XmlElement
	public int getAvailableStock() {
		return availableStock;
	}

	public void setAvailableStock(int availableStock) {
		this.availableStock = availableStock;
	}

	@XmlElement
	public int getSellingPrice() {
		return sellingPrice;
	}

	public void setSellingPrice(int sellingPrice) {
		this.sellingPrice = sellingPrice;
	}

	public Item() {
		super();
	}

	public Item(String itemId, String categoryId, int availableStock, int sellingPrice) {
		super();
		this.itemId = itemId;
		this.categoryId = categoryId;
		this.availableStock = availableStock;
		this.sellingPrice = sellingPrice;
	}
	
	

}

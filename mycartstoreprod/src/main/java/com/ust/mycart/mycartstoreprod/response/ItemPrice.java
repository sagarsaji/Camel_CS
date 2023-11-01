package com.ust.mycart.mycartstoreprod.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemPrice {

	@JsonProperty("basePrice")
	private int basePrice;
	@JsonProperty("sellingPrice")
	private int sellingPrice;

	public ItemPrice(int basePrice, int sellingPrice) {
		super();
		this.basePrice = basePrice;
		this.sellingPrice = sellingPrice;
	}

	public int getBasePrice() {
		return basePrice;
	}

	public void setBasePrice(int basePrice) {
		this.basePrice = basePrice;
	}

	public int getSellingPrice() {
		return sellingPrice;
	}

	public void setSellingPrice(int sellingPrice) {
		this.sellingPrice = sellingPrice;
	}

	public ItemPrice() {
		super();
	}

}

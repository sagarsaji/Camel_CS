package com.ust.mycart.item.entity;

import jakarta.validation.constraints.Min;

public class ItemPrice {

	@Min(value = 1, message = "basePrice should be greater than zero")
	private Integer basePrice;
	@Min(value = 1, message = "sellingPrice should be greater than zero")
	private Integer sellingPrice;

	public ItemPrice(@Min(value = 1, message = "basePrice should be greater than zero") Integer basePrice,
			@Min(value = 1, message = "sellingPrice should be greater than zero") Integer sellingPrice) {
		super();
		this.basePrice = basePrice;
		this.sellingPrice = sellingPrice;
	}

	public Integer getBasePrice() {
		return basePrice;
	}

	public void setBasePrice(Integer basePrice) {
		this.basePrice = basePrice;
	}

	public Integer getSellingPrice() {
		return sellingPrice;
	}

	public void setSellingPrice(Integer sellingPrice) {
		this.sellingPrice = sellingPrice;
	}

	public ItemPrice() {
		super();
	}

}

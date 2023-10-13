package com.ust.mycart.item.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StockDetails {

	@JsonProperty("availableStock")
	@NotNull(message = "availableStock should not be null")
	@NotBlank(message = "availableStock should not be blank")
	private int availableStock;
	@JsonProperty("unitOfMeasure")
	@NotNull(message = "unitOfMeasure should not be null")
	@NotBlank(message = "unitOfMeasure should not be blank")
	private String unitOfMeasure;

	public StockDetails(int availableStock, String unitOfMeasure) {
		super();
		this.availableStock = availableStock;
		this.unitOfMeasure = unitOfMeasure;
	}

	public int getAvailableStock() {
		return availableStock;
	}

	public void setAvailableStock(int availableStock) {
		this.availableStock = availableStock;
	}

	public String getUnitOfMeasure() {
		return unitOfMeasure;
	}

	public void setUnitOfMeasure(String unitOfMeasure) {
		this.unitOfMeasure = unitOfMeasure;
	}

	public StockDetails() {
		super();
	}

}

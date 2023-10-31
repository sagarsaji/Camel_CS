package com.ust.mycart.mycartstore.response;

public class StockDetails {

	private String availableStock;
	private String unitOfMeasure;

	public StockDetails(String availableStock, String unitOfMeasure) {
		super();
		this.availableStock = availableStock;
		this.unitOfMeasure = unitOfMeasure;
	}

	public String getavailableStock() {
		return availableStock;
	}

	public void setavailableStock(String availableStock) {
		this.availableStock = availableStock;
	}

	public String getunitOfMeasure() {
		return unitOfMeasure;
	}

	public void setunitOfMeasure(String unitOfMeasure) {
		this.unitOfMeasure = unitOfMeasure;
	}

	public StockDetails() {
		super();
	}

}

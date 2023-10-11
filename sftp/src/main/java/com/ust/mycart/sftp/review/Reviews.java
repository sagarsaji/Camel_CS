package com.ust.mycart.sftp.review;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Reviews")
public class Reviews {

	private List<ItemReview> items;

	public Reviews(List<ItemReview> items) {
		super();
		this.items = items;
	}

	@XmlElement(name = "item")
	public List<ItemReview> getItems() {
		return items;
	}

	public void setItems(List<ItemReview> items) {
		this.items = items;
	}

	public Reviews() {
		super();
	}

}

package com.ust.mycart.sftp.review;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

public class ItemReview {

	private String id;
	private List<Review> review;

	public ItemReview(String id, List<Review> review) {
		super();
		this.id = id;
		this.review = review;
	}

	@XmlAttribute
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@XmlElement(name = "Review")
	public List<Review> getReview() {
		return review;
	}

	public void setReview(List<Review> list) {
		this.review = list;
	}

	public ItemReview() {
		super();
	}

}

package com.ust.mycart.sftp.review;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(propOrder = {"rating","comment"})
public class Review {

	private String rating;
	private String comment;

	public Review(String rating, String comment) {
		super();
		this.rating = rating;
		this.comment = comment;
	}

	@XmlElement(name = "reviewrating")
	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}

	@XmlElement(name = "reviewcomment")
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Review() {
		super();
	}

}

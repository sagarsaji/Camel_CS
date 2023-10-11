package com.ust.mycart.item.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Review {

	@JsonProperty("rating")
	private String rating;
	@JsonProperty("comment")
	private String comment;

	public Review(String rating, String comment) {
		super();
		this.rating = rating;
		this.comment = comment;
	}

	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}

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

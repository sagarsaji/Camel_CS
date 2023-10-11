package com.ust.mycart.sftp.inventory;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "inventory")
public class Inventory {

	private List<Category> categories = new ArrayList<>();

	@XmlElement(name = "category")
	public List<Category> getCategories() {
		return categories;
	}

	public void setCategories(List<Category> categories) {
		this.categories = categories;
	}

	public Inventory() {
		super();
	}

	public Inventory(List<Category> categories) {
		super();
		this.categories = categories;
	}
	
	

}

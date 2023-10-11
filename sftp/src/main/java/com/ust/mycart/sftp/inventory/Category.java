package com.ust.mycart.sftp.inventory;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(propOrder = {"id","name","items"})
public class Category {

	private String id;
	private String name;
	private List<Item> items = new ArrayList<>();

	public Category(String id, String name, List<Item> items) {
		super();
		this.id = id;
		this.name = name;
		this.items = items;
	}

	@XmlAttribute
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@XmlElement(name = "categoryName")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = "item")
	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	public Category() {
		super();
	}

}

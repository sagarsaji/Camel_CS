package com.ust.mycart.mycartstoreprod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.ust.mycart.mycartstoreprod.route.MyCartStoreProdRoute;

@SpringBootApplication
public class MycartstoreprodApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext config =  SpringApplication.run(MycartstoreprodApplication.class, args);
		MyCartStoreProdRoute mycart = config.getBean(MyCartStoreProdRoute.class);
		mycart.sendHeader();
	}

}

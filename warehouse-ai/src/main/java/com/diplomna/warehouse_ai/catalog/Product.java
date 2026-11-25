package com.diplomna.warehouse_ai.catalog;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String sku;
	private String name;
	private String category;
	private String brand;
	private String packageSize;

	private BigDecimal priceEur;

	private int reorderPoint;

	public Product(String sku, String name, String category, String brand, String packageSize, BigDecimal priceEur,
			int reorderPoint) {
		this.sku = sku;
		this.name = name;
		this.category = category;
		this.brand = brand;
		this.packageSize = packageSize;
		this.priceEur = priceEur;
		this.reorderPoint = reorderPoint;
	}

}

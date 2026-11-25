package com.diplomna.warehouse_ai.catalog;

import java.math.BigDecimal;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierProduct {

	@EmbeddedId
	private SupplierProductId id = new SupplierProductId();

	@MapsId("supplierId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Supplier supplier;

	@MapsId("productId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Product product;

	private BigDecimal costEur;

	public SupplierProduct(Supplier supplier, Product product, BigDecimal costEur) {
		this.supplier = supplier;
		this.product = product;
		this.costEur = costEur;
	}

}

package com.diplomna.warehouse_ai.stock;

import java.time.LocalDateTime;

import com.diplomna.warehouse_ai.catalog.Product;
import com.diplomna.warehouse_ai.catalog.Warehouse;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Warehouse warehouse;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Product product;

	private int quantity;

	@Enumerated(EnumType.STRING)
	private MovementType type;

	private LocalDateTime occurredAt;

	private String note;

	public StockMovement(Warehouse warehouse, Product product, int quantity, MovementType type,
			LocalDateTime occurredAt, String note) {
		this.warehouse = warehouse;
		this.product = product;
		this.quantity = quantity;
		this.type = type;
		this.occurredAt = occurredAt;
		this.note = note;
	}

}

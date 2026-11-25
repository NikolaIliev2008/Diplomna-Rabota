package com.diplomna.warehouse_ai.catalog;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierProductId implements Serializable {

	private Long supplierId;
	private Long productId;

}

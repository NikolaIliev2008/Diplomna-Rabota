package com.diplomna.warehouse_ai.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, SupplierProductId> {

	@EntityGraph(attributePaths = "supplier")
	List<SupplierProduct> findByProductIdOrderByCostEurAsc(Long productId);

}

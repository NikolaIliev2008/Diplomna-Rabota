package com.diplomna.warehouse_ai.stock;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

	@Query("""
			select coalesce(sum(m.quantity), 0) from StockMovement m
			where m.warehouse.id = :warehouseId and m.product.id = :productId
			""")
	long stockOf(Long warehouseId, Long productId);

	@Query("""
			select coalesce(sum(m.quantity), 0) from StockMovement m
			where m.warehouse.id = :warehouseId and m.product.id = :productId and m.occurredAt <= :moment
			""")
	long stockAt(Long warehouseId, Long productId, LocalDateTime moment);

	List<StockMovement> findByWarehouseIdAndProductIdOrderByOccurredAtAscIdAsc(Long warehouseId, Long productId);

}

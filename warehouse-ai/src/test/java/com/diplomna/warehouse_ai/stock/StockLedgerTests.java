package com.diplomna.warehouse_ai.stock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.diplomna.warehouse_ai.IntegrationTest;
import com.diplomna.warehouse_ai.catalog.Product;
import com.diplomna.warehouse_ai.catalog.ProductRepository;
import com.diplomna.warehouse_ai.catalog.SupplierProductRepository;
import com.diplomna.warehouse_ai.catalog.Warehouse;
import com.diplomna.warehouse_ai.catalog.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class StockLedgerTests {

	private static final LocalDateTime MONDAY = LocalDateTime.of(2026, 11, 23, 9, 0);

	@Autowired
	StockMovementRepository movements;

	@Autowired
	WarehouseRepository warehouses;

	@Autowired
	ProductRepository products;

	@Autowired
	SupplierProductRepository offers;

	@Autowired
	JdbcTemplate jdbc;

	Warehouse sofia;
	Warehouse plovdiv;
	Product milk;

	@BeforeEach
	void createCatalog() {
		offers.deleteAllInBatch();
		products.deleteAllInBatch();
		warehouses.deleteAllInBatch();
		sofia =warehouses.save(new Warehouse("SOF", "Sofia Warehouse"));
		plovdiv = warehouses.save(new Warehouse("PLD", "Plovdiv Warehouse"));
		milk = products.save(new Product("P-001", "Fresh Milk Alpha 1 l", "Dairy", "Alpha", "1 l",
				new BigDecimal("1.90"), 10));
	}

	@Test
	void stockIsTheSumOfAllMovements() {
		record(sofia, 50, MovementType.RECEIPT, MONDAY);
		record(sofia, -3, MovementType.SALE, MONDAY.plusHours(2));
		record(sofia, -2, MovementType.ADJUSTMENT, MONDAY.plusHours(5));

		assertThat(movements.stockOf(sofia.getId(), milk.getId())).isEqualTo(45);
	}

	@Test
	void stockIsZeroWithoutMovements() {
		assertThat(movements.stockOf(sofia.getId(), milk.getId())).isZero();
	}

	@Test
	void keepsWarehousesSeparate() {
		record(sofia, 10, MovementType.RECEIPT, MONDAY);
		record(plovdiv, 4, MovementType.RECEIPT, MONDAY);

		assertThat(movements.stockOf(sofia.getId(), milk.getId())).isEqualTo(10);
		assertThat(movements.stockOf(plovdiv.getId(), milk.getId())).isEqualTo(4);
	}

	@Test
	void adjustmentCanAddOrRemoveStock() {
		record(sofia, 10, MovementType.RECEIPT, MONDAY);
		record(sofia, 3, MovementType.ADJUSTMENT, MONDAY.plusHours(1));
		record(sofia, -1, MovementType.ADJUSTMENT, MONDAY.plusHours(2));

		assertThat(movements.stockOf(sofia.getId(), milk.getId())).isEqualTo(12);
	}

	@Test
	void showsStockAtAPastMoment() {
		record(sofia, 50, MovementType.RECEIPT, MONDAY);
		record(sofia, -20, MovementType.SALE, MONDAY.plusDays(1));
		record(sofia, -5, MovementType.SALE, MONDAY.plusDays(2));

		assertThat(movements.stockAt(sofia.getId(), milk.getId(), MONDAY.minusMinutes(1))).isZero();
		assertThat(movements.stockAt(sofia.getId(), milk.getId(), MONDAY)).isEqualTo(50);
		assertThat(movements.stockAt(sofia.getId(), milk.getId(), MONDAY.plusDays(1).plusHours(1))).isEqualTo(30);
		assertThat(movements.stockOf(sofia.getId(), milk.getId())).isEqualTo(25);
	}

	@Test
	void listsHistoryInTimeOrder() {
		record(sofia, -3, MovementType.SALE, MONDAY.plusHours(2));
		record(sofia, 50, MovementType.RECEIPT, MONDAY);
		record(plovdiv, 7, MovementType.RECEIPT, MONDAY.plusHours(1));

		assertThat(movements.findByWarehouseIdAndProductIdOrderByOccurredAtAscIdAsc(sofia.getId(), milk.getId()))
				.extracting(StockMovement::getQuantity)
				.containsExactly(50, -3);
	}

	@Test
	void rejectsZeroQuantity() {
		assertThatThrownBy(() -> record(sofia, 0, MovementType.ADJUSTMENT, MONDAY))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsReceiptThatRemovesStock() {
		assertThatThrownBy(() -> record(sofia, -5, MovementType.RECEIPT, MONDAY))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsSaleThatAddsStock() {
		assertThatThrownBy(() -> record(sofia, 5, MovementType.SALE, MONDAY))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsUnknownMovementType() {
		assertThatThrownBy(() -> jdbc.update("""
				insert into stock_movement (warehouse_id, product_id, quantity, type, occurred_at)
				values (?, ?, 5, 'GIFT', now())
				""", sofia.getId(), milk.getId()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void movementCannotBeChanged() {
		StockMovement receipt = record(sofia, 50, MovementType.RECEIPT, MONDAY);

		assertThatThrownBy(() -> jdbc.update("update stock_movement set quantity = 500 where id = ?", receipt.getId()))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("append-only");
	}

	@Test
	void movementCannotBeDeleted() {
		StockMovement receipt = record(sofia, 50, MovementType.RECEIPT, MONDAY);

		assertThatThrownBy(() -> jdbc.update("delete from stock_movement where id = ?", receipt.getId()))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("append-only");
	}

	@Test
	void ledgerCannotBeEmptied() {
		record(sofia, 50, MovementType.RECEIPT, MONDAY);

		assertThatThrownBy(() -> jdbc.execute("truncate stock_movement"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("append-only");
	}

	private StockMovement record(Warehouse warehouse, int quantity, MovementType type, LocalDateTime at) {
		return movements.save(new StockMovement(warehouse, milk, quantity, type, at, null));
	}

}

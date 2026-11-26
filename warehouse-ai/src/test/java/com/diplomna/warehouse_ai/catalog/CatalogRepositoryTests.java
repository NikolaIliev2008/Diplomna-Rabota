package com.diplomna.warehouse_ai.catalog;

import java.math.BigDecimal;
import java.util.List;

import com.diplomna.warehouse_ai.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class CatalogRepositoryTests {

	@Autowired
	WarehouseRepository warehouses;

	@Autowired
	SupplierRepository suppliers;

	@Autowired
	ProductRepository products;

	@Autowired
	SupplierProductRepository offers;

	@Autowired
	JdbcTemplate jdbc;

	@BeforeEach
	void emptyDatabase() {
		offers.deleteAll();
		products.deleteAll();
		suppliers.deleteAll();
		warehouses.deleteAll();
	}

	@Test
	void savesAndReadsProductWithAllFields() {
		Product saved = products.save(product("P-001", "1.90"));

		Product found = products.findById(saved.getId()).orElseThrow();

		assertThat(found.getSku()).isEqualTo("P-001");
		assertThat(found.getName()).isEqualTo("Fresh Milk Alpha 1 l");
		assertThat(found.getCategory()).isEqualTo("Dairy");
		assertThat(found.getBrand()).isEqualTo("Alpha");
		assertThat(found.getPackageSize()).isEqualTo("1 l");
		assertThat(found.getPriceEur()).isEqualByComparingTo("1.90");
		assertThat(found.getReorderPoint()).isEqualTo(10);
	}

	@Test
	void findsProductBySku() {
		products.save(product("P-001", "1.90"));

		assertThat(products.findBySku("P-001")).isPresent();
		assertThat(products.findBySku("P-999")).isEmpty();
	}

	@Test
	void findsWarehouseByCode() {
		warehouses.save(new Warehouse("SOF", "Sofia Warehouse"));

		assertThat(warehouses.findByCode("SOF")).get().extracting(Warehouse::getName).isEqualTo("Sofia Warehouse");
		assertThat(warehouses.findByCode("XXX")).isEmpty();
	}

	@Test
	void listsOffersForProductCheapestFirst() {
		Product milk = products.save(product("P-001", "1.90"));
		Product coffee = products.save(product("P-002", "4.50"));
		Supplier north = suppliers.save(new Supplier("North Supplies", 2));
		Supplier south = suppliers.save(new Supplier("South Supplies", 3));
		Supplier east = suppliers.save(new Supplier("East Supplies", 4));
		offers.save(new SupplierProduct(north, milk, new BigDecimal("1.30")));
		offers.save(new SupplierProduct(south, milk, new BigDecimal("1.10")));
		offers.save(new SupplierProduct(east, milk, new BigDecimal("1.25")));
		offers.save(new SupplierProduct(north, coffee, new BigDecimal("3.00")));

		List<SupplierProduct> milkOffers = offers.findByProductIdOrderByCostEurAsc(milk.getId());

		assertThat(milkOffers)
				.extracting(offer -> offer.getSupplier().getName())
				.containsExactly("South Supplies", "East Supplies", "North Supplies");
	}

	@Test
	void rejectsDuplicateSku() {
		products.save(product("P-001", "1.90"));

		assertThatThrownBy(() -> products.save(product("P-001", "2.50")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsPriceThatIsNotPositive() {
		assertThatThrownBy(() -> products.save(product("P-001", "-5.00")))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> products.save(product("P-002", "0.00")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsSupplierWithoutLeadTime() {
		assertThatThrownBy(() -> suppliers.save(new Supplier("North Supplies", 0)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsSameSupplierAndProductTwice() {
		Product milk = products.save(product("P-001", "1.90"));
		Supplier north = suppliers.save(new Supplier("North Supplies", 2));
		offers.save(new SupplierProduct(north, milk, new BigDecimal("1.30")));

		assertThatThrownBy(() -> jdbc.update(
				"insert into supplier_product (supplier_id, product_id, cost_eur) values (?, ?, ?)",
				north.getId(), milk.getId(), new BigDecimal("1.20")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsOfferForMissingProduct() {
		Supplier north = suppliers.save(new Supplier("North Supplies", 2));

		assertThatThrownBy(() -> jdbc.update(
				"insert into supplier_product (supplier_id, product_id, cost_eur) values (?, ?, ?)",
				north.getId(), -1L, new BigDecimal("1.20")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void keepsProductThatStillHasOffers() {
		Product milk = products.save(product("P-001", "1.90"));
		Supplier north = suppliers.save(new Supplier("North Supplies", 2));
		offers.save(new SupplierProduct(north, milk, new BigDecimal("1.30")));

		assertThatThrownBy(() -> products.deleteById(milk.getId()))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(products.findBySku("P-001")).isPresent();
	}

	private static Product product(String sku, String price) {
		return new Product(sku, "Fresh Milk Alpha 1 l", "Dairy", "Alpha", "1 l", new BigDecimal(price), 10);
	}

}

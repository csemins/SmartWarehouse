package bd.ac.kuet.swms.service;

import bd.ac.kuet.swms.model.RackCell;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class ReportService {
    private final ProductService products;
    private final WarehouseService warehouse;
    private final FinanceService finance;

    public ReportService(ProductService products, WarehouseService warehouse, FinanceService finance) {
        this.products = products;
        this.warehouse = warehouse;
        this.finance = finance;
    }

    record ProductLine(String sku, String name, String category, int boxes, int units, double stockValue, boolean needsRestock) { }

    record CellLine(int cell, String sku, String product, int units) { }

    record InventoryReport(String generatedAt, List<ProductLine> products, List<CellLine> rack, FinanceService.Summary finance) { }

    public void exportInventoryReport(Path file) throws IOException {
        List<ProductLine> productLines = products.findAll().stream()
                .map(p -> new ProductLine(p.getSku(), p.getName(), p.getCategoryName(), p.getBoxes(), p.getQuantity(), p.getStockValue(), p.needsRestock()))
                .toList();
        List<CellLine> cellLines = warehouse.cells().stream()
                .filter(c -> c.getStatus() == RackCell.Status.OCCUPIED)
                .map(c -> new CellLine(c.getId(), c.getSku(), c.getProductName(), c.getQuantity()))
                .toList();
        InventoryReport report = new InventoryReport(LocalDateTime.now().toString(), productLines, cellLines, finance.summary());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(report, writer);
        }
    }
}

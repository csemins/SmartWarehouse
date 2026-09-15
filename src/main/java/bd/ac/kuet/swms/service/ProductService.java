package bd.ac.kuet.swms.service;

import bd.ac.kuet.swms.model.AuditEntry;
import bd.ac.kuet.swms.model.Category;
import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.repository.CategoryRepository;
import bd.ac.kuet.swms.repository.ProductRepository;

import java.util.List;

public class ProductService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final Session session;
    private final AuditService audit;

    public ProductService(ProductRepository products, CategoryRepository categories, Session session, AuditService audit) {
        this.audit = audit;
        this.products = products;
        this.categories = categories;
        this.session = session;
    }

    public List<Product> findAll() { return products.findAll(); }

    public List<Category> categories() { return categories.findAll(); }

    public Product save(Product product, String categoryName) {
        session.require(Permission.MANAGE_PRODUCTS);
        product.setSku(product.getSku().trim());
        products.findBySku(product.getSku())
                .filter(other -> other.getId() != product.getId())
                .ifPresent(other -> { throw new BusinessException("SKU " + product.getSku() + " already exists"); });
        product.setCategoryId(categoryName == null || categoryName.isBlank() ? null : categories.findOrCreate(categoryName).getId());
        boolean created = product.isNew();
        Product saved = products.save(product);
        audit.record(AuditEntry.Category.PRODUCTS, (created ? "Created product " : "Updated product ") + saved.getName());
        return saved;
    }

    public void delete(Product product) {
        session.require(Permission.MANAGE_PRODUCTS);
        if (product.getBoxes() > 0) throw new BusinessException("Retrieve the boxes of this product from the rack first");
        products.delete(product.getId());
        audit.record(AuditEntry.Category.PRODUCTS, "Deleted product " + product.getName());
    }

}

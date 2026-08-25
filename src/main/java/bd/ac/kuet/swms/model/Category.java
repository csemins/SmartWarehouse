package bd.ac.kuet.swms.model;

public class Category extends BaseEntity {
    private String name;

    public Category() { }
    public Category(String name) { this.name = name; }

    @Override public void validate() throws ValidationException { require(hasText(name), "Category name is required"); }
    @Override public String getDisplayName() { return name; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

package bd.ac.kuet.swms.model;

import java.time.LocalDateTime;

public abstract class BaseEntity implements Validatable {
    private int id;
    private LocalDateTime createdAt = LocalDateTime.now();

    public abstract String getDisplayName();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isNew() { return id == 0; }

    protected static void require(boolean condition, String message) throws ValidationException {
        if (!condition) throw new ValidationException(message);
    }

    protected static boolean hasText(String s) { return s != null && !s.isBlank(); }

    @Override public String toString() { return getDisplayName(); }
}

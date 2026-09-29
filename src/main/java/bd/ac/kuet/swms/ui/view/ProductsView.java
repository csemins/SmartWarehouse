package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.Category;
import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.model.RackCell;
import bd.ac.kuet.swms.ui.Icons;
import bd.ac.kuet.swms.ui.Toasts;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import bd.ac.kuet.swms.ui.Dialogs;
import bd.ac.kuet.swms.ui.FxAsync;
import bd.ac.kuet.swms.util.Formats;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.List;

public class ProductsView extends TableFormView<Product> {
    private static final String FIRST_FREE = "First free cell";
    private static final Image PLACEHOLDER = new Image(ProductsView.class.getResourceAsStream("/bd/ac/kuet/swms/img/3d/package.png"));
    private final TextField sku = new TextField();
    private final TextField name = new TextField();
    private final ComboBox<String> category = new ComboBox<>();
    private final TextField costPrice = new TextField();
    private final TextField unitPrice = new TextField();
    private final Slider unitsPerBox = new Slider(1, 50, 1);
    private final Spinner<Integer> reorderLevel = new Spinner<>(0, 100_000, 0);
    private final TextField imageUrl = new TextField();
    private final ImageView photo = new ImageView();

    private final Spinner<Integer> storeBoxes = new Spinner<>(1, RackCell.COUNT, 1);
    private final ComboBox<String> storeCell = new ComboBox<>();
    private final Label storeSummary = new Label();
    private final Label inRack = new Label();
    private final Runnable changeListener = () -> Platform.runLater(this::refresh);
    private List<RackCell> rackCells = List.of();

    @Override public String title() { return "Products"; }

    @Override protected String itemName() { return "product"; }

    @Override
    protected void addColumns() {
        TableColumn<Product, String> picture = addColumn(table, "", Product::getImageUrl, 0.07);
        picture.setCellFactory(c -> new TableCell<>() {
            private final ImageView thumb = new ImageView();

            @Override
            protected void updateItem(String url, boolean empty) {
                super.updateItem(url, empty);
                thumb.setFitWidth(30);
                thumb.setFitHeight(30);
                thumb.setPreserveRatio(true);
                if (empty) { setGraphic(null); return; }
                thumb.setImage(url == null || url.isBlank() ? PLACEHOLDER : new Image(url, 60, 60, true, true, true));
                setGraphic(thumb);
            }
        });
        addColumn(table, "SKU", Product::getSku, 0.15);
        addColumn(table, "Name", Product::getName, 0.22);
        addColumn(table, "Category", Product::getCategoryName, 0.14);
        addColumn(table, "Boxes", Product::getBoxes, 0.09);
        addColumn(table, "Units", Product::getQuantity, 0.09);
        addColumn(table, "Cost", p -> Formats.wholeMoney(p.getCostPrice()), 0.11);
        addColumn(table, "Price", p -> Formats.wholeMoney(p.getUnitPrice()), 0.11);
        table.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                getStyleClass().remove("row-warning");
                if (!empty && p != null && p.needsRestock()) getStyleClass().add("row-warning");
            }
        });
    }

    @Override
    protected void addFormFields() {
        sku.setId("sku");
        sku.setPromptText("e.g. 3017620422003");
        Button lookup = new Button("Look up", Icons.icon("search", 14));
        lookup.setMinWidth(Region.USE_PREF_SIZE);
        lookup.disableProperty().bind(sku.textProperty().isEmpty());
        lookup.setOnAction(e -> lookUpBarcode());
        HBox skuRow = new HBox(6, sku, lookup);
        HBox.setHgrow(sku, Priority.ALWAYS);
        category.setEditable(true);
        unitsPerBox.setShowTickLabels(true);
        unitsPerBox.setMajorTickUnit(10);
        unitsPerBox.setSnapToTicks(false);
        unitsPerBox.setBlockIncrement(1);
        reorderLevel.setEditable(true);
        Label unitsLabel = new Label();
        unitsLabel.textProperty().bind(unitsPerBox.valueProperty().asString("%.0f"));
        photo.setFitWidth(88);
        photo.setFitHeight(88);
        photo.setPreserveRatio(true);
        imageUrl.setPromptText("https://... (filled by the barcode lookup)");
        imageUrl.textProperty().addListener((o, old, url) -> photo.setImage(url == null || url.isBlank() ? PLACEHOLDER
                : new Image(url, 176, 176, true, true, true)));
        StackPane photoFrame = new StackPane(photo);
        photoFrame.getStyleClass().add("photo-frame");
        Label photoHint = hint("Look up any barcode to fetch the name, category and photo from UPCitemdb, or Open Food Facts for food (HTTP + JSON).");
        HBox photoRow = new HBox(14, photoFrame, photoHint);
        photoRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        addFormRow("", photoRow);
        addFormRow("SKU / barcode", skuRow);
        addFormRow("Name", name);
        addFormRow("Category", category);
        addFormRow("Cost price (Tk)", costPrice);
        addFormRow("Selling price (Tk)", unitPrice);
        addFormRow("Units per box", new HBox(8, unitsPerBox, unitsLabel));
        addFormRow("Reorder level (units)", reorderLevel);
        addFormRow("Photo URL", imageUrl);
        sidePanel.getChildren().add(buildStoreCard());
        ctx.warehouse.onChange(changeListener);
    }

    private VBox buildStoreCard() {
        storeBoxes.setEditable(true);
        storeBoxes.setMaxWidth(Double.MAX_VALUE);
        storeCell.setId("quickStoreCell");
        storeCell.setMaxWidth(Double.MAX_VALUE);
        storeCell.getItems().add(FIRST_FREE);
        storeCell.setValue(FIRST_FREE);
        storeSummary.getStyleClass().add("hint");
        inRack.getStyleClass().add("hint");
        inRack.setWrapText(true);
        storeSummary.textProperty().bind(Bindings.createStringBinding(() -> {
            Product p = table.getSelectionModel().getSelectedItem();
            if (p == null) return "Save or select a product first.";
            int boxes = storeBoxes.getValue() == null ? 1 : storeBoxes.getValue();
            return boxes + " box(es) = " + boxes * p.getUnitsPerBox() + " units, cost " + Formats.money(boxes * p.getUnitsPerBox() * p.getCostPrice());
        }, table.getSelectionModel().selectedItemProperty(), storeBoxes.valueProperty()));

        Button store = new Button("Store boxes", Icons.icon("box-in", 14));
        store.setId("storeBoxes");
        store.getStyleClass().add("primary");
        store.setMaxWidth(Double.MAX_VALUE);
        store.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        store.setOnAction(e -> storeSelectedProduct());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(4);
        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        grid.getColumnConstraints().addAll(half, half);
        grid.add(fieldLabel("Boxes"), 0, 0);
        grid.add(fieldLabel("First box goes to"), 1, 0);
        grid.add(storeBoxes, 0, 1);
        grid.add(storeCell, 1, 1);
        return card("Put into inventory", "The crane brings each box from the entry conveyor into the rack",
                inRack, grid, storeSummary, store);
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private void storeSelectedProduct() {
        Product p = table.getSelectionModel().getSelectedItem();
        int boxes = storeBoxes.getValue();
        Integer cell = FIRST_FREE.equals(storeCell.getValue()) ? null : Integer.parseInt(storeCell.getValue().substring(5));
        FxAsync.run(() -> ctx.warehouse.requestStoreBoxes(p, boxes, cell), jobs -> {
            Toasts.show(jobs.size() + " box(es) of " + p.getName() + " queued for the crane");
            storeCell.setValue(FIRST_FREE);
        });
    }

    private void showRackInfo() {
        Product p = table.getSelectionModel().getSelectedItem();
        if (p == null) { inRack.setText(""); return; }
        List<String> used = rackCells.stream().filter(c -> c.getProductId() != null && c.getProductId() == p.getId())
                .map(c -> c.getId() + (c.getStatus() == RackCell.Status.OCCUPIED ? "" : " (" + c.getStatus().name().toLowerCase() + ")")).toList();
        inRack.setText(used.isEmpty() ? p.getName() + " is not in the rack yet."
                : p.getName() + " is in cell " + String.join(", ", used) + ".");
    }

    // fills name/category/photo, user still has to press save
    private void lookUpBarcode() {
        String code = sku.getText().trim();
        FxAsync.run(() -> ctx.barcodeLookup.lookup(code), result -> result.ifPresentOrElse(info -> {
            name.setText(info.name());
            category.getEditor().setText(info.category());
            if (info.imageUrl() != null) imageUrl.setText(info.imageUrl());
            Toasts.show("Found " + info.name() + " on " + info.source());
        }, () -> Dialogs.info("Not found", "Barcode " + code + " was not found on UPCitemdb or Open Food Facts.")));
    }

    @Override
    protected void showInForm(Product p) {
        showRackInfo();
        sku.setText(p == null ? "" : p.getSku());
        name.setText(p == null ? "" : p.getName());
        category.getEditor().setText(p == null || p.getCategoryName() == null ? "" : p.getCategoryName());
        costPrice.setText(p == null ? "" : String.valueOf(p.getCostPrice()));
        unitPrice.setText(p == null ? "" : String.valueOf(p.getUnitPrice()));
        unitsPerBox.setValue(p == null ? 1 : p.getUnitsPerBox());
        reorderLevel.getValueFactory().setValue(p == null ? 0 : p.getReorderLevel());
        imageUrl.setText(p == null || p.getImageUrl() == null ? "" : p.getImageUrl());
    }

    @Override
    protected Product readForm(Product selected) throws Exception {
        Product p = selected == null ? new Product() : selected;
        p.setSku(sku.getText().trim());
        p.setName(name.getText().trim());
        p.setCostPrice(parseNumber(costPrice, "Cost price"));
        p.setUnitPrice(parseNumber(unitPrice, "Selling price"));
        p.setUnitsPerBox((int) Math.round(unitsPerBox.getValue()));
        p.setReorderLevel(reorderLevel.getValue());
        p.setImageUrl(imageUrl.getText().isBlank() ? null : imageUrl.getText().trim());
        return p;
    }

    @Override
    protected List<Product> loadItems() {
        List<String> names = ctx.products.categories().stream().map(Category::getName).toList();
        List<RackCell> cells = ctx.warehouse.cells();
        Platform.runLater(() -> {
            category.getItems().setAll(names);
            rackCells = cells;
            String chosen = storeCell.getValue();
            storeCell.getItems().setAll(FIRST_FREE);
            cells.stream().filter(c -> c.getStatus() == RackCell.Status.EMPTY).forEach(c -> storeCell.getItems().add("Cell " + c.getId()));
            storeCell.setValue(storeCell.getItems().contains(chosen) ? chosen : FIRST_FREE);
            showRackInfo();
        });
        return ctx.products.findAll();
    }

    @Override protected void saveItem(Product p) { ctx.products.save(p, category.getEditor().getText()); }

    @Override protected void deleteItem(Product p) { ctx.products.delete(p); }

    @Override public void dispose() { ctx.warehouse.removeOnChange(changeListener); }

    @Override protected String searchText(Product p) { return p.getSku() + " " + p.getName() + " " + p.getCategoryName(); }
}

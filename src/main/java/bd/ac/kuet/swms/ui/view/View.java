package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.AppContext;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.function.Function;

public abstract class View {
    protected final AppContext ctx = AppContext.get();
    private Node root;

    public abstract String title();

    protected abstract Node build();

    public void refresh() { }

    public void dispose() { }

    public final Node root() {
        if (root == null) {
            root = build();
        }
        return root;
    }

    protected static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("card-title");
        return label;
    }

    protected static VBox card(String title, Node... content) {
        Label label = new Label(title);
        label.getStyleClass().add("card-title");
        VBox box = new VBox(label);
        box.getChildren().addAll(content);
        box.getStyleClass().add("card");
        return box;
    }

    protected static VBox card(String title, String subtitle, Node... content) {
        VBox box = card(title, content);
        box.getChildren().add(1, hint(subtitle));
        return box;
    }

    protected static ScrollPane scrolling(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    protected static Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("hint");
        label.setWrapText(true);
        return label;
    }

    protected static <S, V> void showAsBadge(TableColumn<S, V> column, Function<V, String> badgeStyle) {
        column.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(V value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                if (empty || value == null) { setGraphic(null); return; }
                Label badge = new Label(value.toString());
                badge.getStyleClass().addAll("badge", badgeStyle.apply(value));
                setGraphic(badge);
            }
        });
    }

    protected static <S, V> TableColumn<S, V> addColumn(TableView<S> table, String title, Function<S, V> value, double widthShare) {
        TableColumn<S, V> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(value.apply(cell.getValue())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        column.setPrefWidth(Math.round(widthShare * 1000));
        column.setMinWidth(36);
        table.getColumns().add(column);
        return column;
    }
}

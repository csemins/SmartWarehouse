package bd.ac.kuet.swms.ui.view;

import bd.ac.kuet.swms.model.RackCell;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class RackCellTile extends VBox {
    private final int cellId;
    private final Label number = new Label();
    private final Label product = new Label();
    private final Label units = new Label();
    private RackCell.Status lastStatus;

    public RackCellTile(int cellId) {
        this.cellId = cellId;
        number.setText(String.valueOf(cellId));
        number.getStyleClass().add("cell-number");
        product.getStyleClass().add("cell-product");
        product.setWrapText(true);
        units.getStyleClass().add("cell-number");
        getChildren().addAll(number, product, units);
        getStyleClass().add("rack-cell");
        setAlignment(Pos.TOP_LEFT);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setMinSize(40, 34);
    }

    public int getCellId() { return cellId; }

    private void flash() {
        ScaleTransition pop = new ScaleTransition(Duration.millis(320), this);
        pop.setFromX(0.85);
        pop.setFromY(0.85);
        pop.setToX(1);
        pop.setToY(1);
        pop.setInterpolator(Interpolator.EASE_OUT);
        pop.play();
    }

    public void show(RackCell cell) {
        if (cell.getStatus() != lastStatus && lastStatus != null) flash();
        lastStatus = cell.getStatus();
        getStyleClass().removeAll("occupied", "busy");
        switch (cell.getStatus()) {
            case EMPTY -> {
                product.setText("");
                units.setText("");
            }
            case OCCUPIED -> {
                getStyleClass().add("occupied");
                product.setText(cell.getProductName());
                units.setText(cell.getQuantity() + " units");
            }
            default -> {
                getStyleClass().add("busy");
                product.setText(cell.getProductName());
                units.setText(cell.getStatus() == RackCell.Status.INBOUND ? "storing..." : "shipping...");
            }
        }
        Tooltip.install(this, new Tooltip("Cell " + cellId + ": " + (cell.getStatus() == RackCell.Status.EMPTY ? "empty"
                : cell.getProductName() + " x" + cell.getQuantity() + " (" + cell.getStatus() + ")")));
    }

    public void setSelected(boolean selected) {
        getStyleClass().remove("selected");
        if (selected) getStyleClass().add("selected");
    }
}

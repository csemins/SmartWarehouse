package bd.ac.kuet.swms.ui;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class TableExport {
    private TableExport() { }

    public static void exportToCsv(TableView<?> table, String fileName) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(fileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));
        File file = chooser.showSaveDialog(Navigator.get().stage());
        if (file == null) return;
        List<List<String>> rows = readRows(table);
        FxAsync.run(() -> { writeCsv(rows, file); return rows.size() - 1; },
                count -> Dialogs.info("Export finished", count + " rows written to\n" + file.getAbsolutePath()));
    }

    static List<List<String>> readRows(TableView<?> table) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(table.getColumns().stream().map(TableColumn::getText).toList());
        for (int r = 0; r < table.getItems().size(); r++) {
            List<String> row = new ArrayList<>();
            for (TableColumn<?, ?> column : table.getColumns()) {
                Object value = column.getCellData(r);
                row.add(value == null ? "" : value.toString());
            }
            rows.add(row);
        }
        return rows;
    }

    static void writeCsv(List<List<String>> rows, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (List<String> row : rows) {
                writer.write(String.join(",", row.stream().map(TableExport::quote).toList()));
                writer.newLine();
            }
        }
    }

    private static String quote(String s) {
        return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}

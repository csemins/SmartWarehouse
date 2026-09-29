package bd.ac.kuet.swms.ui;

import javafx.scene.Parent;
import javafx.scene.text.Font;

public final class Theme {
    private static boolean dark;

    private Theme() { }

    public static void loadFonts() {
        for (String weight : new String[]{"Regular", "Medium", "SemiBold", "Bold"}) {
            var font = Theme.class.getResourceAsStream("/bd/ac/kuet/swms/fonts/Inter-" + weight + ".ttf");
            if (font != null) Font.loadFont(font, 13);
        }
    }

    public static boolean isDark() { return dark; }

    public static void setDark(boolean value) { dark = value; }

    // dark mode = one css class on the root, colours are looked-up vars in app.css
    public static void apply(Parent root) {
        root.getStyleClass().remove("dark");
        if (dark) root.getStyleClass().add("dark");
    }
}

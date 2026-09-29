package bd.ac.kuet.swms.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.SVGPath;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class Icons {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("dashboard", "layout-dashboard"), Map.entry("orders", "shopping-cart"), Map.entry("products", "package"),
            Map.entry("movements", "arrow-left-right"), Map.entry("finance", "trending-up"), Map.entry("settings", "sliders-horizontal"),
            Map.entry("logout", "log-out"), Map.entry("alert", "triangle-alert"), Map.entry("money", "dollar-sign"),
            Map.entry("grid", "grid-3x3"), Map.entry("down", "trending-down"), Map.entry("more", "ellipsis"),
            Map.entry("box-in", "package-plus"), Map.entry("box-out", "package-minus"), Map.entry("monitor", "monitor-cog"),
            Map.entry("external", "external-link"));
    private static final Map<String, String> PATHS = loadPaths();
    private static final Map<String, Image> IMAGES = new HashMap<>();

    private Icons() { }

    private static Map<String, String> loadPaths() {
        Properties properties = new Properties();
        try (InputStream in = Icons.class.getResourceAsStream("/bd/ac/kuet/swms/icons/lucide.properties")) {
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, String> paths = new HashMap<>();
        properties.forEach((name, path) -> paths.put((String) name, (String) path));
        return paths;
    }

    public static Node icon(String name, double size) {
        SVGPath path = new SVGPath();
        path.setContent(PATHS.getOrDefault(ALIASES.getOrDefault(name, name), PATHS.get("package")));
        path.getStyleClass().add("icon");
        path.setScaleX(size / 24);
        path.setScaleY(size / 24);
        return new Group(path);
    }

    public static ImageView logo(double size) {
        ImageView view = new ImageView(IMAGES.computeIfAbsent("logo", n -> new Image(Icons.class.getResourceAsStream("/bd/ac/kuet/swms/img/logo.png"))));
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setSmooth(true);
        return view;
    }

    public static ImageView picture(String name, double size) {
        Image image = IMAGES.computeIfAbsent(name, n -> new Image(Icons.class.getResourceAsStream("/bd/ac/kuet/swms/img/3d/" + n + ".png")));
        ImageView view = new ImageView(image);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setSmooth(true);
        return view;
    }
}

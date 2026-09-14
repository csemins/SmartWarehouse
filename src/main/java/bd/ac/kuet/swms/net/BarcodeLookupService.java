package bd.ac.kuet.swms.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Optional;

public class BarcodeLookupService {
    private static final String OPEN_FOOD_FACTS = "https://world.openfoodfacts.org/api/v2/product/";

    private final HttpJsonClient http;
    private final Gson gson = new Gson();

    public record ProductInfo(String name, String category, String imageUrl, String source) { }

    private record FoodProduct(String product_name, String categories, String quantity, String image_front_small_url) { }

    public BarcodeLookupService(HttpJsonClient http) { this.http = http; }

    // TODO only works for food. electronics etc. are never found
    public Optional<ProductInfo> lookup(String barcode) throws Exception {
        if (!barcode.matches("\\d{8,14}")) throw new IllegalArgumentException("A barcode has 8 to 14 digits");
        return parseOpenFoodFacts(http.get(OPEN_FOOD_FACTS + barcode
                + ".json?fields=product_name,categories,quantity,image_front_small_url"));
    }

    public Optional<ProductInfo> parseOpenFoodFacts(JsonElement json) {
        JsonObject root = json.getAsJsonObject();
        if (root.get("status").getAsInt() != 1 || !root.has("product")) return Optional.empty();
        FoodProduct food = gson.fromJson(root.get("product"), FoodProduct.class);
        String name = food.product_name() == null || food.product_name().isBlank() ? "Unnamed product" : food.product_name().trim();
        if (food.quantity() != null && !food.quantity().isBlank()) name += " (" + food.quantity().trim() + ")";
        String category = food.categories() == null || food.categories().isBlank() ? "" : food.categories().split(",")[0].trim();
        return Optional.of(new ProductInfo(name, category, food.image_front_small_url(), "Open Food Facts"));
    }
}

package bd.ac.kuet.swms.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class BarcodeLookupService {
    private static final String UPC_ITEM_DB = "https://api.upcitemdb.com/prod/trial/lookup?upc=";
    private static final String OPEN_FOOD_FACTS = "https://world.openfoodfacts.org/api/v2/product/";

    private final HttpJsonClient http;
    private final Gson gson = new Gson();

    public record ProductInfo(String name, String category, String imageUrl, String source) { }

    private record UpcItemDbReply(String code, List<UpcItem> items) { }

    private record UpcItem(String title, String brand, String category, List<String> images) { }

    private record FoodProduct(String product_name, String categories, String quantity, String image_front_small_url) { }

    public BarcodeLookupService(HttpJsonClient http) { this.http = http; }

    // upcitemdb trial = 100 req/day. food barcodes are better on open food facts
    public Optional<ProductInfo> lookup(String barcode) throws Exception {
        if (!barcode.matches("\\d{8,14}")) throw new IllegalArgumentException("A barcode has 8 to 14 digits");
        Optional<ProductInfo> product;
        try {
            product = parseUpcItemDb(http.get(UPC_ITEM_DB + barcode));
        } catch (IOException e) {
            product = Optional.empty();
        }
        if (product.isPresent()) return product;
        return parseOpenFoodFacts(http.get(OPEN_FOOD_FACTS + barcode
                + ".json?fields=product_name,categories,quantity,image_front_small_url"));
    }

    public Optional<ProductInfo> parseUpcItemDb(JsonElement json) {
        UpcItemDbReply reply = gson.fromJson(json, UpcItemDbReply.class);
        if (!"OK".equals(reply.code()) || reply.items() == null || reply.items().isEmpty()) return Optional.empty();
        UpcItem item = reply.items().get(0);
        String category = item.category() == null || item.category().isBlank() ? "" : item.category().split(">")[0].trim();
        String image = item.images() == null ? null : item.images().stream().filter(url -> url.startsWith("https"))
                .findFirst().orElse(item.images().isEmpty() ? null : item.images().get(0));
        return Optional.of(new ProductInfo(item.title().trim(), category, image, "UPCitemdb"));
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

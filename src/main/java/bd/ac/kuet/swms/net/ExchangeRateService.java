package bd.ac.kuet.swms.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.TreeMap;

public class ExchangeRateService {
    private final HttpJsonClient http;

    public ExchangeRateService(HttpJsonClient http) { this.http = http; }

    public Rates fetch(String baseCurrency) throws Exception {
        return parse(http.get("https://open.er-api.com/v6/latest/" + baseCurrency));
    }

    public static Rates parse(JsonElement json) {
        JsonObject root = json.getAsJsonObject();
        if (!"success".equals(root.get("result").getAsString())) throw new IllegalStateException("Exchange rate service returned an error");
        Map<String, Double> rates = new TreeMap<>();
        root.getAsJsonObject("rates").entrySet().forEach(e -> rates.put(e.getKey(), e.getValue().getAsDouble()));
        return new Rates(root.get("base_code").getAsString(), root.get("time_last_update_utc").getAsString(), rates);
    }

    public record Rates(String base, String updated, Map<String, Double> rates) {
        public double convert(double amount, String currency) {
            Double rate = rates.get(currency);
            if (rate == null) throw new IllegalArgumentException("Unknown currency " + currency);
            return amount * rate;
        }
    }
}

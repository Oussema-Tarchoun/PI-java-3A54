package Services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class WeatherService {
    private static final String API_KEY = "8e1697a296b42b6a9f6d6349971844b2"; // Placeholder - À remplacer par une clé valide
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?q=Tunis&units=metric&appid=" + API_KEY;

    private final OkHttpClient client = new OkHttpClient();

    public WeatherData fetchCurrentWeather() throws IOException {
        Request request = new Request.Builder().url(BASE_URL).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String jsonData = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                
                double temp = jsonObject.getAsJsonObject("main").get("temp").getAsDouble();
                int humidity = jsonObject.getAsJsonObject("main").get("humidity").getAsInt();
                String condition = jsonObject.getAsJsonArray("weather").get(0).getAsJsonObject().get("main").getAsString();
                String description = jsonObject.getAsJsonArray("weather").get(0).getAsJsonObject().get("description").getAsString();
                
                return new WeatherData(temp, humidity, condition, description);
            }
        } catch (Exception e) {
            // Fallback mock data pour la démo si l'API échoue (clé invalide, etc.)
            System.out.println("Mode Démo: Utilisation de données météo simulées.");
            return new WeatherData(22.5, 45, "Clear", "ciel dégagé");
        }
        return new WeatherData(20.0, 50, "Clouds", "nuageux");
    }

    public static class WeatherData {
        public final double temp;
        public final int humidity;
        public final String condition;
        public final String description;

        public WeatherData(double temp, int humidity, String condition, String description) {
            this.temp = temp;
            this.humidity = humidity;
            this.condition = condition;
            this.description = description;
        }

        @Override
        public String toString() {
            return String.format("%.1f°C, %s (%s), Humidité: %d%%", temp, condition, description, humidity);
        }
    }
}

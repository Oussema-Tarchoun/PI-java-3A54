package utils;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherService {

    private static final String API_KEY = "db6f97daac59c73247a5377b230d5e89";

    public JSONObject getWeather(String city) {
        try {
            String urlStr = "https://api.openweathermap.org/data/2.5/weather?q="
                    + city + "&appid=" + API_KEY + "&units=metric";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();

            // 🔴 IMPORTANT : check HTTP status first
            BufferedReader reader;

            if (code == 200) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                System.out.println("API Error Code: " + code);
            }

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            if (code != 200) {
                System.out.println("Response: " + response);
                return null;
            }

            return new JSONObject(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
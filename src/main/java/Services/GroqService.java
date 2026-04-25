package Services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GroqService {
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";
    // REMPLACER PAR VOTRE CLÉ API GROQ

    private final OkHttpClient client;
    private final Gson gson;

    public GroqService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public String generateRecommendation(String type, float consommation, String source, float periode) throws IOException {
        String prompt = String.format(
            "En tant qu'expert en efficacité énergétique, donne-moi un seul conseil très court et percutant en français (maximum 2 phrases) pour ma consommation d'énergie. " +
            "Données : Type d'énergie: %s, Consommation: %.1f unités, Source: %s, Période: %.0f jours. " +
            "Ne fais pas de liste, va droit au but.",
            type, consommation, source, periode
        );

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("model", MODEL);
        
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        
        jsonBody.add("messages", messages);

        RequestBody body = RequestBody.create(
            jsonBody.toString(),
            MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "No error body";
                System.err.println("Groq API Error Detail: " + errBody);
                throw new IOException("Erreur API Groq: " + response.code() + " - " + errBody);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            return jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .get("message").getAsJsonObject()
                    .get("content").getAsString();
        }
    }

    public String generateRawAdvice(String prompt) throws IOException {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("model", MODEL);
        
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        
        jsonBody.add("messages", messages);

        RequestBody body = RequestBody.create(
            jsonBody.toString(),
            MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "No error body";
                System.err.println("Groq Raw Advice Error Detail: " + errBody);
                throw new IOException("Erreur API Groq: " + response.code() + " - " + errBody);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            return jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .get("message").getAsJsonObject()
                    .get("content").getAsString();
        }
    }
}

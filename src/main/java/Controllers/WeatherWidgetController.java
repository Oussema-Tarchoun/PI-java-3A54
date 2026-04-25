package Controllers;

import Services.AIWeatherAdvisorService;
import Services.WeatherService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WeatherWidgetController implements Initializable {

    @FXML private Label weatherIcon;
    @FXML private Label tempLabel;
    @FXML private Label conditionLabel;
    @FXML private Label humidityLabel;
    @FXML private Label aiAdviceLabel;
    @FXML private Label updateLabel;
    @FXML private ProgressIndicator aiLoading;

    private WeatherService weatherService;
    private AIWeatherAdvisorService aiAdvisorService;
    private ScheduledExecutorService scheduler;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        weatherService = new WeatherService();
        aiAdvisorService = new AIWeatherAdvisorService();
        
        startAutoUpdate();
    }

    private void startAutoUpdate() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // Update every 3 minutes (180 seconds)
        scheduler.scheduleAtFixedRate(this::refreshData, 0, 180, TimeUnit.SECONDS);
    }

    public void refreshData() {
        Platform.runLater(() -> aiLoading.setVisible(true));
        
        WeatherService.WeatherData data = null;
        try {
            data = weatherService.fetchCurrentWeather();
            final WeatherService.WeatherData finalData = data;
            
            // On met à jour la météo immédiatement
            Platform.runLater(() -> updateWeatherOnly(finalData));

            // On tente d'avoir le conseil IA
            String advice;
            try {
                advice = aiAdvisorService.getAdvice(finalData);
            } catch (Exception ae) {
                advice = "Mode Économie : Pensez à éteindre les lumières inutiles.";
                ae.printStackTrace();
            }
            
            final String finalAdvice = advice;
            Platform.runLater(() -> {
                aiAdviceLabel.setText(finalAdvice);
                aiLoading.setVisible(false);
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                aiAdviceLabel.setText("Données indisponibles.");
                aiLoading.setVisible(false);
            });
            e.printStackTrace();
        }
    }

    private void updateWeatherOnly(WeatherService.WeatherData data) {
        tempLabel.setText(String.format("%.1f°C", data.temp));
        conditionLabel.setText(data.description.substring(0, 1).toUpperCase() + data.description.substring(1));
        humidityLabel.setText("Humidité: " + data.humidity + "%");
        
        String icon = switch (data.condition.toLowerCase()) {
            case "clear" -> "☀️";
            case "clouds" -> "☁️";
            case "rain", "drizzle" -> "🌧️";
            case "thunderstorm" -> "⛈️";
            case "snow" -> "❄️";
            default -> "⛅";
        };
        weatherIcon.setText(icon);
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        updateLabel.setText("Mis à jour à " + LocalTime.now().format(dtf));
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
    }
}

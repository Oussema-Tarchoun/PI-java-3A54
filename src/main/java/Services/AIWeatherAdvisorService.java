package Services;

import java.io.IOException;

public class AIWeatherAdvisorService {
    private final GroqService groqService;

    public AIWeatherAdvisorService() {
        this.groqService = new GroqService();
    }

    public String getAdvice(WeatherService.WeatherData weather) throws IOException {
        String prompt = String.format(
            "En tant qu'expert en gestion d'énergie intelligente, donne-moi un conseil court et percutant en français basé sur la météo actuelle.\n" +
            "Météo : %.1f°C, Condition: %s, Humidité: %d%%.\n" +
            "Règles :\n" +
            "- Si Soleil: suggérer de charger batteries/optimiser solaire.\n" +
            "- Si Pluie: suggérer de fermer fenêtres/réduire conso externe.\n" +
            "- Si Froid: suggérer chauffage intelligent.\n" +
            "- Sinon: mode économie.\n" +
            "Réponds en une ou deux phrases maximum.",
            weather.temp, weather.condition, weather.humidity
        );

        // On réutilise la structure de GroqService mais avec un prompt personnalisé
        // Note: GroqService.generateRecommendation attend (type, conso, source, periode)
        // On va plutôt utiliser une méthode plus générique si possible ou adapter.
        
        // Pour éviter de casser GroqService, on va juste faire l'appel ici
        return groqService.generateRawAdvice(prompt);
    }
}

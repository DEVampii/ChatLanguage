package com.mod.chatlanguage;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Chatlanguage extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, String> playerLanguages = new HashMap<>();

    // Idiomas disponibles según LibreTranslate
    private final Map<String, String> languages = new HashMap<String, String>() {{
        put("es", "Spanish");
        put("en", "English");
        put("fr", "French");
        put("de", "German");
        put("it", "Italian");
        put("pt", "Portuguese");
        put("ru", "Russian");
        put("zh", "Chinese");
    }};

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("language").setExecutor(this);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("Idiomas disponibles:");
            for (Map.Entry<String, String> entry : languages.entrySet()) {
                player.sendMessage(entry.getKey() + " - " + entry.getValue());
            }
            return true;
        }

        String selectedLanguage = args[0].toLowerCase();

        if (!languages.containsKey(selectedLanguage)) {
            player.sendMessage("Idioma no válido. Usa /language para ver la lista de idiomas.");
            return true;
        }

        playerLanguages.put(player.getUniqueId(), selectedLanguage);
        player.sendMessage("Has cambiado tu idioma a " + languages.get(selectedLanguage));

        return true;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerLanguage = playerLanguages.getOrDefault(player.getUniqueId(), "en"); // Por defecto en inglés

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String translatedMessage = translateMessage(event.getMessage(), playerLanguage);
            event.setMessage(translatedMessage);
        });
    }

    private String translateMessage(String message, String targetLanguage) {
        try {
            String apiUrl = "https://libretranslate.com/translate";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Cuerpo de la solicitud JSON para traducir
            String jsonInputString = String.format(
                    "{\"q\": \"%s\", \"source\": \"en\", \"target\": \"%s\", \"format\": \"text\"}",
                    message, targetLanguage
            );

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Leer la respuesta
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // Extraer el texto traducido de la respuesta JSON
            String translatedText = response.toString();
            return translatedText.split("\"translatedText\":\"")[1].split("\"")[0];

        } catch (Exception e) {
            e.printStackTrace();
            return message; // Si algo falla, devolver el mensaje original
        }
    }
}
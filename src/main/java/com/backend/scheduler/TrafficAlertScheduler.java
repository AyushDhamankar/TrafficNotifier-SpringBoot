package com.backend.scheduler;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.backend.entity.TrafficEntity;
import com.backend.repository.TrafficRepository;
import com.backend.services.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.mail.MessagingException;

@Component
public class TrafficAlertScheduler {

    private final TrafficRepository trafficRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient(); // Use single instance

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Value("${map.base.uri}")
    private String AUTO_BASE_URI;

    @Value("${map.key}")
    private String API_KEY;

    public TrafficAlertScheduler(TrafficRepository trafficRepository, EmailService emailService) {
        this.trafficRepository = trafficRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 * * * * *") // Runs every minute
    @Transactional
    public void executeJob() {
        System.out.println("Traffic alert job started at " + System.currentTimeMillis());

        List<TrafficEntity> schedules = trafficRepository.findAll();
        LocalTime currentTime = LocalTime.now().plusHours(5).plusMinutes(30);
        String formattedTime = currentTime.format(TIME_FORMATTER);

        for (TrafficEntity schedule : schedules) {
            if (schedule.getTime().contains(formattedTime)) {
                System.out.println("Schedule Data: "+ schedule);
                System.out.println(schedule.getSource());
                System.out.println(schedule.getDestination());
                System.out.println(schedule.getExpectedTime());
                System.out.println(schedule.getEmail());
                processTrafficAlert(schedule);
            }
        }

        System.out.println("Traffic alert job completed.");
    }

    private void processTrafficAlert(TrafficEntity schedule) {
        try {
            String encodedSource = URLEncoder.encode(schedule.getSource(), StandardCharsets.UTF_8);
            String encodedDestination = URLEncoder.encode(schedule.getDestination(), StandardCharsets.UTF_8);
            String url = String.format("%s%s&destinations=%s&mode=driving&departure_time=now&key=%s", 
                                        AUTO_BASE_URI, encodedSource, encodedDestination, API_KEY);

            System.out.println("Fetching data from: " + url);

            HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(url)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Failed to fetch data. HTTP Code: " + response.statusCode());
                return;
            }

            JsonNode jsonResponse = objectMapper.readTree(response.body());
            JsonNode elements = jsonResponse.path("rows").get(0).path("elements").get(0);

            JsonNode durationValue = elements.path("duration").path("value");
            JsonNode distanceText = elements.path("distance").path("text");
            JsonNode durationText = elements.path("duration").path("text");
            JsonNode trafficDurationText = elements.path("duration_in_traffic").path("text");

            if (!durationValue.isMissingNode() && schedule.getExpectedTime() > durationValue.asInt()) {
                System.out.println("Sending No Traffic Mail");
                sendEmail(schedule, "Good News! No Traffic 🚗", NoTrafficTemplate, distanceText, durationText, trafficDurationText);
            } else {
                System.out.println("Sending Traffic Mail");
                sendEmail(schedule, "Traffic Alert 🚦", TrafficTemplate, distanceText, durationText, trafficDurationText);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error while processing traffic alert: " + e.getMessage());
        }
    }

    private void sendEmail(TrafficEntity schedule, String subject, String template, JsonNode distance, JsonNode avgTime, JsonNode trafficTime) {
        String emailContent = template
            .replace("[SOURCE_LOCATION]", schedule.getSource())
            .replace("[DESTINATION_LOCATION]", schedule.getDestination())
            .replace("[DISTANCE]", distance.asText())
            .replace("[AVG_TIME]", avgTime.asText())
            .replace("[TRAFFIC_TIME]", trafficTime.asText())
            .replace("[MAP_LINK]", String.format("https://www.google.com/maps/dir/%s/%s", schedule.getSource(), schedule.getDestination()));

            System.out.println("Mail Data: "+schedule.getSource() +" "+ schedule.getEmail());

        try {
            emailService.sendHtmlEmail(schedule.getEmail(), subject, emailContent);
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    private static final String NoTrafficTemplate = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                <title>No Traffic Alert</title>
                <style type="text/css">
                    body { font-family: Arial, sans-serif; background-color: #f6f6f6; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: auto; background: white; padding: 20px; border-radius: 5px; box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.1); }
                    .header { background-color: #28A745; color: white; text-align: center; padding: 10px; font-size: 18px; font-weight: bold; border-radius: 5px 5px 0 0; }
                    .content { padding: 20px; font-size: 14px; color: #333; }
                    .button { display: inline-block; padding: 10px 15px; margin-top: 10px; background-color: #348eda; color: white; text-decoration: none; border-radius: 5px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">Good News! No Traffic 🚗</div>
                    <div class="content">
                        <p>Your route from <strong>[SOURCE_LOCATION]</strong> to <strong>[DESTINATION_LOCATION]</strong> is clear.</p>
                        <p><strong>Distance:</strong> [DISTANCE]</p>
                        <p><strong>Expected Travel Time:</strong> [AVG_TIME]</p>
                        <p>Enjoy a smooth and hassle-free journey.</p>
                        <a href="[MAP_LINK]" class="button">Check Route</a>
                    </div>
                </div>
            </body>
            </html>""";

    private static final String TrafficTemplate = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                <title>Traffic Alert</title>
                <style type="text/css">
                    body { font-family: Arial, sans-serif; background-color: #f6f6f6; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: auto; background: white; padding: 20px; border-radius: 5px; box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.1); }
                    .header { background-color: #FF4C4C; color: white; text-align: center; padding: 10px; font-size: 18px; font-weight: bold; border-radius: 5px 5px 0 0; }
                    .content { padding: 20px; font-size: 14px; color: #333; }
                    .button { display: inline-block; padding: 10px 15px; margin-top: 10px; background-color: #348eda; color: white; text-decoration: none; border-radius: 5px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">Traffic Alert 🚦</div>
                    <div class="content">
                        <p>There is traffic on your route from <strong>[SOURCE_LOCATION]</strong> to <strong>[DESTINATION_LOCATION]</strong>.</p>
                        <p><strong>Distance:</strong> [DISTANCE]</p>
                        <p><strong>Average Travel Time:</strong> [AVG_TIME]</p>
                        <p><strong>Current Traffic Time:</strong> [TRAFFIC_TIME]</p>
                        <p>We recommend leaving early or choosing an alternate route.</p>
                        <a href="[MAP_LINK]" class="button">Check Route</a>
                    </div>
                </div>
            </body>
            </html>""";
}
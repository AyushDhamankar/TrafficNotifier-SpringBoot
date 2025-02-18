package com.backend.scheduler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

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
    private final EmailService emailService; // Inject email service
    ObjectMapper objectMapper = new ObjectMapper();

    public TrafficAlertScheduler(TrafficRepository trafficRepository, EmailService emailService) {
        this.trafficRepository = trafficRepository;
        this.emailService = emailService;
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static String NoTrafficTemplate = """
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
            </html>
                        """;

    private static String TrafficTemplate = """
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
            </html>
                        """;

    @Value("${map.base.uri}")
    private String AUTO_BASE_URI;

    @Value("${map.key}")
    private String API_KEY;

    @Scheduled(cron = "0 * * * * *") // Runs every minute
    @Transactional // Ensures Hibernate session is active
    public void executeJob() throws MessagingException, IOException, InterruptedException {
        System.out.println("Start Job - " + System.currentTimeMillis());
        System.out.println("I am Started");

        List<com.backend.entity.TrafficEntity> schedules = trafficRepository.findAll();
        System.out.println(schedules);

        //String currentTime = LocalTime.now().format(TIME_FORMATTER);

        LocalTime localTime = LocalTime.now();
        LocalTime updatedTime = localTime.plusHours(5).plusMinutes(30); // Add 5 hours and 30 minutes
        String currentTime = updatedTime.format(TIME_FORMATTER);

        for (TrafficEntity schedule : schedules) {
            Set<String> times = schedule.getTime();
            System.out.println(times + " " + currentTime);

            if (times.contains(currentTime)) {
                System.out.println(
                        "Traffic alert for route: " + schedule.getSource() + " to " + schedule.getDestination());

                // String encodedSource = URLEncoder.encode(schedule.getSource(), StandardCharsets.UTF_8);
                // String encodedDestination = URLEncoder.encode(schedule.getDestination(), StandardCharsets.UTF_8);

                // String url = AUTO_BASE_URI + encodedSource +
                //         "&destinations=" + encodedDestination +
                //         "&mode=driving&departure_time=now&key="+ API_KEY;

                String url = AUTO_BASE_URI + schedule.getSource() +
                        "&destinations=" + schedule.getDestination() +
                        "&mode=driving&departure_time=now&key="+ API_KEY;

                System.out.println("Fetching data from: " + url);

                // HTTP Request
                var request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(url))
                        .build();
                var client = HttpClient.newHttpClient();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Response Code: " + response.statusCode());
                System.out.println("Response Body: " + response.body());

                JsonNode jsonResponse = objectMapper.readTree(response.body());
                System.out.println("Parsed JSON: " + jsonResponse);

                // Example: Extract travel duration
                JsonNode rows = jsonResponse.path("rows");
                if (rows.isArray() && !rows.isEmpty()) {
                    JsonNode elements = rows.get(0).path("elements");
                    if (elements.isArray() && !elements.isEmpty()) {
                        JsonNode duration = elements.get(0).path("duration").path("text");
                        System.out.println("Travel Duration: " + duration.asText());
                    }
                }

                JsonNode maptimecmp = jsonResponse.path("rows").get(0).path("elements").get(0).path("duration").path("value");
                JsonNode maptime = jsonResponse.path("rows").get(0).path("elements").get(0).path("duration").path("text");
                JsonNode mapdistance = jsonResponse.path("rows").get(0).path("elements").get(0).path("distance").path("text");
                JsonNode maptraffictime = jsonResponse.path("rows").get(0).path("elements").get(0).path("duration_in_traffic").path("text");

                System.out.println(schedule.getExpectedTime() +" "+ maptimecmp.asInt());
                if (schedule.getExpectedTime() > maptimecmp.asInt()) {
                    System.out.println("Sending Mail");
                    emailService.sendHtmlEmail(schedule.getEmail(), "Good News! No Traffic 🚗",
                        NoTrafficTemplate.replace("[SOURCE_LOCATION]", schedule.getSource())
                        .replace("[DESTINATION_LOCATION]", schedule.getDestination())
                        .replace("[DISTANCE]", String.valueOf(mapdistance))
                        .replace("[AVG_TIME]", String.valueOf(maptime))
                        .replace("[TRAFFIC_TIME]", String.valueOf(maptraffictime))
                        .replace("[MAP_LINK]", "https://www.google.com/maps/dir/"+schedule.getSource()+"/"+schedule.getDestination()));
                } else {
                    emailService.sendHtmlEmail(schedule.getEmail(), "Traffic Alert 🚦",
                        TrafficTemplate.replace("[SOURCE_LOCATION]", schedule.getSource())
                        .replace("[DESTINATION_LOCATION]", schedule.getDestination())
                        .replace("[DISTANCE]", String.valueOf(mapdistance))
                        .replace("[AVG_TIME]", String.valueOf(maptime))
                        .replace("[TRAFFIC_TIME]", String.valueOf(maptraffictime))
                        .replace("[MAP_LINK]", "https://www.google.com/maps/dir/"+schedule.getSource()+"/"+schedule.getDestination()));
                }
            }
        }

        System.out.println("I am Stopped");
    }
}
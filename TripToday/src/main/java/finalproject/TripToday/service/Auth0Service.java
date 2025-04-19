package finalproject.TripToday.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Auth0Service {

    private static final String CLIENT_ID = "yDqKD2YmmwBCbMgx3hEq9P8YZ2jX7B99";
    private static final String CLIENT_SECRET = "30QJIcLVHKOyD7G_4Arnfb5WK5B1ZN6bS2XP64X6dg3TJ7T36rMmcolOtNFrougW";
    private static final String AUDIENCE = "https://dev-an6hxzzvf6uoryjw.us.auth0.com/api/v2/";
    private static final String DOMAIN = "https://dev-an6hxzzvf6uoryjw.us.auth0.com";
    private static final String TOKEN_URL = DOMAIN + "/oauth/token";

    private final RestTemplate restTemplate = new RestTemplate();

    private String cachedToken;
    private long tokenExpirationTime; // in seconds

    private String getApi2Token() {
        // Verificam daca avem un token valid in cache
        if (cachedToken != null && Instant.now().getEpochSecond() < tokenExpirationTime) {
            return cachedToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = Map.of(
                "grant_type", "client_credentials",
                "client_id", CLIENT_ID,
                "client_secret", CLIENT_SECRET,
                "audience", AUDIENCE
        );

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(TOKEN_URL, HttpMethod.POST, requestEntity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            cachedToken = response.getBody().get("access_token").toString();

            // Obținem durata de viață a tokenului în secunde (default Auth0: 24h = 86400 secunde)
            Integer expiresIn = (Integer) response.getBody().getOrDefault("expires_in", 3600); // fallback 1 ora

            // Calculam momentul expirarii
            tokenExpirationTime = Instant.now().getEpochSecond() + expiresIn - 60; // scadem 60 secunde ca siguranta

            return cachedToken;
        } else {
            throw new RuntimeException("Could not retrieve access token");
        }
    }

    public String getGuideDescription(String userId) {
        String url = DOMAIN + "/api/v2/users/" + userId;
        String accessToken = "Bearer " + getApi2Token();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> userMetadata = (Map<String, Object>) response.getBody().get("user_metadata");
                if (userMetadata != null && userMetadata.containsKey("description")) {
                    return (String) userMetadata.get("description");
                }
            }
        } catch (HttpClientErrorException.NotFound ex) {
            System.out.println("User not found in Auth0: " + userId);
        } catch (HttpClientErrorException ex) {
            System.out.println("Client error when retrieving user: " + ex.getStatusCode());
        } catch (Exception ex) {
            System.out.println("Unexpected error: " + ex.getMessage());
        }

        return "";
    }

    public Map<String, String> getGuideIdAndEmail(String guideId) {
        String url = DOMAIN + "/api/v2/users/" + guideId;
        String accessToken = "Bearer " + getApi2Token();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Map.of(
                        "guideId", guideId,
                        "guideEmail", (String) response.getBody().get("email")
                );
            }
        } catch (HttpClientErrorException.NotFound ex) {
            System.out.println("Guide not found in Auth0: " + guideId);
        } catch (HttpClientErrorException ex) {
            System.out.println("Client error when retrieving guide: " + ex.getStatusCode());
        } catch (Exception ex) {
            System.out.println("Unexpected error: " + ex.getMessage());
        }

        return Map.of(
                "guideId", guideId,
                "guideEmail", ""
        );
    }

    public List<Map<String, String>> getAllGuides() {
        String roleId = "rol_y9CbKaiBPr8RoikW";
        String url = DOMAIN + "/api/v2/roles/" + roleId + "/users";
        String accessToken = "Bearer " + getApi2Token();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> users = response.getBody();

                List<Map<String, String>> guides = new ArrayList<>();
                for (Map<String, Object> user : users) {
                    if (user.containsKey("email") && user.containsKey("user_id")) {
                        Map<String, String> guide = new HashMap<>();
                        guide.put("id", (String) user.get("user_id"));  // ID-ul ghidului
                        guide.put("email", (String) user.get("email"));  // Email-ul ghidului
                        guides.add(guide);
                    }
                }

                return guides;
            }
        } catch (HttpClientErrorException.NotFound ex) {
            System.out.println("Role not found in Auth0: " + roleId);
        } catch (HttpClientErrorException ex) {
            System.out.println("Client error when retrieving role users: " + ex.getStatusCode());
        } catch (Exception ex) {
            System.out.println("Unexpected error: " + ex.getMessage());
        }

        return new ArrayList<>(); // Dacă nu găsește nimic, întoarce listă goală
    }


}

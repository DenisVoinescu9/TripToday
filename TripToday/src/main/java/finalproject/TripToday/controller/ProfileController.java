package finalproject.TripToday.controller;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


@Controller
public class ProfileController {

    private static final String CLIENT_ID = "yDqKD2YmmwBCbMgx3hEq9P8YZ2jX7B99";
    private static final String CLIENT_SECRET = "30QJIcLVHKOyD7G_4Arnfb5WK5B1ZN6bS2XP64X6dg3TJ7T36rMmcolOtNFrougW";
    private static final String AUDIENCE = "https://dev-an6hxzzvf6uoryjw.us.auth0.com/api/v2/";
    private static final String DOMAIN = "https://dev-an6hxzzvf6uoryjw.us.auth0.com";
    private static final String TOKEN_URL = DOMAIN + "/oauth/token";

    private String getApi2Token() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = Map.of("grant_type", "client_credentials", "client_id", CLIENT_ID, "client_secret", CLIENT_SECRET, "audience", AUDIENCE);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(TOKEN_URL, HttpMethod.POST, requestEntity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().get("access_token").toString();
        } else {
            throw new RuntimeException("Could not retrieve access token");
        }
    }

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal OidcUser principal) {
        if (principal != null) {
            model.addAttribute("user", principal.getClaims());
        }
        assert principal != null;
       String userId = principal.getName();

        String description = getGuideDescription(userId);
        model.addAttribute("description", description);


        return "profile";
    }

    public String getGuideDescription(String userId) {
        String url = "https://dev-an6hxzzvf6uoryjw.us.auth0.com/api/v2/users/" + userId;
        String accessToken = "Bearer " + getApi2Token();

        // Configurare headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);

        // Configurare request pentru obținerea datelor utilizatorului
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        // Efectuare cerere GET pentru a obține detaliile utilizatorului
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        // Verificăm dacă cererea a fost cu succes
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            // Extragem metadata utilizatorului din răspuns
            Map<String, Object> userMetadata = (Map<String, Object>) response.getBody().get("user_metadata");

            // Verificăm dacă există descrierea
            if (userMetadata != null && userMetadata.containsKey("description")) {
                return (String) userMetadata.get("description");
            }
        }

        // Dacă nu există descriere, returnăm un string gol
        return "";
    }

}








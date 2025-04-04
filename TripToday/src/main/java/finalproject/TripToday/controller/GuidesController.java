package finalproject.TripToday.controller;
import org.springframework.http.*;
import org.springframework.ui.Model;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Controller
public class GuidesController {

    private static final String CLIENT_ID = "yDqKD2YmmwBCbMgx3hEq9P8YZ2jX7B99";
    private static final String CLIENT_SECRET = "30QJIcLVHKOyD7G_4Arnfb5WK5B1ZN6bS2XP64X6dg3TJ7T36rMmcolOtNFrougW";
    private static final String AUDIENCE = "https://dev-an6hxzzvf6uoryjw.us.auth0.com/api/v2/";
    private static final String DOMAIN = "https://dev-an6hxzzvf6uoryjw.us.auth0.com";

    private static final String TOKEN_URL = DOMAIN + "/oauth/token";

    private String getApi2Token() {
        RestTemplate restTemplate = new RestTemplate();

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
            return response.getBody().get("access_token").toString();
        } else {
            throw new RuntimeException("Could not retrieve access token");
        }
    }

    @GetMapping("/guides")
    public String guides(Model model) {

        String url = "https://dev-an6hxzzvf6uoryjw.us.auth0.com/api/v2/roles/rol_y9CbKaiBPr8RoikW/users";
        String accessToken = "Bearer " + getApi2Token();


        // Configurare headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);

        // Configurare request
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        // Efectuare cerere GET
        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
        List<Map<String, Object>> users = response.getBody();

        // Adăugare date în model pentru Thymeleaf
        model.addAttribute("users", users);

        return "guides";


    }


}

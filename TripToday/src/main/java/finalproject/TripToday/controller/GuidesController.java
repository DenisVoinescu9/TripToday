package finalproject.TripToday.controller;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
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

        Map<String, String> requestBody = Map.of("grant_type", "client_credentials", "client_id", CLIENT_ID, "client_secret", CLIENT_SECRET, "audience", AUDIENCE);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(TOKEN_URL, HttpMethod.POST, requestEntity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().get("access_token").toString();
        } else {
            throw new RuntimeException("Could not retrieve access token");
        }
    }

    @GetMapping("/guides")
    public String guides(Model model, @AuthenticationPrincipal OidcUser principal) {
        String accessToken = "Bearer " + getApi2Token();

        String getARolesUsersAPI = "https://dev-an6hxzzvf6uoryjw.us.auth0.com/api/v2/roles/rol_y9CbKaiBPr8RoikW/users";
        String getUserByIdAPI = "https://dev-an6hxzzvf6uoryjw.us.auth0.com/api/v2/users/";

        // Configurare headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);

        // Configurare request pentru obținerea ghidurilor
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        // Efectuare cerere GET pentru ghiduri
        ResponseEntity<List> response = restTemplate.exchange(getARolesUsersAPI, HttpMethod.GET, entity, List.class);
        List<Map<String, Object>> guides = response.getBody();

        // Iterăm prin lista de ghiduri și facem un request pentru fiecare pentru a obține "user_metadata.description"
        if (guides != null && !guides.isEmpty()) {
            for (Map<String, Object> guide : guides) {
                String userId = (String) guide.get("user_id");  // presupunem că există un câmp user_id în fiecare guide

                String userApiUrl = getUserByIdAPI + "/" + userId;

                HttpEntity<String> userEntity = new HttpEntity<>(headers);

                ResponseEntity<Map> userResponse = restTemplate.exchange(userApiUrl, HttpMethod.GET, userEntity, Map.class);
                Map<String, Object> userDetails = userResponse.getBody();

                System.out.println(userDetails);
                assert userDetails != null;
                if (userDetails.containsKey("user_metadata")) {
                    Map<String, Object> userMetadata = (Map<String, Object>) userDetails.get("user_metadata");

                    if (userMetadata.containsKey("description")) {

                        if (userMetadata.get("description").toString().trim().isEmpty())

                            // user.user_metadata has key description but it's empty
                            guide.put("description", "The guide has no description...");
                        else guide.put("description", userMetadata.get("description").toString());
                    } else {

                        // user.user_metadata doesn't have key 'description'
                        guide.put("description", "The guide has no description.");
                    }
                } else {

                    // user.user_metadata is null
                    guide.put("description", "The guide has no description.");
                }


            }


        }


        // Adăugare date în model pentru Thymeleaf
        model.addAttribute("guides", guides);

        return "guides";
    }


}

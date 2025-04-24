package finalproject.TripToday.service;

// --- IMPORTURI NECESARE ---
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder; // Importă RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration; // Optional: pentru timeout-uri
import java.time.Instant;
import java.util.*;


@Service
public class Auth0Service {

    private static final Logger logger = LoggerFactory.getLogger(Auth0Service.class);

    // --- CONSTANTELE tale ---
    private static final String CLIENT_ID = "yDqKD2YmmwBCbMgx3hEq9P8YZ2jX7B99";
    private static final String CLIENT_SECRET = "30QJIcLVHKOyD7G_4Arnfb5WK5B1ZN6bS2XP64X6dg3TJ7T36rMmcolOtNFrougW"; // NU stoca în cod în producție!
    private static final String AUDIENCE = "https://dev-an6hxzzvf6uoryjw.us.auth0.com/api/v2/";
    private static final String DOMAIN = "https://dev-an6hxzzvf6uoryjw.us.auth0.com";
    private static final String TOKEN_URL = DOMAIN + "/oauth/token";
    private static final String USERS_API_URL_BASE = DOMAIN + "/api/v2/users/"; // Doar baza
    private static final String ROLES_API_URL_BASE = DOMAIN + "/api/v2/roles/"; // Doar baza
    private static final String GUIDE_ROLE_ID = "rol_y9CbKaiBPr8RoikW"; // Externalizează sau numește mai generic

    private final RestTemplate restTemplate; // Acum e final din nou

    // --- Cache pentru token ---
    private String cachedToken;
    private long tokenExpirationTime;

    // --- CONSTRUCTOR care injectează RestTemplateBuilder ---
    public Auth0Service(RestTemplateBuilder builder) {
        // Spring Boot va configura automat builder-ul să folosească
        // Apache HttpClient 5 dacă detectează dependența pe classpath.
        // Poți customiza aici, de ex. timeout-uri:
        // this.restTemplate = builder
        //         .setConnectTimeout(Duration.ofSeconds(5))
        //         .setReadTimeout(Duration.ofSeconds(10))
        //         .build();
        this.restTemplate = builder.build(); // Construiește instanța
        logger.info("Auth0Service initialized with RestTemplate configured by RestTemplateBuilder.");
    }

    // --- RESTUL METODELOR (getApi2Token, getGuideDescription, updateGuideDescription etc.) ---
    // NU SE SCHIMBĂ NIMIC ÎN LOGICA LOR INTERNĂ, ele vor folosi acum
    // instanța 'this.restTemplate' configurată de builder.

    // --- Metoda getApi2Token (rămâne la fel ca în versiunea anterioară cu logging) ---
    private String getApi2Token() {
        if (cachedToken != null && Instant.now().getEpochSecond() < tokenExpirationTime) {
            logger.debug("Using cached Auth0 API token.");
            return cachedToken;
        }
        logger.info("Fetching new Auth0 API token...");
        // ... (restul logicii try-catch pentru fetch token) ...
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("client_id", CLIENT_ID);
        requestBody.add("client_secret", CLIENT_SECRET);
        requestBody.add("audience", AUDIENCE);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(TOKEN_URL, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                cachedToken = (String) response.getBody().get("access_token");
                if (cachedToken == null || cachedToken.trim().isEmpty()) {
                    logger.error("Auth0 returned OK status but token is missing or empty in response body.");
                    throw new RuntimeException("Auth0 returned OK status but token is missing or empty.");
                }
                Integer expiresIn = (Integer) response.getBody().getOrDefault("expires_in", 3600); // fallback 1 ora
                tokenExpirationTime = Instant.now().getEpochSecond() + expiresIn - 60; // Marja de siguranta
                logger.info("Successfully fetched new Auth0 API token."); // Nu loga token-ul întreg!
                return cachedToken;
            } else {
                String responseBodyStr = response.getBody() != null ? response.getBody().toString() : "null";
                logger.error("Failed to retrieve access token. Status: {}, Body: {}", response.getStatusCode(), responseBodyStr);
                throw new RuntimeException("Could not retrieve access token, status code: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            logger.error("HTTP Client Error fetching Auth0 API token: {} - Response Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("HTTP Error ("+ e.getStatusCode() + ") fetching Auth0 API token", e);
        } catch (ResourceAccessException e) {
            logger.error("Network Error fetching Auth0 API token: {}", e.getMessage(), e);
            throw new RuntimeException("Network error while fetching Auth0 API token", e);
        } catch (Exception e) {
            logger.error("Unexpected error fetching Auth0 API token: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected technical error fetching Auth0 API token", e);
        }
    }


    // --- Metodele getGuideDescription, getGuideIdAndEmail, getAllGuides (rămân la fel) ---
    public String getGuideDescription(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("getGuideDescription called with null or empty userId.");
            return "";
        }
        String url = USERS_API_URL_BASE + userId.trim(); // Trim userId
        logger.debug("Fetching user data for description from URL: {}", url);
        String accessToken = getApi2Token(); // Poate arunca exceptie daca nu obtine token

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.debug("Successfully fetched user data for userId: {}", userId);
                Map<String, Object> userMetadata = (Map<String, Object>) response.getBody().getOrDefault("user_metadata", Collections.emptyMap());
                String description = (String) userMetadata.get("description");
                if (description != null) {
                    logger.debug("Found description for userId {}: '{}'", userId, description);
                    return description;
                } else {
                    logger.debug("User {} found, but no description in user_metadata.", userId);
                    return "";
                }
            } else {
                logger.warn("Received non-OK status {} when fetching user data for {}", response.getStatusCode(), userId);
                return "";
            }
        } catch (HttpClientErrorException.NotFound ex) {
            logger.warn("User not found in Auth0 when getting description: {}", userId);
            return "";
        } catch (HttpClientErrorException ex) {
            logger.error("HTTP Client Error getting user data for {}: {} - {}", userId, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            return "";
        } catch (ResourceAccessException ex) {
            logger.error("Network Error getting user data for {}: {}", userId, ex.getMessage(), ex);
            return "";
        } catch (Exception ex) {
            logger.error("Unexpected error getting user data for {}: {}", userId, ex.getMessage(), ex);
            return "";
        }
    }

    // ... getGuideIdAndEmail ... (rămâne la fel)
    public Map<String, String> getGuideIdAndEmail(String guideId) {
        if (guideId == null || guideId.trim().isEmpty()) {
            logger.warn("getGuideIdAndEmail called with null or empty guideId.");
            return Map.of("guideId", Objects.requireNonNullElse(guideId, ""), "guideEmail", "");
        }
        String url = USERS_API_URL_BASE + guideId.trim();
        logger.debug("Fetching guide email from URL: {}", url);
        String accessToken = getApi2Token(); // Poate arunca exceptie

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String email = (String) response.getBody().get("email");
                if (email != null) {
                    logger.debug("Found email '{}' for guideId {}", email, guideId);
                    return Map.of("guideId", guideId, "guideEmail", email);
                } else {
                    logger.warn("Guide {} found, but email is null in response.", guideId);
                }
            } else {
                logger.warn("Received non-OK status {} when fetching guide email for {}", response.getStatusCode(), guideId);
            }
        } catch (HttpClientErrorException.NotFound ex) {
            logger.warn("Guide not found in Auth0 when getting email: {}", guideId);
        } catch (HttpClientErrorException ex) {
            logger.error("HTTP Client Error getting guide email for {}: {} - {}", guideId, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } catch (ResourceAccessException ex) {
            logger.error("Network Error getting guide email for {}: {}", guideId, ex.getMessage(), ex);
        } catch (Exception ex) {
            logger.error("Unexpected error getting guide email for {}: {}", guideId, ex.getMessage(), ex);
        }
        return Map.of("guideId", guideId, "guideEmail", "");
    }


    // ... getAllGuides ... (rămâne la fel)
    public List<Map<String, String>> getAllGuides() {
        String url = ROLES_API_URL_BASE + GUIDE_ROLE_ID + "/users";
        logger.debug("Fetching all guides with role {} from URL: {}", GUIDE_ROLE_ID, url);
        String accessToken = getApi2Token(); // Poate arunca exceptie

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        List<Map<String, String>> guides = new ArrayList<>();

        try {
            ParameterizedTypeReference<List<Map<String, Object>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<List<Map<String, Object>>> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, responseType);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> users = response.getBody();
                logger.info("Found {} users with role {}", users.size(), GUIDE_ROLE_ID);

                for (Map<String, Object> user : users) {
                    Object userIdObj = user.get("user_id");
                    Object emailObj = user.get("email");

                    if (userIdObj instanceof String && emailObj instanceof String) {
                        String userId = (String) userIdObj;
                        String email = (String) emailObj;
                        Map<String, String> guide = new HashMap<>();
                        guide.put("id", userId);
                        guide.put("email", email);
                        guides.add(guide);
                    } else {
                        logger.warn("Skipping user due to missing or invalid email/user_id: {}", user);
                    }
                }
                return guides;
            } else {
                logger.error("Failed to retrieve users for role {}. Status: {}", GUIDE_ROLE_ID, response.getStatusCode());
            }
        } catch (HttpClientErrorException.NotFound ex) {
            logger.warn("Role not found in Auth0 when getting guides: {}", GUIDE_ROLE_ID);
        } catch (HttpClientErrorException ex) {
            logger.error("HTTP Client Error getting guides for role {}: {} - {}", GUIDE_ROLE_ID, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } catch (ResourceAccessException ex) {
            logger.error("Network Error getting guides for role {}: {}", GUIDE_ROLE_ID, ex.getMessage(), ex);
        } catch (Exception ex) {
            logger.error("Unexpected error getting guides for role {}: {}", GUIDE_ROLE_ID, ex.getMessage(), ex);
        }
        return guides;
    }

    // --- Metodele updatePicture și updateGuideDescription (rămân la fel) ---
    public void updatePicture(String userId, String imageUrl) {
        if (userId == null || userId.trim().isEmpty() || imageUrl == null || imageUrl.trim().isEmpty()) {
            logger.warn("updatePicture called with null or empty userId or imageUrl.");
            throw new IllegalArgumentException("User ID and Image URL cannot be empty.");
        }
        String url = USERS_API_URL_BASE + userId.trim();
        logger.debug("Attempting to update picture for userId {} to URL: {}", userId, imageUrl);
        String accessToken = null;
        try {
            accessToken = getApi2Token();
            logger.debug("Successfully obtained token for updatePicture");
        } catch (Exception e) {
            logger.error("Failed to get API token before updating picture for user {}", userId, e);
            throw new RuntimeException("Update failed: Could not obtain API token.", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> requestBody = Map.of("picture", imageUrl.trim());
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
        logger.debug("Attempting PATCH request to URL: {}", url);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Void.class);
            logger.info("Picture update PATCH request for user {} completed with status: {}", userId, response.getStatusCode());
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Picture update for user {} failed with non-2xx status: {}", userId, response.getStatusCode());
                throw new RuntimeException("Picture update failed, status code: " + response.getStatusCode());
            }
            logger.info("Profile picture updated successfully for user: {}", userId);
        } catch (HttpClientErrorException ex) {
            logger.error("HTTP Client Error during picture update for user {}: {} - {}", userId, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new RuntimeException("Picture update failed: API error " + ex.getStatusCode(), ex);
        } catch (ResourceAccessException ex) {
            logger.error("Network Error during picture update for user {}: {}", userId, ex.getMessage(), ex);
            throw new RuntimeException("Picture update failed: Network error", ex);
        } catch (Exception e) {
            logger.error("Unexpected Error during picture update for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Picture update failed: Unexpected technical error", e);
        }
    }

    public void updateGuideDescription(String userId, String description) {
        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("updateGuideDescription called with null or empty userId.");
            throw new IllegalArgumentException("User ID cannot be empty.");
        }
        String descriptionToSave = description != null ? description : "";
        String url = USERS_API_URL_BASE + userId.trim();
        logger.debug("Attempting to update description for userId {} to: '{}'", userId, descriptionToSave);
        String accessToken = null;
        try {
            accessToken = getApi2Token();
            logger.debug("Successfully obtained token for updateGuideDescription");
        } catch (Exception e) {
            logger.error("Failed to get API token before updating description for user {}", userId, e);
            throw new RuntimeException("Update failed: Could not obtain API token.", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> userMetadata = new HashMap<>();
        userMetadata.put("description", descriptionToSave);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_metadata", userMetadata);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        logger.debug("Attempting PATCH request to URL: {}", url);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Void.class);
            logger.info("Description update PATCH request for user {} completed with status: {}", userId, response.getStatusCode());
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Description update for user {} failed with non-2xx status: {}", userId, response.getStatusCode());
                throw new RuntimeException("Description update failed, status code: " + response.getStatusCode());
            }
            logger.info("Guide description updated successfully for user: {}", userId);
        } catch (HttpClientErrorException ex) {
            logger.error("HTTP Client Error during description update for user {}: {} - {}", userId, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new RuntimeException("Description update failed: API error " + ex.getStatusCode(), ex);
        } catch (ResourceAccessException ex) {
            logger.error("Network Error during description update for user {}: {}", userId, ex.getMessage(), ex);
            throw new RuntimeException("Description update failed: Network error", ex);
        } catch (Exception e) {
            logger.error("Unexpected Error during description update for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Description update failed: Unexpected technical error", e);
        }
    }

}
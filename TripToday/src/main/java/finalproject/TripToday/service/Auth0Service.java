package finalproject.TripToday.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException; // Import needed
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class Auth0Service {

    private final String clientId;
    private final String clientSecret;
    private final String audience;
    private final String guideRoleId;

    private final String tokenUrl;
    private final String usersApiBaseUrl;
    private final String rolesApiBaseUrl;

    private final RestTemplate restTemplate;

    private volatile String cachedApiToken = null;
    private volatile Instant apiTokenExpiryTime = Instant.MIN;
    private final Object apiTokenLock = new Object();

    public Auth0Service(RestTemplateBuilder builder,
                        @Value("${AUTH0_CLIENT_ID}") String clientId,
                        @Value("${AUTH0_CLIENT_SECRET}") String clientSecret,
                        @Value("${AUTH0_AUDIENCE}") String audience,
                        @Value("${AUTH0_ISSUER}") String issuerUrl, // Parameter still needed
                        @Value("${AUTH0_ROLE_GUIDE_ID}") String guideRoleId) {
        this.restTemplate = builder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.audience = audience;
        this.guideRoleId = guideRoleId;
        this.tokenUrl = issuerUrl + "/oauth/token";
        String apiBase = "";
        if (audience != null && !audience.trim().isEmpty()) {
            apiBase = audience.endsWith("/") ? audience : audience + "/";
        }
        this.usersApiBaseUrl = apiBase + "users/";
        this.rolesApiBaseUrl = apiBase + "roles/";
    }


    public String getApiToken() {
        Instant now = Instant.now();
        if (cachedApiToken != null && now.isBefore(apiTokenExpiryTime.minus(60, ChronoUnit.SECONDS))) {
            return cachedApiToken;
        }

        synchronized (apiTokenLock) {
            now = Instant.now();
            if (cachedApiToken == null || now.isAfter(apiTokenExpiryTime.minus(60, ChronoUnit.SECONDS))) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                    MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
                    requestBody.add("grant_type", "client_credentials");
                    requestBody.add("client_id", this.clientId);
                    requestBody.add("client_secret", this.clientSecret);
                    requestBody.add("audience", this.audience);

                    HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

                    ResponseEntity<Map> response = restTemplate.exchange(this.tokenUrl, HttpMethod.POST, requestEntity, Map.class);

                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        Map<String, Object> responseBody = response.getBody();
                        String token = Optional.ofNullable(responseBody.get("access_token"))
                                .map(Object::toString)
                                .filter(t -> !t.trim().isEmpty())
                                .orElseThrow(() -> new RuntimeException("Auth0 response OK but 'access_token' missing or empty."));

                        int expiresInSeconds = Optional.ofNullable(responseBody.get("expires_in"))
                                .map(Object::toString)
                                .map(Integer::parseInt)
                                .orElse(3600);

                        this.cachedApiToken = token;
                        this.apiTokenExpiryTime = Instant.now().plusSeconds(expiresInSeconds);

                    } else {
                        throw new RuntimeException("Could not retrieve Auth0 API token, status code: " + response.getStatusCode());
                    }
                } catch (RestClientException e) {
                    this.cachedApiToken = null;
                    this.apiTokenExpiryTime = Instant.MIN;
                    throw new RuntimeException("Failed to fetch Auth0 API token: " + e.getMessage(), e);
                } catch (Exception e) {
                    this.cachedApiToken = null;
                    this.apiTokenExpiryTime = Instant.MIN;
                    throw new RuntimeException("Unexpected error fetching Auth0 API token: " + e.getMessage(), e);
                }
            }
            return cachedApiToken;
        }
    }

    public String getGuideDescription(String userId) {
        if (this.usersApiBaseUrl == null || this.usersApiBaseUrl.isEmpty() || userId == null || userId.trim().isEmpty()) {
            return "";
        }
        String url = this.usersApiBaseUrl + userId.trim();
        String accessToken = getApiToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Optional.ofNullable(response.getBody().get("user_metadata"))
                        .filter(um -> um instanceof Map)
                        .map(um -> ((Map<?, ?>) um).get("description"))
                        .map(Object::toString)
                        .orElse("");
            }
        } catch (RestClientException e) {
            // Existing handling - kept as is
            System.out.println(e.getMessage());
        }
        return "";
    }


    public Map<String, String> getGuideIdAndEmail(String guideId) {
        Map<String, String> defaultResult = Map.of("guideId", Objects.requireNonNullElse(guideId, ""), "guideEmail", "");
        if (this.usersApiBaseUrl == null || this.usersApiBaseUrl.isEmpty() || guideId == null || guideId.trim().isEmpty()) {
            return defaultResult;
        }
        String url = this.usersApiBaseUrl + guideId.trim();
        String accessToken = getApiToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String email = Optional.ofNullable(response.getBody().get("email"))
                        .map(Object::toString)
                        .orElse(null);
                if (email != null) {
                    return Map.of("guideId", guideId, "guideEmail", email);
                }
            }
        }
        catch (Exception e) {
            // Existing handling - kept as is
            System.out.println(e.getMessage());
        }
        return defaultResult;
    }

    // START: Added Method
    public Map<String, Object> getUserDetails(String userId) {
        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("user_id", userId); // Always include userId

        if (this.usersApiBaseUrl == null || this.usersApiBaseUrl.isEmpty() || userId == null || userId.trim().isEmpty()) {
            // Cannot fetch without config or ID
            return defaultResult;
        }
        String url = this.usersApiBaseUrl + userId.trim();
        String accessToken = getApiToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> userDetails = response.getBody();
                Map<String, Object> result = new HashMap<>();
                result.put("user_id", userDetails.getOrDefault("user_id", userId));
                result.put("email", userDetails.getOrDefault("email", "N/A"));
                result.put("name", userDetails.getOrDefault("name", "N/A"));
                // Return null if picture is missing, let frontend handle default
                result.put("picture", userDetails.get("picture"));
                return result;
            }
        } catch (HttpClientErrorException.NotFound e) {
            // User not found, return default map with just ID
            System.out.println("User not found for ID: " + userId); // Kept simple output
        } catch (Exception e) {
            // Other errors (network, parsing, etc.), return default map with just ID
            System.out.println("Error fetching details for user ID " + userId + ": " + e.getMessage()); // Kept simple output
        }
        return defaultResult; // Return map with only userId on any failure
    }
    // END: Added Method


    public List<Map<String, String>> getAllGuides() {
        List<Map<String, String>> guides = new ArrayList<>();
        if (this.rolesApiBaseUrl == null || this.rolesApiBaseUrl.isEmpty() || this.guideRoleId == null || this.guideRoleId.isEmpty()) {
            return guides;
        }
        String url = this.rolesApiBaseUrl + this.guideRoleId + "/users";
        String accessToken = getApiToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ParameterizedTypeReference<List<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> users = response.getBody();
                for (Map<String, Object> user : users) {
                    Optional<String> userIdOpt = Optional.ofNullable(user.get("user_id")).map(Object::toString);
                    Optional<String> emailOpt = Optional.ofNullable(user.get("email")).map(Object::toString);

                    if (userIdOpt.isPresent() && emailOpt.isPresent()) {
                        Map<String, String> guide = new HashMap<>();
                        guide.put("id", userIdOpt.get());
                        guide.put("email", emailOpt.get());
                        guide.put("name", Optional.ofNullable(user.get("name")).map(Object::toString).orElse("N/A"));
                        // Return empty string if picture is missing to avoid null downstream? Or keep null? Let's keep empty string for consistency with original code.
                        guide.put("picture", Optional.ofNullable(user.get("picture")).map(Object::toString).orElse(""));
                        guides.add(guide);
                    }
                }
            }
        }  catch (Exception e) {
            // Existing handling - kept as is
            System.out.println(e.getMessage());
        }
        return guides;
    }

    public void updatePicture(String userId, String imageUrl) {
        if (this.usersApiBaseUrl == null || this.usersApiBaseUrl.isEmpty()) {
            throw new IllegalStateException("User API endpoint not configured.");
        }
        if (userId == null || userId.trim().isEmpty() || imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID and Image URL cannot be empty.");
        }
        String url = this.usersApiBaseUrl + userId.trim();
        String accessToken = getApiToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> requestBody = Map.of("picture", imageUrl.trim());
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Picture update failed, status code: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            throw new RuntimeException("Picture update failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Picture update failed: Unexpected error - " + e.getMessage(), e);
        }
    }

    public void updateGuideDescription(String userId, String description) {
        if (this.usersApiBaseUrl == null || this.usersApiBaseUrl.isEmpty()) {
            throw new IllegalStateException("User API endpoint not configured.");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty.");
        }
        String descriptionToSave = (description != null) ? description : "";
        String url = this.usersApiBaseUrl + userId.trim();
        String accessToken = getApiToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> userMetadata = Map.of("description", descriptionToSave);
        Map<String, Object> requestBody = Map.of("user_metadata", userMetadata);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Description update failed, status code: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            throw new RuntimeException("Description update failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Description update failed: Unexpected error - " + e.getMessage(), e);
        }
    }
}
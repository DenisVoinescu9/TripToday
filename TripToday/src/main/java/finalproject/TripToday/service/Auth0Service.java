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

    // Auth0 client credentials
    private final String clientId;
    private final String clientSecret;
    private final String audience;
    private final String guideRoleId; // Specific role ID for guides

    // Auth0 API URLs
    private final String tokenUrl;
    private final String usersApiBaseUrl;
    private final String rolesApiBaseUrl;

    // HTTP client
    private final RestTemplate restTemplate;

    // API token cache fields
    private volatile String cachedApiToken = null;
    private volatile Instant apiTokenExpiryTime = Instant.MIN;
    private final Object apiTokenLock = new Object(); // Sync lock for token

    public Auth0Service(RestTemplateBuilder builder,
                        @Value("${AUTH0_CLIENT_ID}") String clientId,
                        @Value("${AUTH0_CLIENT_SECRET}") String clientSecret,
                        @Value("${AUTH0_AUDIENCE}") String audience,
                        @Value("${AUTH0_ISSUER}") String issuerUrl, // Parameter still needed
                        @Value("${AUTH0_ROLE_GUIDE_ID}") String guideRoleId) {
        this.restTemplate = builder.build(); // Build RestTemplate
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.audience = audience;
        this.guideRoleId = guideRoleId;
        // Construct Auth0 URLs
        this.tokenUrl = issuerUrl + "/oauth/token";
        String apiBase = "";
        if (audience != null && !audience.trim().isEmpty()) {
            apiBase = audience.endsWith("/") ? audience : audience + "/"; // Ensure trailing slash
        }
        this.usersApiBaseUrl = apiBase + "users/"; // Users API endpoint
        this.rolesApiBaseUrl = apiBase + "roles/"; // Roles API endpoint
    }


    public String getApiToken() {
        Instant now = Instant.now(); // Current time
        // Check cached token validity
        if (cachedApiToken != null && now.isBefore(apiTokenExpiryTime.minus(60, ChronoUnit.SECONDS))) {
            return cachedApiToken; // Return valid cached token
        }

        synchronized (apiTokenLock) { // Synchronize token fetching
            now = Instant.now(); // Re-check current time
            // Double-check cache validity
            if (cachedApiToken == null || now.isAfter(apiTokenExpiryTime.minus(60, ChronoUnit.SECONDS))) {
                try {
                    HttpHeaders headers = new HttpHeaders(); // Prepare HTTP headers
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // Set content type

                    // Prepare token request body
                    MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
                    requestBody.add("grant_type", "client_credentials");
                    requestBody.add("client_id", this.clientId);
                    requestBody.add("client_secret", this.clientSecret);
                    requestBody.add("audience", this.audience);

                    // Create HTTP entity
                    HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

                    // Request new API token
                    ResponseEntity<Map> response = restTemplate.exchange(this.tokenUrl, HttpMethod.POST, requestEntity, Map.class);

                    // Check response status
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        Map<String, Object> responseBody = response.getBody();
                        // Extract access token
                        String token = Optional.ofNullable(responseBody.get("access_token"))
                                .map(Object::toString)
                                .filter(t -> !t.trim().isEmpty())
                                .orElseThrow(() -> new RuntimeException("Auth0 response OK but 'access_token' missing or empty."));

                        // Extract token expiry time
                        int expiresInSeconds = Optional.ofNullable(responseBody.get("expires_in"))
                                .map(Object::toString)
                                .map(Integer::parseInt)
                                .orElse(3600); // Default 1 hour

                        // Cache new token
                        this.cachedApiToken = token;
                        this.apiTokenExpiryTime = Instant.now().plusSeconds(expiresInSeconds);

                    } else {
                        // Token retrieval failed
                        throw new RuntimeException("Could not retrieve Auth0 API token, status code: " + response.getStatusCode());
                    }
                } catch (RestClientException e) {
                    // Handle client errors
                    this.cachedApiToken = null; // Clear cache on error
                    this.apiTokenExpiryTime = Instant.MIN;
                    throw new RuntimeException("Failed to fetch Auth0 API token: " + e.getMessage(), e);
                } catch (Exception e) {
                    // Handle unexpected errors
                    this.cachedApiToken = null; // Clear cache on error
                    this.apiTokenExpiryTime = Instant.MIN;
                    throw new RuntimeException("Unexpected error fetching Auth0 API token: " + e.getMessage(), e);
                }
            }
            return cachedApiToken; // Return new or cached token
        }
    }

    public String getGuideDescription(String userId) {
        // Validate inputs for URL
        if (this.usersApiBaseUrl == null || this.usersApiBaseUrl.isEmpty() || userId == null || userId.trim().isEmpty()) {
            return ""; // Return empty if invalid
        }
        String url = this.usersApiBaseUrl + userId.trim(); // User details URL
        String accessToken = getApiToken(); // Get valid API token

        HttpHeaders headers = new HttpHeaders(); // Prepare request headers
        headers.setBearerAuth(accessToken); // Set auth token
        HttpEntity<String> entity = new HttpEntity<>(headers); // Create HTTP entity

        try {
            // Fetch user details
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            // If request successful
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Extract description from metadata
                return Optional.ofNullable(response.getBody().get("user_metadata"))
                        .filter(um -> um instanceof Map)
                        .map(um -> ((Map<?, ?>) um).get("description"))
                        .map(Object::toString)
                        .orElse(""); // Return empty if no desc
            }
        } catch (RestClientException e) {
            // Existing handling - kept as is
            System.out.println(e.getMessage()); // Log client error
        }
        return ""; // Default empty description
    }


    public Map<String, String> getGuideIdAndEmail(String guideId) {
        // Default result map
        Map<String, String> defaultResult = Map.of("guideId", Objects.requireNonNullElse(guideId, ""), "guideEmail", "");
        // Validate inputs for URL
        if (this.usersApiBaseUrl == null || this.usersApiBaseUrl.isEmpty() || guideId == null || guideId.trim().isEmpty()) {
            return defaultResult; // Return default if invalid
        }
        String url = this.usersApiBaseUrl + guideId.trim(); // User details URL
        String accessToken = getApiToken(); // Get valid API token

        HttpHeaders headers = new HttpHeaders(); // Prepare request headers
        headers.setBearerAuth(accessToken); // Set auth token
        HttpEntity<String> entity = new HttpEntity<>(headers); // Create HTTP entity

        try {
            // Fetch user details
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            // If request successful
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Extract email from response
                String email = Optional.ofNullable(response.getBody().get("email"))
                        .map(Object::toString)
                        .orElse(null);
                if (email != null) {
                    // Return ID and email
                    return Map.of("guideId", guideId, "guideEmail", email);
                }
            }
        }
        catch (Exception e) {
            // Existing handling - kept as is
            System.out.println(e.getMessage()); // Log any error
        }
        return defaultResult; // Return default on failure
    }

    // START: Added Method
    public Map<String, Object> getUserDetails(String userId) {
        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("user_id", userId); // Always include user_id

        // Validate inputs for URL
        if (this.usersApiBaseUrl == null || this.usersApiBaseUrl.isEmpty() || userId == null || userId.trim().isEmpty()) {
            // Cannot fetch without config or ID
            return defaultResult; // Return default if invalid
        }
        String url = this.usersApiBaseUrl + userId.trim(); // User details URL
        String accessToken = getApiToken(); // Get valid API token

        HttpHeaders headers = new HttpHeaders(); // Prepare request headers
        headers.setBearerAuth(accessToken); // Set auth token
        HttpEntity<String> entity = new HttpEntity<>(headers); // Create HTTP entity

        try {
            // Fetch user details (generic map)
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {} // For generic map response
            );

            // If request successful
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> userDetails = response.getBody();
                Map<String, Object> result = new HashMap<>(); // Prepare result map
                // Populate result map
                result.put("user_id", userDetails.getOrDefault("user_id", userId));
                result.put("email", userDetails.getOrDefault("email", "N/A"));
                result.put("name", userDetails.getOrDefault("name", "N/A"));
                result.put("picture", userDetails.get("picture")); // Can be null
                return result; // Return fetched details
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
        List<Map<String, String>> guides = new ArrayList<>(); // Initialize guides list
        // Validate inputs for URL
        if (this.rolesApiBaseUrl == null || this.rolesApiBaseUrl.isEmpty() || this.guideRoleId == null || this.guideRoleId.isEmpty()) {
            return guides; // Return empty if invalid
        }
        // URL for users in guide role
        String url = this.rolesApiBaseUrl + this.guideRoleId + "/users";
        String accessToken = getApiToken(); // Get valid API token

        HttpHeaders headers = new HttpHeaders(); // Prepare request headers
        headers.setBearerAuth(accessToken); // Set auth token
        HttpEntity<String> entity = new HttpEntity<>(headers); // Create HTTP entity

        try {
            // Define response type (list of maps)
            ParameterizedTypeReference<List<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            // Fetch users in role
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);

            // If request successful
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> users = response.getBody();
                // Process each user
                for (Map<String, Object> user : users) {
                    // Extract user ID and email
                    Optional<String> userIdOpt = Optional.ofNullable(user.get("user_id")).map(Object::toString);
                    Optional<String> emailOpt = Optional.ofNullable(user.get("email")).map(Object::toString);

                    // If ID and email present
                    if (userIdOpt.isPresent() && emailOpt.isPresent()) {
                        Map<String, String> guide = new HashMap<>(); // Create guide map
                        // Populate guide map
                        guide.put("id", userIdOpt.get());
                        guide.put("email", emailOpt.get());
                        guide.put("name", Optional.ofNullable(user.get("name")).map(Object::toString).orElse("N/A"));
                        guide.put("picture", Optional.ofNullable(user.get("picture")).map(Object::toString).orElse(""));
                        guides.add(guide); // Add to list
                    }
                }
            }
        }  catch (Exception e) {
            // Existing handling - kept as is
            System.out.println(e.getMessage()); // Log any error
        }
        return guides; // Return list of guides
    }

    public void updatePicture(String userId, String imageUrl) {
        // Validate API URL config
        if (this.usersApiBaseUrl == null || this.usersApiBaseUrl.isEmpty()) {
            throw new IllegalStateException("User API endpoint not configured.");
        }
        // Validate input parameters
        if (userId == null || userId.trim().isEmpty() || imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID and Image URL cannot be empty.");
        }
        String url = this.usersApiBaseUrl + userId.trim(); // User details URL
        String accessToken = getApiToken(); // Get valid API token

        HttpHeaders headers = new HttpHeaders(); // Prepare request headers
        headers.setBearerAuth(accessToken); // Set auth token
        headers.setContentType(MediaType.APPLICATION_JSON); // Set content type
        // Prepare request body (picture URL)
        Map<String, String> requestBody = Map.of("picture", imageUrl.trim());
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers); // Create HTTP entity

        try {
            // Send PATCH request
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Void.class);
            // Check for non-success status
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Picture update failed, status code: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            // Handle client errors
            throw new RuntimeException("Picture update failed: " + e.getMessage(), e);
        } catch (Exception e) {
            // Handle unexpected errors
            throw new RuntimeException("Picture update failed: Unexpected error - " + e.getMessage(), e);
        }
    }

    public void updateGuideDescription(String userId, String description) {
        // Validate API URL config
        if (this.usersApiBaseUrl == null || this.usersApiBaseUrl.isEmpty()) {
            throw new IllegalStateException("User API endpoint not configured.");
        }
        // Validate user ID
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty.");
        }
        // Ensure description is not null
        String descriptionToSave = (description != null) ? description : "";
        String url = this.usersApiBaseUrl + userId.trim(); // User details URL
        String accessToken = getApiToken(); // Get valid API token

        HttpHeaders headers = new HttpHeaders(); // Prepare request headers
        headers.setBearerAuth(accessToken); // Set auth token
        headers.setContentType(MediaType.APPLICATION_JSON); // Set content type

        // Prepare request body (metadata)
        Map<String, Object> userMetadata = Map.of("description", descriptionToSave);
        Map<String, Object> requestBody = Map.of("user_metadata", userMetadata);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers); // Create HTTP entity

        try {
            // Send PATCH request
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Void.class);
            // Check for non-success status
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Description update failed, status code: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            // Handle client errors
            throw new RuntimeException("Description update failed: " + e.getMessage(), e);
        } catch (Exception e) {
            // Handle unexpected errors
            throw new RuntimeException("Description update failed: Unexpected error - " + e.getMessage(), e);
        }
    }
}
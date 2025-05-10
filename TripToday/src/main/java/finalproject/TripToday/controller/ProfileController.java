package finalproject.TripToday.controller;

import finalproject.TripToday.entity.Trip; // Presupunem că Trip este importat corect
import finalproject.TripToday.service.TripService;
import finalproject.TripToday.service.Auth0Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ProfileController {

    // Logger object for logging events within this controller
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
    // Define number of trips to be displayed per page for upcoming and past trips sections
    private static final int PAGE_SIZE = 3;

    // Service for handling trip-related business logic
    private final TripService tripService;
    // Service for handling Auth0 user-related operations (like fetching/updating descriptions, pictures)
    private final Auth0Service auth0Service;

    // Constructor for dependency injection
    @Autowired
    public ProfileController(TripService tripService, Auth0Service auth0Service) {
        // Initialize services
        this.tripService = tripService;
        this.auth0Service = auth0Service;
    }

    // Method to populate the model with static UI text elements for the profile page
    private void addPageTextToModel(Model model) {
        // Create a map to hold various text labels and messages for the UI
        Map<String, String> pageText = new HashMap<>();
        // Add page title
        pageText.put("title", "My profile | TripToday");

        pageText.put("profilePageMainTitle", "Your profile");

        // Add default picture URL in case user has no picture
        pageText.put("defaultPictureUrl", "/images/default_avatar.png");

        // Add labels for user information section
        pageText.put("emailLabel", "Email:");
        pageText.put("roleLabel", "Account role(s):");
        pageText.put("noUserDetails", "User details not available.");

        // Add labels and placeholders for guide description form
        pageText.put("guideDescriptionLabel", "Your description (as a guide):");
        pageText.put("guideDescriptionPlaceholder", "Enter your description...");
        pageText.put("guideDescriptionCharCounterSuffix", "/200");
        pageText.put("guideDescriptionSaveButton", "Save description");

        // Add labels and placeholders for picture update form
        pageText.put("pictureUpdateLabel", "Update profile picture:");
        pageText.put("pictureUpdatePlaceholder", "Enter URL of picture here (e.g., https://...)");
        pageText.put("pictureUpdateButton", "Update picture");

        // Add labels for trip sections toggle and titles
        pageText.put("upcomingTripsButton", "Upcoming trips");
        pageText.put("pastTripsButton", "Past trips");
        pageText.put("upcomingTripsTitle", "Upcoming trips");
        pageText.put("pastTripsTitle", "Past trips");
        // Add messages for when no trips are found
        pageText.put("noUpcomingTrips", "No upcoming trips found.");
        pageText.put("noPastTrips", "No past trips found.");

        // Add messages related to canceled trips
        pageText.put("tripCanceledMessageUser", "This trip was canceled. The enrolling fee was transferred back to you.");
        pageText.put("tripCanceledMessageGeneric", "This trip was canceled.");
        // Add labels for trip details display
        pageText.put("departureLocationLabel", "Departure location");
        pageText.put("departureDateLabel", "Departure date");
        pageText.put("departureHourLabel", "Departure hour");
        pageText.put("returnDateLabel", "Return date");
        pageText.put("durationLabel", "Duration");
        pageText.put("availableSpotsLabel", "Available spots");
        pageText.put("remainingSpotsLabel", "Remaining spots");
        pageText.put("enrollmentFeeLabel", "Enrollment fee");
        pageText.put("feeLabel", "Fee");
        pageText.put("hotelLabel", "Hotel");
        pageText.put("guideLabel", "Guide");

        // Add units for display with trip details
        pageText.put("durationUnit", " days");
        pageText.put("spotsUnit", " spots");
        pageText.put("feeUnit", " RON");
        pageText.put("hourUnit", " UTC");
        // Add fallback texts for trip details
        pageText.put("hotelNotSpecified", "Not specified");
        pageText.put("guideYouUpcoming", "You are the guide");
        pageText.put("guideYouPast", "You were the guide");
        pageText.put("guideNotAvailable", "N/A");

        // Add symbols for pagination buttons
        pageText.put("paginationPreviousSymbol", "\u2190"); // Left arrow
        pageText.put("paginationNextSymbol", "\u2192");     // Right arrow

        // Add the entire map of texts to the model
        model.addAttribute("pageText", pageText);
    }

    // Handler for the profile page
    @GetMapping("/profile")
    public String profile(Model model,
                          @RequestParam(name="up_page", defaultValue = "0") int upcomingPageParam, // Upcoming trips page number from URL
                          @RequestParam(name="pa_page", defaultValue = "0") int pastPageParam,     // Past trips page number from URL
                          @AuthenticationPrincipal OidcUser principal) {                         // Authenticated user details

        // Populate model with static UI texts
        addPageTextToModel(model);
        // Retrieve the pageText map (primarily for defaultPictureUrl usage below, also available to template)
        Map<String, String> pageText = (Map<String, String>) model.getAttribute("pageText");

        // If user is not authenticated, redirect to home page
        if (principal == null) {
            return "redirect:/";
        }

        // Get user claims, ID, email, and picture from authenticated principal
        Map<String, Object> claims = principal.getClaims();
        String userId = principal.getName();
        String userEmail = principal.getEmail();
        String userPicture = principal.getPicture();

        // Process user roles from claims
        Object rolesClaim = claims.get("role"); // Assuming 'role' is the claim key
        List<String> userDisplayRoles = new ArrayList<>();
        // Handle if roles are a single string (pipe-separated) or a list
        if (rolesClaim instanceof String) {
            userDisplayRoles.addAll(List.of(((String)rolesClaim).split("\\s*\\|\\s*")));
        } else if (rolesClaim instanceof List) {
            for(Object roleObj : (List<?>)rolesClaim) {
                if (roleObj instanceof String) {
                    userDisplayRoles.add((String)roleObj);
                }
            }
        }

        // Determine if the user is a guide and if their account is an Auth0 native account
        boolean isGuide = userDisplayRoles.contains("Guide");
        boolean isAuth0User = userId != null && userId.startsWith("auth0|"); // Check for Auth0 prefix in user ID

        // Add user-specific information to the model
        model.addAttribute("userEmail", userEmail);
        // Set display picture, use default if not available
        model.addAttribute("displayUserPicture", userPicture != null && !userPicture.isEmpty() ? userPicture : (pageText != null ? pageText.get("defaultPictureUrl") : "/images/default_avatar.png"));
        model.addAttribute("userDisplayRoles", userDisplayRoles);
        model.addAttribute("isGuide", isGuide);
        model.addAttribute("isAuth0User", isAuth0User);
        model.addAttribute("currentUserId", userId); // User's own ID for comparisons

        // Log profile loading attempt
        logger.info("Loading profile for user: {} - up_page: {}, pa_page: {}", userId, upcomingPageParam, pastPageParam);
        // Initialize error message variable
        String errorMessage = null;

        try {
            // Attempt to fetch and add guide's description to the model
            String description = auth0Service.getGuideDescription(userId);
            model.addAttribute("description", description);

            // Split user's trips into upcoming and past based on current date
            Map<String, List<Trip>> splitTrips = tripService.splitUserTripsByDate(userId);
            // Get all upcoming trips for the user
            List<Trip> allUpcomingTrips = splitTrips.getOrDefault("upcomingTrips", Collections.emptyList());
            // Get all past trips for the user
            List<Trip> allPastTrips = splitTrips.getOrDefault("pastTrips", Collections.emptyList());

            // Fetch all guides and create a map of guide ID to email for quick lookup
            Map<String, String> guideEmailMap = auth0Service.getAllGuides().stream()
                    .filter(g -> g != null && g.get("id") != null && g.get("email") != null) // Filter out invalid guide entries
                    .collect(Collectors.toMap(g -> (String)g.get("id"), g -> (String)g.get("email"), (e1, e2) -> e1)); // Ensure unique keys

            // --- Upcoming Trips Pagination ---
            // Set current page for upcoming trips from request parameter
            int upcomingPage = upcomingPageParam;
            // Get total number of upcoming trips
            int totalUpcomingTrips = allUpcomingTrips.size();
            // Calculate total pages needed for upcoming trips
            int totalUpcomingPages = (int) Math.ceil((double) totalUpcomingTrips / PAGE_SIZE);
            // Ensure at least one page is shown, even if no trips
            if (totalUpcomingPages == 0) totalUpcomingPages = 1;
            // Validate upcomingPage number to be within bounds
            if (upcomingPage < 0) upcomingPage = 0;
            else if (upcomingPage >= totalUpcomingPages) upcomingPage = Math.max(0, totalUpcomingPages - 1);

            // Calculate from/to indices for sublist
            int upcomingFromIndex = upcomingPage * PAGE_SIZE;
            int upcomingToIndex = Math.min(upcomingFromIndex + PAGE_SIZE, totalUpcomingTrips);
            // Get sublist of upcoming trips for the current page
            List<Trip> pageUpcomingTrips = (upcomingFromIndex < totalUpcomingTrips && upcomingFromIndex >=0) ?
                    allUpcomingTrips.subList(upcomingFromIndex, upcomingToIndex) : Collections.emptyList();
            // Get guide emails for the upcoming trips on the current page
            List<String> upcomingTripGuideEmailsOnPage = pageUpcomingTrips.stream()
                    .map(trip -> trip.getGuideId() != null ? guideEmailMap.get(String.valueOf(trip.getGuideId())) : null) // Ensure guideId is string for map key
                    .collect(Collectors.toList());
            // Add upcoming trips pagination data to the model
            model.addAttribute("pageUpcomingTrips", pageUpcomingTrips);
            model.addAttribute("upcomingPage", upcomingPage);
            model.addAttribute("totalUpcomingPages", totalUpcomingPages);
            model.addAttribute("upcomingTripGuideEmailsOnPage", upcomingTripGuideEmailsOnPage);

            // --- Past Trips Pagination ---
            // Set current page for past trips from request parameter
            int pastPage = pastPageParam;
            // Get total number of past trips
            int totalPastTrips = allPastTrips.size();
            // Calculate total pages needed for past trips
            int totalPastPages = (int) Math.ceil((double) totalPastTrips / PAGE_SIZE);
            // Ensure at least one page is shown, even if no trips
            if (totalPastPages == 0) totalPastPages = 1;
            // Validate pastPage number to be within bounds
            if (pastPage < 0) pastPage = 0;
            else if (pastPage >= totalPastPages) pastPage = Math.max(0, totalPastPages - 1);

            // Calculate from/to indices for sublist
            int pastFromIndex = pastPage * PAGE_SIZE;
            int pastToIndex = Math.min(pastFromIndex + PAGE_SIZE, totalPastTrips);
            // Get sublist of past trips for the current page
            List<Trip> pagePastTrips = (pastFromIndex < totalPastTrips && pastFromIndex >= 0) ?
                    allPastTrips.subList(pastFromIndex, pastToIndex) : Collections.emptyList();
            // Get guide emails for the past trips on the current page
            List<String> pastTripGuideEmailsOnPage = pagePastTrips.stream()
                    .map(trip -> trip.getGuideId() != null ? guideEmailMap.get(String.valueOf(trip.getGuideId())) : null) // Ensure guideId is string for map key
                    .collect(Collectors.toList());
            // Add past trips pagination data to the model
            model.addAttribute("pagePastTrips", pagePastTrips);
            model.addAttribute("pastPage", pastPage);
            model.addAttribute("totalPastPages", totalPastPages);
            model.addAttribute("pastTripGuideEmailsOnPage", pastTripGuideEmailsOnPage);

        } catch (Exception e) {
            // Handle any exceptions during data loading
            logger.error("Error loading profile data for user {}", userId, e);
            errorMessage = "Could not load profile data. Please try again later.";
            // Set default/empty values for model attributes in case of error
            model.addAttribute("description", ""); // Default empty description
            model.addAttribute("pageUpcomingTrips", Collections.emptyList());
            model.addAttribute("upcomingPage", 0);
            model.addAttribute("totalUpcomingPages", 1); // To display "no trips" message correctly
            model.addAttribute("upcomingTripGuideEmailsOnPage", Collections.emptyList());
            model.addAttribute("pagePastTrips", Collections.emptyList());
            model.addAttribute("pastPage", 0);
            model.addAttribute("totalPastPages", 1); // To display "no trips" message correctly
            model.addAttribute("pastTripGuideEmailsOnPage", Collections.emptyList());
        }

        // If an error message was generated, add it to the model
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
        }

        // Return the name of the Thymeleaf template for the profile page
        return "profile-page";
    }

    // Handler for updating user's guide description
    @PostMapping("/profile/update-description")
    public String updateDescription(@RequestParam("description") String description,
                                    @AuthenticationPrincipal OidcUser principal,
                                    RedirectAttributes redirectAttributes) {
        // Redirect if user is not authenticated
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication required.");
            return "redirect:/"; // Redirect to home if not authenticated
        }
        // Get user ID from principal
        String userId = principal.getName();

        try {
            // Call service to update guide description
            auth0Service.updateGuideDescription(userId, description);
            // Add success message for redirect
            redirectAttributes.addFlashAttribute("successMessage", "Description updated successfully!");
        } catch (Exception e) {
            // Log error and add error message for redirect
            logger.error("Error updating description for user {}", userId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update description. Reason: " + e.getMessage());
        }
        // Redirect back to the profile page
        return "redirect:/profile";
    }

    // Handler for updating user's profile picture
    @PostMapping("/profile/update-picture")
    public String updatePicture(@RequestParam("imageUrl") String imageUrl,
                                @AuthenticationPrincipal OidcUser principal,
                                RedirectAttributes redirectAttributes) {
        // Redirect if user is not authenticated
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication required.");
            return "redirect:/"; // Redirect to home if not authenticated
        }
        // Get user ID from principal
        String userId = principal.getName();

        // Validate the provided image URL
        if (imageUrl == null || imageUrl.trim().isEmpty() || !imageUrl.trim().toLowerCase().startsWith("http")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid image URL provided. Please enter a valid URL.");
            return "redirect:/profile"; // Redirect back to profile if URL is invalid
        }

        try {
            // Call service to update user's picture
            auth0Service.updatePicture(userId, imageUrl.trim());
            // Add success message for redirect
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated successfully! Please log in again to see the changes.");
        } catch (Exception e) {
            // Log error and add error message for redirect
            logger.error("Error updating picture for user {}", userId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile picture. Reason: " + e.getMessage());
        }
        // Redirect back to the profile page
        return "redirect:/profile";
    }
}
package finalproject.TripToday.controller;

import finalproject.TripToday.entity.Trip;
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

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
    private static final int PAGE_SIZE = 3;

    private final TripService tripService;
    private final Auth0Service auth0Service;

    @Autowired
    public ProfileController(TripService tripService, Auth0Service auth0Service) {
        this.tripService = tripService;
        this.auth0Service = auth0Service;
    }

    private void addPageTextToModel(Model model) {
        Map<String, String> pageText = new HashMap<>();
        pageText.put("title", "My profile | TripToday");
        pageText.put("defaultPictureUrl", "/images/default_avatar.png");

        pageText.put("emailLabel", "Email:");
        pageText.put("roleLabel", "Account role(s):");
        pageText.put("noUserDetails", "User details not available.");

        pageText.put("guideDescriptionLabel", "Your description (as a guide):");
        pageText.put("guideDescriptionPlaceholder", "Enter your description...");
        pageText.put("guideDescriptionCharCounterSuffix", "/200");
        pageText.put("guideDescriptionSaveButton", "Save description");

        pageText.put("pictureUpdateLabel", "Update profile picture:");
        pageText.put("pictureUpdatePlaceholder", "Enter URL of picture here (e.g., https://...)");
        pageText.put("pictureUpdateButton", "Update picture");

        pageText.put("upcomingTripsButton", "Upcoming trips");
        pageText.put("pastTripsButton", "Past trips");
        pageText.put("upcomingTripsTitle", "Upcoming trips");
        pageText.put("pastTripsTitle", "Past trips");
        pageText.put("noUpcomingTrips", "No upcoming trips found.");
        pageText.put("noPastTrips", "No past trips found.");

        pageText.put("tripCanceledMessageUser", "This trip was canceled. The enrolling fee was transferred back to you.");
        pageText.put("tripCanceledMessageGeneric", "This trip was canceled.");
        pageText.put("departureLocationLabel", "Departure location");
        pageText.put("departureDateLabel", "Departure date");
        pageText.put("departureHourLabel", "Departure hour");
        pageText.put("returnDateLabel", "Return date");
        pageText.put("durationLabel", "Duration");
        pageText.put("availableSpotsLabel", "Available spots"); // Ajustat la Sentence case
        pageText.put("remainingSpotsLabel", "Remaining spots");
        pageText.put("enrollmentFeeLabel", "Enrollment fee");
        pageText.put("feeLabel", "Fee");
        pageText.put("hotelLabel", "Hotel");
        pageText.put("guideLabel", "Guide");

        pageText.put("durationUnit", " days");
        pageText.put("spotsUnit", " spots");
        pageText.put("feeUnit", " RON");
        pageText.put("hourUnit", " UTC");
        pageText.put("hotelNotSpecified", "Not specified");
        pageText.put("guideYouUpcoming", "You are the guide");
        pageText.put("guideYouPast", "You were the guide");
        pageText.put("guideNotAvailable", "N/A");

        pageText.put("paginationPreviousSymbol", "\u2190");
        pageText.put("paginationNextSymbol", "\u2192");

        model.addAttribute("pageText", pageText);
    }


    @GetMapping("/profile")
    public String profile(Model model,
                          @RequestParam(name="up_page", defaultValue = "0") int upcomingPageParam,
                          @RequestParam(name="pa_page", defaultValue = "0") int pastPageParam,
                          @AuthenticationPrincipal OidcUser principal) {

        addPageTextToModel(model);
        Map<String, String> pageText = (Map<String, String>) model.getAttribute("pageText");


        if (principal == null) {
            return "redirect:/";
        }

        Map<String, Object> claims = principal.getClaims();
        String userId = principal.getName();
        String userEmail = principal.getEmail();
        String userPicture = principal.getPicture();

        Object rolesClaim = claims.get("role");
        List<String> userDisplayRoles = new ArrayList<>();
        if (rolesClaim instanceof String) {
            userDisplayRoles.addAll(List.of(((String)rolesClaim).split("\\s*\\|\\s*")));
        } else if (rolesClaim instanceof List) {
            for(Object roleObj : (List<?>)rolesClaim) {
                if (roleObj instanceof String) {
                    userDisplayRoles.add((String)roleObj);
                }
            }
        }

        boolean isGuide = userDisplayRoles.contains("Guide");
        boolean isAuth0User = userId != null && userId.startsWith("auth0|");

        model.addAttribute("userEmail", userEmail);
        model.addAttribute("displayUserPicture", userPicture != null && !userPicture.isEmpty() ? userPicture : (pageText != null ? pageText.get("defaultPictureUrl") : "/images/default_avatar.png"));
        model.addAttribute("userDisplayRoles", userDisplayRoles);
        model.addAttribute("isGuide", isGuide);
        model.addAttribute("isAuth0User", isAuth0User);
        model.addAttribute("currentUserId", userId);

        logger.info("Loading profile for user: {} - up_page: {}, pa_page: {}", userId, upcomingPageParam, pastPageParam);
        String errorMessage = null;

        try {
            String description = auth0Service.getGuideDescription(userId);
            model.addAttribute("description", description);

            Map<String, List<Trip>> splitTrips = tripService.splitUserTripsByDate(userId);
            List<Trip> allUpcomingTrips = splitTrips.getOrDefault("upcomingTrips", Collections.emptyList());
            List<Trip> allPastTrips = splitTrips.getOrDefault("pastTrips", Collections.emptyList());

            Map<String, String> guideEmailMap = auth0Service.getAllGuides().stream()
                    .filter(g -> g != null && g.get("id") != null && g.get("email") != null)
                    .collect(Collectors.toMap(g -> (String)g.get("id"), g -> (String)g.get("email"), (e1, e2) -> e1));

            int upcomingPage = upcomingPageParam;
            int pastPage = pastPageParam;

            int totalUpcomingTrips = allUpcomingTrips.size();
            int totalUpcomingPages = (int) Math.ceil((double) totalUpcomingTrips / PAGE_SIZE);
            if (totalUpcomingPages == 0) totalUpcomingPages = 1;
            if (upcomingPage < 0) upcomingPage = 0;
            else if (upcomingPage >= totalUpcomingPages) upcomingPage = Math.max(0, totalUpcomingPages - 1);
            int upcomingFromIndex = upcomingPage * PAGE_SIZE;
            int upcomingToIndex = Math.min(upcomingFromIndex + PAGE_SIZE, totalUpcomingTrips);
            List<Trip> pageUpcomingTrips = (upcomingFromIndex < totalUpcomingTrips && upcomingFromIndex >=0) ?
                    allUpcomingTrips.subList(upcomingFromIndex, upcomingToIndex) : Collections.emptyList();
            List<String> upcomingTripGuideEmailsOnPage = pageUpcomingTrips.stream()
                    .map(trip -> guideEmailMap.get(trip.getGuideId()))
                    .collect(Collectors.toList());
            model.addAttribute("pageUpcomingTrips", pageUpcomingTrips);
            model.addAttribute("upcomingPage", upcomingPage);
            model.addAttribute("totalUpcomingPages", totalUpcomingPages);
            model.addAttribute("upcomingTripGuideEmailsOnPage", upcomingTripGuideEmailsOnPage);

            int totalPastTrips = allPastTrips.size();
            int totalPastPages = (int) Math.ceil((double) totalPastTrips / PAGE_SIZE);
            if (totalPastPages == 0) totalPastPages = 1;
            if (pastPage < 0) pastPage = 0;
            else if (pastPage >= totalPastPages) pastPage = Math.max(0, totalPastPages - 1);
            int pastFromIndex = pastPage * PAGE_SIZE;
            int pastToIndex = Math.min(pastFromIndex + PAGE_SIZE, totalPastTrips);
            List<Trip> pagePastTrips = (pastFromIndex < totalPastTrips && pastFromIndex >= 0) ?
                    allPastTrips.subList(pastFromIndex, pastToIndex) : Collections.emptyList();
            List<String> pastTripGuideEmailsOnPage = pagePastTrips.stream()
                    .map(trip -> guideEmailMap.get(trip.getGuideId()))
                    .collect(Collectors.toList());
            model.addAttribute("pagePastTrips", pagePastTrips);
            model.addAttribute("pastPage", pastPage);
            model.addAttribute("totalPastPages", totalPastPages);
            model.addAttribute("pastTripGuideEmailsOnPage", pastTripGuideEmailsOnPage);

        } catch (Exception e) {
            logger.error("Error loading profile data for user {}", userId, e);
            errorMessage = "Could not load profile data. Please try again later.";
            model.addAttribute("description", "");
            model.addAttribute("pageUpcomingTrips", Collections.emptyList());
            model.addAttribute("upcomingPage", 0);
            model.addAttribute("totalUpcomingPages", 1);
            model.addAttribute("upcomingTripGuideEmailsOnPage", Collections.emptyList());
            model.addAttribute("pagePastTrips", Collections.emptyList());
            model.addAttribute("pastPage", 0);
            model.addAttribute("totalPastPages", 1);
            model.addAttribute("pastTripGuideEmailsOnPage", Collections.emptyList());
        }

        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
        }

        return "profile-page";
    }

    @PostMapping("/profile/update-description")
    public String updateDescription(@RequestParam("description") String description, @AuthenticationPrincipal OidcUser principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication required.");
            return "redirect:/";
        }
        String userId = principal.getName();

        try {
            auth0Service.updateGuideDescription(userId, description);
            redirectAttributes.addFlashAttribute("successMessage", "Description updated successfully!");
        } catch (Exception e) {
            logger.error("Error updating description for user {}", userId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update description. Reason: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/update-picture")
    public String updatePicture(@RequestParam("imageUrl") String imageUrl, @AuthenticationPrincipal OidcUser principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication required.");
            return "redirect:/";
        }
        String userId = principal.getName();

        if (imageUrl == null || imageUrl.trim().isEmpty() || !imageUrl.trim().toLowerCase().startsWith("http")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid image URL provided. Please enter a valid URL.");
            return "redirect:/profile";
        }

        try {
            auth0Service.updatePicture(userId, imageUrl.trim());
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated successfully! Please log in again to see the changes.");
        } catch (Exception e) {
            logger.error("Error updating picture for user {}", userId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile picture. Reason: " + e.getMessage());
        }
        return "redirect:/profile";
    }
}
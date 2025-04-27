package finalproject.TripToday.controller;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.service.TripService;
import finalproject.TripToday.service.Auth0Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections; // Pentru Collections.emptyList()
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // Pentru colectarea emailurilor

@Controller
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final TripService tripService;
    private final Auth0Service auth0Service;

    public ProfileController(TripService tripService, Auth0Service auth0Service) {
        this.tripService = tripService;
        this.auth0Service = auth0Service;
    }

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {

            return "redirect:/";
        }

        Map<String, Object> claims = principal.getClaims();
        model.addAttribute("user", claims);

        String userId = principal.getName();

        try {

            String description = auth0Service.getGuideDescription(userId);
            model.addAttribute("description", description);


            Map<String, List<Trip>> splitTrips = tripService.splitUserTripsByDate(userId);
            List<Trip> pastTrips = splitTrips.getOrDefault("pastTrips", Collections.emptyList());
            List<Trip> upcomingTrips = splitTrips.getOrDefault("upcomingTrips", Collections.emptyList());
            model.addAttribute("pastTrips", pastTrips);
            model.addAttribute("upcomingTrips", upcomingTrips);


            List<String> upcomingTripGuideEmails = upcomingTrips.stream().map(Trip::getGuideId).distinct()
                    .map(auth0Service::getGuideIdAndEmail)
                    .collect(Collectors.toMap(map -> map.get("guideId"), map -> map.get("guideEmail")))
                    .entrySet().stream()
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    .values().stream().collect(Collectors.toList());

            model.addAttribute("upcomingTripGuideEmails", upcomingTripGuideEmails);


            List<String> pastTripGuideEmails = pastTrips.stream().map(Trip::getGuideId).distinct().map(auth0Service::getGuideIdAndEmail).collect(Collectors.toMap(map -> map.get("guideId"), map -> map.get("guideEmail"))).entrySet().stream().filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).values().stream().collect(Collectors.toList());


            model.addAttribute("pastTripGuideEmails", pastTripGuideEmails);


        } catch (Exception e) {

            model.addAttribute("errorMessage", "Could not load profile data. Please try again later.");

            model.addAttribute("description", "");
            model.addAttribute("pastTrips", Collections.emptyList());
            model.addAttribute("upcomingTrips", Collections.emptyList());
            model.addAttribute("pastTripGuideEmails", Collections.emptyList());
            model.addAttribute("upcomingTripGuideEmails", Collections.emptyList());
        }

        return "profile";
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
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile picture. Reason: " + e.getMessage());
        }

        return "redirect:/profile";
    }
}
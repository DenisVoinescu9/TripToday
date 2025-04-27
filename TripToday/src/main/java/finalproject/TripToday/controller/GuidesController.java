package finalproject.TripToday.controller;

import finalproject.TripToday.service.Auth0Service; // Service import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;



@Controller
public class GuidesController {

    private final Auth0Service auth0Service;

    @Autowired
    public GuidesController(Auth0Service auth0Service) {
        this.auth0Service = auth0Service;
    }

    @GetMapping("/guides")
    public String guides(Model model, @AuthenticationPrincipal OidcUser principal) {
        List<Map<String, String>> guidesForView = new ArrayList<>();

        try {
            // 1. Get initial guide list (id, email, name, picture) from service
            List<Map<String, String>> initialGuides = auth0Service.getAllGuides();

            // 2. For each guide, get description and add to the map
            for (Map<String, String> guide : initialGuides) {
                String guideId = guide.get("id");
                // Proceed only if guideId is valid
                if (guideId != null && !guideId.isEmpty()) {
                    // Get description (service returns "" on error or if not found)
                    String description = auth0Service.getGuideDescription(guideId);
                    // Add description to the existing map, providing a default if empty
                    guide.put("description", description.isEmpty() ? "The guide has no description." : description);
                    // Add the completed map to the list for the view
                    guidesForView.add(guide);
                }
                // Guides with missing IDs are implicitly skipped
            }

            // 3. Add the final list (with descriptions) to the model
            model.addAttribute("guides", guidesForView);

            // Add logged-in user profile if available
            if (principal != null) {
                model.addAttribute("profile", principal.getClaims());
            }

            return "guides"; // Return the view name

        } catch (Exception e) {
            // Catch major exceptions (e.g., failure in getAllGuides or token fetching)
            // Add a generic error message for the UI
            model.addAttribute("errorMessage", "An unexpected error occurred while loading guide information. Please try again later.");
            // Return the general error page name
            return "error"; // Ensure you have an error.html template
        }
    }
}
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
            List<Map<String, String>> initialGuides = auth0Service.getAllGuides();

            for (Map<String, String> guide : initialGuides) {
                String guideId = guide.get("id");
                if (guideId != null && !guideId.isEmpty()) {
                    String description = auth0Service.getGuideDescription(guideId);
                    guide.put("description", description.isEmpty() ? "The guide has no description." : description);
                    guidesForView.add(guide);
                }
            }

            model.addAttribute("guides", guidesForView);

            if (principal != null) {
                model.addAttribute("profile", principal.getClaims());
            }


        } catch (Exception e) {

            model.addAttribute("errorMessage", "An unexpected error occurred while loading guide information. Please try again later.");

        }
        return "guides-page";


    }
}
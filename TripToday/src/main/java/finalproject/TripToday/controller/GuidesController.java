package finalproject.TripToday.controller;

import finalproject.TripToday.service.Auth0Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class GuidesController {

    private final Auth0Service auth0Service;
    private static final Logger logger = LoggerFactory.getLogger(GuidesController.class);


    @Autowired
    public GuidesController(Auth0Service auth0Service) {
        this.auth0Service = auth0Service;
    }

    @GetMapping("/guides")
    public String guides(@RequestParam(defaultValue = "0") int page, Model model, @AuthenticationPrincipal OidcUser principal) {
        List<Map<String, String>> guidesForView = new ArrayList<>();
        String loadErrorMessage = null;

        try {
            List<Map<String, String>> initialGuides = auth0Service.getAllGuides();

            for (Map<String, String> guide : initialGuides) {
                String guideId = guide.get("id");
                if (guideId != null && !guideId.isEmpty()) {
                    String description = auth0Service.getGuideDescription(guideId);
                    guide.put("description", description == null || description.isEmpty() ? "The guide has no description yet." : description);
                    guidesForView.add(guide);
                }
            }

            int pageSize = 4;
            int totalGuides = guidesForView.size();
            int totalPages = (int) Math.ceil((double) totalGuides / pageSize);


            if (page < 0) {
                page = 0;
            } else if (page >= totalPages && totalPages > 0) {
                page = totalPages - 1;
            }

            int fromIndex = page * pageSize;

            int toIndex = Math.min(fromIndex + pageSize, totalGuides);

            List<Map<String, String>> pageGuides;

            if (fromIndex >= 0 && fromIndex < totalGuides) {
                pageGuides = guidesForView.subList(fromIndex, toIndex);
            } else {
                pageGuides = new ArrayList<>();
                if (totalGuides > 0) page = 0;
            }

            model.addAttribute("guides", pageGuides);
            model.addAttribute("page", page);
            model.addAttribute("totalPages", totalPages);

            if (principal != null) {
                model.addAttribute("profile", principal.getClaims());
            }

        } catch (Exception e) {
            logger.error("Error loading guides data", e);
            loadErrorMessage = "An unexpected error occurred while loading guide information. Please try again later.";
        }

        model.addAttribute("pageTitle", "Meet the guides | TripToday");
        model.addAttribute("mainHeading", "Meet our team of experienced guides");
        model.addAttribute("introParagraph", "Get to know the knowledgeable and friendly faces who will bring your destinations to life. Our guides aren't just experts in history and nature; they're passionate storytellers dedicated to making your TripToday experience authentic and unforgettable. Find out more about the individuals ready to lead your next adventure!");
        model.addAttribute("noGuidesMessage", "No guides found.");


        if (loadErrorMessage != null) {
            model.addAttribute("errorMessage", loadErrorMessage);
            if (!model.containsAttribute("guides")) model.addAttribute("guides", new ArrayList<>());
            if (!model.containsAttribute("page")) model.addAttribute("page", 0);
            if (!model.containsAttribute("totalPages")) model.addAttribute("totalPages", 0);
        }
        return "guides-page";
    }
}
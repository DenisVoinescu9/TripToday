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
import java.util.List;
import java.util.Map;

@Controller
public class GuidesController {

    // Declare Auth0 Service

    private final Auth0Service auth0Service;

    // Define logger object

    private static final Logger logger = LoggerFactory.getLogger(GuidesController.class);

    // Define page size for pagination

    private static final int pageSize = 4;

    @Autowired
    public GuidesController(Auth0Service auth0Service) {

        // Initialize Auth0 Service

        this.auth0Service = auth0Service;
    }

    // Define guides page

    @GetMapping("/guides")
    public String guides(@RequestParam(defaultValue = "0") int page, Model model, @AuthenticationPrincipal OidcUser principal) {

        // Page value will be set dynamically, default is 0 (first page of the pagination)

        // Declare list of guides that will be added to the model and displayed

        List<Map<String, String>> guidesForView = new ArrayList<>();
        String loadErrorMessage = null;

        try {

            // Initialize list of all guides

            List<Map<String, String>> initialGuides = auth0Service.getAllGuides();

            // Iterate through the list of all guides

            for (Map<String, String> guide : initialGuides) {
                String guideId = guide.get("id");
                if (guideId != null && !guideId.isEmpty()) {

                    // Get guide's description based on the ID

                    String description = auth0Service.getGuideDescription(guideId);

                    // Add the description field to the current guide

                    guide.put("description", description == null || description.isEmpty() ? "The guide has no description yet." : description);

                    // Add the current guide to the list of guides that will be displayed

                    guidesForView.add(guide);
                }
            }




            // Get size of the list of displayed guides

            int totalGuides = guidesForView.size();

            // Calculate number of needed pages, based on the size of guides list and the number of displayed guides per page

            int totalPages = (int) Math.ceil((double) totalGuides / pageSize);


            if (page < 0) {

                // If page value from URL is negative, page will be set to 0 (first page of the pagination)

                page = 0;
            } else if (page >= totalPages && totalPages > 0) {

                // If page value from URL is greater than the number of pages, page will be set to the last one

                page = totalPages - 1;
            }

            // Calculate the starting index in the full list (guidesForView) for the current page's subset of guides

            int fromIndex = page * pageSize;

            // Calculate the ending index for the sublist, ensuring it doesn't exceed the total number of guides

            // Math.min is used to prevent an IndexOutOfBoundsException if it's the last page and it's not full

            int toIndex = Math.min(fromIndex + pageSize, totalGuides);

            // Declare the list that will hold the guides for the currently displayed page

            List<Map<String, String>> pageGuides;

            // Check if the calculated fromIndex is a valid position within the guidesForView list

            if (fromIndex >= 0 && fromIndex < totalGuides) {

                // If the fromIndex is valid, extract the sublist of guides for the current page

                pageGuides = guidesForView.subList(fromIndex, toIndex);
            } else {

                // If fromIndex is invalid (e.g., guidesForView is empty or page is somehow still out of calculated bounds),
                // initialize pageGuides as an empty list to avoid errors and display nothing

                pageGuides = new ArrayList<>();

                // As a safeguard, if there are guides in total but fromIndex was invalid (which ideally shouldn't happen with prior page validation),
                // reset the page variable to 0 to ensure consistency if an empty list is shown.

                if (totalGuides > 0) {
                    page = 0;
                }
            }

            // Add guides for each page, current page number and total pages to the model

            model.addAttribute("guides", pageGuides);
            model.addAttribute("page", page);
            model.addAttribute("totalPages", totalPages);

            // If authenticated principal is not null, add its claims to the model under "profile" object

            if (principal != null) {
                model.addAttribute("profile", principal.getClaims());
            }

        } catch (Exception e) {

            // Catch eventual exceptions and print their message along with a more user friendly message

            logger.error("Error loading guides data", e);
            loadErrorMessage = "An unexpected error occurred while loading guide information. Please try again later.";
        }

        // Add page labels to the model

        model.addAttribute("pageTitle", "Meet the guides | TripToday");
        model.addAttribute("mainHeading", "Meet our team of experienced guides");
        model.addAttribute("introParagraph", "Get to know the knowledgeable and friendly faces who will bring your destinations to life. Our guides aren't just experts in history and nature; they're passionate storytellers dedicated to making your TripToday experience authentic and unforgettable. Find out more about the individuals ready to lead your next adventure!");
        model.addAttribute("noGuidesMessage", "No guides found.");

        // In case an exception was caught, 'loadErrorMessage' will not be null

        if (loadErrorMessage != null) {

            // If 'loadErrorMessage' isn't null, add the error message along with default values to the model

            model.addAttribute("errorMessage", loadErrorMessage);
            if (!model.containsAttribute("guides")) model.addAttribute("guides", new ArrayList<>());
            if (!model.containsAttribute("page")) model.addAttribute("page", 0);
            if (!model.containsAttribute("totalPages")) model.addAttribute("totalPages", 0);
        }

        // Return thymeleaf template

        return "guides-page";
    }
}
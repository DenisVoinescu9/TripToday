package finalproject.TripToday.controller;

import finalproject.TripToday.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;

    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String profile(Model model, @RequestParam(name = "up_page", defaultValue = "0") int upcomingPageParam, @RequestParam(name = "pa_page", defaultValue = "0") int pastPageParam, @AuthenticationPrincipal OidcUser principal) {

        if (principal == null) {
            return "redirect:/";
        }

        logger.info("Loading profile page for user: {} - up_page: {}, pa_page: {}", principal.getName(), upcomingPageParam, pastPageParam);

        Map<String, Object> profilePageData = profileService.prepareProfilePageData(principal, upcomingPageParam, pastPageParam);
        profilePageData.forEach(model::addAttribute);

        if (model.containsAttribute("errorMessage")) {
            logger.error("Error loading profile page for user {}: {}", principal.getName(), model.getAttribute("errorMessage"));
        }

        return "profile-page";
    }

    @PostMapping("/update-description")
    public String updateDescription(@RequestParam("description") String description, @AuthenticationPrincipal OidcUser principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication required.");
            return "redirect:/";
        }
        String userId = principal.getName();
        try {
            profileService.updateUserDescription(userId, description);
            redirectAttributes.addFlashAttribute("successMessage", "Description updated successfully!");
        } catch (Exception e) {
            logger.error("Error updating description for user {}", userId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update description. Reason: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/update-picture")
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
            profileService.updateUserPicture(userId, imageUrl.trim());
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated successfully! Please log in again to see the changes.");
        } catch (Exception e) {
            logger.error("Error updating picture for user {}", userId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile picture. Reason: " + e.getMessage());
        }
        return "redirect:/profile";
    }
}
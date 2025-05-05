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
import org.springframework.transaction.annotation.Transactional; // Importat pt /enroll
import org.springframework.dao.DataIntegrityViolationException; // Importat pt /enroll
import finalproject.TripToday.repository.TripRepository; // Importat pt /enroll

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional; // Importat pt /enroll
import java.util.stream.Collectors;

@Controller
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final TripService tripService;
    private final Auth0Service auth0Service;
    // Asigura-te ca TripRepository este injectat daca e folosit in alta parte
    // private final TripRepository tripRepository;

    @Autowired
    public ProfileController(TripService tripService, Auth0Service auth0Service) {
        this.tripService = tripService;
        this.auth0Service = auth0Service;
        // this.tripRepository = tripRepository; // Daca l-ai injecta
    }

    @GetMapping("/profile")
    public String profile(Model model,
                          @RequestParam(name="up_page", defaultValue = "0") int upcomingPage,
                          @RequestParam(name="pa_page", defaultValue = "0") int pastPage,
                          @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return "redirect:/";
        }

        Map<String, Object> claims = principal.getClaims();
        model.addAttribute("user", claims);
        String userId = principal.getName();
        logger.info("Loading profile for user: {} - up_page: {}, pa_page: {}", userId, upcomingPage, pastPage);
        String errorMessage = null;

        try {
            String description = auth0Service.getGuideDescription(userId);
            model.addAttribute("description", description);

            Map<String, List<Trip>> splitTrips = tripService.splitUserTripsByDate(userId);
            List<Trip> allUpcomingTrips = splitTrips.getOrDefault("upcomingTrips", Collections.emptyList());
            List<Trip> allPastTrips = splitTrips.getOrDefault("pastTrips", Collections.emptyList());

            // logger.debug("Total upcoming trips found: {}", allUpcomingTrips.size());
            // logger.debug("Total past trips found: {}", allPastTrips.size());

            // Optimizare: Ia toti ghizii o data
            Map<String, String> guideEmailMap = auth0Service.getAllGuides().stream()
                    .filter(g -> g != null && g.get("id") != null && g.get("email") != null)
                    .collect(Collectors.toMap(g -> g.get("id"), g -> g.get("email"), (e1, e2) -> e1));


            int pageSize = 3; // Seteaza dimensiunea paginii la 3

            // --- Paginare Upcoming Trips ---
            int totalUpcomingTrips = allUpcomingTrips.size();
            int totalUpcomingPages = (int) Math.ceil((double) totalUpcomingTrips / pageSize);
            if (totalUpcomingPages == 0) totalUpcomingPages = 1; // Minim o pagina

            // Valideaza pagina ceruta
            if (upcomingPage < 0) upcomingPage = 0;
            else if (upcomingPage >= totalUpcomingPages) upcomingPage = totalUpcomingPages - 1;

            int upcomingFromIndex = upcomingPage * pageSize;
            int upcomingToIndex = Math.min(upcomingFromIndex + pageSize, totalUpcomingTrips);

            // Extrage sublista doar daca indexul de start e valid
            List<Trip> pageUpcomingTrips = (upcomingFromIndex < totalUpcomingTrips && upcomingFromIndex >= 0) ?
                    allUpcomingTrips.subList(upcomingFromIndex, upcomingToIndex) : Collections.emptyList();

            // logger.debug("Upcoming Trips Pagination: total={}, totalPages={}, requestedPage={}, from={}, to={}, resultingSize={}",
            //        totalUpcomingTrips, totalUpcomingPages, upcomingPage, upcomingFromIndex, upcomingToIndex, pageUpcomingTrips.size());

            List<String> upcomingTripGuideEmailsOnPage = new ArrayList<>();
            for (Trip trip : pageUpcomingTrips) {
                String guideId = trip.getGuideId();
                upcomingTripGuideEmailsOnPage.add(guideId != null ? guideEmailMap.get(guideId.toString()) : null);
            }

            model.addAttribute("pageUpcomingTrips", pageUpcomingTrips);
            model.addAttribute("upcomingPage", upcomingPage);
            model.addAttribute("totalUpcomingPages", totalUpcomingPages);
            model.addAttribute("upcomingTripGuideEmailsOnPage", upcomingTripGuideEmailsOnPage);
            // --- Sfarsit Paginare Upcoming Trips ---


            // --- Paginare Past Trips ---
            int totalPastTrips = allPastTrips.size();
            int totalPastPages = (int) Math.ceil((double) totalPastTrips / pageSize);
            if (totalPastPages == 0) totalPastPages = 1; // Minim o pagina

            // Valideaza pagina ceruta
            if (pastPage < 0) pastPage = 0;
            else if (pastPage >= totalPastPages) pastPage = totalPastPages - 1;

            int pastFromIndex = pastPage * pageSize;
            int pastToIndex = Math.min(pastFromIndex + pageSize, totalPastTrips);

            // Extrage sublista doar daca indexul de start e valid
            List<Trip> pagePastTrips = (pastFromIndex < totalPastTrips && pastFromIndex >= 0) ?
                    allPastTrips.subList(pastFromIndex, pastToIndex) : Collections.emptyList();

            // logger.debug("Past Trips Pagination: total={}, totalPages={}, requestedPage={}, from={}, to={}, resultingSize={}",
            //         totalPastTrips, totalPastPages, pastPage, pastFromIndex, pastToIndex, pagePastTrips.size());


            List<String> pastTripGuideEmailsOnPage = new ArrayList<>();
            for (Trip trip : pagePastTrips) {
                String guideId = trip.getGuideId();
                pastTripGuideEmailsOnPage.add(guideId != null ? guideEmailMap.get(guideId.toString()) : null);
            }

            model.addAttribute("pagePastTrips", pagePastTrips);
            model.addAttribute("pastPage", pastPage);
            model.addAttribute("totalPastPages", totalPastPages);
            model.addAttribute("pastTripGuideEmailsOnPage", pastTripGuideEmailsOnPage);
            // --- Sfarsit Paginare Past Trips ---


        } catch (Exception e) {
            logger.error("Error loading profile data for user {}", userId, e);
            errorMessage = "Could not load profile data. Please try again later.";
            model.addAttribute("description", "");
            model.addAttribute("pagePastTrips", Collections.emptyList());
            model.addAttribute("pastPage", 0);
            model.addAttribute("totalPastPages", 0);
            model.addAttribute("pastTripGuideEmailsOnPage", Collections.emptyList());
            model.addAttribute("pageUpcomingTrips", Collections.emptyList()); // Foloseste pageUpcomingTrips aici
            model.addAttribute("upcomingPage", 0);
            model.addAttribute("totalUpcomingPages", 0);
            model.addAttribute("upcomingTripGuideEmailsOnPage", Collections.emptyList());
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

    // Metoda /enroll - presupunem ca exista din controllerul UpcomingTripsController
    // Daca vrei si logica de enroll de pe pagina de profil, trebuie adaugata/adaptata aici
}
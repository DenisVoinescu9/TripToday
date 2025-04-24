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
            logger.warn("Access attempt to /profile without authentication.");
            return "redirect:/"; // Redirecționează la pagina principală sau de login
        }

        Map<String, Object> claims = principal.getClaims();
        model.addAttribute("user", claims); // Trimite claims-urile user-ului (inclusiv user_id, picture, email, role)

        String userId = principal.getName(); // ID-ul utilizatorului autentificat
        logger.info("Loading profile page for user ID: {}", userId);

        try {
            // Preia descrierea ghidului
            String description = auth0Service.getGuideDescription(userId);
            model.addAttribute("description", description);
            logger.debug("Fetched description for user {}: '{}'", userId, description);

            // Preia și împarte excursiile
            Map<String, List<Trip>> splitTrips = tripService.splitUserTripsByDate(userId);
            List<Trip> pastTrips = splitTrips.getOrDefault("pastTrips", Collections.emptyList());
            List<Trip> upcomingTrips = splitTrips.getOrDefault("upcomingTrips", Collections.emptyList());
            model.addAttribute("pastTrips", pastTrips);
            model.addAttribute("upcomingTrips", upcomingTrips);
            logger.debug("Fetched {} past trips and {} upcoming trips for user {}", pastTrips.size(), upcomingTrips.size(), userId);

            // Preia emailurile ghizilor pentru excursiile viitoare (mai eficient)
            List<String> upcomingTripGuideEmails = upcomingTrips.stream()
                    .map(Trip::getGuideId)
                    .distinct() // Extragem ID-uri unice
                    .map(auth0Service::getGuideIdAndEmail) // Luăm emailul pentru fiecare
                    .collect(Collectors.toMap(map -> map.get("guideId"), map -> map.get("guideEmail"))) // Creăm un Map<ID, Email>
                    .entrySet().stream() // Iterăm prin Map
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty()) // Filtrăm emailurile valide
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)) // Reconstruim Map-ul filtrat
                    .values().stream().collect(Collectors.toList()); // Colectăm doar emailurile într-o listă
            // Alternativ, dacă vrei să păstrezi corespondența indexului din HTML:
                 /*
                 List<String> upcomingTripGuideEmails = upcomingTrips.stream()
                    .map(trip -> auth0Service.getGuideIdAndEmail(trip.getGuideId()).get("guideEmail"))
                    .collect(Collectors.toList());
                 */
            model.addAttribute("upcomingTripGuideEmails", upcomingTripGuideEmails);


            // Preia emailurile ghizilor pentru excursiile trecute
            List<String> pastTripGuideEmails = pastTrips.stream()
                    .map(Trip::getGuideId)
                    .distinct()
                    .map(auth0Service::getGuideIdAndEmail)
                    .collect(Collectors.toMap(map -> map.get("guideId"), map -> map.get("guideEmail")))
                    .entrySet().stream()
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    .values().stream().collect(Collectors.toList());

            // List<String> pastTripGuideEmails = pastTrips.stream()
            //      .map(trip -> auth0Service.getGuideIdAndEmail(trip.getGuideId()).get("guideEmail"))
            //      .collect(Collectors.toList());
            model.addAttribute("pastTripGuideEmails", pastTripGuideEmails);


        } catch (Exception e) {
            // Prinde orice excepție neașteptată la încărcarea datelor profilului
            logger.error("Error loading profile data for user {}: {}", userId, e.getMessage(), e);
            model.addAttribute("errorMessage", "Could not load profile data. Please try again later.");
            // Asigură valori default pentru a evita erori în Thymeleaf
            model.addAttribute("description", "");
            model.addAttribute("pastTrips", Collections.emptyList());
            model.addAttribute("upcomingTrips", Collections.emptyList());
            model.addAttribute("pastTripGuideEmails", Collections.emptyList());
            model.addAttribute("upcomingTripGuideEmails", Collections.emptyList());
        }

        return "profile"; // Numele template-ului Thymeleaf
    }

    // Endpoint pentru actualizare descriere
    @PostMapping("/profile/update-description")
    public String updateDescription(@RequestParam("description") String description,
                                    @AuthenticationPrincipal OidcUser principal,
                                    RedirectAttributes redirectAttributes) {

        if (principal == null) {
            logger.warn("Attempt to update description without authentication.");
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication required.");
            return "redirect:/";
        }
        String userId = principal.getName();
        logger.info("User {} attempting to update description.", userId);
        logger.debug("Description data received from form for user {}: '{}'", userId, description);

        try {
            // Apelam serviciul care acum poate arunca exceptii mai specifice
            auth0Service.updateGuideDescription(userId, description);
            logger.info("Successfully updated description for user {}", userId);
            redirectAttributes.addFlashAttribute("successMessage", "Description updated successfully!");
        } catch (IllegalArgumentException e) { // Prindem erori de validare simple
            logger.warn("Failed to update description for user {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Update failed: " + e.getMessage());
        } catch (RuntimeException e) { // Prindem erorile re-aruncate din Auth0Service
            logger.error("Failed to update description for user {}: {}", userId, e.getMessage(), e);
            // Afisam un mesaj mai prietenos, eroarea detaliată e deja in loguri
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update description. Reason: " + e.getMessage());
        }

        return "redirect:/profile"; // Redirecționează inapoi la profil
    }

    // Endpoint pentru actualizare poză
    @PostMapping("/profile/update-picture")
    public String updatePicture(@RequestParam("imageUrl") String imageUrl,
                                @AuthenticationPrincipal OidcUser principal,
                                RedirectAttributes redirectAttributes) {

        if (principal == null) {
            logger.warn("Attempt to update picture without authentication.");
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication required.");
            return "redirect:/";
        }
        String userId = principal.getName();
        logger.info("User {} attempting to update picture.", userId);
        logger.debug("Image URL received from form for user {}: '{}'", userId, imageUrl);


        // Validare simpla URL (optional, dar recomandat) - mutata aici
        if (imageUrl == null || imageUrl.trim().isEmpty() || !imageUrl.trim().toLowerCase().startsWith("http")) {
            logger.warn("Invalid image URL provided by user {}: {}", userId, imageUrl);
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid image URL provided. Please enter a valid URL starting with http or https.");
            return "redirect:/profile"; // Opreste executia si redirectioneaza
        }

        try {
            auth0Service.updatePicture(userId, imageUrl.trim()); // Trimite URL curatat
            logger.info("Successfully updated picture for user {}", userId);
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated successfully!");
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update picture for user {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Update failed: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Failed to update picture for user {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile picture. Reason: " + e.getMessage());
        }

        return "redirect:/profile"; // Redirecționează inapoi la profil
    }
}
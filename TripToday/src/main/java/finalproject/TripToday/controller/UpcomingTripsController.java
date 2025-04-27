package finalproject.TripToday.controller;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.entity.UserTrip;
import finalproject.TripToday.service.Auth0Service;
import finalproject.TripToday.service.TripService;
import finalproject.TripToday.service.UserTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class UpcomingTripsController {

    private final TripService tripService;

    private final UserTripService userTripService;

    private final Auth0Service auth0Service;


    @Autowired
    public UpcomingTripsController(TripService tripService, UserTripService userTripService, Auth0Service auth0Service) {
        this.tripService = tripService;
        this.userTripService = userTripService;
        this.auth0Service = auth0Service;
    }

    @GetMapping("/trips")
    public String trips(Model model, @AuthenticationPrincipal OidcUser principal) {
        if (principal != null) {
            model.addAttribute("user", principal.getClaims());
        }

        List<Trip> trips = tripService.getUpcomingTrips();
        model.addAttribute("trips", trips);


        List<Map<String, String>> guides = auth0Service.getAllGuides();
        model.addAttribute("guides", guides);

        return "upcoming-trips";
    }


    @PostMapping("/create-trip")
    public String createTrip(@ModelAttribute Trip trip, Model model) {

        tripService.createTrip(trip);
        model.addAttribute("trip", trip);

        return "redirect:/trips";
    }

    @PostMapping("/update-trip")
    public String updateTrip(@ModelAttribute Trip trip) {
        tripService.updateTrip(trip.getId(), trip);
        return "redirect:/trips";
    }

    @PostMapping("/delete-trip")
    public String deleteTrip(@RequestParam("id") Integer tripId) {
        tripService.deleteTrip(tripId);
        return "redirect:/trips";
    }


    @PostMapping("/enroll")
    public String enrollInTrip(@RequestParam("tripId") Integer tripId,
                               @RequestParam(required = false) String cardNumber, // Le păstrăm chiar dacă nu le folosim explicit aici
                               @RequestParam(required = false) String cvv,
                               @RequestParam(required = false) String expirationDate,
                               @AuthenticationPrincipal OidcUser principal,
                               RedirectAttributes redirectAttributes) { // Adăugăm RedirectAttributes

        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Trebuie să fii autentificat pentru a te înscrie.");
            return "redirect:/trips";
        }

        String userId = principal.getName();
        UserTrip userTrip = new UserTrip();
        userTrip.setUserId(userId);
        userTrip.setTripId(tripId);

        try {
            userTripService.createUserTrip(userTrip);
            redirectAttributes.addFlashAttribute("successMessage", "You successfully enrolled in this trip!");
        } catch (DataIntegrityViolationException e) {

            redirectAttributes.addFlashAttribute("errorMessage", "You are already enrolled in this trip.");

        }


        return "redirect:/trips";
    }
}

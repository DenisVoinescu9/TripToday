package finalproject.TripToday.controller;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.entity.UserTrip;
import finalproject.TripToday.service.Auth0Service;
import finalproject.TripToday.service.TripService;
import finalproject.TripToday.service.UserTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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

        // Obține excursiile viitoare
        List<Trip> trips = tripService.getUpcomingTrips();
        model.addAttribute("trips", trips);

        // Obține ghizii, cu id și email (pentru a le trimite în model)
        List<Map<String, String>> guides = auth0Service.getAllGuides(); // Lista de ghizi cu id și email
        model.addAttribute("guides", guides); // Trimitem ghizii în model

        return "upcoming-trips";
    }


    @PostMapping("/create-trip")
    public String createTrip(@ModelAttribute Trip trip, Model model) {

        tripService.createTrip(trip);
        model.addAttribute("trip", trip);

        return "redirect:/trips";
    }


    @PostMapping("/enroll")
    public String enrollInTrip(@RequestParam("tripId") Integer tripId,
                               @AuthenticationPrincipal OidcUser principal) {

        if (principal != null) {
            String userId = principal.getName(); // User ID from the authentication principal
            UserTrip userTrip = new UserTrip();
            userTrip.setUserId(userId);
            userTrip.setTripId(tripId);

            // Save the UserTrip entity (enrollment)
            userTripService.createUserTrip(userTrip);

            // Decrement available spots for the trip
            boolean success = tripService.decrementAvailableSpots(tripId);

            if (!success) {
                // Handle the case when there are no available spots
                return "redirect:/trips?error=No available spots";
            }
        }

        return "redirect:/trips"; // Redirect after enrollment
    }
}

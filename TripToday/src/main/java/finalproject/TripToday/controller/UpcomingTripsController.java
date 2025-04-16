package finalproject.TripToday.controller;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.entity.UserTrip;
import finalproject.TripToday.service.TripService;
import finalproject.TripToday.service.UserTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class UpcomingTripsController {

    private final TripService tripService;

    private final UserTripService userTripService;

    @Autowired
    public UpcomingTripsController(TripService tripService, UserTripService userTripService) {
        this.tripService = tripService;
        this.userTripService = userTripService;
    }

    @GetMapping("/trips")
    public String trips(Model model, @AuthenticationPrincipal OidcUser principal) {
        if (principal != null) {
            model.addAttribute("user", principal.getClaims());
        }
        List<Trip> trips = tripService.getUpcomingTrips();
        model.addAttribute("trips", trips);
        System.out.println(trips);
        return "upcoming-trips";
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

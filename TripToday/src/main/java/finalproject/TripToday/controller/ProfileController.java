package finalproject.TripToday.controller;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.service.TripService;
import finalproject.TripToday.service.Auth0Service;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class ProfileController {

    private final TripService tripService;
    private final Auth0Service auth0Service;

    public ProfileController(TripService tripService, Auth0Service auth0Service) {
        this.tripService = tripService;
        this.auth0Service = auth0Service;
    }

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal OidcUser principal) {
        if (principal != null) {
            model.addAttribute("user", principal.getClaims());
        }
        assert principal != null;
        String userId = principal.getName();

        String description = auth0Service.getGuideDescription(userId);
        model.addAttribute("description", description);

        Map<String, List<Trip>> splitTrips = tripService.splitUserTripsByDate(userId);

        List<Trip> pastTrips = splitTrips.get("pastTrips");
        List<Trip> upcomingTrips = splitTrips.get("upcomingTrips");

        List<String> pastTripGuideEmails = new ArrayList<>();
        List<String> upcomingTripGuideEmails = new ArrayList<>();

        for (Trip trip : pastTrips) {
            Map<String, String> guideInfo = auth0Service.getGuideIdAndEmail(trip.getGuideId());
            pastTripGuideEmails.add(guideInfo.get("guideEmail"));
        }

        for (Trip trip : upcomingTrips) {
            Map<String, String> guideInfo = auth0Service.getGuideIdAndEmail(trip.getGuideId());
            upcomingTripGuideEmails.add(guideInfo.get("guideEmail"));
        }

        model.addAttribute("pastTrips", pastTrips);
        model.addAttribute("upcomingTrips", upcomingTrips);
        model.addAttribute("pastTripGuideEmails", pastTripGuideEmails);
        model.addAttribute("upcomingTripGuideEmails", upcomingTripGuideEmails);

        return "profile";
    }
}

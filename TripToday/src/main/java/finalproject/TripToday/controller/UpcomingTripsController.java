package finalproject.TripToday.controller;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.service.TripService;
import finalproject.TripToday.service.UpcomingTripsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/trips")
public class UpcomingTripsController {

    private static final Logger logger = LoggerFactory.getLogger(UpcomingTripsController.class);

    private final UpcomingTripsService upcomingTripsService;
    private final TripService tripService;


    @Autowired
    public UpcomingTripsController(UpcomingTripsService upcomingTripsService, TripService tripService) {
        this.upcomingTripsService = upcomingTripsService;
        this.tripService = tripService;
    }

    @GetMapping
    public String trips(@RequestParam(name = "page", defaultValue = "0") int upcomingPageParam, @RequestParam(name = "pa_page", defaultValue = "0") int pastPageParam, Model model, @AuthenticationPrincipal OidcUser principal) {

        Map<String, String> staticData = upcomingTripsService.getStaticPageData();
        staticData.forEach(model::addAttribute);

        Map<String, Object> viewData = upcomingTripsService.prepareTripsViewData(upcomingPageParam, pastPageParam, principal);
        viewData.forEach(model::addAttribute);

        return "upcoming-trips-page";
    }

    @PostMapping("/create-trip")
    public String createTrip(@ModelAttribute Trip trip, RedirectAttributes redirectAttributes) {
        try {
            tripService.createTrip(trip);
            redirectAttributes.addFlashAttribute("successMessage", "Trip successfully created!");
        } catch (Exception e) {
            logger.error("Error creating trip", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating trip. Please try again. Details: " + e.getMessage());
        }
        return "redirect:/trips";
    }

    @PostMapping("/update-trip")
    public String updateTrip(@ModelAttribute Trip trip, RedirectAttributes redirectAttributes) {
        try {
            Trip updatedTrip = tripService.updateTrip(trip.getId(), trip);
            if (updatedTrip != null) {
                redirectAttributes.addFlashAttribute("successMessage", "Trip successfully updated!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Trip not found for update.");
            }
        } catch (Exception e) {
            logger.error("Error updating trip {}", trip.getId(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating trip. Please try again. Details: " + e.getMessage());
        }
        return "redirect:/trips";
    }

    @PostMapping("/delete-trip")
    public String deleteTripPost(@RequestParam("id") Integer tripId, RedirectAttributes redirectAttributes) {
        try {
            boolean success = tripService.deleteTrip(tripId);
            if (success) {
                redirectAttributes.addFlashAttribute("successMessage", "Trip was successfully marked as canceled.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Could not find the trip to mark as canceled.");
            }
        } catch (Exception e) {
            logger.error("Error canceling trip {}", tripId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error canceling trip. Please try again. Details: " + e.getMessage());
        }
        return "redirect:/trips";
    }

    @PostMapping("/enroll")
    public String enrollInTrip(@RequestParam("tripId") Integer tripId, @AuthenticationPrincipal OidcUser principal, RedirectAttributes redirectAttributes) {

        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "You must be authenticated to enroll in a trip.");
            return "redirect:/trips";
        }
        String userId = principal.getName();

        try {
            String enrollmentResult = tripService.enrollUserInTrip(userId, tripId);
            if ("SUCCESS".equals(enrollmentResult)) {
                redirectAttributes.addFlashAttribute("successMessage", "You successfully enrolled in this trip!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", enrollmentResult);
            }
        } catch (Exception e) {
            logger.error("Unexpected error during enrollment for user {} trip {}: {}", userId, tripId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during enrollment process.");
        }
        return "redirect:/trips";
    }
}
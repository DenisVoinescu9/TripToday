package finalproject.TripToday.controller;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.entity.UserTrip;
import finalproject.TripToday.repository.TripRepository;
import finalproject.TripToday.service.Auth0Service;
import finalproject.TripToday.service.TripService;
import finalproject.TripToday.service.UserTripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class UpcomingTripsController {

    private static final Logger logger = LoggerFactory.getLogger(UpcomingTripsController.class);
    private static final int PAGE_SIZE = 3;

    private final TripService tripService;
    private final UserTripService userTripService;
    private final Auth0Service auth0Service;
    private final TripRepository tripRepository;

    @Autowired
    public UpcomingTripsController(TripService tripService,
                                   UserTripService userTripService,
                                   Auth0Service auth0Service,
                                   TripRepository tripRepository) {
        this.tripService = tripService;
        this.userTripService = userTripService;
        this.auth0Service = auth0Service;
        this.tripRepository = tripRepository;
    }

    @GetMapping("/trips")
    public String trips(@RequestParam(name = "page", defaultValue = "0") int upcomingPageParam,
                        @RequestParam(name = "pa_page", defaultValue = "0") int pastPageParam,
                        Model model, @AuthenticationPrincipal OidcUser principal) {

        Map<String, Object> userClaims;
        boolean isUserManager = false;

        if (principal != null) {
            userClaims = principal.getClaims();
            model.addAttribute("user", userClaims);

            Object rolesClaimObj = userClaims.get("role");
            if (rolesClaimObj != null) {
                String rolesClaimStr = rolesClaimObj.toString();
                if (Arrays.stream(rolesClaimStr.split("\\s*\\|\\s*")).anyMatch(role -> role.trim().equals("Manager"))) {
                    isUserManager = true;
                }
            }
        }
        model.addAttribute("isManagerActual", isUserManager);

        List<Trip> allUpcomingTripsFiltered = tripService.getUpcomingTrips().stream()
                .filter(trip -> trip.getCanceled() != null && !trip.getCanceled())
                .collect(Collectors.toList());

        int totalUpcomingPages = (int) Math.ceil((double) allUpcomingTripsFiltered.size() / PAGE_SIZE);
        int currentUpcomingPage = upcomingPageParam;
        if (currentUpcomingPage < 0) currentUpcomingPage = 0;
        if (currentUpcomingPage >= totalUpcomingPages) {
            currentUpcomingPage = totalUpcomingPages > 0 ? totalUpcomingPages - 1 : 0;
        }


        int upcomingFromIndex = currentUpcomingPage * PAGE_SIZE;
        int upcomingToIndex = Math.min(upcomingFromIndex + PAGE_SIZE, allUpcomingTripsFiltered.size());
        List<Trip> pageUpcomingTrips = allUpcomingTripsFiltered.isEmpty() ? Collections.emptyList() : allUpcomingTripsFiltered.subList(upcomingFromIndex, upcomingToIndex);

        model.addAttribute("trips", pageUpcomingTrips);
        model.addAttribute("page", currentUpcomingPage);
        model.addAttribute("totalPages", totalUpcomingPages);

        if (isUserManager) {
            List<Trip> allPastTripsFiltered = tripService.getPastTrips().stream()
                    .filter(trip -> trip.getCanceled() != null && !trip.getCanceled())
                    .collect(Collectors.toList());

            int totalPastPages = (int) Math.ceil((double) allPastTripsFiltered.size() / PAGE_SIZE);
            int currentPastPage = pastPageParam;
            if (currentPastPage < 0) currentPastPage = 0;
            if (currentPastPage >= totalPastPages) {
                currentPastPage = totalPastPages > 0 ? totalPastPages - 1 : 0;
            }

            int pastFromIndex = currentPastPage * PAGE_SIZE;
            int pastToIndex = Math.min(pastFromIndex + PAGE_SIZE, allPastTripsFiltered.size());
            List<Trip> pagePastTripsList = allPastTripsFiltered.isEmpty() ? Collections.emptyList() : allPastTripsFiltered.subList(pastFromIndex, pastToIndex);

            model.addAttribute("pagePastTrips", pagePastTripsList);
            model.addAttribute("pastPage", currentPastPage);
            model.addAttribute("totalPastPages", totalPastPages);
        }

        model.addAttribute("guides", auth0Service.getAllGuides());

        model.addAttribute("pageTitle", "Upcoming trips | TripToday");
        model.addAttribute("introHeading", "Your next adventure awaits!");
        model.addAttribute("introParagraph", "Browse our currently available trips below. Find the perfect destination, check the details, and secure your spot for an unforgettable journey. Let's explore together!");
        model.addAttribute("labelDestination", "Destination");
        model.addAttribute("labelDescription", "Description");
        model.addAttribute("labelDepartureLocation", "Departure location");
        model.addAttribute("labelDepartureDate", "Departure date");
        model.addAttribute("labelDepartureHour", "Departure hour");
        model.addAttribute("labelReturnDate", "Return date");
        model.addAttribute("labelDuration", "Duration (days):");
        model.addAttribute("labelRemainingSpots", "Remaining spots");
        model.addAttribute("labelEnrollmentFee", "Enrollment fee");
        model.addAttribute("labelGuide", "Guide");
        model.addAttribute("labelHotel", "Hotel");
        model.addAttribute("textNoGuideAssigned", "No guide assigned");
        model.addAttribute("buttonEnroll", "Enroll");
        model.addAttribute("buttonFull", "No available spots");
        model.addAttribute("buttonViewTravelers", "View travelers");
        model.addAttribute("buttonEdit", "Edit trip");
        model.addAttribute("buttonCancelTrip", "Cancel trip");
        model.addAttribute("buttonCreateTrip", "Create trip");
        model.addAttribute("unitDays", "days");
        model.addAttribute("unitSpots", "spots");
        model.addAttribute("unitCurrency", "RON");
        model.addAttribute("unitTimezone", "UTC");
        model.addAttribute("paginationPreviousSymbol", "\u2190");
        model.addAttribute("paginationNextSymbol", "\u2192");
        model.addAttribute("tripCanceledMessageGeneric", "This trip was canceled.");
        model.addAttribute("buttonUpcomingTrips", "Upcoming trips");
        model.addAttribute("buttonPastTrips", "Past trips");
        model.addAttribute("titleUpcomingTrips", "Upcoming trips");
        model.addAttribute("titlePastTrips", "Past trips");
        model.addAttribute("messageNoUpcomingTrips", "There are no upcoming trips at the moment.");
        model.addAttribute("messageNoPastTrips", "No past trips found.");

        return "upcoming-trips-page";
    }

    @PostMapping("/create-trip")
    public String createTrip(@ModelAttribute Trip trip, RedirectAttributes redirectAttributes) {
        try {
            tripService.createTrip(trip);
            redirectAttributes.addFlashAttribute("successMessage", "Trip successfully created!");
        } catch (Exception e) {
            logger.error("Error creating trip", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating trip. Please try again.");
        }
        return "redirect:/trips";
    }

    @PostMapping("/update-trip")
    public String updateTrip(@ModelAttribute Trip trip, RedirectAttributes redirectAttributes) {
        try {
            tripService.updateTrip(trip.getId(), trip);
            redirectAttributes.addFlashAttribute("successMessage", "Trip successfully updated!");
        } catch (Exception e) {
            logger.error("Error updating trip {}", trip.getId(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating trip. Please try again.");
        }
        return "redirect:/trips";
    }

    @PostMapping("/delete-trip")
    public String deleteTripPost(@RequestParam("id") Integer tripId, RedirectAttributes redirectAttributes) {
        boolean success = tripService.deleteTrip(tripId);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Trip was successfully canceled.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not find the trip to mark as canceled.");
        }
        return "redirect:/trips";
    }

    @PostMapping("/enroll")
    @Transactional
    public String enrollInTrip(@RequestParam("tripId") Integer tripId,
                               @RequestParam(required = false) String cardNumber,
                               @RequestParam(required = false) String cvv,
                               @RequestParam(required = false) String expirationDate,
                               @AuthenticationPrincipal OidcUser principal,
                               RedirectAttributes redirectAttributes) {

        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "You must be authenticated to enroll in a trip.");
            return "redirect:/trips";
        }
        String userId = principal.getName();
        try {
            Optional<Trip> optionalTrip = tripService.getTripById(tripId);
            if (optionalTrip.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Trip not found.");
                return "redirect:/trips";
            }
            Trip trip = optionalTrip.get();
            if (trip.getCanceled() != null && trip.getCanceled()){
                redirectAttributes.addFlashAttribute("errorMessage", "This trip has been canceled.");
                return "redirect:/trips";
            }
            if (trip.getAvailableSpots() <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Sorry, this trip is full.");
                return "redirect:/trips";
            }
            String guideIdAsString = (trip.getGuideId() != null) ? String.valueOf(trip.getGuideId()) : null;
            if (guideIdAsString != null && userId.equals(guideIdAsString)) {
                redirectAttributes.addFlashAttribute("errorMessage", "You cannot enroll in a trip you are guiding.");
                return "redirect:/trips";
            }
            if (userTripService.isUserEnrolled(userId, tripId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "You are already enrolled in this trip.");
                return "redirect:/trips";
            }
            UserTrip userTrip = new UserTrip();
            userTrip.setUserId(userId);
            userTrip.setTripId(tripId);
            userTripService.createUserTrip(userTrip);
            trip.setAvailableSpots(trip.getAvailableSpots() - 1);
            tripRepository.save(trip);
            redirectAttributes.addFlashAttribute("successMessage", "You successfully enrolled in this trip!");
        } catch (Exception e) {
            logger.error("Unexpected error during enrollment for user {} trip {}: {}", userId, tripId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during enrollment process.");
        }
        return "redirect:/trips";
    }
}
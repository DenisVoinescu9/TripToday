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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller // Spring MVC Controller
public class UpcomingTripsController {

    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(UpcomingTripsController.class);

    // Service and Repository dependencies
    private final TripService tripService;
    private final UserTripService userTripService;
    private final Auth0Service auth0Service;
    private final TripRepository tripRepository;

    @Autowired // Constructor injection
    public UpcomingTripsController(TripService tripService,
                                   UserTripService userTripService,
                                   Auth0Service auth0Service,
                                   TripRepository tripRepository) {
        // Initialize all dependencies
        this.tripService = tripService;
        this.userTripService = userTripService;
        this.auth0Service = auth0Service;
        this.tripRepository = tripRepository;
    }

    // Handle /trips GET request
    @GetMapping("/trips")
    public String trips(@RequestParam(defaultValue = "0") int page, Model model, @AuthenticationPrincipal OidcUser principal) { // Page, model, user data
        // If user is authenticated
        if (principal != null) {
            // Add user data to model
            model.addAttribute("user", principal.getClaims());
        }

        // Get non-canceled upcoming trips
        List<Trip> allTrips = tripService.getUpcomingTrips()
                .stream()
                .filter(trip -> !trip.getCanceled()) // Filter canceled trips
                .toList(); // Collect to List

        // Define items per page
        int pageSize = 3;
        // Calculate total pages
        int totalPages = (int) Math.ceil((double) allTrips.size() / pageSize);

        // Sanitize page number
        if (page < 0) page = 0; // Floor page to 0
        if (page >= totalPages && totalPages > 0) page = totalPages - 1; // Cap page to max

        // Calculate list slice indices
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allTrips.size()); // Avoid out of bounds

        // Get trips for current page
        List<Trip> pageTrips = allTrips.subList(fromIndex, toIndex);

        // Add data to model
        model.addAttribute("trips", pageTrips); // Trips for current page
        model.addAttribute("page", page);             // Current page number
        model.addAttribute("totalPages", totalPages); // Total pages count

        // Get all guide details
        List<Map<String, String>> guides = auth0Service.getAllGuides();
        // Add guides to model
        model.addAttribute("guides", guides);

        // Add UI text to model
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
        model.addAttribute("buttonFull", "Full");
        model.addAttribute("buttonViewTravelers", "View travelers");
        model.addAttribute("buttonEdit", "Edit trip");
        model.addAttribute("buttonCancelTrip", "Cancel trip");
        model.addAttribute("buttonCreateTrip", "Create trip");
        model.addAttribute("messageNoTrips", "No trips available at the moment.");
        model.addAttribute("unitDays", "days"); // Although labelDuration includes (days): this can be for other uses
        model.addAttribute("unitSpots", "spots");
        model.addAttribute("unitCurrency", "RON");
        model.addAttribute("unitTimezone", "UTC");
        model.addAttribute("paginationPreviousSymbol", "\u2190"); // Left arrow
        model.addAttribute("paginationNextSymbol", "\u2192");     // Right arrow


        // Return view template name
        return "upcoming-trips-page";
    }


    // Handle POST to /create-trip
    @PostMapping("/create-trip")
    public String createTrip(@ModelAttribute Trip trip, Model model, RedirectAttributes redirectAttributes) { // Trip from form data
        try {
            // Attempt to create trip
            tripService.createTrip(trip);
            // Add success flash message
            redirectAttributes.addFlashAttribute("successMessage", "Trip successfully created!");
        } catch (Exception e) {
            // Log creation error
            logger.error("Error creating trip", e);
            // Add error flash message
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating trip. Please try again.");
        }
        // Redirect to trips list
        return "redirect:/trips";
    }

    // Handle POST to /update-trip
    @PostMapping("/update-trip")
    public String updateTrip(@ModelAttribute Trip trip, RedirectAttributes redirectAttributes) { // Trip from form data
        try {
            // Attempt to update trip
            tripService.updateTrip(trip.getId(), trip);
            // Add success flash message
            redirectAttributes.addFlashAttribute("successMessage", "Trip successfully updated!");
        } catch (Exception e) {
            // Log update error
            logger.error("Error updating trip {}", trip.getId(), e);
            // Add error flash message
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating trip. Please try again.");
        }
        // Redirect to trips list
        return "redirect:/trips";
    }

    // Handle POST to /delete-trip
    @PostMapping("/delete-trip")
    public String deleteTripPost(@RequestParam("id") Integer tripId, RedirectAttributes redirectAttributes) { // Trip ID from request
        // Attempt to delete trip
        boolean success = tripService.deleteTrip(tripId);
        // Check deletion status
        if (success) {
            // Add success flash message
            redirectAttributes.addFlashAttribute("successMessage", "Trip was successfully canceled.");
        } else {
            // Add error flash message
            redirectAttributes.addFlashAttribute("errorMessage", "Could not find the trip to mark as canceled.");
        }
        // Redirect to trips list
        return "redirect:/trips";
    }

    // Handle POST to /enroll
    @PostMapping("/enroll")
    @Transactional // Marks as transactional
    public String enrollInTrip(@RequestParam("tripId") Integer tripId,          // Trip ID to enroll
                               @RequestParam(required = false) String cardNumber, // Card details optional
                               @RequestParam(required = false) String cvv,
                               @RequestParam(required = false) String expirationDate,
                               @AuthenticationPrincipal OidcUser principal,     // Authenticated user info
                               RedirectAttributes redirectAttributes) {

        // Check if user logged in
        if (principal == null) {
            // Set auth error message
            redirectAttributes.addFlashAttribute("errorMessage", "You must be authenticated to enroll in a trip.");
            // Redirect if not logged in
            return "redirect:/trips";
        }

        // Get user's unique ID
        String userId = principal.getName();

        try {
            // Fetch trip by ID
            Optional<Trip> optionalTrip = tripService.getTripById(tripId);

            // If trip not found
            if (optionalTrip.isEmpty()) {
                // Set not found message
                redirectAttributes.addFlashAttribute("errorMessage", "Trip not found.");
                return "redirect:/trips";
            }

            // Get Trip object
            Trip trip = optionalTrip.get();

            // Check if spots available
            if (trip.getAvailableSpots() <= 0) {
                // Set trip full message
                redirectAttributes.addFlashAttribute("errorMessage", "Sorry, this trip is full.");
                return "redirect:/trips";
            }

            // Get guide ID as string
            String guideIdAsString = (trip.getGuideId() != null) ? String.valueOf(trip.getGuideId()) : null;
            // Prevent guide enrolling self
            if (guideIdAsString != null && userId.equals(guideIdAsString)) {
                redirectAttributes.addFlashAttribute("errorMessage", "You cannot enroll in a trip you are guiding.");
                return "redirect:/trips";
            }

            // Check if already enrolled
            if (userTripService.isUserEnrolled(userId, tripId)) { // Original Romanian comment: Verifica daca utilizatorul este DEJA inscris INAINTE de a incerca insertia // Original Romanian comment: !!! Implementeaza aceasta metoda in UserTripService !!!
                // Set already enrolled message
                redirectAttributes.addFlashAttribute("errorMessage", "You are already enrolled in this trip.");
                return "redirect:/trips";
            }

            // Prepare enrollment object
            UserTrip userTrip = new UserTrip();
            userTrip.setUserId(userId); // Set user for enrollment
            userTrip.setTripId(tripId); // Set trip for enrollment

            // Create enrollment record
            userTripService.createUserTrip(userTrip);

            // Decrement available spots
            trip.setAvailableSpots(trip.getAvailableSpots() - 1);
            // Save updated trip spots
            tripRepository.save(trip);

            // Set success message
            redirectAttributes.addFlashAttribute("successMessage", "You successfully enrolled in this trip!");

        } catch (Exception e) { // Original Romanian comment: Prinde orice alta exceptie (ex: probleme la salvare, etc.)
            // Log any enrollment error
            logger.error("Unexpected error during enrollment for user {} trip {}: {}", userId, tripId, e.getMessage(), e);
            // Set generic error message
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during enrollment process.");
        }

        // Redirect to trips list
        return "redirect:/trips";
    }
}
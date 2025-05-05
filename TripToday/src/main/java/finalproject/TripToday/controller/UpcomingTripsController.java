package finalproject.TripToday.controller;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.entity.UserTrip;
import finalproject.TripToday.repository.TripRepository;
import finalproject.TripToday.service.Auth0Service;
import finalproject.TripToday.service.TripService;
import finalproject.TripToday.service.UserTripService;
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

@Controller
public class UpcomingTripsController {

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
    public String trips(@RequestParam(defaultValue = "0") int page, Model model, @AuthenticationPrincipal OidcUser principal) {
        if (principal != null) {
            model.addAttribute("user", principal.getClaims());
        }

        List<Trip> allTrips = tripService.getUpcomingTrips()
                .stream()
                .filter(trip -> !trip.getCanceled())
                .toList();

        int pageSize = 3;
        int totalPages = (int) Math.ceil((double) allTrips.size() / pageSize);

        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allTrips.size());

        List<Trip> pageTrips = allTrips.subList(fromIndex, toIndex);

        model.addAttribute("trips", pageTrips);
        model.addAttribute("page", page);
        model.addAttribute("totalPages", totalPages);

        List<Map<String, String>> guides = auth0Service.getAllGuides();
        model.addAttribute("guides", guides);
        System.out.println(guides);

        model.addAttribute("pageTitle", "Upcoming trips | TripToday");
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
        model.addAttribute("unitDays", "days");
        model.addAttribute("unitSpots", "spots");
        model.addAttribute("unitCurrency", "RON");
        model.addAttribute("unitTimezone", "UTC");

        return "upcoming-trips-page";
    }



    @PostMapping("/create-trip")
    public String createTrip(@ModelAttribute Trip trip, Model model, RedirectAttributes redirectAttributes) {
        try {
            tripService.createTrip(trip);
            redirectAttributes.addFlashAttribute("successMessage", "Trip successfully created!");
            // model.addAttribute("trip", trip); // Nu mai este necesar la redirect
        } catch (Exception e) {
            // Logheaza eroarea
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating trip. Please try again.");
            e.printStackTrace(); // Pentru debug
        }
        return "redirect:/trips";
    }

    @PostMapping("/update-trip")
    public String updateTrip(@ModelAttribute Trip trip, RedirectAttributes redirectAttributes) {
        try {
            tripService.updateTrip(trip.getId(), trip);
            redirectAttributes.addFlashAttribute("successMessage", "Trip successfully updated!");
        } catch (Exception e) {
            // Logheaza eroarea
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating trip. Please try again.");
            e.printStackTrace(); // Pentru debug
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

        try {
            Optional<Trip> optionalTrip = tripService.getTripById(tripId);

            if (optionalTrip.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Trip not found.");
                return "redirect:/trips";
            }

            Trip trip = optionalTrip.get();

            // ✅ Verifică dacă utilizatorul curent este ghidul
            String userEmail = principal.getEmail();
            if (trip.getGuideId() != null) {
                List<Map<String, String>> guides = auth0Service.getAllGuides();
                for (Map<String, String> guide : guides) {
                    if (guide != null
                            && guide.get("id").equals(trip.getGuideId())
                            && guide.get("email").equalsIgnoreCase(userEmail)) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Enrollment failed because you are the guide for that trip.");
                        return "redirect:/trips";
                    }
                }
            }

            if (trip.getAvailableSpots() <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Sorry, this trip is full.");
                return "redirect:/trips";
            }

            String userId = principal.getName();
            UserTrip userTrip = new UserTrip();
            userTrip.setUserId(userId);
            userTrip.setTripId(tripId);

            try {
                userTripService.createUserTrip(userTrip);
                trip.setAvailableSpots(trip.getAvailableSpots() - 1);
                tripRepository.save(trip);
                redirectAttributes.addFlashAttribute("successMessage", "You successfully enrolled in this trip!");
            } catch (DataIntegrityViolationException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "You are already enrolled in this trip.");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during enrollment process.");
            e.printStackTrace();
        }

        return "redirect:/trips";
    }


}
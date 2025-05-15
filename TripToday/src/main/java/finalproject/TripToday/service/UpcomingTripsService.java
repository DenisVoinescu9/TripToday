package finalproject.TripToday.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class UpcomingTripsService {

    private static final Logger logger = LoggerFactory.getLogger(UpcomingTripsService.class);
    private static final int PAGE_SIZE = 3;

    private final TripService tripService;
    private final Auth0Service auth0Service;

    @Autowired
    public UpcomingTripsService(TripService tripService, Auth0Service auth0Service) {
        this.tripService = tripService;
        this.auth0Service = auth0Service;
    }

    public Map<String, String> getStaticPageData() {
        Map<String, String> staticData = new HashMap<>();
        staticData.put("pageTitle", "Upcoming trips | TripToday");
        staticData.put("introHeading", "Your next adventure awaits!");
        staticData.put("introParagraph", "Browse our currently available trips below. Find the perfect destination, check the details, and secure your spot for an unforgettable journey. Let's explore together!");
        staticData.put("labelDestination", "Destination");
        staticData.put("labelDescription", "Description");
        staticData.put("labelDepartureLocation", "Departure location");
        staticData.put("labelDepartureDate", "Departure date");
        staticData.put("labelDepartureHour", "Departure hour");
        staticData.put("labelReturnDate", "Return date");
        staticData.put("labelDuration", "Duration (days):");
        staticData.put("labelRemainingSpots", "Remaining spots");
        staticData.put("labelEnrollmentFee", "Enrollment fee");
        staticData.put("labelGuide", "Guide");
        staticData.put("labelHotel", "Hotel");
        staticData.put("textNoGuideAssigned", "No guide assigned");
        staticData.put("buttonEnroll", "Enroll");
        staticData.put("buttonFull", "No available spots");
        staticData.put("buttonViewTravelers", "View travelers");
        staticData.put("buttonEdit", "Edit trip");
        staticData.put("buttonCancelTrip", "Cancel trip");
        staticData.put("buttonCreateTrip", "Create trip");
        staticData.put("unitDays", "days");
        staticData.put("unitSpots", "spots");
        staticData.put("unitCurrency", "RON");
        staticData.put("unitTimezone", "UTC");
        staticData.put("paginationPreviousSymbol", "\u2190");
        staticData.put("paginationNextSymbol", "\u2192");
        staticData.put("tripCanceledMessageGeneric", "This trip was canceled.");
        staticData.put("buttonUpcomingTrips", "Upcoming trips");
        staticData.put("buttonPastTrips", "Past trips");
        staticData.put("titleUpcomingTrips", "Upcoming trips");
        staticData.put("titlePastTrips", "Past trips");
        staticData.put("messageNoUpcomingTrips", "There are no upcoming trips at the moment.");
        staticData.put("messageNoPastTrips", "No past trips found.");
        return staticData;
    }

    public Map<String, Object> prepareTripsViewData(int upcomingPageParam, int pastPageParam, OidcUser principal) {
        Map<String, Object> viewData = new HashMap<>();
        boolean isUserManager = false;

        if (principal != null) {
            Map<String, Object> userClaims = principal.getClaims();
            viewData.put("user", userClaims);
            Object rolesClaimObj = userClaims.get("role");
            if (rolesClaimObj != null) {
                isUserManager = Arrays.stream(rolesClaimObj.toString().split("\\s*\\|\\s*"))
                        .anyMatch(role -> "Manager".equals(role.trim()));
            }
        }
        viewData.put("isManagerActual", isUserManager);

        Map<String, Object> upcomingTripsData = tripService.getPaginatedFilteredUpcomingTrips(upcomingPageParam, PAGE_SIZE);
        viewData.put("trips", upcomingTripsData.get("trips"));
        viewData.put("page", upcomingTripsData.get("currentPage"));
        viewData.put("totalPages", upcomingTripsData.get("totalPages"));

        if (isUserManager) {
            Map<String, Object> pastTripsData = tripService.getPaginatedFilteredPastTrips(pastPageParam, PAGE_SIZE);
            viewData.put("pagePastTrips", pastTripsData.get("trips"));
            viewData.put("pastPage", pastTripsData.get("currentPage"));
            viewData.put("totalPastPages", pastTripsData.get("totalPages"));
        } else {
            viewData.put("pagePastTrips", Collections.emptyList());
            viewData.put("pastPage", 0);
            viewData.put("totalPastPages", 0);
        }

        try {
            viewData.put("guides", auth0Service.getAllGuides());
        } catch (Exception e) {
            logger.error("Error fetching guides for trips page data", e);
            viewData.put("guides", Collections.emptyList());
            viewData.put("errorMessageGuides", "Could not load guide information.");
        }
        return viewData;
    }
}
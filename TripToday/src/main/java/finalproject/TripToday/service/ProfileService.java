package finalproject.TripToday.service;

import finalproject.TripToday.entity.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);
    private static final int PAGE_SIZE = 3;

    private final TripService tripService;
    private final Auth0Service auth0Service;

    @Autowired
    public ProfileService(TripService tripService, Auth0Service auth0Service) {
        this.tripService = tripService;
        this.auth0Service = auth0Service;
    }

    private Map<String, String> getPageTextData() {
        Map<String, String> pageText = new HashMap<>();
        pageText.put("title", "My profile | TripToday");
        pageText.put("profilePageMainTitle", "My profile");
        pageText.put("defaultPictureUrl", "/images/default_avatar.png");
        pageText.put("emailLabel", "Email:");
        pageText.put("roleLabel", "Account role(s):");
        pageText.put("noUserDetails", "User details not available.");
        pageText.put("guideDescriptionLabel", "Your description (as a guide):");
        pageText.put("guideDescriptionPlaceholder", "Enter your description...");
        pageText.put("guideDescriptionCharCounterSuffix", "/200");
        pageText.put("guideDescriptionSaveButton", "Save description");
        pageText.put("pictureUpdateLabel", "Update profile picture:");
        pageText.put("pictureUpdatePlaceholder", "Enter URL of picture here (e.g., https://...)");
        pageText.put("pictureUpdateButton", "Update picture");
        pageText.put("upcomingTripsButton", "Upcoming trips");
        pageText.put("pastTripsButton", "Past trips");
        pageText.put("upcomingTripsTitle", "My upcoming trips");
        pageText.put("pastTripsTitle", "My past trips");
        pageText.put("noUpcomingTrips", "No upcoming trips found.");
        pageText.put("noPastTrips", "No past trips found.");
        pageText.put("tripCanceledMessageUser", "This trip was canceled. The enrolling fee was transferred back to you.");
        pageText.put("tripCanceledMessageGeneric", "This trip was canceled.");
        pageText.put("departureLocationLabel", "Departure location");
        pageText.put("departureDateLabel", "Departure date");
        pageText.put("departureHourLabel", "Departure hour");
        pageText.put("returnDateLabel", "Return date");
        pageText.put("durationLabel", "Duration");
        pageText.put("availableSpotsLabel", "Available spots");
        pageText.put("remainingSpotsLabel", "Remaining spots");
        pageText.put("enrollmentFeeLabel", "Enrollment fee");
        pageText.put("feeLabel", "Enrollment fee");
        pageText.put("hotelLabel", "Hotel");
        pageText.put("guideLabel", "Guide");
        pageText.put("durationUnit", " days");
        pageText.put("spotsUnit", " spots");
        pageText.put("feeUnit", " RON");
        pageText.put("hourUnit", " UTC");
        pageText.put("hotelNotSpecified", "Not specified");
        pageText.put("guideYouUpcoming", "You are the guide for this trip");
        pageText.put("guideYouPast", "You were the guide for this trip");
        pageText.put("guideNotAvailable", "N/A");
        pageText.put("paginationPreviousSymbol", "\u2190");
        pageText.put("paginationNextSymbol", "\u2192");
        return pageText;
    }

    private Map<String, Object> getProfileDetails(OidcUser principal, Map<String, String> pageTextDefaults) {
        Map<String, Object> userDetails = new HashMap<>();
        String userId = principal.getName();

        userDetails.put("currentUserId", userId);
        userDetails.put("userEmail", principal.getEmail());

        String rawUserPicture = principal.getPicture();
        userDetails.put("displayUserPicture", rawUserPicture != null && !rawUserPicture.isEmpty() ?
                rawUserPicture : pageTextDefaults.get("defaultPictureUrl"));


        Map<String, Object> claims = principal.getClaims();
        Object rolesClaim = claims.get("role");
        List<String> userDisplayRoles = new ArrayList<>();
        if (rolesClaim instanceof String) {
            userDisplayRoles.addAll(List.of(((String) rolesClaim).split("\\s*\\|\\s*")));
        } else if (rolesClaim instanceof List) {
            for (Object roleObj : (List<?>) rolesClaim) {
                if (roleObj instanceof String) {
                    userDisplayRoles.add((String) roleObj);
                }
            }
        }
        userDetails.put("userDisplayRoles", userDisplayRoles);
        boolean isGuide = userDisplayRoles.contains("Guide");
        userDetails.put("isGuide", isGuide);
        userDetails.put("isAuth0User", userId != null && userId.startsWith("auth0|"));

        try {
            userDetails.put("description", auth0Service.getGuideDescription(userId));
        } catch (Exception e) {
            logger.error("Error fetching guide description for user {}", userId, e);
            userDetails.put("description", "");
            userDetails.put("errorMessageDescription", "Could not load guide description.");
        }
        return userDetails;
    }

    private Map<String, Object> getPaginatedTripsForProfile(String userId, boolean isGuide, int pageParam, String tripType, Map<String, String> allGuideEmailsMap) {
        Map<String, Object> paginatedTripsData = new HashMap<>();
        List<Trip> finalTrips;

        try {
            boolean fetchUpcoming = "upcoming".equalsIgnoreCase(tripType);

            Map<String, List<Trip>> enrolledSplitTrips = tripService.splitUserTripsByDate(userId);
            List<Trip> enrolledTrips = fetchUpcoming ?
                    enrolledSplitTrips.getOrDefault("upcomingTrips", Collections.emptyList()) :
                    enrolledSplitTrips.getOrDefault("pastTrips", Collections.emptyList());

            Set<Trip> tripsSet = new HashSet<>(enrolledTrips);

            if (isGuide) {
                List<Trip> systemTrips = fetchUpcoming ? tripService.getUpcomingTrips() : tripService.getPastTrips();
                for (Trip trip : systemTrips) {
                    if (userId.equals(String.valueOf(trip.getGuideId()))) {
                        tripsSet.add(trip);
                    }
                }
            }

            finalTrips = new ArrayList<>(tripsSet);
            if (fetchUpcoming) {
                finalTrips.sort(Comparator.comparing(Trip::getDepartureDate));
            } else {
                finalTrips.sort(Comparator.comparing(Trip::getDepartureDate).reversed());
            }

            int currentPage = pageParam;
            int totalTrips = finalTrips.size();
            int totalPages = (int) Math.ceil((double) totalTrips / PAGE_SIZE);
            if (totalPages == 0) totalPages = 1;
            if (currentPage < 0) currentPage = 0;
            else if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);

            int fromIndex = currentPage * PAGE_SIZE;
            int toIndex = Math.min(fromIndex + PAGE_SIZE, totalTrips);
            List<Trip> pageTrips = (fromIndex <= toIndex && fromIndex < totalTrips && totalTrips > 0) ?
                    finalTrips.subList(fromIndex, toIndex) : Collections.emptyList();

            List<String> tripGuideEmailsOnPage = pageTrips.stream()
                    .map(trip -> trip.getGuideId() != null ? allGuideEmailsMap.get(String.valueOf(trip.getGuideId())) : null)
                    .collect(Collectors.toList());

            paginatedTripsData.put("page" + capitalize(tripType) + "Trips", pageTrips);
            paginatedTripsData.put(tripType + "Page", currentPage);
            paginatedTripsData.put("total" + capitalize(tripType) + "Pages", totalPages);
            paginatedTripsData.put(tripType + "TripGuideEmailsOnPage", tripGuideEmailsOnPage);

        } catch (Exception e) {
            logger.error("Error fetching {} trips for user {}: {}", tripType, userId, e.getMessage(), e);
            paginatedTripsData.put("page" + capitalize(tripType) + "Trips", Collections.emptyList());
            paginatedTripsData.put(tripType + "Page", 0);
            paginatedTripsData.put("total" + capitalize(tripType) + "Pages", 1);
            paginatedTripsData.put(tripType + "TripGuideEmailsOnPage", Collections.emptyList());
            paginatedTripsData.put("errorMessage" + capitalize(tripType) + "Trips", "Could not load " + tripType + " trips.");
        }
        return paginatedTripsData;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }


    public Map<String, String> getAllGuideEmailsMap() {
        try {
            return auth0Service.getAllGuides().stream()
                    .filter(g -> g != null && g.get("id") != null && g.get("email") != null)
                    .collect(Collectors.toMap(g -> g.get("id"), g -> g.get("email"), (e1, e2) -> e1));
        } catch (Exception e) {
            logger.error("Error fetching all guide emails map", e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> prepareProfilePageData(OidcUser principal, int upcomingPageParam, int pastPageParam) {
        Map<String, Object> modelData = new HashMap<>();
        StringBuilder overallErrorMessage = new StringBuilder();

        Map<String, String> pageText = getPageTextData();
        modelData.put("pageText", pageText);

        Map<String, Object> userDetails = getProfileDetails(principal, pageText);
        modelData.putAll(userDetails);
        if (userDetails.containsKey("errorMessageDescription")) {
            overallErrorMessage.append(userDetails.get("errorMessageDescription")).append(" ");
        }

        String userId = principal.getName();
        boolean isGuide = (Boolean) userDetails.getOrDefault("isGuide", false);
        Map<String, String> allGuideEmailsMap = getAllGuideEmailsMap();


        Map<String, Object> upcomingTripsData = getPaginatedTripsForProfile(userId, isGuide, upcomingPageParam, "upcoming", allGuideEmailsMap);
        modelData.putAll(upcomingTripsData);
        if (upcomingTripsData.containsKey("errorMessageUpcomingTrips")) {
            overallErrorMessage.append(upcomingTripsData.get("errorMessageUpcomingTrips")).append(" ");
        }

        Map<String, Object> pastTripsData = getPaginatedTripsForProfile(userId, isGuide, pastPageParam, "past", allGuideEmailsMap);
        modelData.putAll(pastTripsData);
        if (pastTripsData.containsKey("errorMessagePastTrips")) {
            overallErrorMessage.append(pastTripsData.get("errorMessagePastTrips")).append(" ");
        }

        if (!overallErrorMessage.isEmpty()) {
            modelData.put("errorMessage", overallErrorMessage.toString().trim());
        }

        return modelData;
    }


    public void updateUserDescription(String userId, String description) throws Exception {
        auth0Service.updateGuideDescription(userId, description);
    }

    public void updateUserPicture(String userId, String imageUrl) throws Exception {
        auth0Service.updatePicture(userId, imageUrl);
    }
}
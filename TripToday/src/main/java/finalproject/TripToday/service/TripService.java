package finalproject.TripToday.service;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.entity.UserTrip;
import finalproject.TripToday.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service // Marks as Spring service
public class TripService {

    // Repository for Trip
    private final TripRepository tripRepository;
    // UserTrip service dependency
    private final UserTripService userTripService;


    // Constructor with dependencies
    public TripService(TripRepository tripRepository, UserTripService userTripService) {
        // Initialize repositories/services
        this.tripRepository = tripRepository;
        this.userTripService = userTripService;
    }

    // Creates a new trip
    public Trip createTrip(Trip trip) {
        // Save trip to DB
        return tripRepository.save(trip);
    }

    // Retrieves trip by ID
    public Optional<Trip> getTripById(Integer id) {
        // Find trip in DB
        return tripRepository.findById(id);
    }

    // Gets all trips for user
    public List<Optional<Trip>> getUserTripsByUserId(String userId) {
        // Fetch user's trip records
        List<UserTrip> userTrips = userTripService.getAllUserTripsByUserId(userId);
        // Init list for results
        List<Optional<Trip>> trips = new ArrayList<>();

        // Iterate through user's trips
        for (UserTrip userTrip : userTrips) {
            int currentUserTripId = userTrip.getTripId(); // Get trip ID
            // Fetch full trip details
            Optional<Trip> trip = getTripById(currentUserTripId);
            trips.add(trip); // Add to list
        }
        return trips; // Return found trips
    }

    // Splits user trips by date
    public Map<String, List<Trip>> splitUserTripsByDate(String userId) {
        // Get user's trip records
        List<UserTrip> userTrips = userTripService.getAllUserTripsByUserId(userId);

        // Init lists for sorting
        List<Trip> pastTrips = new ArrayList<>();
        List<Trip> upcomingTrips = new ArrayList<>();
        LocalDate today = LocalDate.now(); // Get current date

        // Iterate over user's trips
        for (UserTrip userTrip : userTrips) {
            // Get full trip details
            Optional<Trip> tripOptional = getTripById(userTrip.getTripId());
            // If trip found
            tripOptional.ifPresent(trip -> {
                // Check departure against today
                if (trip.getDepartureDate().isBefore(today) || trip.getDepartureDate().isEqual(today)) {
                    pastTrips.add(trip); // Add to past
                } else {
                    upcomingTrips.add(trip); // Add to upcoming
                }
            });
        }

        // Prepare result map
        Map<String, List<Trip>> result = new HashMap<>();
        result.put("pastTrips", pastTrips);
        result.put("upcomingTrips", upcomingTrips);

        return result; // Return sorted trips
    }

    // Decrements available spots
    @Transactional // DB transaction
    public boolean decrementAvailableSpots(Integer tripId) {
        // Find trip by ID
        Optional<Trip> tripOpt = tripRepository.findById(tripId);

        // If trip exists
        if (tripOpt.isPresent()) {
            Trip trip = tripOpt.get();
            // If spots available
            if (trip.getAvailableSpots() > 0) {
                // Reduce spot count
                trip.setAvailableSpots(trip.getAvailableSpots() - 1);
                tripRepository.save(trip); // Save changes
                return true; // Spots decremented
            }
        }
        return false; // No spots or no trip
    }

    // Gets all trips
    public List<Trip> getAllTrips() {
        // Fetch all from DB
        return tripRepository.findAll();
    }
    // Gets upcoming trips
    public List<Trip> getUpcomingTrips() {
        // Fetch based on date
        return tripRepository.findUpcomingTrips(LocalDate.now());
    }


    // Updates an existing trip
    public Trip updateTrip(Integer id, Trip tripDetails) {
        // Find trip by ID
        Optional<Trip> optionalTrip = tripRepository.findById(id);
        // If trip found
        if (optionalTrip.isPresent()) {
            Trip trip = optionalTrip.get();
            // Set new trip details
            trip.setDestination(tripDetails.getDestination());
            trip.setDepartureLocation(tripDetails.getDepartureLocation());
            trip.setDepartureDate(tripDetails.getDepartureDate());
            trip.setReturnDate(tripDetails.getReturnDate());
            trip.setDurationDays(tripDetails.getDurationDays());
            trip.setAvailableSpots(tripDetails.getAvailableSpots());
            trip.setRegistrationFee(tripDetails.getRegistrationFee());
            trip.setGuideId(tripDetails.getGuideId());
            trip.setDescription(tripDetails.getDescription());
            trip.setPicture(tripDetails.getPicture());
            trip.setHotelName(tripDetails.getHotelName());
            trip.setDepartureHour(tripDetails.getDepartureHour());

            // Save updated trip
            return tripRepository.save(trip);
        }
        return null; // Trip not found
    }

    // Marks a trip as canceled
    @Transactional // DB transaction
    public boolean deleteTrip(Integer id) { // Note: "delete" means cancel
        // Find trip by ID
        Optional<Trip> optionalTrip = tripRepository.findById(id);
        // If trip found
        if (optionalTrip.isPresent()) {
            Trip trip = optionalTrip.get();
            // Set canceled to true
            trip.setCanceled(true);
            tripRepository.save(trip); // Save change
            return true; // Successfully canceled
        }
        return false; // Trip not found
    }
}
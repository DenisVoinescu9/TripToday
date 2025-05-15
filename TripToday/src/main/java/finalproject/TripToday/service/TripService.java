package finalproject.TripToday.service;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.entity.UserTrip;
import finalproject.TripToday.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final UserTripService userTripService;


    public TripService(TripRepository tripRepository, UserTripService userTripService) {
        this.tripRepository = tripRepository;
        this.userTripService = userTripService;
    }

    public Trip createTrip(Trip trip) {
        return tripRepository.save(trip);
    }

    public Optional<Trip> getTripById(Integer id) {
        return tripRepository.findById(id);
    }

    public Map<String, List<Trip>> splitUserTripsByDate(String userId) {
        List<UserTrip> userTrips = userTripService.getAllUserTripsByUserId(userId);

        List<Trip> pastTrips = new ArrayList<>();
        List<Trip> upcomingTrips = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (UserTrip userTrip : userTrips) {
            Optional<Trip> tripOptional = getTripById(userTrip.getTripId());
            tripOptional.ifPresent(trip -> {
                if (trip.getDepartureDate().isBefore(today) || trip.getDepartureDate().isEqual(today)) {
                    pastTrips.add(trip);
                } else {
                    upcomingTrips.add(trip);
                }
            });
        }

        Map<String, List<Trip>> result = new HashMap<>();
        result.put("pastTrips", pastTrips);
        result.put("upcomingTrips", upcomingTrips);

        return result;
    }


    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public List<Trip> getUpcomingTrips() {
        return tripRepository.findUpcomingTrips(LocalDate.now());
    }


    public Trip updateTrip(Integer id, Trip tripDetails) {
        Optional<Trip> optionalTrip = tripRepository.findById(id);
        if (optionalTrip.isPresent()) {
            Trip trip = optionalTrip.get();
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

            return tripRepository.save(trip);
        }
        return null;
    }

    public List<Trip> getPastTrips() {
        return tripRepository.findPastTrips(LocalDate.now());
    }

    @Transactional
    public boolean deleteTrip(Integer id) {
        Optional<Trip> optionalTrip = tripRepository.findById(id);
        if (optionalTrip.isPresent()) {
            Trip trip = optionalTrip.get();
            trip.setCanceled(true);
            tripRepository.save(trip);
            return true;
        }
        return false;
    }

    public Map<String, Object> getPaginatedFilteredUpcomingTrips(int pageParam, int pageSize) {
        List<Trip> allUpcomingTripsFiltered = getUpcomingTrips().stream()
                .filter(trip -> trip.getCanceled() != null && !trip.getCanceled())
                .collect(Collectors.toList());
        return getPaginatedTrips(allUpcomingTripsFiltered, pageParam, pageSize);
    }

    public Map<String, Object> getPaginatedFilteredPastTrips(int pageParam, int pageSize) {
        List<Trip> allPastTripsFiltered = getPastTrips().stream()
                .filter(trip -> trip.getCanceled() != null && !trip.getCanceled())
                .collect(Collectors.toList());
        return getPaginatedTrips(allPastTripsFiltered, pageParam, pageSize);
    }

    private Map<String, Object> getPaginatedTrips(List<Trip> trips, int pageParam, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        int totalPages = (int) Math.ceil((double) trips.size() / pageSize);
        int currentPage = pageParam;
        if (currentPage < 0) currentPage = 0;
        if (currentPage >= totalPages) {
            currentPage = totalPages > 0 ? totalPages - 1 : 0;
        }

        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, trips.size());
        List<Trip> pageTrips = trips.isEmpty() ? Collections.emptyList() : trips.subList(fromIndex, toIndex);

        result.put("trips", pageTrips);
        result.put("currentPage", currentPage);
        result.put("totalPages", totalPages);
        return result;
    }

    @Transactional
    public String enrollUserInTrip(String userId, Integer tripId) {
        Optional<Trip> optionalTrip = getTripById(tripId);
        if (optionalTrip.isEmpty()) {
            return "Trip not found.";
        }
        Trip trip = optionalTrip.get();
        if (trip.getCanceled() != null && trip.getCanceled()) {
            return "This trip has been canceled.";
        }
        if (trip.getAvailableSpots() <= 0) {
            return "Sorry, this trip is full.";
        }
        String guideIdAsString = (trip.getGuideId() != null) ? String.valueOf(trip.getGuideId()) : null;
        if (userId.equals(guideIdAsString)) {
            return "You cannot enroll in a trip you are guiding.";
        }
        if (userTripService.isUserEnrolled(userId, tripId)) {
            return "You are already enrolled in this trip.";
        }

        UserTrip userTrip = new UserTrip();
        userTrip.setUserId(userId);
        userTrip.setTripId(tripId);
        userTripService.createUserTrip(userTrip);

        trip.setAvailableSpots(trip.getAvailableSpots() - 1);
        tripRepository.save(trip);

        return "SUCCESS";
    }
}
package finalproject.TripToday.service;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.entity.UserTrip;
import finalproject.TripToday.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

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

    @Transactional // DB transaction
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
}
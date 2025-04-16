package finalproject.TripToday.service;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TripService {

    private final TripRepository tripRepository;


    public TripService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }


    public Trip createTrip(Trip trip) {
        return tripRepository.save(trip);
    }


    public Optional<Trip> getTripById(Integer id) {
        return tripRepository.findById(id);
    }

    @Transactional
    public boolean decrementAvailableSpots(Integer tripId) {
        Optional<Trip> tripOpt = tripRepository.findById(tripId);

        if (tripOpt.isPresent()) {
            Trip trip = tripOpt.get();
            if (trip.getAvailableSpots() > 0) {
                trip.setAvailableSpots(trip.getAvailableSpots() - 1);  // Decrease available spots by 1
                tripRepository.save(trip);  // Save the updated trip back to the database
                return true;
            }
        }
        return false;
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
            trip.setDepartureDate(tripDetails.getDepartureDate());
            return tripRepository.save(trip);
        }
        return null;
    }


    public boolean deleteTrip(Integer id) {
        Optional<Trip> optionalTrip = tripRepository.findById(id);
        if (optionalTrip.isPresent()) {
            tripRepository.deleteById(id);
            return true;
        }
        return false;
    }
}

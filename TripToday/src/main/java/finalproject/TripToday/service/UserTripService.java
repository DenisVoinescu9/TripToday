package finalproject.TripToday.service;

import finalproject.TripToday.entity.UserTrip;
import finalproject.TripToday.repository.UserTripRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserTripService {

    private final UserTripRepository userTripRepository;

    public UserTripService(UserTripRepository userTripRepository) {
        this.userTripRepository = userTripRepository;
    }

    public UserTrip createUserTrip(UserTrip userTrip) {
        return userTripRepository.save(userTrip);
    }

    public Optional<UserTrip> getUserTripById(Integer id) {
        return userTripRepository.findById(id);
    }

    public List<UserTrip> getAllUserTrips() {
        return userTripRepository.findAll();
    }

    public UserTrip updateUserTrip(Integer id, UserTrip userTripDetails) {
        Optional<UserTrip> optionalUserTrip = userTripRepository.findById(id);
        if (optionalUserTrip.isPresent()) {
            UserTrip userTrip = optionalUserTrip.get();
            userTrip.setUserId(userTripDetails.getUserId());
            userTrip.setTripId(userTripDetails.getTripId());
            return userTripRepository.save(userTrip);
        }
        return null;
    }

    public boolean deleteUserTrip(Integer id) {
        Optional<UserTrip> optionalUserTrip = userTripRepository.findById(id);
        if (optionalUserTrip.isPresent()) {
            userTripRepository.deleteById(id);
            return true;
        }
        return false;
    }
}

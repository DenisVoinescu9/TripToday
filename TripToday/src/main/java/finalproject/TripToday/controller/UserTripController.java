package finalproject.TripToday.controller;

import finalproject.TripToday.entity.UserTrip;
import finalproject.TripToday.service.UserTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/user-trips")
public class UserTripController {

    private final UserTripService userTripService;

    @Autowired
    public UserTripController(UserTripService userTripService) {
        this.userTripService = userTripService;
    }

    @PostMapping
    public ResponseEntity<UserTrip> createUserTrip(@RequestBody UserTrip userTrip) {
        UserTrip createdUserTrip = userTripService.createUserTrip(userTrip);
        return new ResponseEntity<>(createdUserTrip, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<UserTrip>> getAllUserTrips() {
        List<UserTrip> userTrips = userTripService.getAllUserTrips();
        return new ResponseEntity<>(userTrips, HttpStatus.OK);
    }

    @GetMapping("/trip/{id}/details")
    public ResponseEntity<List<Map<String, Object>>> getTravelerDetailsByTripId(@PathVariable Integer id) {
        List<Map<String, Object>> travelerDetails = userTripService.getTravelerDetailsForTrip(id);
        return new ResponseEntity<>(travelerDetails, HttpStatus.OK);
    }

    @GetMapping("/trip/{id}")
    public ResponseEntity<List<UserTrip>> getAllUserTripsByTripId(@PathVariable Integer id) {
        List<UserTrip> userTrips = userTripService.getAllUserTripsByTripId(id);
        return new ResponseEntity<>(userTrips, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserTrip> getUserTripById(@PathVariable Integer id) {
        Optional<UserTrip> userTrip = userTripService.getUserTripById(id);
        return userTrip.map(value -> new ResponseEntity<>(value, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserTrip> updateUserTrip(@PathVariable Integer id, @RequestBody UserTrip userTripDetails) {
        UserTrip updatedUserTrip = userTripService.updateUserTrip(id, userTripDetails);
        if (updatedUserTrip != null) {
            return new ResponseEntity<>(updatedUserTrip, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserTrip(@PathVariable Integer id) {
        boolean deleted = userTripService.deleteUserTrip(id);
        if (deleted) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
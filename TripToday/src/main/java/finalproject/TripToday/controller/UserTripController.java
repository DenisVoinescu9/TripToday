package finalproject.TripToday.controller;

import finalproject.TripToday.entity.Trip;
import finalproject.TripToday.entity.UserTrip;
import finalproject.TripToday.service.TripService;
import finalproject.TripToday.service.UserTripService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/user-trips")
public class UserTripController {

    private final UserTripService userTripService;
    private final TripService tripService;

    public UserTripController(UserTripService userTripService, TripService tripService) {
        this.userTripService = userTripService;
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<UserTrip> createUserTrip(@RequestBody UserTrip userTrip) {
        UserTrip createdUserTrip = userTripService.createUserTrip(userTrip);
        return ResponseEntity.ok(createdUserTrip);
    }

    @GetMapping
    public ResponseEntity<List<UserTrip>> getAllUserTrips() {
        List<UserTrip> userTrips = userTripService.getAllUserTrips();
        return ResponseEntity.ok(userTrips);
    }



    @GetMapping("/{id}")
    public ResponseEntity<UserTrip> getUserTripById(@PathVariable Integer id) {
        Optional<UserTrip> userTrip = userTripService.getUserTripById(id);
        return userTrip.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserTrip> updateUserTrip(@PathVariable Integer id, @RequestBody UserTrip userTripDetails) {
        UserTrip updatedUserTrip = userTripService.updateUserTrip(id, userTripDetails);
        if (updatedUserTrip != null) {
            return ResponseEntity.ok(updatedUserTrip);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserTrip(@PathVariable Integer id) {
        boolean deleted = userTripService.deleteUserTrip(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

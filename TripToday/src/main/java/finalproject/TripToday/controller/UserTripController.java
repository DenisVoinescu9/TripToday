package finalproject.TripToday.controller;

import finalproject.TripToday.entity.UserTrip;
import finalproject.TripToday.service.Auth0Service; // Import Auth0Service
import finalproject.TripToday.service.UserTripService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map; // Import Map
import java.util.stream.Collectors; // Import Collectors

@RestController
@RequestMapping("/api/v2/user-trips")
public class UserTripController {

    private final UserTripService userTripService;
    private final Auth0Service auth0Service;

     public UserTripController(UserTripService userTripService, Auth0Service auth0Service) {
        this.userTripService = userTripService;
        this.auth0Service = auth0Service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserTrip createUserTrip(@RequestBody UserTrip userTrip) {
        return userTripService.createUserTrip(userTrip);
    }

    @GetMapping
    public List<UserTrip> getAllUserTrips() {
         return userTripService.getAllUserTrips();
    }

    @GetMapping("/trip/{id}/details")
    public List<Map<String, Object>> getTravelerDetailsByTripId(@PathVariable Integer id) {
        List<UserTrip> userTrips = userTripService.getAllUserTripsByTripId(id);


        return userTrips.stream()
                .map(UserTrip::getUserId)
                .filter(userId -> userId != null && !userId.trim().isEmpty())
                .map(auth0Service::getUserDetails)
                .collect(Collectors.toList());
    }

    @GetMapping("/trip/{id}")
    public List<UserTrip> getAllUserTripsByTripId(@PathVariable Integer id) {
        return userTripService.getAllUserTripsByTripId(id);
    }


    @GetMapping("/{id}")
    public UserTrip getUserTripById(@PathVariable Integer id) {
        return userTripService.getUserTripById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UserTrip not found with id: " + id));
    }

    @PutMapping("/{id}")
    public UserTrip updateUserTrip(@PathVariable Integer id, @RequestBody UserTrip userTripDetails) {
        UserTrip updatedUserTrip = userTripService.updateUserTrip(id, userTripDetails);
        if (updatedUserTrip == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "UserTrip not found with id: " + id + " for update");
        }
        return updatedUserTrip;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserTrip(@PathVariable Integer id) {
        boolean deleted = userTripService.deleteUserTrip(id);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "UserTrip not found with id: " + id + " for deletion");
        }
    }
}
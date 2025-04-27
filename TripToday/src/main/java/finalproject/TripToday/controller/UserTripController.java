package finalproject.TripToday.controller;

// import finalproject.TripToday.entity.Trip; // Unused import
import finalproject.TripToday.entity.UserTrip;
// import finalproject.TripToday.service.TripService; // Unused import
import finalproject.TripToday.service.UserTripService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// import java.util.ArrayList; // Unused import
import java.util.List;
// import java.util.Optional; // No longer directly needed here

@RestController
@RequestMapping("/api/v2/user-trips")
public class UserTripController {

    private final UserTripService userTripService;
    // private final TripService tripService; // Not used in provided methods

    public UserTripController(UserTripService userTripService /*, TripService tripService */) {
        this.userTripService = userTripService;
        // this.tripService = tripService;
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

    @GetMapping("/trip/{id}")
    public List<UserTrip> getAllUserTripsByTripId(@PathVariable Integer id) {
        // Service now returns List directly or empty list
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
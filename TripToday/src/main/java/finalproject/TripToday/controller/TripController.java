package finalproject.TripToday.controller;

import finalproject.TripToday.entity.Trip;       // Trip data entity
import finalproject.TripToday.service.TripService;   // Handles trip logic
import org.springframework.beans.factory.annotation.Autowired; // For dependency injection
import org.springframework.http.HttpStatus;         // HTTP status codes
import org.springframework.http.ResponseEntity;     // HTTP response wrapper
import org.springframework.stereotype.Controller;     // Marks as controller
import org.springframework.web.bind.annotation.*;   // For request mapping

import java.util.List;                            // For lists
import java.util.Optional;                        // For optional results

@Controller
@RequestMapping("/api/v2/trips")
public class TripController {

     private final TripService tripService;

    @Autowired
    public TripController(TripService tripService) {
         this.tripService = tripService;
    }

     @PostMapping
    public ResponseEntity<Trip> createTrip(@RequestBody Trip trip) {
         Trip createdTrip = tripService.createTrip(trip);
         return new ResponseEntity<>(createdTrip, HttpStatus.CREATED);
    }

     @GetMapping
    public ResponseEntity<List<Trip>> getAllTrips() {
         List<Trip> trips = tripService.getAllTrips();
         return new ResponseEntity<>(trips, HttpStatus.OK);
    }

     @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripById(@PathVariable Integer id) {
         Optional<Trip> trip = tripService.getTripById(id);
         return trip.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

     @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable Integer id, @RequestBody Trip tripDetails) {
         Trip updatedTrip = tripService.updateTrip(id, tripDetails);
         if (updatedTrip != null) {
             return new ResponseEntity<>(updatedTrip, HttpStatus.OK);
        } else {
             return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

     @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Integer id) {
         boolean success = tripService.deleteTrip(id);
         if (success) {
             return new ResponseEntity<>(HttpStatus.OK);
        } else {
             return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
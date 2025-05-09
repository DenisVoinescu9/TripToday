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

@Controller                                       // Spring MVC Controller
@RequestMapping("/api/v2/trips")                // Base API path
public class TripController {

    // Declare TripService
    private final TripService tripService;

    @Autowired                                    // Inject TripService
    public TripController(TripService tripService) {
        // Initialize TripService
        this.tripService = tripService;
    }

    // Create a new trip
    @PostMapping
    public ResponseEntity<Trip> createTrip(@RequestBody Trip trip) { // Trip from request body
        // Call service to create
        Trip createdTrip = tripService.createTrip(trip);
        // Return created, 201 status
        return new ResponseEntity<>(createdTrip, HttpStatus.CREATED);
    }

    // Get all trips
    @GetMapping
    public ResponseEntity<List<Trip>> getAllTrips() {
        // Call service for all
        List<Trip> trips = tripService.getAllTrips();
        // Return list, 200 status
        return new ResponseEntity<>(trips, HttpStatus.OK);
    }

    // Get trip by ID
    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripById(@PathVariable Integer id) { // ID from path
        // Call service by ID
        Optional<Trip> trip = tripService.getTripById(id);
        // Map result to response
        return trip.map(value -> new ResponseEntity<>(value, HttpStatus.OK)) // Found: return trip, 200
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));   // Else: return 404
    }

    // Update existing trip
    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable Integer id, @RequestBody Trip tripDetails) { // ID and trip data
        // Call service to update
        Trip updatedTrip = tripService.updateTrip(id, tripDetails);
        // Check update result
        if (updatedTrip != null) {
            // Success: return updated, 200
            return new ResponseEntity<>(updatedTrip, HttpStatus.OK);
        } else {
            // Fail: return 404
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Delete trip by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Integer id) { // ID from path
        // Call service to delete
        boolean success = tripService.deleteTrip(id);
        // Check delete result
        if (success) {
            // Success: return 200 (OK)
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            // Fail: return 404
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
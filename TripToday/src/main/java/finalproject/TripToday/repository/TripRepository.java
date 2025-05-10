package finalproject.TripToday.repository;

import finalproject.TripToday.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer> {
    @Query("SELECT t FROM Trip t WHERE t.departureDate >= :currentDate ORDER BY t.departureDate ASC")
    List<Trip> findUpcomingTrips(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT t FROM Trip t WHERE t.departureDate < :currentDate ORDER BY t.departureDate DESC")
    List<Trip> findPastTrips(@Param("currentDate") LocalDate currentDate);
}
package finalproject.TripToday.repository;

import finalproject.TripToday.entity.UserTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTripRepository extends JpaRepository<UserTrip, Integer> {
}

package finalproject.TripToday.repository;

import finalproject.TripToday.entity.UserTrip;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTripRepository extends JpaRepository<UserTrip, Integer> {
    Optional<List<UserTrip>> findAllByUserId(@Size(max = 255) @NotNull String userId);
    Optional<List<UserTrip>> findAllByTripId(@Size(max = 255) @NotNull Integer tripId);
    boolean existsByUserIdAndTripId(String userId, Integer tripId);

}

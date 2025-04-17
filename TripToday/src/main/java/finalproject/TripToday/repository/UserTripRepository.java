package finalproject.TripToday.repository;

import finalproject.TripToday.entity.UserTrip;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserTripRepository extends JpaRepository<UserTrip, Integer> {
    List<UserTrip> findAllByUserId(@Size(max = 255) @NotNull String userId);
}

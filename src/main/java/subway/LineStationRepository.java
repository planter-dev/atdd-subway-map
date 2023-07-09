package subway;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LineStationRepository extends JpaRepository<LineStation, Long> {

    LineStation findFirstByLineIdOrderBySequence(Long id);
    LineStation findFirstByLineIdOrderBySequenceDesc(Long id);

}

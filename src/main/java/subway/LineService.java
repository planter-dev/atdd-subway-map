package subway;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LineService {
    private LineRepository lineRepository;
    private LineStationRepository lineStationRepository;
    private StationRepository stationRepository;

    public LineService(LineRepository lineRepository, LineStationRepository lineStationRepository, StationRepository stationRepository) {
        this.lineRepository = lineRepository;
        this.lineStationRepository = lineStationRepository;
        this.stationRepository = stationRepository;
    }

    @Transactional
    public LineResponse createLine(LineRequest request) {
        Line line = lineRepository.save(new Line(request.getName(), request.getColor(), request.getDistance()));
        Station upStation = stationRepository.findById(request.getUpStationId()).orElseThrow(() -> new NoSuchFieldError("해당 지하철역이 없습니다."));
        Station downStation = stationRepository.findById(request.getDownStationId()).orElseThrow(() -> new NoSuchFieldError("해당 지하철역이 없습니다."));
        lineStationRepository.save(new LineStation(0, line, upStation));
        lineStationRepository.save(new LineStation(request.getDistance() - 1, line, downStation));
        return createLineResponse(line, upStation, downStation);
    }

    private LineResponse createLineResponse(Line line, Station upStation , Station downStation) {
        return new LineResponse(line.getId(),
                line.getName(),
                line.getColor(),
                List.of(new StationResponse(upStation.getId(), upStation.getName()), new StationResponse(downStation.getId(), downStation.getName())));
    }

    public List<LineResponse> findLines() {
        List<Line> lines = lineRepository.findAll();
        return lines.stream().map(this::createLineResponse).collect(Collectors.toList());
    }

    public LineResponse findLine(Long id) {
        Line line = lineRepository.findById(id).orElseThrow(() -> new RuntimeException("해당 아이디의 지하철 노선이 없습니다."));
        return createLineResponse(line);
    }

    private StationResponse createStationResponse(LineStation lineStation) {
        return new StationResponse(lineStation.getStation().getId(), lineStation.getStation().getName());
    }

    private LineResponse createLineResponse(Line line) {
        LineStation up = lineStationRepository.findFirstByLineIdOrderBySequence(line.getId());
        LineStation down = lineStationRepository.findFirstByLineIdOrderBySequenceDesc(line.getId());
        return new LineResponse(line.getId(), line.getName(), line.getColor(), List.of(createStationResponse(up), createStationResponse(down)));
    }

    public void updateLine(Long id, LineRequest request) {
        Line line = lineRepository.findById(id).orElseThrow(() -> new RuntimeException("해당 아이디의 지하철 노선이 없습니다."));
        line.updateLine(request.getName(), request.getColor());
    }

    public void deleteLine(Long id) {
        lineRepository.deleteById(id);
    }
}

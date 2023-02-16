package subway.line.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import subway.line.business.service.LineService;
import subway.line.web.dto.LineRequest;
import subway.line.web.dto.LineResponse;
import subway.line.web.dto.LineUpdateRequest;
import subway.line.web.dto.SectionRequest;
import subway.line.web.dto.SectionResponse;

import java.net.URI;
import java.util.List;

@RequestMapping("lines")
@RestController
@RequiredArgsConstructor
public class LineController {

    private final LineService lineService;

    @PostMapping("")
    public ResponseEntity<LineResponse> createLine(@RequestBody LineRequest request) {
        LineResponse response = lineService.create(request);
        return ResponseEntity.created(URI.create("/lines/"+response.getId())).body(response);
    }

    @GetMapping("")
    public ResponseEntity<List<LineResponse>> getAllLines() {
        List<LineResponse> response = lineService.getAllLines();
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LineResponse> getLine(@PathVariable Long id) {
        LineResponse response = lineService.getLineResponse(id);
        return ResponseEntity.ok().body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateLine(@PathVariable Long id, @RequestBody LineUpdateRequest request) {
        lineService.modify(id, request.getName(), request.getColor());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<LineResponse> deleteLine(@PathVariable Long id) {
        lineService.remove(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/sections")
    public ResponseEntity<SectionResponse> getSections(@PathVariable Long id) {
        SectionResponse response = lineService.getSections(id);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/{id}/sections")
    public ResponseEntity<Void> addSection(@PathVariable Long id, @RequestBody SectionRequest request) {
        lineService.addSection(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/sections")
    public ResponseEntity<Void> removeSection(@PathVariable Long id, @RequestParam Long stationId) {
        lineService.removeSection(id, stationId);
        return ResponseEntity.noContent().build();
    }

}
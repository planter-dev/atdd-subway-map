package subway.line;

import io.restassured.http.Method;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import subway.RestTestUtils;
import subway.line.business.constant.SectionExceptionMessage;
import subway.line.web.dto.LineRequest;
import subway.line.web.dto.SectionRequest;
import subway.station.web.StationResponse;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

@DisplayName("지하철 구간 관련 기능")
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SectionAcceptanceTest {

    private LineRequest lineRequest;

    @PostConstruct
    void init() {
        createStations();
        this.lineRequest = LineRequest.builder()
                .name("신분당선")
                .color("bg-red-600")
                .upStationId(1L)
                .downStationId(2L)
                .distance(10)
                .build();
    }

    /**
     * Given : 지하철 노선을 등록하고
     * When : 해당 노선에 새로운 구간을 추가하면
     *   - 새로운 구간의 상행역은 해당 노선에 등록되어있는 하행 종점역이다.
     *   - 새로운 구간의 하행역은 해당 노선에 등록되어있지 않은 역이다.
     * Then : 해당 노선에 새로운 구간이 추가된다
     */
    @DisplayName("지하철 구간 등록 - 정상 케이스")
    @Test
    public void createSection() {
        // given
        Long lineId = createLine();
        List<StationResponse> beforeStations = RestTestUtils.getListFromResponse(getLine(lineId), "stations", StationResponse.class);

        // when
        Long upStationId = lineRequest.getDownStationId();
        Long downStationId = 3L;
        createSection(lineId, downStationId, upStationId, 10);

        // then
        List<StationResponse> afterStations = RestTestUtils.getListFromResponse(getLine(lineId), "stations", StationResponse.class);
        assertThat(afterStations.size()).isGreaterThan(beforeStations.size());
    }

    /**
     * Given : 지하철 노선을 등록하고
     * When : 해당 노선에 새로운 구간을 추가하면
     *   - 새로운 구간의 상행역은 해당 노선에 등록된 하행 종점역이 아니다.
     * Then : 새로운 구간이 추가되지 않는다.
     */
    @DisplayName("지하철 구간 등록 - 예외 케이스 : 새로운 구간의 상행역이 해당 노선에 등록되어있는 하행 종점역이 아닌 경우")
    @Test
    public void createSection_InvalidCase1() {
        // given
        Long lineId = createLine();

        // when
        Long upStationId = 1L;
        Long downStationId = 3L;

        assertThat(upStationId).isNotEqualTo(lineRequest.getDownStationId());

        var response = createSection(lineId, downStationId, upStationId, 10);

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.asString()).isEqualTo(SectionExceptionMessage.INVALID_UP_STATION)
        );
    }

    /**
     * Given : 지하철 노선을 등록하고
     * When : 해당 노선에 새로운 구간을 추가하면
     *   - 새로운 구간의 상행역은 해당 노선에 등록되어있는 하행 종점역이어야 한다.
     *   - 새로운 구간의 하행역은 해당 노선에 등록되어있는 역일 수 없다.
     * Then : 해당 노선에 새로운 구간이 추가된다
     *   - 새로운 구간 등록시 위 조건에 부합하지 않는 경우 에러 처리한다.
     */
    @DisplayName("지하철 구간 등록 - 예외 케이스 : 새로운 구간의 하행역이 해당 노선에 등록되어있는 역인 경우")
    @Test
    public void createSection_InvalidCase2() {
        // given
        Long lineId = createLine();

        // when
        Long upStationId = lineRequest.getDownStationId();
        Long downStationId = lineRequest.getUpStationId();

        var response = createSection(lineId, downStationId, upStationId, 10);

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.asString()).isEqualTo(SectionExceptionMessage.ALREADY_EXIST_DOWN_STATION)
        );
    }

    /**
     * Given : 구간이 두 개 이상인 지하철 노선을 등록하고
     * When : 하행 종점역을 제거하면
     * Then : 해당 종점역을 포함한 구간이 제거된다
     */
    @DisplayName("지하철 구간 제거 - 정상 케이스")
    @Test
    public void deleteSection() {
        // given
        Long lineId = createLine();
        Long upStationId = lineRequest.getDownStationId();
        Long downStationId = 3L;
        createSection(lineId, downStationId, upStationId, 10);

        List<StationResponse> beforeStations = RestTestUtils.getListFromResponse(getLine(lineId), "stations", StationResponse.class);
        Long beforeLastSectionId = RestTestUtils.getLongFromResponse(getSections(lineId), "lastSectionId");

        // when
        deleteSection(lineId, downStationId);

        // then
        Long afterLastSectionId = RestTestUtils.getLongFromResponse(getSections(lineId), "lastSectionId");
        assertThat(afterLastSectionId).isNotEqualTo(beforeLastSectionId);

        List<StationResponse> afterStations = RestTestUtils.getListFromResponse(getLine(lineId), "stations", StationResponse.class);
        assertThat(beforeStations.size()).isGreaterThan(afterStations.size());
        assertThat(afterStations.stream().map(StationResponse::getId).collect(Collectors.toList())).doesNotContain(downStationId);
    }

    /**
     * Given : 구간이 한 개인 지하철 노선을 등록하고
     * When : 하행 종점역을 제거하면
     * Then : 해당 구간이 제거되지 않는다.
     */
    @DisplayName("지하철 구간 제거 - 예외 케이스 : 구간이 한 개인 노선의 하행 종점역을 제거하려는 경우")
    @Test
    public void deleteSection_InvalidCase1() {
        // given
        Long lineId = createLine();

        // when
        var response = deleteSection(lineId, lineRequest.getDownStationId());

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.asString()).isEqualTo(SectionExceptionMessage.CANNOT_REMOVE_SECTION_WHEN_ONLY_ONE)
        );
    }

    /**
     * Given : 구간이 두 개 이상인 지하철 노선을 등록하고
     * When : 하행 종점역이 아닌 역을 제거하면
     * Then : 해당 구간이 제거되지 않는다.
     */
    @DisplayName("지하철 구간 제거 - 예외 케이스 : 하행 종점역이 아닌 역을 제거하려는 경우")
    @Test
    public void deleteSection_InvalidCase2() {
        // given
        Long lineId = createLine();
        Long upStationId = lineRequest.getDownStationId();
        Long downStationId = 3L;

        createSection(lineId, downStationId, upStationId, 10);

        // when
        var response = deleteSection(lineId, lineRequest.getUpStationId());

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.asString()).isEqualTo(SectionExceptionMessage.CANNOT_REMOVE_SECTION_WHEN_NOT_LAST_STATION)
        );
    }

    private void createStations() {
        List<String> names = List.of("강남역", "양재역", "판교역");
        for (String name : names) {
            RestTestUtils.request(Method.POST, "/stations", Map.of("name", name));
        }
    }

    private Long createLine() {
        var response = RestTestUtils.request(Method.POST, "/lines", lineRequest);
        return RestTestUtils.getLongFromResponse(response, "id");
    }

    private ExtractableResponse<Response> getLine(Long id) {
        return RestTestUtils.request(Method.GET, "/lines/"+id);
    }

    private ExtractableResponse<Response> getSections(Long id) {
        return RestTestUtils.request(Method.GET, "/lines/"+id+"/sections");
    }

    private ExtractableResponse<Response> createSection(Long lineId, Long downStationId, Long upStationId, int distance) {
        return RestTestUtils.request(Method.POST, "/lines/"+lineId+"/sections", new SectionRequest(downStationId, upStationId, distance));
    }

    private ExtractableResponse<Response> deleteSection(Long lineId, Long stationId) {
        return RestTestUtils.request(Method.DELETE, "/lines/"+lineId+"/sections?stationId="+stationId);
    }


}
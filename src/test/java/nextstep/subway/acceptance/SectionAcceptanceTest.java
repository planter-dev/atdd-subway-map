package nextstep.subway.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.acceptance.utils.LineAcceptanceTestUtils;
import nextstep.subway.acceptance.utils.StationAcceptanceTestUtils;
import nextstep.subway.applicaion.dto.LineRequest;
import nextstep.subway.applicaion.dto.LineResponse;
import nextstep.subway.applicaion.dto.SectionRequest;
import nextstep.subway.applicaion.dto.StationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static nextstep.subway.acceptance.LineAcceptanceTest.*;
import static nextstep.subway.acceptance.StationAcceptanceTest.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철 구간 관련 기능")
public class SectionAcceptanceTest extends BaseTest {

    private static final Long DISTANCE_STATION2_TO_STATION3 = 3L;

    private static final String EQUAL_CURRENT_DOWN_STATION_WITH_NEW_UP_STATION
            = "새로운 구간의 상행역은 해당 노선에 등록되어있는 하행 종점역과 같아야 합니다.";

    private static final String NOT_EXIST_NEW_DOWN_STATION
            = "새로운 구간의 하행역은 해당 노선에 등록되어있는 역일 수 없습니다.";

    private final StationAcceptanceTestUtils stationAcceptanceTestUtils = new StationAcceptanceTestUtils();

    private final LineAcceptanceTestUtils lineAcceptanceTestUtils = new LineAcceptanceTestUtils();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private LineRequest LINE_5;

    private StationResponse station1;

    private StationResponse station2;

    private StationResponse station3;

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        station1 = objectMapper.readValue(
                stationAcceptanceTestUtils.지하철_역_생성(STATION_NAME1).body().asString(), StationResponse.class);
        station2 = objectMapper.readValue(
                stationAcceptanceTestUtils.지하철_역_생성(STATION_NAME2).body().asString(), StationResponse.class);
        station3 = objectMapper.readValue(
                stationAcceptanceTestUtils.지하철_역_생성(STATION_NAME3).body().asString(), StationResponse.class);
        LINE_5 = new LineRequest(LINE_NAME_5, LINE_COLOR_5, station1.getId(), station2.getId(), LINE_DISTANCE_5);
    }

    @AfterEach
    public void initialize() {
        databaseInitializer.execute();
    }

    /**
     * Given 지하철 노선을 생성하고
     * When 상행역이 해당 노선에 등록되어있는 하행 종점역이고
     * When 하행역이 해당 노선에 등록되어있지 않은 새로운 구간을 등록한다면
     * Then 새로운 구간이 해당 노선에 등록된다.
     */
    @DisplayName("지하철 구간을 등록한다.")
    @Test
    void createSection() throws JsonProcessingException {
        // given
        Long lineId = lineAcceptanceTestUtils.지하철_노선_생성(LINE_5).jsonPath().getLong("id");

        // when
        SectionRequest request = SectionRequest.builder()
                .upStationId(station2.getId().toString())
                .downStationId(station3.getId().toString())
                .distance(DISTANCE_STATION2_TO_STATION3)
                .build();

        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post("/lines/" + lineId + "/sections")
                .then().log().all()
                .extract();

        // then
        ExtractableResponse<Response> line = RestAssured.given().log().all()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when().get("/lines/" + lineId)
                .then().log().all()
                .extract();

        LineResponse lineResponse = objectMapper.readValue(line.body().asString(), LineResponse.class);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(lineResponse.getStations()).contains(station1, station2, station3);
    }

    /**
     * Given 지하철 노선을 생성하고
     * WHEN 새로운 구간의 상행역이 해당 노선에 등록되어있는 하행 종점역이 아닌 구간을 등록할 경우
     * THEN Exception 을 발생시켜, 400 error code 와 실패 사유를 message 로 전달한다.
     */
    @DisplayName("새로운 구간의 상행역이 해당 노선에 등록되어있는 하행 종점역이 아닐 경우 Exception")
    @Test
    void notEqualCurrentDownStationIdWithNewUpStationId() {
        // given
        Long id = lineAcceptanceTestUtils.지하철_노선_생성(LINE_5).jsonPath().getLong("id");
        SectionRequest request = SectionRequest.builder()
                .upStationId(station3.getId().toString())
                .downStationId(station2.getId().toString())
                .distance(DISTANCE_STATION2_TO_STATION3)
                .build();

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when().post("/lines/" + id + "/sections")
                .then().log().all().extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo(EQUAL_CURRENT_DOWN_STATION_WITH_NEW_UP_STATION);
    }

    /**
     * Given 지하철 노선을 생성하고
     * WHEN 새로운 구간의 상행역이 해당 노선에 등록되어있는 하행 종점역이 아닌 구간을 등록할 경우
     * THEN Exception 을 발생시켜, 400 error code 와 실패 사유를 message 로 전달한다.
     */
    @DisplayName("새로운 구간의 하행역이 해당 노선에 존재할 경우 Exception")
    @Test
    void notExistDownStationIdOrThrow() {
        // given
        Long id = lineAcceptanceTestUtils.지하철_노선_생성(LINE_5).jsonPath().getLong("id");
        SectionRequest request = SectionRequest.builder()
                .upStationId(station2.getId().toString())
                .downStationId(station1.getId().toString())
                .distance(DISTANCE_STATION2_TO_STATION3)
                .build();

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when().post("/lines/" + id + "/sections")
                .then().log().all().extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("message")).isEqualTo(NOT_EXIST_NEW_DOWN_STATION);
    }
}

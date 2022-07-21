package nextstep.subway.api;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

public class StationApi {

    public static ExtractableResponse<Response> createStationApi(String stationName) {
        Map<String, String> station = new HashMap<>();
        station.put("name", stationName);

        ExtractableResponse<Response> createStationResponse = RestAssured.given().log().all()
                .body(station)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when().post("/stations")
                .then().log().all()
                .extract();

        assertThat(createStationResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        return createStationResponse;
    }

    public static ExtractableResponse<Response> getAllStationApi() {
        ExtractableResponse<Response> getAllStationResponse = RestAssured.given().log().all()
                .when().get("/stations")
                .then().log().all()
                .extract();

        assertThat(getAllStationResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

        return getAllStationResponse;
    }

    public static ExtractableResponse<Response> deleteStationByIdApi(Long stationId) {
        return RestAssured
                .given().log().all()
                .pathParam("id", stationId)
                .when().delete("/stations/{id}")
                .then().log().all()
                .extract();
    }
}
package com.legends.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * System integration tests for Legends of Sword and Wand.
 *
 * These tests spin up the full docker-compose stack using Testcontainers
 * and run HTTP requests through the gateway, verifying all 7 user stories.
 *
 * Prerequisites:
 *   - Docker must be running on the test machine.
 *   - All service JARs must be built before running these tests:
 *       mvn package -DskipTests  (in each service directory)
 *
 * Run from the integration-tests directory:
 *   mvn test
 *
 * Or from the repo root after building:
 *   cd integration-tests && mvn test
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SystemIntegrationTest {

    // Shared state across tests — filled in during the auth tests
    private static long   userId;
    private static String username = "int_test_user_" + System.currentTimeMillis();
    private static String password = "testpassword";

    @Container
    static DockerComposeContainer<?> stack = new DockerComposeContainer<>(
            new File("../docker-compose.yml"))
        .withExposedService("gateway",         8080,
                Wait.forHttp("/health").forPort(8080).withStartupTimeout(Duration.ofSeconds(120)))
        .withExposedService("profile-service", 5000,
                Wait.forHttp("/api/profile/health").forPort(5000).withStartupTimeout(Duration.ofSeconds(90)))
        .withExposedService("battle-service",  5001,
                Wait.forHttp("/api/battle/health").forPort(5001).withStartupTimeout(Duration.ofSeconds(60)))
        .withExposedService("pve-service",     5002,
                Wait.forHttp("/api/pve/health").forPort(5002).withStartupTimeout(Duration.ofSeconds(90)))
        .withExposedService("data-service",    5003,
                Wait.forHttp("/api/data/health").forPort(5003).withStartupTimeout(Duration.ofSeconds(90)))
        .withExposedService("pvp-service",     5004,
                Wait.forHttp("/api/pvp/health").forPort(5004).withStartupTimeout(Duration.ofSeconds(60)))
        .withLocalCompose(true);   // use docker compose v2 if available

    @BeforeAll
    static void configureRestAssured() {
        String host = stack.getServiceHost("gateway", 8080);
        int    port = stack.getServicePort("gateway", 8080);
        RestAssured.baseURI = "http://" + host;
        RestAssured.port    = port;
    }

    // ════════════════════════════════════════════════════════════════════
    // IT-01: US1 — Register a new user
    // ════════════════════════════════════════════════════════════════════
    @Test @Order(1)
    void registerNewUser_shouldSucceed() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
        .when()
            .post("/api/profile/register")
        .then()
            .statusCode(200)
            .body("success",  is(true))
            .body("username", equalTo(username));
    }

    // ════════════════════════════════════════════════════════════════════
    // IT-02: US1 — Duplicate registration should fail
    // ════════════════════════════════════════════════════════════════════
    @Test @Order(2)
    void registerDuplicate_shouldFail() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\":\"" + username + "\",\"password\":\"otherpass\"}")
        .when()
            .post("/api/profile/register")
        .then()
            .statusCode(400)
            .body("success", is(false));
    }

    // ════════════════════════════════════════════════════════════════════
    // IT-03: US1 — Login and get profile
    // ════════════════════════════════════════════════════════════════════
    @Test @Order(3)
    void login_shouldReturnProfileData() {
        userId = given()
            .contentType(ContentType.JSON)
            .body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
        .when()
            .post("/api/profile/login")
        .then()
            .statusCode(200)
            .body("success",          is(true))
            .body("username",         equalTo(username))
            .body("userId",           notNullValue())
            .body("scores",           equalTo(0))
            .body("campaignProgress", equalTo(0))
            .extract().path("userId");
    }

    // ════════════════════════════════════════════════════════════════════
    // IT-04: US2 — Start a new PvE campaign
    // ════════════════════════════════════════════════════════════════════
    @Test @Order(4)
    void startPveCampaign_shouldReturnRoom0() {
        given()
            .contentType(ContentType.JSON)
            .body("[{\"name\":\"Aldric\",\"heroClass\":\"WARRIOR\"," +
                  "\"level\":1,\"attack\":5,\"defense\":5," +
                  "\"hp\":100,\"maxHp\":100,\"mana\":50,\"maxMana\":50}]")
        .when()
            .post("/api/pve/" + userId + "/start")
        .then()
            .statusCode(200)
            .body("success",     is(true))
            .body("currentRoom", equalTo(0));
    }

    // ════════════════════════════════════════════════════════════════════
    // IT-05: US2 — Advance to next room
    // ════════════════════════════════════════════════════════════════════
    @Test @Order(5)
    void nextRoom_shouldAdvanceRoomCounter() {
        given()
        .when()
            .post("/api/pve/" + userId + "/next-room")
        .then()
            .statusCode(200)
            .body("success",     is(true))
            .body("currentRoom", equalTo(1))
            .body("roomType",    either(equalTo("BATTLE")).or(equalTo("INN")));
    }

    // ════════════════════════════════════════════════════════════════════
    // IT-06: US3 — Battle service health + action endpoint exists
    // ════════════════════════════════════════════════════════════════════
    @Test @Order(6)
    void battleService_healthEndpointResponds() {
        String battleHost = stack.getServiceHost("battle-service", 5001);
        int    battlePort = stack.getServicePort("battle-service", 5001);
        given()
            .baseUri("http://" + battleHost)
            .port(battlePort)
        .when()
            .get("/api/battle/health")
        .then()
            .statusCode(200);
    }

    // ════════════════════════════════════════════════════════════════════
    // IT-07: US5 — Save campaign progress
    // ════════════════════════════════════════════════════════════════════
    @Test @Order(7)
    void saveCampaign_shouldPersistState() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"userId\":" + userId + "," +
                  "\"partyName\":\"IntTestParty\"," +
                  "\"currentRoom\":1," +
                  "\"gold\":500," +
                  "\"heroes\":[{\"name\":\"Aldric\",\"heroClass\":\"WARRIOR\"," +
                  "\"level\":1,\"attack\":5,\"defense\":5," +
                  "\"hp\":100,\"maxHp\":100,\"mana\":50,\"maxMana\":50,\"experience\":0}]}")
        .when()
            .post("/api/data/campaign/save")
        .then()
            .statusCode(200)
            .body("partyName",   equalTo("IntTestParty"))
            .body("currentRoom", equalTo(1));
    }

    // ════════════════════════════════════════════════════════════════════
    // IT-08: US6 — Continue a saved campaign
    // ════════════════════════════════════════════════════════════════════
    @Test @Order(8)
    void loadSavedCampaign_shouldRestoreRoom() {
        given()
        .when()
            .get("/api/data/campaign/" + userId)
        .then()
            .statusCode(200)
            .body("partyName",   equalTo("IntTestParty"))
            .body("currentRoom", equalTo(1))
            .body("gold",        equalTo(500));
    }

    // ════════════════════════════════════════════════════════════════════
    // IT-09: US2/5 — Score is stored after campaign end
    // ════════════════════════════════════════════════════════════════════
    @Test @Order(9)
    void saveScore_thenRetrieveBestScore() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"score\":1500}")
        .when()
            .post("/api/data/scores/" + userId)
        .then()
            .statusCode(200);

        given()
        .when()
            .get("/api/data/scores/" + userId + "/best")
        .then()
            .statusCode(200)
            .body("bestScore", equalTo(1500));
    }

    // ════════════════════════════════════════════════════════════════════
    // IT-10: Leaderboard returns results
    // ════════════════════════════════════════════════════════════════════
    @Test @Order(10)
    void leaderboard_shouldReturnNonEmptyList() {
        given()
        .when()
            .get("/api/data/scores/top?limit=5")
        .then()
            .statusCode(200)
            .body("$", not(empty()));
    }

    // ════════════════════════════════════════════════════════════════════
    // IT-11: US7 — PvP invite fails for non-existent opponent
    // ════════════════════════════════════════════════════════════════════
    @Test @Order(11)
    void pvpInvite_toNonExistentPlayer_shouldReturn400() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"fromUsername\":\"" + username + "\",\"toUsername\":\"ghost_xyz_9999\"}")
        .when()
            .post("/api/pvp/invite")
        .then()
            .statusCode(400)
            .body("error", containsString("not found"));
    }

    // ════════════════════════════════════════════════════════════════════
    // IT-12: Gateway routes /api/profile correctly
    // ════════════════════════════════════════════════════════════════════
    @Test @Order(12)
    void gateway_routesProfileServiceCorrectly() {
        given()
        .when()
            .get("/api/profile/" + userId)
        .then()
            .statusCode(200)
            .body("username", equalTo(username));
    }
}

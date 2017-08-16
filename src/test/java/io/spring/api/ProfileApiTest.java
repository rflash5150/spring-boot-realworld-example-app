package io.spring.api;

import io.restassured.RestAssured;
import io.spring.application.profile.ProfileData;
import io.spring.application.profile.ProfileQueryService;
import io.spring.core.article.Article;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProfileApiTest extends TestWithCurrentUser {
    @LocalServerPort
    private int port;
    private Article article;
    private User anotherUser;

    @MockBean
    private ProfileQueryService profileQueryService;

    private ProfileData profileData;

    @Before
    public void setUp() throws Exception {
        RestAssured.port = port;
        userFixture();
        anotherUser = new User("username@test.com", "username", "123", "", "");
        profileData = new ProfileData(anotherUser.getId(), anotherUser.getUsername(), anotherUser.getBio(), anotherUser.getImage(), false);
    }

    @Test
    public void should_get_user_profile_success() throws Exception {
        when(profileQueryService.findByUsername(eq(profileData.getUsername()), eq(null)))
            .thenReturn(Optional.of(profileData));
        RestAssured.when()
            .get("/profiles/{username}", profileData.getUsername())
            .prettyPeek()
            .then()
            .statusCode(200)
            .body("profile.username", equalTo(profileData.getUsername()));
    }

    @Test
    public void should_follow_user_success() throws Exception {
        when(userRepository.findByUsername(eq(anotherUser.getUsername()))).thenReturn(Optional.of(anotherUser));
        when(profileQueryService.findByUsername(eq(profileData.getUsername()), eq(user))).thenReturn(Optional.of(profileData));
        given()
            .header("Authorization", "Token " + token)
            .when()
            .post("/profiles/{username}/follow", anotherUser.getUsername())
            .prettyPeek()
            .then()
            .statusCode(200);

    }
}
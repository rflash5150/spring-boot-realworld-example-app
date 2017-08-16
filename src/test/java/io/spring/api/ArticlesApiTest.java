package io.spring.api;

import io.restassured.RestAssured;
import io.spring.application.article.ArticleData;
import io.spring.application.article.ArticleQueryService;
import io.spring.application.profile.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ArticlesApiTest extends TestWithCurrentUser {
    @LocalServerPort
    private int port;

    @MockBean
    private ArticleRepository articleRepository;

    @MockBean
    private ArticleQueryService articleQueryService;

    protected String email;
    protected String username;
    protected String defaultAvatar;

    @Before
    public void setUp() throws Exception {
        RestAssured.port = port;
        email = "john@jacob.com";
        username = "johnjacob";
        defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
        userFixture();
    }

    @Test
    public void should_create_article_success() throws Exception {
        String title = "How to train your dragon";
        String slug = "how-to-train-your-dragon";
        String description = "Ever wonder how?";
        String body = "You have to believe";
        String[] tagList = {"reactjs", "angularjs", "dragons"};
        Map<String, Object> param = prepareParam(title, description, body, tagList);

        ArticleData articleData = new ArticleData(
            "123",
            slug,
            title,
            description,
            body,
            false,
            0,
            new DateTime(),
            new DateTime(),
            Arrays.asList(tagList),
            new ProfileData("userid", user.getUsername(), user.getBio(), user.getImage(), false));

        when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

        given()
            .contentType("application/json")
            .header("Authorization", "Token " + token)
            .body(param)
            .when()
            .post("/articles")
            .then()
            .statusCode(200)
            .body("article.title", equalTo(title))
            .body("article.favorited", equalTo(false))
            .body("article.body", equalTo(body))
            .body("article.favoritesCount", equalTo(0))
            .body("article.author.username", equalTo(user.getUsername()))
            .body("article.author.id", equalTo(null));

        verify(articleRepository).save(any());
    }

    @Test
    public void should_get_error_message_with_wrong_parameter() throws Exception {
        String title = "How to train your dragon";
        String description = "Ever wonder how?";
        String body = "";
        String[] tagList = {"reactjs", "angularjs", "dragons"};
        Map<String, Object> param = prepareParam(title, description, body, tagList);

        given()
            .contentType("application/json")
            .header("Authorization", "Token " + token)
            .body(param)
            .when()
            .post("/articles")
            .prettyPeek()
            .then()
            .statusCode(422)
            .body("errors.body[0]", equalTo("can't be empty"));

    }

    @Test
    public void should_read_article_success() throws Exception {
        String slug = "test-new-article";
        Article article = new Article("Test New Article", "Desc", "Body", new String[]{"java", "spring", "jpg"}, user.getId());

        DateTime time = new DateTime();
        ArticleData articleData = new ArticleData(
            article.getId(),
            article.getSlug(),
            article.getTitle(),
            article.getDescription(),
            article.getBody(),
            false,
            0,
            time,
            time,
            Arrays.asList("joda"),
            new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));

        when(articleQueryService.findBySlug(eq(slug), eq(null))).thenReturn(Optional.of(articleData));

        RestAssured.when()
            .get("/articles/{slug}", slug)
            .then()
            .statusCode(200)
            .body("article.slug", equalTo(slug))
            .body("article.body", equalTo(articleData.getBody()))
            .body("article.createdAt", equalTo(time.toDateTimeISO().toString()));

    }

    @Test
    public void should_404_if_article_not_found() throws Exception {
        when(articleQueryService.findBySlug(anyString(), any())).thenReturn(Optional.empty());
        RestAssured.when()
            .get("/articles/not-exists")
            .then()
            .statusCode(404);
    }

    @Test
    public void should_update_article_content_success() throws Exception {
        String title = "new-title";
        String body = "new body";
        String description = "new description";
        Map<String, Object> updateParam = prepareUpdateParam(title, body, description);

        Article article = new Article(title, description, body, new String[]{"java", "spring", "jpg"}, user.getId());

        DateTime time = new DateTime();
        ArticleData articleData = new ArticleData(
            article.getId(),
            article.getSlug(),
            article.getTitle(),
            article.getDescription(),
            article.getBody(),
            false,
            0,
            time,
            time,
            Arrays.asList("joda"),
            new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));

        when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
        when(articleQueryService.findBySlug(eq(article.getSlug()), eq(user))).thenReturn(Optional.of(articleData));

        given()
            .contentType("application/json")
            .header("Authorization", "Token " + token)
            .body(updateParam)
            .when()
            .put("/articles/{slug}", article.getSlug())
            .then()
            .statusCode(200)
            .body("article.slug", equalTo(articleData.getSlug()));
    }

    @Test
    public void should_get_403_if_not_author_to_update_article() throws Exception {
        String title = "new-title";
        String body = "new body";
        String description = "new description";
        Map<String, Object> updateParam = prepareUpdateParam(title, body, description);

        User anotherUser = new User("test@test.com", "test", "123123", "", "");

        Article article = new Article(title, description, body, new String[]{"java", "spring", "jpg"}, anotherUser.getId());

        DateTime time = new DateTime();
        ArticleData articleData = new ArticleData(
            article.getId(),
            article.getSlug(),
            article.getTitle(),
            article.getDescription(),
            article.getBody(),
            false,
            0,
            time,
            time,
            Arrays.asList("joda"),
            new ProfileData(anotherUser.getId(), anotherUser.getUsername(), anotherUser.getBio(), anotherUser.getImage(), false));

        when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
        when(articleQueryService.findBySlug(eq(article.getSlug()), eq(user))).thenReturn(Optional.of(articleData));

        given()
            .contentType("application/json")
            .header("Authorization", "Token " + token)
            .body(updateParam)
            .when()
            .put("/articles/{slug}", article.getSlug())
            .then()
            .statusCode(403);
    }

    @Test
    public void should_delete_article_success() throws Exception {
        String title = "title";
        String body = "body";
        String description = "description";

        Article article = new Article(title, description, body, new String[]{"java", "spring", "jpg"}, user.getId());
        when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

        given()
            .header("Authorization", "Token " + token)
            .when()
            .delete("/articles/{slug}", article.getSlug())
            .then()
            .statusCode(204);

        verify(articleRepository).remove(eq(article));
    }

    @Test
    public void should_403_if_not_author_delete_article() throws Exception {
        String title = "new-title";
        String body = "new body";
        String description = "new description";
        Map<String, Object> updateParam = prepareUpdateParam(title, body, description);

        User anotherUser = new User("test@test.com", "test", "123123", "", "");

        Article article = new Article(title, description, body, new String[]{"java", "spring", "jpg"}, anotherUser.getId());

        when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
        given()
            .header("Authorization", "Token " + token)
            .when()
            .delete("/articles/{slug}", article.getSlug())
            .then()
            .statusCode(403);
    }

    private HashMap<String, Object> prepareUpdateParam(final String title, final String body, final String description) {
        return new HashMap<String, Object>() {{
            put("article", new HashMap<String, Object>() {{
                put("title", title);
                put("body", body);
                put("description", description);
            }});
        }};
    }

    private HashMap<String, Object> prepareParam(final String title, final String description, final String body, final String[] tagList) {
        return new HashMap<String, Object>() {{
            put("article", new HashMap<String, Object>() {{
                put("title", title);
                put("description", description);
                put("body", body);
                put("tagList", tagList);
            }});
        }};
    }
}
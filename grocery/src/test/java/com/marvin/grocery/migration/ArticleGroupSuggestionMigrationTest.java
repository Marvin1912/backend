package com.marvin.grocery.migration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies that the {@code V1_6__grocery_add_article_group_suggestions} Flyway migration creates a
 * partial unique index enforcing at most one {@code PENDING} suggestion per article, while allowing a
 * {@code PENDING} suggestion to coexist with an already-decided one for the same article.
 */
@Testcontainers
class ArticleGroupSuggestionMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    @Test
    void onlyOnePendingSuggestionPerArticleIsAllowed() throws Exception {
        migrateGrocery();

        try (Connection connection = openConnection()) {
            final long groupId = insertGroup(connection, "Dairy");
            final long articleId = insertArticle(connection, "Vollmilch", "vollmilch");
            insertSuggestion(connection, articleId, groupId, 0.8, "PENDING");

            assertThatThrownBy(() -> insertSuggestion(connection, articleId, groupId, 0.85, "PENDING"))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void aPendingAndAnAcceptedSuggestionCanCoexistForTheSameArticle() throws Exception {
        migrateGrocery();

        try (Connection connection = openConnection()) {
            final long groupId = insertGroup(connection, "Dairy");
            final long articleId = insertArticle(connection, "Butter", "butter");
            insertSuggestion(connection, articleId, groupId, 0.8, "PENDING");

            insertSuggestion(connection, articleId, groupId, 0.9, "ACCEPTED");
        }
    }

    private long insertGroup(Connection connection, String name) throws SQLException {
        final String sql = "INSERT INTO grocery.article_group (name, creation_date, last_modified) VALUES (?, ?, ?) "
                + "RETURNING id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setObject(2, LocalDateTime.now());
            statement.setObject(3, LocalDateTime.now());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private long insertArticle(Connection connection, String name, String normalizedName) throws SQLException {
        final String sql = "INSERT INTO grocery.article (name, normalized_name, creation_date, last_modified) "
                + "VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, normalizedName);
            statement.setObject(3, LocalDateTime.now());
            statement.setObject(4, LocalDateTime.now());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private void insertSuggestion(Connection connection, long articleId, long groupId, double score, String status)
            throws SQLException {
        final String sql = "INSERT INTO grocery.article_group_suggestion "
                + "(article_id, suggested_group_id, score, source, status, creation_date, last_modified) "
                + "VALUES (?, ?, ?, 'HEURISTIC', ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, articleId);
            statement.setLong(2, groupId);
            statement.setDouble(3, score);
            statement.setString(4, status);
            statement.setObject(5, LocalDateTime.now());
            statement.setObject(6, LocalDateTime.now());
            statement.executeUpdate();
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void migrateGrocery() {
        final FluentConfiguration configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("grocery")
                .locations("classpath:db/migration/grocery");
        configuration.load().migrate();
    }
}

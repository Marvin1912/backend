package com.marvin.grocery.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies that the {@code V1_5__grocery_add_article_groups} Flyway migration backfills the new
 * {@code article} table cleanly against pre-existing {@code receipt_item} data: one article per
 * distinct normalized name, no duplicates, and every existing row linked via {@code article_id}.
 */
@Testcontainers
class ArticleBackfillMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    @Test
    void backfillCreatesOneArticlePerNormalizedNameAndLinksAllExistingReceiptItems() throws Exception {
        migrateGroceryUpTo("1.4");

        final UUID receiptId = UUID.randomUUID();
        try (Connection connection = openConnection()) {
            insertReceipt(connection, receiptId);
            insertReceiptItem(connection, receiptId, "Tomaten rot");
            insertReceiptItem(connection, receiptId, " tomaten ROT ");
            insertReceiptItem(connection, receiptId, "Gurke");
        }

        migrateGroceryUpTo(null);

        try (Connection connection = openConnection()) {
            assertArticleCountAndUniqueness(connection, 2);
            assertEveryReceiptItemHasAnArticle(connection);
            assertDuplicateNormalizedNamesShareTheSameArticle(connection);
        }
    }

    private void assertArticleCountAndUniqueness(final Connection connection, final int expectedCount) throws Exception {
        final Set<String> normalizedNames = new HashSet<>();
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT normalized_name FROM grocery.article")) {
            while (resultSet.next()) {
                normalizedNames.add(resultSet.getString(1));
            }
        }
        assertThat(normalizedNames).hasSize(expectedCount);
    }

    private void assertEveryReceiptItemHasAnArticle(final Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement
                        .executeQuery("SELECT COUNT(*) FROM grocery.receipt_item WHERE article_id IS NULL")) {
            resultSet.next();
            assertThat(resultSet.getInt(1)).isZero();
        }
    }

    private void assertDuplicateNormalizedNamesShareTheSameArticle(final Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT DISTINCT article_id FROM grocery.receipt_item WHERE LOWER(TRIM(name)) = 'tomaten rot'")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.next()).isFalse();
        }
    }

    private void insertReceipt(final Connection connection, final UUID receiptId) throws Exception {
        final String sql = "INSERT INTO grocery.receipt (id, receipt_date, creation_date, last_modified) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, receiptId);
            statement.setObject(2, LocalDateTime.now().toLocalDate());
            statement.setObject(3, LocalDateTime.now());
            statement.setObject(4, LocalDateTime.now());
            statement.executeUpdate();
        }
    }

    private void insertReceiptItem(final Connection connection, final UUID receiptId, final String name) throws Exception {
        final String sql = "INSERT INTO grocery.receipt_item (receipt_id, name, price) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, receiptId);
            statement.setString(2, name);
            statement.setBigDecimal(3, BigDecimal.valueOf(1.99));
            statement.executeUpdate();
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void migrateGroceryUpTo(final String targetVersion) {
        final FluentConfiguration configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("grocery")
                .locations("classpath:db/migration/grocery");
        if (targetVersion != null) {
            configuration.target(targetVersion);
        }
        configuration.load().migrate();
    }
}

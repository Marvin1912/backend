package com.marvin.grocery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marvin.grocery.entity.ArticleGroupEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Repository integration test for {@link ArticleGroupEntity}, exercising the real grocery Flyway migrations. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ArticleGroupRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private ArticleGroupRepository articleGroupRepository;

    @Test
    void saveAndFindByIdPersistsNameAndAuditTimestamps() {
        final ArticleGroupEntity group = new ArticleGroupEntity();
        group.setName("Tomatoes");

        final ArticleGroupEntity saved = articleGroupRepository.save(group);

        final Optional<ArticleGroupEntity> found = articleGroupRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Tomatoes");
        assertThat(found.get().getCreationDate()).isNotNull();
        assertThat(found.get().getLastModified()).isNotNull();
    }
}

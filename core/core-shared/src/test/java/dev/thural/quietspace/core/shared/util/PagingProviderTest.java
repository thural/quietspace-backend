package dev.thural.quietspace.core.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PagingProviderTest {

    private final PagingProvider provider = new PagingProvider();

    @Test
    void buildPageRequest_defaultsToFirstPageSize25() {
        var pageRequest = provider.buildPageRequest(null, null, null);

        assertThat(pageRequest.getPageNumber()).isEqualTo(0);
        assertThat(pageRequest.getPageSize()).isEqualTo(25);
        assertThat(pageRequest.getSort()).isEqualTo(PagingProvider.DEFAULT_SORT_OPTION);
    }

    @Test
    void buildPageRequest_customParams() {
        var pageRequest = provider.buildPageRequest(2, 50, Sort.by("name").ascending());

        assertThat(pageRequest.getPageNumber()).isEqualTo(2);
        assertThat(pageRequest.getPageSize()).isEqualTo(50);
        assertThat(pageRequest.getSort()).isEqualTo(Sort.by("name").ascending());
    }

    @Test
    void buildPageRequest_boundsPageSize() {
        var pageRequest = provider.buildPageRequest(0, 2000, Sort.by("id").ascending());

        assertThat(pageRequest.getPageSize()).isEqualTo(1000);
    }

    @Test
    void buildPageRequest_givenZeroPage_usesDefault() {
        var pageRequest = provider.buildPageRequest(0, 10, Sort.by("id").ascending());

        assertThat(pageRequest.getPageNumber()).isEqualTo(0);
    }

    @Test
    void buildPageRequest_givenNegativePage_usesDefault() {
        var pageRequest = provider.buildPageRequest(-1, 10, Sort.by("id").ascending());

        assertThat(pageRequest.getPageNumber()).isEqualTo(0);
    }
}
package dev.thural.quietspace.shared.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class PagingProviderTest {

    @Test
    void buildPageRequest_givenNullPageNumber_returnsPage0() {
        PageRequest pr = PagingProvider.buildPageRequest(null, 25, null);
        assertThat(pr.getPageNumber()).isZero();
    }

    @Test
    void buildPageRequest_givenPageNumber3_returnsPage3() {
        PageRequest pr = PagingProvider.buildPageRequest(3, 25, null);
        assertThat(pr.getPageNumber()).isEqualTo(3);
    }

    @Test
    void buildPageRequest_givenNullPageSize_returnsSize25() {
        PageRequest pr = PagingProvider.buildPageRequest(0, null, null);
        assertThat(pr.getPageSize()).isEqualTo(25);
    }

    @Test
    void buildPageRequest_givenPageSize2000_capsAt1000() {
        PageRequest pr = PagingProvider.buildPageRequest(0, 2000, null);
        assertThat(pr.getPageSize()).isEqualTo(1000);
    }

    @Test
    void buildPageRequest_givenValidPageSize_usesIt() {
        PageRequest pr = PagingProvider.buildPageRequest(0, 10, null);
        assertThat(pr.getPageSize()).isEqualTo(10);
    }

    @Test
    void buildPageRequest_givenNullSort_usesDefaultDescending() {
        PageRequest pr = PagingProvider.buildPageRequest(0, 25, null);
        assertThat(pr.getSort()).isEqualTo(Sort.by("createDate").descending());
    }

    @Test
    void buildPageRequest_givenCustomSort_usesIt() {
        Sort customSort = Sort.by("text").ascending();
        PageRequest pr = PagingProvider.buildPageRequest(0, 25, customSort);
        assertThat(pr.getSort()).isEqualTo(customSort);
    }
}

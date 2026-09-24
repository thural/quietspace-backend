package dev.thural.quietspace.shared.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageUtilsTest {

    @Test
    void pageFromList_givenFullPage_returnsSubListWithTotalCount() {
        List<String> list = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        Pageable pageable = PageRequest.of(0, 5);

        Page<String> page = PageUtils.pageFromList(list, pageable);

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(10);
    }

    @Test
    void pageFromList_givenLastPartialPage_returnsRemaining() {
        List<String> list = List.of("a", "b", "c", "d", "e", "f", "g");
        Pageable pageable = PageRequest.of(1, 5);

        Page<String> page = PageUtils.pageFromList(list, pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(7);
    }

    @Test
    void pageFromList_givenEmptyList_returnsEmptyPage() {
        List<String> list = List.of();
        Pageable pageable = PageRequest.of(0, 5);

        Page<String> page = PageUtils.pageFromList(list, pageable);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }
}

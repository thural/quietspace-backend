package dev.thural.quietspace.core.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PageUtilsTest {

    @Test
    void pageFromList_givenListAndPageable_returnsPage() {
        List<String> list = List.of("a", "b", "c", "d", "e");
        PageRequest pageable = PageRequest.of(0, 2);

        Page<String> page = PageUtils.pageFromList(list, pageable);

        assertThat(page.getContent()).containsExactly("a", "b");
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void pageFromList_givenOffsetBeyondSize_returnsEmptyPage() {
        List<String> list = List.of("a", "b");
        PageRequest pageable = PageRequest.of(2, 2);

        Page<String> page = PageUtils.pageFromList(list, pageable);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    void pageFromList_givenLastPage_returnsRemainingElements() {
        List<String> list = List.of("a", "b", "c", "d", "e");
        PageRequest pageable = PageRequest.of(2, 2);

        Page<String> page = PageUtils.pageFromList(list, pageable);

        assertThat(page.getContent()).containsExactly("e");
        assertThat(page.getTotalElements()).isEqualTo(5);
    }
}
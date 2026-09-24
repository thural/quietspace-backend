package dev.thural.quietspace.shared.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class PageUtils<T> {
    
    public static <T> Page<T> pageFromList(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= list.size()) return Page.empty(pageable);
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<T> sublist = list.subList(start, end);
        return new PageImpl<>(sublist, pageable, list.size());
    }

}

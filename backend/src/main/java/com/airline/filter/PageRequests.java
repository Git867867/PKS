package com.airline.filter;

import com.airline.error.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageRequests {

    private PageRequests() {
    }

    public static Pageable of(int page, int size, String sort, String direction, int maxSize, Set<String> allowed) {
        if (page < 0) {
            throw new BadRequestException("Номер страницы не может быть отрицательным.");
        }
        if (size < 1 || size > maxSize) {
            throw new BadRequestException("Размер страницы от 1 до " + maxSize + ".");
        }
        if (!allowed.contains(sort)) {
            throw new BadRequestException("Сортировка по полю «" + sort + "» запрещена.");
        }
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new BadRequestException("Направление сортировки: asc или desc.");
        }
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(dir, sort));
    }
}

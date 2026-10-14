package com.navan.commerce.transactions.transaction.pagination;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class OffsetPageRequest implements Pageable {

    private final long offset;
    private final int pageSize;
    private final Sort sort;

    public OffsetPageRequest(long offset, int pageSize, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be negative");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        this.offset = offset;
        this.pageSize = pageSize;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, offset / pageSize));
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(offset + pageSize, pageSize, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious()
                ? new OffsetPageRequest(Math.max(0, offset - pageSize), pageSize, sort)
                : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, pageSize, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page index must not be negative");
        }
        return new OffsetPageRequest((long) pageNumber * pageSize, pageSize, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}

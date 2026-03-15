package com.rameshkumar.placementsystem.dto;

import java.util.List;

public class PaginationResponse<T> {

    private List<T> data;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PaginationResponse(List<T> data, int page, int size, long totalElements, int totalPages) {
        this.data = data;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<T> getData() {
        return data;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
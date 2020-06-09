package com.flexicore.model.auditing;

import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.service.AuditingService;

import java.util.List;
import java.util.stream.Collectors;

public class PaginationAuditingContainer<T> {
    private long totalRecords;
    private long totalPages;
    private long startPage;
    private long endPage;
    private List<?> list;

    public PaginationAuditingContainer(PaginationResponse<T> paginationResponse) {
        this.totalPages=paginationResponse.getTotalPages();
        this.totalRecords=paginationResponse.getTotalRecords();
        this.startPage=paginationResponse.getStartPage();
        this.endPage=paginationResponse.getEndPage();
        this.list=paginationResponse.getList().parallelStream().map(f -> AuditingService.contain(f)).filter(o -> o != null).collect(Collectors.toList());
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public PaginationAuditingContainer setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
        return this;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public PaginationAuditingContainer setTotalPages(long totalPages) {
        this.totalPages = totalPages;
        return this;
    }

    public long getStartPage() {
        return startPage;
    }

    public PaginationAuditingContainer setStartPage(long startPage) {
        this.startPage = startPage;
        return this;
    }

    public long getEndPage() {
        return endPage;
    }

    public PaginationAuditingContainer setEndPage(long endPage) {
        this.endPage = endPage;
        return this;
    }

    public List<?> getList() {
        return list;
    }

    public PaginationAuditingContainer setList(List<?> list) {
        this.list = list;
        return this;
    }
}

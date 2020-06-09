package com.flexicore.data.jsoncontainers;

import com.flexicore.service.AuditingService;

import java.util.List;
import java.util.stream.Collectors;

public class ListHolder<T> {
    private List<?> list;

    public ListHolder(List<T> f) {
        list = f.parallelStream().map(o -> AuditingService.contain(o)).collect(Collectors.toList());
    }

    public List<?> getList() {
        return list;
    }

    public ListHolder<T> setList(List<?> list) {
        this.list = list;
        return this;
    }
}

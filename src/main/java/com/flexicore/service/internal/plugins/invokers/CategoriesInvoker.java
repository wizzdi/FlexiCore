package com.flexicore.service.internal.plugins.invokers;

import com.flexicore.annotations.plugins.PluginInfo;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.dynamic.InvokerInfo;
import com.flexicore.interfaces.dynamic.InvokerMethodInfo;
import com.flexicore.interfaces.dynamic.ListingInvoker;
import com.flexicore.model.Category;
import com.flexicore.request.CategoryFilter;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.CategoryService;

import org.springframework.beans.factory.annotation.Autowired;

@PluginInfo(version = 1)
@InvokerInfo()
public class CategoriesInvoker implements ListingInvoker<Category,CategoryFilter> {

    @Autowired
    private CategoryService categoryService;

    @Override
    @InvokerMethodInfo(displayName = "listAllCategories",description = "lists all categories")
    public PaginationResponse<Category> listAll(CategoryFilter filter, SecurityContext securityContext) {
        return categoryService.getAllCategories(filter,securityContext);
    }

    @Override
    public Class<CategoryFilter> getFilterClass() {
        return CategoryFilter.class;
    }

    @Override
    public Class<?> getHandlingClass() {
        return Category.class;
    }
}

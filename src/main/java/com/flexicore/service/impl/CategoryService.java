/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import javax.inject.Named;

import com.flexicore.data.BaselinkRepository;
import com.flexicore.data.CategoryRepository;
import com.flexicore.data.ClazzRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.model.SortParameter;
import com.flexicore.model.Baseclass;
import com.flexicore.model.Category;
import com.flexicore.model.CategoryToBaseClass;
import com.flexicore.model.CategoryToClazz;
import com.flexicore.model.Clazz;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.Tenant;
import com.flexicore.model.User;
import com.flexicore.request.*;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.BaseclassNewService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Named
@Primary
@Component
public class CategoryService implements com.flexicore.service.CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ClazzRepository clazzrepository;

    @Autowired
    private BaselinkRepository baselinkRepository;

    @Autowired
    private BaseclassNewService baseclassNewService;

   private Logger logger = Logger.getLogger(getClass().getCanonicalName());

    public CategoryService() {
        // TODO Auto-generated constructor stub
    }

    public <T extends Baseclass> List<Category> getAllowedCategories(Class<T> c, List<SortParameter> sortby, SecurityContext securityContext) {
        QueryInformationHolder<Category> queryInformationHolder = new QueryInformationHolder<>( null, sortby, -1, -1, Category.class, securityContext);
        return categoryRepository.getAllCategoriesAllowed(c.getCanonicalName(), queryInformationHolder);
    }

    public boolean connectCategory(String baseId, String categoryId, SecurityContext securityContext, boolean checkForExisting) {
        Baseclass base = categoryRepository.findById(baseId);
        Category category = categoryRepository.findById(categoryId);
        List<CategoryToBaseClass> categories=getConnectedCategories(base);
        if(checkForExisting){
            for (CategoryToBaseClass categoryToBaseClass : categories) {
                if(categoryToBaseClass.getLeftside().getId().equals(categoryId)){
                    return false;
                }
            }
        }

        CategoryToBaseClass categoryToBaseClass = connectCategoryNoMerge(base, category, securityContext);
        categoryRepository.merge(categoryToBaseClass);
        return categoryToBaseClass != null;
        // }
        // return false;

    }


    public CategoryToBaseClass connectCategoryNoMerge(Baseclass base, Category category, SecurityContext securityContext) {

        // if(categoryRepository.isAllowed(base.getClazz().getName(),
        // category)){ //TODO:fix
        CategoryToBaseClass categoryToBaseClass = new CategoryToBaseClass(category.getName(), securityContext);
        categoryToBaseClass.setLeftside(category);
        categoryToBaseClass.setRightside(base);
        base.addCategory(categoryToBaseClass);
        return categoryToBaseClass;
        // }
        // return false;

    }


    public boolean disconnectCategory(String baseId, String categoryId, User user) {
        Baseclass base = categoryRepository.findById(baseId);
        List<CategoryToBaseClass> links = getConnectedCategories(base);
        Set<String> toRemove=new HashSet<>();
        List<CategoryToBaseClass> toRemoveLinks=new ArrayList<>();
        for (CategoryToBaseClass categoryToBaseClass : links) {
            if (categoryToBaseClass.getLeftside().getId().equals(categoryId)) {
                toRemove.add(categoryToBaseClass.getId());
                toRemoveLinks.add(categoryToBaseClass);

            }


        }
        if(!toRemove.isEmpty()){
            if(categoryRepository.deleteCategoryLinks(toRemove)){
                base.getCategories().removeAll(toRemoveLinks);
                return true;
            }

        }
        return false;

    }

    public void enableCategory(Category category, Class<? extends Baseclass> c, SecurityContext securityContext)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        CategoryToClazz categoryToClazz = enableCategoryNoMerge(category, c, securityContext);
        categoryRepository.merge(categoryToClazz);


    }

    public CategoryToClazz enableCategoryNoMerge(Category category, Class<? extends Baseclass> c, SecurityContext securityContext) {
        Clazz clazz = Baseclass.getClazzbyname(c.getCanonicalName());

        CategoryToClazz categoryToClazz = new CategoryToClazz(category.getName(), securityContext);
        categoryToClazz.setLeftside(category);
        categoryToClazz.setRightside(clazz);
        return categoryToClazz;


    }

    // TODO:complete
    public void disableCategory(Category category, String className, SecurityContext securityContext) {
        Clazz clazz = Baseclass.getClazzbyname(className);
        List<CategoryToClazz> exsitings = clazz.getCategoriesToClazz();
        QueryInformationHolder<CategoryToClazz> queryInformationHolder = new QueryInformationHolder<>( null, null, -1, -1, CategoryToClazz.class, securityContext);
        List<CategoryToClazz> links = categoryRepository.getLinksOfClazzLeftandCatRight(clazz, category, queryInformationHolder);
        for (CategoryToClazz categoryToClazz : links) {
            clazzrepository.remove(categoryToClazz, CategoryToClazz.class);
        }


    }

    public <T extends Baseclass> Category createCategory(String categoryName, boolean checkForExisting, SecurityContext securityContext) {

        if(checkForExisting){
            List<Category> categories=baselinkRepository.getByName(categoryName,Category.class,null,null);
            if(!categories.isEmpty()){
                return categories.get(0);
            }
        }
            User user = securityContext.getUser();
            List<Tenant> tenants = securityContext.getTenants();
            Category cat = new Category(categoryName, securityContext);
            categoryRepository.merge(cat);
            for (Tenant tenant : tenants) {
               // baselinkRepository.addbaseClassToTenant(cat, tenant, securityContext);

            }
            return cat;



    }

    public List<Category> getByName(String name, SecurityContext securityContext) {
        QueryInformationHolder<Category> queryInformationHolder = new QueryInformationHolder<>( null, null, -1, -1, Category.class, securityContext);
        return categoryRepository.getByName(name, queryInformationHolder);
    }

    public Category getFirstByName(String name, SecurityContext securityContext) {
      List<Category> list=getByName(name,securityContext);
      return list.isEmpty()?null:list.get(0);
    }

    public List<Category> getAll(SecurityContext securityContext) {
        return getAllowedCategories(Category.class, null, securityContext);
    }


    public List<CategoryToBaseClass> getConnectedCategories(Baseclass base){
        return categoryRepository.getConnectedCategories(base);
    }

    public List<Category> getAllCategories(int pagesize, int currentpage, SecurityContext securityContext) {
        QueryInformationHolder<Category> cats = new QueryInformationHolder<>(Category.class, securityContext);
        cats.setPageSize(pagesize);
        cats.setCurrentPage(currentpage);
        return categoryRepository.getAllFiltered(cats);
    }

    @Override
    public PaginationResponse<Category> getAllCategories(CategoryFilter categoryFilter, SecurityContext securityContext) {
        QueryInformationHolder<Category> cats = new QueryInformationHolder<>(categoryFilter,Category.class, securityContext);

        List<Category> list = categoryRepository.getAllFiltered(cats);
        long count=categoryRepository.countAllFiltered(cats);
        return new PaginationResponse<>(list,categoryFilter,count);
    }

    @Override
    public List<Category> listAllCategories(CategoryFilter categoryFilter, SecurityContext securityContext) {
        return categoryRepository.listAllCategories(categoryFilter,securityContext);
    }

    @Override
    public Category createCategoryNoMerge(CategoryCreate categoryCreate, SecurityContext securityContext) {
        Category category=new Category(categoryCreate.getName(),securityContext);
        updateCategoryNoMerge(categoryCreate,category);
        return category;
    }

    @Override
    public Category createCategory(CategoryCreate categoryCreate, SecurityContext securityContext) {
        Category category=createCategoryNoMerge(categoryCreate,securityContext);
        categoryRepository.merge(category);
        return category;
    }

    @Override
    public Category updateCategory(CategoryUpdate categoryUpdate, SecurityContext securityContext) {
        Category category=categoryUpdate.getCategory();
        if(updateCategoryNoMerge(categoryUpdate,category)){
            categoryRepository.merge(category);
        }
        return category;
    }

    @Override
    public boolean updateCategoryNoMerge(CategoryCreate categoryCreate, Category category) {
        return baseclassNewService.updateBaseclassNoMerge(categoryCreate,category);
    }

    @Override
    public PaginationResponse<CategoryToBaseClass> getAllCategoryToBaseclass(CategoryToBaseclassFilter categoryToBaseclassFilter, SecurityContext securityContext) {
        List<CategoryToBaseClass> categoryToBaseClasses=listAllCategoryToBaseclass(categoryToBaseclassFilter,securityContext);
        long count=categoryRepository.countAllCategoryToBaseclass(categoryToBaseclassFilter,securityContext);
        return new PaginationResponse<>(categoryToBaseClasses,categoryToBaseclassFilter,count);
    }

    @Override
    public List<CategoryToBaseClass> listAllCategoryToBaseclass(CategoryToBaseclassFilter categoryToBaseclassFilter, SecurityContext securityContext) {
        return categoryRepository.listAllCategoryToBaseclass(categoryToBaseclassFilter,securityContext);
    }

    @Override
    public CategoryToBaseClass createCategoryToBaseclassNoMerge(CategoryToBaseclassCreate categoryToBaseclassCreate, SecurityContext securityContext) {
        CategoryToBaseClass categoryToBaseClass=new CategoryToBaseClass(categoryToBaseclassCreate.getName(),securityContext);
        updateCategoryToBaseclassNoMerge(categoryToBaseclassCreate,categoryToBaseClass);
        return categoryToBaseClass;
    }

    @Override
    public CategoryToBaseClass createCategoryToBaseclass(CategoryToBaseclassCreate categoryToBaseclassCreate, SecurityContext securityContext) {
        CategoryToBaseClass categoryToBaseClass=createCategoryToBaseclassNoMerge(categoryToBaseclassCreate,securityContext);
        categoryRepository.merge(categoryToBaseClass);
        return categoryToBaseClass;
    }

    @Override
    public CategoryToBaseClass updateCategoryToBaseclass(CategoryToBaseclassUpdate categoryToBaseclassUpdate, SecurityContext securityContext) {
        CategoryToBaseClass categoryToBaseClass=categoryToBaseclassUpdate.getCategoryToBaseClass();
        if(updateCategoryToBaseclassNoMerge(categoryToBaseclassUpdate,categoryToBaseClass)){
            categoryRepository.merge(categoryToBaseClass);
        }
        return categoryToBaseClass;
    }

    @Override
    public boolean updateCategoryToBaseclassNoMerge(CategoryToBaseclassCreate categoryToBaseclassCreate, CategoryToBaseClass categoryToBaseClass) {
        boolean update=baseclassNewService.updateBaseclassNoMerge(categoryToBaseclassCreate,categoryToBaseClass);
        if(categoryToBaseclassCreate.getCategory()!=null && (categoryToBaseClass.getLeftside()==null||!categoryToBaseclassCreate.getCategory().getId().equals(categoryToBaseClass.getLeftside().getId()))){
            categoryToBaseClass.setLeftside(categoryToBaseclassCreate.getCategory());
            update=true;
        }
        if(categoryToBaseclassCreate.getBaseclass()!=null && (categoryToBaseClass.getRightside()==null||!categoryToBaseclassCreate.getBaseclass().getId().equals(categoryToBaseClass.getRightside().getId()))){
            categoryToBaseClass.setRightside(categoryToBaseclassCreate.getBaseclass());
            update=true;
        }
        return update;
    }

    public Category get(String catId) {
        return categoryRepository.findById(Category.class, catId);
    }

    public List<Baseclass> getAll(String id, SecurityContext securityContext) {
        Baseclass b = baselinkRepository.findById(Baseclass.class, id);
        return null;//baselinkRepository.getBaseLinksBaseClassesBySide(CategoryToBaseClass.class, b, true, securityContext);
    }

    public void massMerge(List<Object> toMerge) {
        categoryRepository.massMerge(toMerge);
    }

    public List<Category> getCategoriesByNames(Set<String> categoryNames, SecurityContext securityContext) {
        if(categoryNames.isEmpty()) {
            return new ArrayList<>();
        }
        return categoryRepository.listByNames(Category.class,categoryNames,securityContext);
    }

    public void refrehEntityManager() {
        baselinkRepository.refrehEntityManager();
        categoryRepository.refrehEntityManager();
        clazzrepository.refrehEntityManager();
    }
}

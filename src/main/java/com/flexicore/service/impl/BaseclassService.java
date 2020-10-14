/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexicore.annotations.Baseclassroot;
import com.flexicore.data.BaseclassRepository;
import com.flexicore.data.BaselinkRepository;
import com.flexicore.data.jsoncontainers.CrossLoaderResolver;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.data.jsoncontainers.SetBaseclassTenantRequest;
import com.flexicore.data.jsoncontainers.Views;
import com.flexicore.interfaces.dynamic.ListFieldInfo;
import com.flexicore.interfaces.dynamic.ListingInvoker;
import com.flexicore.model.*;
import com.flexicore.request.*;
import com.flexicore.response.BaseclassCount;
import com.flexicore.response.ParameterInfo;
import com.flexicore.security.SecurityContext;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.persistence.OneToMany;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.ServiceUnavailableException;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Primary
@Component
public class BaseclassService implements com.flexicore.service.BaseclassService {
    @Autowired
    @Baseclassroot
    private BaseclassRepository baseclassRepository;

    @Autowired
    private BaselinkRepository baselinkRepository;

    private Logger logger = Logger.getLogger(getClass().getCanonicalName());
    private static ObjectMapper objectMapper;

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    @Lazy
    private PluginManager pluginManager;

    @Autowired
    private OperationService operationService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private BaselinkService baselinkService;

    private static Map<String, LinkSide> sideCache = new ConcurrentHashMap<>();


    static {
        objectMapper = new ObjectMapper();
        objectMapper.setConfig(objectMapper.getSerializationConfig().withView(Views.ForSwaggerOnly.class).with());


    }

    private static final Set<String> knownTypes = new HashSet<>(Arrays.asList(OffsetDateTime.class.getCanonicalName(),
            Date.class.getCanonicalName(), ZonedDateTime.class.getCanonicalName(), List.class.getCanonicalName(), Map.class.getCanonicalName()));


    public BaseclassService() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public <T extends Baseclass> long count(Class<T> clazz, FilteringInformationHolder filteringInformationHolder, SecurityContext securityContext) {
        QueryInformationHolder<T> queryInformationHolder = new QueryInformationHolder<>(filteringInformationHolder, clazz, securityContext);
        return baseclassRepository.countAllFiltered(queryInformationHolder);
    }


    @Override
    public void persist(Baseclass base) {
        baseclassRepository.Persist(base);
    }

    @Override
    public <T extends Baseclass> T find(Class<T> type, String id) {
        return baseclassRepository.findById(type, id);
    }

    @Override
    public <T extends Baseclass> T findByIdOrNull(Class<T> type, String id) {
        return baseclassRepository.findByIdOrNull(type, id);
    }


    @Override
    public <T extends Baseclass> List<T> listByIds(Class<T> c, Set<String> ids, SecurityContext securityContext) {
        return baseclassRepository.listByIds(c, ids, securityContext);
    }

    @Override
    public boolean remove(Baseclass base) {
        return baseclassRepository.remove(base, Baseclass.class);
    }

    @Override
    public <T extends Baseclass> int removeById(String id, QueryInformationHolder<T> queryInformationHolder) {
        return baseclassRepository.removeById(id, queryInformationHolder);
    }

    @Override
    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString, SecurityContext securityContext) {
        return baseclassRepository.getByIdOrNull(id, c, batchString, securityContext);
    }

    @Override
    public <T extends Baseclass> List<T> getAllByKeyWordAndCategory(QueryInformationHolder<T> queryInformationHolder) {
        return baseclassRepository.getAllFiltered(queryInformationHolder);
    }

    @Override
    public <T extends Baseclass> List<T> getAllFiltered(QueryInformationHolder<T> queryInformationHolder) {
        return baseclassRepository.getAllFiltered(queryInformationHolder);
    }

    @Override
    public <T extends Baseclass> List<T> getAllFiltered(FilteringInformationHolder filteringInformationHolder, Class<T> c, SecurityContext securityContext) {
        return baseclassRepository.getAllFiltered(new QueryInformationHolder<>(filteringInformationHolder, c, securityContext));
    }

    @Override
    public <T extends Baseclass> long countAllFiltered(QueryInformationHolder<T> queryInformationHolder) {
        return baseclassRepository.countAllFiltered(queryInformationHolder);
    }

    @Override
    public <T extends Baseclass> long countAllFiltered(FilteringInformationHolder filteringInformationHolder, Class<T> c, SecurityContext securityContext) {
        return baseclassRepository.countAllFiltered(new QueryInformationHolder<>(filteringInformationHolder, c, securityContext));
    }


    @Override
    public <T extends Baseclass> String serializeBaseclssForExport(T baseclass, SecurityContext securityContext) throws JsonProcessingException {
        return objectMapper.writeValueAsString(baseclass);
    }


    @Override
    public <T extends Baseclass> T deserializeBaseclassForImport(String json, Class<T> type, SecurityContext securityContext) throws IOException {
        return objectMapper.readValue(json, type);
    }

    @Override
    public void validate(MassDeleteRequest massDeleteRequest, SecurityContext securityContext) {
        Set<String> ids = massDeleteRequest.getIds();
        Map<String, Baseclass> map = ids.isEmpty() ? new HashMap<>() : listByIds(Baseclass.class, ids, securityContext).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));
        ids.removeAll(map.keySet());
        massDeleteRequest.setBaseclass(new ArrayList<>(map.values()));
    }

    @Override
    public void massDelete(MassDeleteRequest massDeleteRequest, SecurityContext securityContext) {
        if (massDeleteRequest.getBaseclass().isEmpty()) {
            return;
        }
        baseclassRepository.massDelete(massDeleteRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Baselink, E extends Baseclass> List<String> getConnectedClasses(Clazz c, String id, Clazz linkClazz,
                                                                                      FilteringInformationHolder filteringInformationHolder, int pagesize, int currentPage, Baseclass value, String simpleValue, SecurityContext securityContext) {
        try {
            ;
            Class<E> type = (Class<E>) Class.forName(c.getName());
            Class<T> linkclass = (Class<T>) Class.forName(linkClazz.getName());
            Boolean right = onRight(type, linkclass);
            if (right == null) {
                throw new BadRequestException("could not find connection between " + c.getName() + " and " + linkClazz.getName());
            }
            Baseclass base = baseclassRepository.getByIdOrNull(id, Baseclass.class, null, securityContext);
            if (base == null) {
                throw new BadRequestException("No Baseclass with id " + id);
            }

            return null;//baselinkRepository.getConnectedClasses(linkclass, type, base, right, filteringInformationHolder, pagesize, currentPage, value, simpleValue, securityContext);
        } catch (ClassNotFoundException e) {
            throw new BadRequestException("could not find class ", e);
        }


    }

    @Override
    public ParameterInfo getClassInfo(GetClassInfo filteringInformationHolder) {
        return null;
    }

    @Override
    public Object getExample(GetClassInfo filteringInformationHolder) {
        String className = filteringInformationHolder.getClassName();
        Class<?> c = CrossLoaderResolver.getRegisteredClass(className);
        if (c == null) {
            throw new BadRequestException("No Class " + filteringInformationHolder.getClassName());
        }

        Object exampleCached = getExampleCached(c);
        if (exampleCached == null) {
            throw new ServiceUnavailableException("Class " + className + " is not suitable for examples");
        }
        return exampleCached;

    }

    private Object getExampleCached(Class<?> c) {
        Object existing = exampleCache.getIfPresent(c.getCanonicalName());
        if (existing == null) {
            existing = getExample(c);
        }
        return existing;
    }

    private String getSetterName(String name) {
        if (name.startsWith("get")) {
            return name.replaceFirst("get", "set");
        }

        if (name.startsWith("is")) {
            return name.replaceFirst("is", "set");
        }
        return null;
    }


    private Object getExample(Class<?> c) {
        if (ClassUtils.isPrimitiveOrWrapper(c) || c.equals(String.class)) {
            return getPrimitiveValue(c);
        }
        if (c.isArray()) {
            return Array.newInstance(c, 0);
        }
        if (isKnownType(c)) {
            return getKnownTypeValue(c);
        }


        Object example = null;
        try {
            example = c.newInstance();
            exampleCache.put(c.getCanonicalName(), example);
            BeanInfo beanInfo = Introspector.getBeanInfo(c, Object.class);
            for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                try {
                    Method getter = propertyDescriptor.getReadMethod();
                    if (getter != null) {
                        if (getter.getAnnotation(JsonIgnore.class) == null) {
                            String setterName = getSetterName(getter.getName());
                            if (setterName != null) {
                                Method setter = c.getMethod(setterName, propertyDescriptor.getPropertyType());
                                if (setter != null) {

                                    Class<?> toRet = getter.getReturnType();
                                    if (Collection.class.isAssignableFrom(toRet)) {
                                        ListFieldInfo listFieldInfo = getter.getAnnotation(ListFieldInfo.class);
                                        if (listFieldInfo != null) {
                                            Class<?> collectionType = listFieldInfo.listType();
                                            Object o = getExampleCached(collectionType);
                                            Collection collection = (Collection) toRet.newInstance();
                                            collection.add(o);
                                            setter.invoke(example, collection);

                                        }
                                    } else {
                                        setter.invoke(example, getExampleCached(toRet));
                                    }

                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.info("failed setting example value for " + propertyDescriptor.getName());
                }

            }
            return example;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "failed getting example value for " + c.getCanonicalName(), e);
        }
        return example;
    }

    private Object getKnownTypeValue(Class<?> c) {
        if (c.equals(OffsetDateTime.class)) return OffsetDateTime.now();
        if (c.equals(ZonedDateTime.class)) return ZonedDateTime.now();
        if (c.equals(Date.class)) return Date.from(Instant.now());
        if (c.equals(List.class)) return new ArrayList<>();
        if (c.equals(Map.class)) return new HashMap<>();


        return null;
    }

    private boolean isKnownType(Class<?> c) {
        return knownTypes.contains(c.getCanonicalName());
    }

    private static Cache<String, Object> exampleCache = CacheBuilder.newBuilder().maximumSize(200).build();

    private Object getPrimitiveValue(Class<?> c) {
        if (c.equals(String.class)) return "string";
        if (c.equals(int.class) || c.equals(Integer.class)) return 0;
        if (c.equals(double.class) || c.equals(Double.class)) return 0.0;
        if (c.equals(float.class) || c.equals(Float.class)) return 0.0f;
        if (c.equals(byte.class) || c.equals(Byte.class)) return (byte) 0;
        if (c.equals(short.class) || c.equals(Short.class)) return (short) 0;
        if (c.equals(long.class) || c.equals(Long.class)) return 0L;
        if (c.equals(boolean.class) || c.equals(Boolean.class)) return false;

        return null;
    }

    @Override
    public <T, E extends FilteringInformationHolder> FileResource exportBaseclassGeneric(ExportBaseclassGeneric<E> baseclassGeneric, SecurityContext securityContext) {
        FilteringInformationHolder filteringInformationHolder = baseclassGeneric.getFilter();
        Map<String, String> fieldToName = baseclassGeneric.getFieldToName();
        Map<String, Method> fieldNameToMethod = new HashMap<>();
        PaginationResponse<T> paginationResponse = listAllBaseclassGeneric(filteringInformationHolder, securityContext);
        List<T> collection = paginationResponse.getList();
        Collection<String> headers = fieldToName.values();
        String[] headersArr = new String[headers.size()];
        headers.toArray(headersArr);
        CSVFormat format = baseclassGeneric.getCsvFormat();
        File file = new File(com.flexicore.service.FileResourceService.generateNewPathForFileResource("dynamic-execution-csv", securityContext.getUser()) + ".csv");

        if (CSVFormat.EXCEL.equals(format)) {
            try (Writer out = new OutputStreamWriter(new FileOutputStream(file, true))) {
                out.write(ByteOrderMark.UTF_BOM);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "failed writing UTF-8 BOM", e);
            }


        }
        format = format.withHeader(headersArr);
        try (Writer out = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(out, format)) {
            DynamicInvokersService.exportCollection(fieldToName, fieldNameToMethod, csvPrinter, collection);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "failed exporting data", e);
        }


        FileResource fileResource = fileResourceService.createDontPersist(file.getAbsolutePath(), securityContext);
        fileResource.setKeepUntil(OffsetDateTime.now().plusMinutes(30));
        fileResourceService.merge(fileResource);
        return fileResource;


    }

    @Override
    public List<BaseclassCount> getBaseclassCount(BaseclassCountRequest baseclassCountRequest, SecurityContext securityContext) {
        return baseclassRepository.getBaseclassCount(baseclassCountRequest, securityContext);
    }

    public enum LinkSide {
        RIGHT, LEFT, NONE
    }

    @Override
    public void validate(GetDisconnected getDisconnected, SecurityContext securityContext) {
        if (getDisconnected.getWantedClassName() == null) {
            throw new BadRequestException("No Classname " + getDisconnected.getWantedClassName());
        }
        try {
            Class<? extends Baseclass> wantedClass = (Class<? extends Baseclass>) Class.forName(getDisconnected.getWantedClassName());
            getDisconnected.setWantedClass(wantedClass);
        } catch (ClassNotFoundException e) {
            throw new BadRequestException("No Class with name " + getDisconnected.getWantedClassName());
        }
        BaselinkFilter baselinkFilter = getDisconnected.getBaselinkFilter();
        if (baselinkFilter == null) {
            throw new BadRequestException("baselink filter must be non null");
        }
        if (baselinkFilter.getLinkClassName() == null) {
            throw new BadRequestException("Baselink filter class name must be non null");
        }
        baselinkService.validate(baselinkFilter, securityContext);
    }

    @Override
    public void validate(GetConnected getConnected, SecurityContext securityContext) {
        if (getConnected.getWantedClassName() == null) {
            throw new BadRequestException("No Classname " + getConnected.getWantedClassName());
        }
        try {
            Class<? extends Baseclass> wantedClass = (Class<? extends Baseclass>) Class.forName(getConnected.getWantedClassName());
            getConnected.setWantedClass(wantedClass);
        } catch (ClassNotFoundException e) {
            throw new BadRequestException("No Class with name " + getConnected.getWantedClassName());
        }
        if (getConnected.getBaselinkFilter() == null) {
            throw new BadRequestException("baselink filter must be non null");
        }
        if (getConnected.getBaselinkFilter().getLinkClassName() == null) {
            throw new BadRequestException("Baselink filter class name must be non null");
        }
        baselinkService.validate(getConnected.getBaselinkFilter(), securityContext);
    }


    @Override
    public <T extends Baselink, E extends Baseclass> PaginationResponse<E> getDisconnected(GetDisconnected getDisconnected, SecurityContext securityContext) {
        Class<E> type = (Class<E>) getDisconnected.getWantedClass();
        Class<T> linkclass = (Class<T>) getDisconnected.getBaselinkFilter().getLinkClass();
        Boolean right = onRight(type, linkclass);
        if (right == null) {
            throw new BadRequestException("could not find connection between " + type.getName() + " and " + linkclass.getName());
        }
        List<E> list = baselinkRepository.getDisconnected(getDisconnected, right, securityContext);
        long count = baselinkRepository.countDisconnected(getDisconnected, right, securityContext);
        ;
        return new PaginationResponse<>(list, getDisconnected, count);
    }

    @Override
    public <T extends Baselink, E extends Baseclass> PaginationResponse<E> getConnected(GetConnected getConnected, SecurityContext securityContext) {
        Class<E> type = (Class<E>) getConnected.getWantedClass();
        Class<T> linkclass = (Class<T>) getConnected.getBaselinkFilter().getLinkClass();
        Boolean right = onRight(type, linkclass);
        if (right == null) {
            throw new BadRequestException("could not find connection between " + type.getName() + " and " + linkclass.getName());
        }
        List<E> list = baselinkRepository.getConnected(getConnected, right, securityContext);
        long count = baselinkRepository.countConnected(getConnected, right, securityContext);
        return new PaginationResponse<>(list, getConnected, count);

    }

    @Override
    public <T extends Baselink> Boolean onRight(Class<?> wanted, Class<T> link) {
        String key = getOnRightKey(wanted, link);

        LinkSide res = sideCache.get(key);
        if (res == null) {

            int inheritenceDist = Integer.MAX_VALUE;
            List<Field> fields = FieldUtils.getFieldsListWithAnnotation(wanted, OneToMany.class);

            for (Field field : fields) {
                if (field.getType().equals(List.class)) {
                    OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                    if (oneToMany != null) {
                        Class<?> aClass = oneToMany.targetEntity();
                        if (aClass.isAssignableFrom(link)) {
                            int dist = calculateInheritanceDist(aClass, link);
                            if (dist < inheritenceDist) {
                                res = oneToMany.mappedBy().equalsIgnoreCase("rightside") ? LinkSide.RIGHT : LinkSide.LEFT;
                                inheritenceDist = dist;

                            }
                        }
                        if (link.isAssignableFrom(aClass)) {
                            int dist = calculateInheritanceDist(link, aClass);
                            if (dist < inheritenceDist) {
                                res = oneToMany.mappedBy().equalsIgnoreCase("rightside") ? LinkSide.RIGHT : LinkSide.LEFT;
                                inheritenceDist = dist;

                            }
                        }
                    }
                }

            }
            if (res == null) {
                res = LinkSide.NONE;
            }
            sideCache.put(key, res);
        }

        return LinkSide.NONE.equals(res) ? null : LinkSide.RIGHT.equals(res);
    }

    private String getOnRightKey(Class<?> wanted, Class<?> link) {
        return wanted.getCanonicalName() + "-" + link.getCanonicalName();
    }

    private int calculateInheritanceDist(Class<?> parent, Class<?> child) {
        int i = 0;
        for (Class<?> current = child; current != null; current = current.getSuperclass()) {
            if (current.equals(parent)) {
                return i;
            }
            i++;
        }
        return Integer.MAX_VALUE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Baselink> List<Baseclass> getDisconnected(Clazz c, String id, Clazz linkClazz,
                                                                FilteringInformationHolder filteringInformationHolder, int pagesize, int currentPage, Baseclass value, String simpleValue, SecurityContext securityContext) throws Exception {
        List<Baseclass> list;
        Class<?> type = Class.forName(c.getName());
        Class<T> linkclass = (Class<T>) Class.forName(linkClazz.getName());
        Boolean right = onRight(type, linkclass);
        if (right == null) {
            throw new Exception("could not find connection between " + c.getName() + " and " + linkClazz.getName());
        }
        Baseclass base = baseclassRepository.getById(id, Baseclass.class, null, securityContext);

        list = null;//baselinkRepository.getdisconnectedBaseLinksBaseClassesBySide(linkclass, c, base, right, filteringInformationHolder, pagesize, currentPage, value, simpleValue, securityContext);


        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Baselink> long countDisconnected(Clazz c, String id, Clazz linkClazz,
                                                       FilteringInformationHolder filteringInformationHolder, int pagesize, int currentPage, Baseclass value, String simpleValue, SecurityContext securityContext) throws Exception {
        Class<?> type = Class.forName(c.getName());
        Class<T> linkclass = (Class<T>) Class.forName(linkClazz.getName());
        Boolean right = onRight(type, linkclass);
        if (right == null) {
            throw new Exception("could not find connection between " + c.getName() + " and " + linkClazz.getName());
        }
        Baseclass base = baseclassRepository.getById(id, Baseclass.class, null, securityContext);

        return 0;//baselinkRepository.countDisconnectedBaseLinksBaseClassesBySide(linkclass, c, base, right, filteringInformationHolder, pagesize, currentPage, value, simpleValue, securityContext);


    }


    @Override
    @SuppressWarnings("unchecked")
    public <T extends Baselink, E extends Baseclass> List<E> getConnected(Clazz c, String id, Clazz linkClazz,
                                                                          FilteringInformationHolder filteringInformationHolder, int pagesize, int currentPage, Baseclass value, String simpleValue, SecurityContext securityContext) {
        try {
            ;
            Class<E> type = (Class<E>) Class.forName(c.getName());
            Class<T> linkclass = (Class<T>) Class.forName(linkClazz.getName());
            Boolean right = onRight(type, linkclass);
            if (right == null) {
                throw new BadRequestException("could not find connection between " + c.getName() + " and " + linkClazz.getName());
            }
            Baseclass base = baseclassRepository.getByIdOrNull(id, Baseclass.class, null, securityContext);
            if (base == null) {
                throw new BadRequestException("No Baseclass with id " + id);
            }

            return null;//baselinkRepository.getBaseLinksBaseClassesBySide(linkclass, type, base, right, filteringInformationHolder, pagesize, currentPage, value, simpleValue, securityContext);
        } catch (ClassNotFoundException e) {
            throw new BadRequestException("could not find class ", e);
        }


    }


    @Override
    @SuppressWarnings("unchecked")
    public <T extends Baselink, E extends Baseclass> long countConnected(Clazz c, String id, Clazz linkClazz,
                                                                         FilteringInformationHolder filteringInformationHolder, int pagesize, int currentPage, Baseclass value, String simpleValue, SecurityContext securityContext) {
        try {
            List<Baseclass> list = new ArrayList<>();
            Class<E> type = (Class<E>) Class.forName(c.getName());
            Class<T> linkclass = (Class<T>) Class.forName(linkClazz.getName());
            Boolean right = onRight(type, linkclass);
            if (right == null) {
                throw new BadRequestException("could not find connection between " + c.getName() + " and " + linkClazz.getName());
            }
            Baseclass base = baseclassRepository.getByIdOrNull(id, Baseclass.class, null, securityContext);
            if (base == null) {
                throw new BadRequestException("No Baseclass with id " + id);
            }

            return 0;//baselinkRepository.countBaseLinksBaseClassesBySide(linkclass, type, base, right, filteringInformationHolder, pagesize, currentPage, value, simpleValue, securityContext);

        } catch (ClassNotFoundException e) {
            throw new BadRequestException("could not find class ", e);
        }

    }

    @Override
    public boolean updateInfo(String id, String name, String description, SecurityContext securityContext) {
        Baseclass b = baseclassRepository.getById(id, Baseclass.class, null, securityContext);
        boolean changed = false;
        if (b != null) {
            if (name != null && name.length() < 51 && name.length() > 0) {
                b.setName(name);
                changed = true;
            }

            if (description != null) {
                b.setDescription(description);
                changed = true;
            }
            if (changed) {
                b.setUpdateDate(OffsetDateTime.now());
                baseclassRepository.merge(b);
                return true;
            }
        }

        return false;
    }


    @Override
    public <T extends Baseclass> List<T> getByNameLike(String name, Class<T> c, List<String> batchString,
                                                       SecurityContext securityContext) {
        return baseclassRepository.getByNameLike(name, c, batchString, securityContext);
    }

    @Override
    public <T extends Baseclass> List<T> getAllUnsecure(Class<T> c) {
        return baseclassRepository.getAll(c);
    }


    @Override
    public void merge(Object base) {
        baseclassRepository.merge(base);
    }

    @Override
    public void softDelete(Baseclass baseclass, SecurityContext securityContext) {
        baseclass.setSoftDelete(true);
        baseclassRepository.merge(baseclass);

    }

    @Override
    public long setBaseclassTenant(SetBaseclassTenantRequest setBaseclassTenantRequest, SecurityContext securityContext) {
        List<Object> toMerge = new ArrayList<>();
        Tenant targetTenant = setBaseclassTenantRequest.getTenant();
        for (Baseclass baseclass : setBaseclassTenantRequest.getBaseclasses()) {
            if (baseclass.getTenant() == null || !baseclass.getTenant().getId().equals(targetTenant.getId())) {
                baseclass.setTenant(targetTenant);
                toMerge.add(baseclass);

            }
        }
        baseclassRepository.massMerge(toMerge);
        return toMerge.size();
    }

    @Override
    public void refrehEntityManager() {
        baseclassRepository.refrehEntityManager();
        baselinkRepository.refrehEntityManager();
        fileResourceService.refrehEntityManager();
    }

    @Override
    public <T, E extends FilteringInformationHolder> PaginationResponse<T> listAllBaseclassGeneric(E filteringInformationHolder, SecurityContext securityContext) {

        List<ListingInvoker> plugins = pluginManager.getExtensions(ListingInvoker.class);
        String msg;
        for (ListingInvoker<?, ?> plugin : plugins) {
            if (plugin.getFilterClass().equals(filteringInformationHolder.getClass()) && plugin.getHandlingClass().getCanonicalName().equals(filteringInformationHolder.getResultType())) {
                try {
                    Method method = plugin.getClass().getDeclaredMethod("listAll", plugin.getFilterClass(), SecurityContext.class);
                    String operationId = Baseclass.generateUUIDFromString(method.toString());
                    Operation operation = operationService.findById(operationId);
                    securityContext.setOperation(operation);
                    if (securityService.checkIfAllowed(securityContext)) {
                        ListingInvoker<T, E> invoker = (ListingInvoker<T, E>) plugin;
                        return invoker.listAll(filteringInformationHolder, securityContext);
                    } else {
                        throw new NotAuthorizedException("user is not authorized for this resource");
                    }


                } catch (NoSuchMethodException e) {
                    logger.log(Level.SEVERE, "unable to get method", e);
                }

            }
        }
        msg = "no invoker matches  " + filteringInformationHolder.getResultType() + " with filter type " + filteringInformationHolder.getClass();
        logger.log(Level.SEVERE, msg);

        throw new BadRequestException(msg);


    }


}

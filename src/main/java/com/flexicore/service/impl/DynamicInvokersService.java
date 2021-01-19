/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import com.flexicore.data.DynamicInvokersRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.Syncable;
import com.flexicore.interfaces.dynamic.Invoker;
import com.flexicore.model.Baseclass;
import com.flexicore.model.FileResource;
import com.flexicore.model.Operation;
import com.flexicore.model.dynamic.*;
import com.flexicore.request.*;
import com.flexicore.response.*;
import com.flexicore.security.SecurityContext;
import com.flexicore.utils.InheritanceUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.lang3.StringUtils;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Primary
@Component
public class DynamicInvokersService implements com.flexicore.service.DynamicInvokersService {

    private static List<InvokerInfo> equipmentHandlersListingCache = null;

    @Lazy
    @Autowired
    private PluginManager pluginManager;

    @Autowired
    private DynamicInvokersRepository dynamicInvokersRepository;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private FileResourceService fileResourceService;



    private static final Logger logger = LoggerFactory.getLogger(DynamicInvokersService.class);

    @Override
    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString, SecurityContext securityContext) {
        return dynamicInvokersRepository.getByIdOrNull(id, c, batchString, securityContext);
    }

    @Override
    public <T extends Baseclass> List<T> listByIds(Class<T> c, Set<String> ids, SecurityContext securityContext) {
        return dynamicInvokersRepository.listByIds(c, ids, securityContext);
    }

    @Override
    public PaginationResponse<InvokerInfo> getAllInvokersInfo(InvokersFilter invokersFilter, SecurityContext securityContext) {
        PaginationResponse<DynamicInvoker> response = getAllInvokers(invokersFilter, securityContext);
        List<Operation> ops = getInvokerOperations(new InvokersOperationFilter().setInvokers(response.getList()), securityContext);

        Map<String, InvokerInfo> invokerInfoMap = getInvokers(ops.parallelStream().map(f -> f.getId()).collect(Collectors.toSet())).parallelStream().collect(Collectors.toMap(f -> f.getName().getCanonicalName(), f -> f));
        List<InvokerInfo> list = response.getList().parallelStream().map(f -> invokerInfoMap.getOrDefault(f.getCanonicalName(), new InvokerInfo(f))).collect(Collectors.toList());

        return new PaginationResponse<>(list, invokersFilter, response.getTotalRecords());
    }

    @Override
    public PaginationResponse<DynamicInvoker> getAllInvokers(InvokersFilter invokersFilter, SecurityContext securityContext) {
        List<DynamicInvoker> list = dynamicInvokersRepository.getAllInvokers(invokersFilter, securityContext);
        long count = dynamicInvokersRepository.countAllInvokers(invokersFilter, securityContext);
        return new PaginationResponse<>(list, invokersFilter, count);
    }

    private boolean filterInvokers(InvokerInfo InvokerInfo, InvokersFilter filter) {
        return (filter.getInvokerTypes() == null || filter.getInvokerTypes().contains(InvokerInfo.getName().getCanonicalName()))
                && (filter.getClassTypes() == null || classIsAllOf(filter.getClassTypes(), InvokerInfo.getHandlingType()));
    }

    private boolean classIsAllOf(Set<Class<?>> set, Class<?> c) {
        for (Class<?> aClass : set) {
            if (!c.isAssignableFrom(aClass)) {
                return false;
            }
        }
        return true;
    }

    public List<InvokerInfo> getInvokers(Set<String> allowedOps) {
        if (equipmentHandlersListingCache == null) {
            Collection<Invoker> plugins = pluginManager.getExtensions(Invoker.class);
            equipmentHandlersListingCache = plugins.parallelStream().map(f -> new InvokerInfo(f)).sorted(Comparator.comparing(InvokerInfo::getDisplayName)).collect(Collectors.toList());
        }
        return equipmentHandlersListingCache.parallelStream().map(f -> new InvokerInfo(f, allowedOps)).collect(Collectors.toList());


    }

    public ExecuteInvokersResponse executeInvoker(DynamicExecution dynamicExecution, SecurityContext securityContext) {
        return executeInvoker(dynamicExecution, null, securityContext);

    }


    public ExecuteInvokersResponse executeInvoker(DynamicExecution dynamicExecution, ExecutionContext executionContext, SecurityContext securityContext) {
        ExecuteInvokerRequest executeInvokerRequest = getExecuteInvokerRequest(dynamicExecution, executionContext, securityContext);
        return executeInvoker(executeInvokerRequest, securityContext);

    }


    @Override
    public ExecuteInvokersResponse executeInvoker(ExecuteInvokerRequest executeInvokerRequest, SecurityContext securityContext) {
        Collection<Invoker> plugins = pluginManager.getExtensions(Invoker.class);
        Map<String, Invoker> invokerMap = plugins.parallelStream().collect(Collectors.toMap(f -> f.getClass().getCanonicalName(), f -> f, (a, b) -> a));
        Map<String, DynamicInvoker> dynamicInvokerMap = getAllInvokers(new InvokersFilter().setInvokerTypes(invokerMap.keySet()), null).getList().parallelStream().collect(Collectors.toMap(f -> f.getCanonicalName(), f -> f));
        Map<String, Map<String, Operation>> invokersOperations = getInvokerOperations(new InvokersOperationFilter().setInvokers(new ArrayList<>(dynamicInvokerMap.values())), null)
                .parallelStream().collect(Collectors.groupingBy(f -> f.getDynamicInvoker().getCanonicalName(), Collectors.toMap(f -> f.getId(), f -> f)));
        List<ExecuteInvokerResponse<?>> responses = new ArrayList<>();
        Object executionParametersHolder = executeInvokerRequest.getExecutionParametersHolder();
        if (executionParametersHolder instanceof ExecutionParametersHolder) {
            ExecutionParametersHolder executionParametersHolderActual = (ExecutionParametersHolder) executionParametersHolder;
            executionParametersHolderActual.setSecurityContext(securityContext);

        }
        ExecutionContext executionContext = executeInvokerRequest.getExecutionContext();

        for (String invokerName : executeInvokerRequest.getInvokerNames()) {
            Map<String, Operation> operationMap = invokersOperations.get(invokerName);
            if (operationMap == null) {
                logger.error( "invoker " + invokerName + " has no registered operations - will not execute");
                continue;
            }
            try {
                Invoker invoker = invokerMap.get(invokerName);
                if (invoker == null) {
                    String msg = "No Handler " + invokerName;
                    logger.error(msg);
                    responses.add(new ExecuteInvokerResponse<>(invokerName, false, msg));
                    continue;
                }
                Class<? extends Invoker> clazz = invoker.getClass();

                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (method.isBridge()) {
                        continue;
                    }
                    Class<?>[] parameterTypes = method.getParameterTypes();

                    if (method.getName().equals(executeInvokerRequest.getInvokerMethodName()) && parameterTypes.length > 0 && parameterTypes[0].isAssignableFrom(executionParametersHolder.getClass())) {

                        String operationId = Baseclass.generateUUIDFromString(method.toString());
                        Operation operation = operationMap.get(operationId);
                        if (operation == null) {
                            logger.error( "invoker " + invokerName + " has no registered operation with id " + operationId + " - will not execute");
                            break;
                        }
                        securityContext.setOperation(operation);

                        if (securityService.checkIfAllowed(securityContext)) {
                            long start = System.currentTimeMillis();
                            Object[] parameters = new Object[parameterTypes.length];
                            parameters[0] = executionParametersHolder;
                            for (int i = 1; i < parameterTypes.length; i++) {
                                Class<?> parameterType = parameterTypes[i];
                                if (SecurityContext.class.equals(parameterType)) {
                                    parameters[i] = securityContext;
                                }
                                if (executionContext != null && parameterType.isAssignableFrom(executionContext.getClass())) {
                                    parameters[i] = executionContext;
                                }
                            }
                            Object ret = method.invoke(invoker, parameters);
                            ExecuteInvokerResponse<?> e=null;
                            if(ret instanceof PaginationResponse){
                                PaginationResponse<?> paginationResponse= (PaginationResponse<?>) ret;
                                if(!paginationResponse.getList().isEmpty()){
                                    Object o=paginationResponse.getList().get(0);
                                    if(o instanceof Syncable){
                                        e=new ExecuteInvokerResponseSyncable(invokerName,true, (PaginationResponse<? extends Syncable>) paginationResponse);
                                    }
                                 }
                            }

                            //auditingService.addAuditingJob(new AuditingJob(securityContext,null,ret,System.currentTimeMillis()-start, Date.from(Instant.now()), DefaultAuditingTypes.REST.name()));
                            if(e==null){
                                e= new ExecuteInvokerResponse<>(invokerName, true, ret);
                            }
                            responses.add(e);
                            break;

                        } else {
                            throw new NotAuthorizedException("user is not authorized for this resource");
                        }

                    }
                }
            } catch (Exception e) {
                logger.error( "failed executing " + invokerName, e);
                responses.add(new ExecuteInvokerResponse<>(invokerName, false, e));
            }

        }


        return new ExecuteInvokersResponse(responses);


    }

    @Override
    @Transactional
    public void massMerge(List<?> toMerge) {
        dynamicInvokersRepository.massMerge(toMerge);
    }

    @Override
    public ExecuteInvokerRequest getExecuteInvokerRequest(DynamicExecution dynamicExecution, SecurityContext securityContext) {
        return getExecuteInvokerRequest(dynamicExecution, null, securityContext);
    }


    @Override
    public ExecuteInvokerRequest getExecuteInvokerRequest(DynamicExecution dynamicExecution, ExecutionContext executionContext, SecurityContext securityContext) {
        Set<String> invokerNames = dynamicInvokersRepository.getAllServiceCanonicalNames(dynamicExecution).parallelStream().map(f -> f.getServiceCanonicalName()).collect(Collectors.toSet());
        return new ExecuteInvokerRequest()
                .setLastExecuted(dynamicExecution.getLastExecuted())
                .setInvokerNames(invokerNames)
                .setInvokerMethodName(dynamicExecution.getMethodName())
                .setExecutionContext(executionContext)
                .setExecutionParametersHolder(dynamicExecution.getExecutionParametersHolder() != null ? dynamicExecution.getExecutionParametersHolder().setSecurityContext(securityContext) : null);
    }

    public DynamicInvoker createInvokerNoMerge(CreateInvokerRequest createInvokerRequest, SecurityContext securityContext) {
        DynamicInvoker dynamicInvoker = new DynamicInvoker(createInvokerRequest.getDisplayName(), securityContext);
        if (createInvokerRequest.getCanonicalName() != null) {
            dynamicInvoker.setId(getDynamicInvokerId(createInvokerRequest.getCanonicalName()));
        }
        updateInvokerNoMerge(createInvokerRequest, dynamicInvoker);
        return dynamicInvoker;
    }

    @Override
    public DynamicExecution createDynamicExecutionNoMerge(CreateDynamicExecution createInvokerRequest, List<Object> toMerge, SecurityContext securityContext) {
        DynamicExecution dynamicExecution = new DynamicExecution(createInvokerRequest.getName(), securityContext);
        updateDynamicExecutionNoMerge(createInvokerRequest, dynamicExecution, toMerge);
        return dynamicExecution;

    }

    @Override
    public DynamicExecution createDynamicExecution(CreateDynamicExecution createInvokerRequest, SecurityContext securityContext) {
        List<Object> toMerge = new ArrayList<>();
        DynamicExecution dynamicExecution = createDynamicExecutionNoMerge(createInvokerRequest, toMerge, securityContext);
        toMerge.add(dynamicExecution);
        dynamicInvokersRepository.massMerge(toMerge);
        dynamicInvokersRepository.flush();
        return dynamicExecution;
    }

    @Override
    public DynamicExecution updateDynamicExecution(UpdateDynamicExecution updateDynamicExecution, SecurityContext securityContext) {
        DynamicExecution dynamicExecution = updateDynamicExecution.getDynamicExecution();
        List<Object> toMerge = new ArrayList<>();
        if (updateDynamicExecutionNoMerge(updateDynamicExecution, dynamicExecution, toMerge)) {
            toMerge.add(dynamicExecution);
            dynamicInvokersRepository.massMerge(toMerge);
        }
        return dynamicExecution;
    }

    @Override
    public boolean updateDynamicExecutionNoMerge(CreateDynamicExecution createDynamicExecution, DynamicExecution dynamicExecution, List<Object> toMerge) {
        boolean update = false;
        if (createDynamicExecution.getMethodName() != null && !createDynamicExecution.getMethodName().equals(dynamicExecution.getMethodName())) {
            dynamicExecution.setMethodName(createDynamicExecution.getMethodName());
            update = true;
        }

        if (createDynamicExecution.getName() != null && !createDynamicExecution.getName().equals(dynamicExecution.getName())) {
            dynamicExecution.setName(createDynamicExecution.getName());
            update = true;
        }

        if (createDynamicExecution.getDescription() != null && !createDynamicExecution.getDescription().equals(dynamicExecution.getDescription())) {
            dynamicExecution.setDescription(createDynamicExecution.getDescription());
            update = true;
        }


        if (createDynamicExecution.getServiceCanonicalNames() != null) {
            Set<String> ids = dynamicInvokersRepository.getAllServiceCanonicalNames(dynamicExecution).parallelStream().map(f -> f.getServiceCanonicalName()).collect(Collectors.toSet());
            createDynamicExecution.getServiceCanonicalNames().removeAll(ids);
            if (!createDynamicExecution.getServiceCanonicalNames().isEmpty()) {
                List<ServiceCanonicalName> list = createDynamicExecution.getServiceCanonicalNames().parallelStream()
                        .map(f -> new ServiceCanonicalName()
                                .setId(Baseclass.getBase64ID())
                                .setDynamicExecution(dynamicExecution)
                                .setServiceCanonicalName(f)
                        )
                        .collect(Collectors.toList());
                dynamicExecution.getServiceCanonicalNames().addAll(list);
                toMerge.addAll(list);
                update = true;
            }


        }
        if (createDynamicExecution.getExecutionParametersHolder() != null) {
            createDynamicExecution.getExecutionParametersHolder().prepareForSave();
            dynamicExecution.setExecutionParametersHolder(createDynamicExecution.getExecutionParametersHolder());
            update = true;

        }
        return update;
    }

    private String getDynamicInvokerId(String canonicalName) {
        return Baseclass.generateUUIDFromString("DynamicInvoker-" + canonicalName);
    }

    public List<Operation> getInvokerOperations(InvokersOperationFilter invokersOperationFilter, SecurityContext securityContext) {
        return dynamicInvokersRepository.getInvokersOperations(invokersOperationFilter, securityContext);
    }

    public PaginationResponse<Operation> getInvokerOperationsPagination(InvokersOperationFilter invokersOperationFilter, SecurityContext securityContext) {
        List<Operation> list = dynamicInvokersRepository.getInvokersOperations(invokersOperationFilter, securityContext);
        long count = dynamicInvokersRepository.countInvokersOperations(invokersOperationFilter, securityContext);
        return new PaginationResponse<>(list, invokersOperationFilter, count);
    }

    public boolean updateInvokerNoMerge(CreateInvokerRequest createInvokerRequest, DynamicInvoker invoker) {
        boolean update = false;
        if (createInvokerRequest.getCanonicalName() != null && !createInvokerRequest.getCanonicalName().equals(invoker.getCanonicalName())) {
            invoker.setCanonicalName(createInvokerRequest.getCanonicalName());
            update = true;
        }
        if (createInvokerRequest.getDisplayName() != null && !createInvokerRequest.getDisplayName().equals(invoker.getName())) {
            invoker.setName(createInvokerRequest.getDisplayName());
            update = true;
        }
        if (createInvokerRequest.getDescription() != null && !createInvokerRequest.getDescription().equals(invoker.getDescription())) {
            invoker.setDescription(createInvokerRequest.getDescription());
            update = true;
        }
        if (!invoker.isSystemObject()) {
            invoker.setSystemObject(true);
            update = true;
        }
        return update;
    }

    @Override
    public void validate(DynamicExecutionFilter dynamicExecutionFilter, SecurityContext securityContext) {
        String executionParameterHolderCanonicalName = dynamicExecutionFilter.getExecutionParameterHolderCanonicalName();
        if (executionParameterHolderCanonicalName != null) {
            try {
                Class<? extends ExecutionParametersHolder> c = (Class<? extends ExecutionParametersHolder>) Class.forName(executionParameterHolderCanonicalName);
                dynamicExecutionFilter.setExecutionParameterHolderType(c);
            } catch (Throwable e) {
                logger.error( "cannot find execution parameter holder type " + executionParameterHolderCanonicalName);
            }
        }

    }

    @Override
    public void validate(ExecuteDynamicExecution executeDynamicExecution, SecurityContext securityContext) {

        String id = executeDynamicExecution.getDynamicExecutionId();
        DynamicExecution dynamicExecution = id != null ? dynamicInvokersRepository.getByIdOrNull(id, DynamicExecution.class, null, securityContext) : null;
        if (dynamicExecution == null) {
            throw new BadRequestException("No Dynamic Execution with id " + id);
        }
        executeDynamicExecution.setDynamicExecution(dynamicExecution);

        if (dynamicExecution.getMethodName() == null || dynamicExecution.getMethodName().isEmpty()) {
            throw new BadRequestException("Dynamic Execution Method name must be non null and not empty");
        }

        Set<String> invokerNames = dynamicInvokersRepository.getAllServiceCanonicalNames(dynamicExecution).parallelStream().map(f -> f.getServiceCanonicalName()).collect(Collectors.toSet());
        if (invokerNames.isEmpty()) {
            throw new BadRequestException("Dynamic Execution must have at least a single invoker canonical name");
        }

    }

    @Override
    public PaginationResponse<DynamicExecution> getAllDynamicExecutions(DynamicExecutionFilter dynamicExecutionFilter, SecurityContext securityContext) {
        List<DynamicExecution> list = dynamicInvokersRepository.listAllDynamicExecutions(dynamicExecutionFilter, securityContext);
        long count = dynamicInvokersRepository.countAllDynamicExecutions(dynamicExecutionFilter, securityContext);
        return new PaginationResponse<>(list, dynamicExecutionFilter, count);
    }

    @Override
    public ExecuteInvokersResponse executeInvoker(ExecuteDynamicExecution executeDynamicExecution, SecurityContext securityContext) {
        DynamicExecution dynamicExecution = executeDynamicExecution.getDynamicExecution();
        ExecuteInvokersResponse executeInvokersResponse = executeInvoker(dynamicExecution, securityContext);
        dynamicExecution.setLastExecuted(OffsetDateTime.now());
        dynamicInvokersRepository.merge(dynamicExecution);
        return executeInvokersResponse;
    }

    @Override
    public FileResource exportDynamicExecutionResultToCSV(ExportDynamicExecution exportDynamicExecution, SecurityContext securityContext) {
        ExecuteInvokersResponse executeInvokersResponse = executeInvoker(exportDynamicExecution, securityContext);

        File file = new File(com.flexicore.service.FileResourceService.generateNewPathForFileResource("dynamic-execution-csv", securityContext.getUser()) + ".csv");
        Map<String, String> fieldToName = exportDynamicExecution.getFieldToName();
        Collection<String> headers = fieldToName.values();
        String[] headersArr = new String[headers.size()];
        headers.toArray(headersArr);
        CSVFormat format = exportDynamicExecution.getCsvFormat().withHeader(headersArr);
        Map<String, Method> fieldNameToMethod = new HashMap<>();
        if (CSVFormat.EXCEL.equals(format)) {
            try (Writer out = new OutputStreamWriter(new FileOutputStream(file, true))) {
                out.write(ByteOrderMark.UTF_BOM);

            } catch (Exception e) {
                logger.error( "failed writing UTF-8 BOM", e);
            }


        }
        try (Writer out = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(out, format)) {

            for (ExecuteInvokerResponse<?> respons : executeInvokersResponse.getResponses()) {
                if (respons.getResponse() instanceof Collection) {
                    Collection<?> collection = (Collection<?>) respons.getResponse();
                    exportCollection(fieldToName, fieldNameToMethod, csvPrinter, collection);

                }
            }
            csvPrinter.flush();
        } catch (Exception e) {
            logger.error( "unable to create csv");
        }
        FileResource fileResource = fileResourceService.createDontPersist(file.getAbsolutePath(), securityContext);
        fileResource.setKeepUntil(OffsetDateTime.now().plusMinutes(30));
        fileResourceService.merge(fileResource);
        return fileResource;


    }

    public static void exportCollection(Map<String, String> fieldToName, Map<String, Method> fieldNameToMethod, CSVPrinter csvPrinter, Collection<?> collection) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
        if (!collection.isEmpty()) {
            Class<?> c = collection.iterator().next().getClass();
            List<Object> list = new ArrayList<>();
            Set<String> failed = new HashSet<>();

            for (Object o : collection) {
                for (String field : fieldToName.keySet()) {
                    String[] split = field.split("\\.");
                    String canonical = "";
                    Object current = o;
                    Class<?> currentClass = c;
                    for (String s : split) {
                        if (failed.contains(s)) {
                            list.add("");
                            continue;
                        }
                        canonical += s;
                        Method method = fieldNameToMethod.get(canonical);
                        if (method == null) {
                            try {
                                method = currentClass.getMethod("get" + StringUtils.capitalize(s));
                            } catch (Exception ignored) {
                            }
                            if (method == null) {
                                try {
                                    method = currentClass.getMethod("is" + StringUtils.capitalize(s));
                                } catch (Exception ignored) {
                                    failed.add(s);
                                    list.add("");
                                    continue;
                                }
                            }

                            fieldNameToMethod.put(canonical, method);
                        }
                        Object data = method.invoke(current);
                        if (data == null) {
                            current = null;
                            break;
                        }
                        current = data;
                        currentClass = current.getClass();
                    }
                    list.add(current);


                }
                csvPrinter.printRecord(list);
                list.clear();

            }
        }
    }

    @Override
    public void validateExportDynamicExecution(ExportDynamicExecution exportDynamicExecution, SecurityContext securityContext) {
        validate(exportDynamicExecution, securityContext);
        if (exportDynamicExecution.getFieldToName() == null || exportDynamicExecution.getFieldToName().isEmpty()) {
            throw new BadRequestException("Field to name map must be non null and not empty");
        }
        if (exportDynamicExecution.getCsvFormat() == null) {
            exportDynamicExecution.setCsvFormat(CSVFormat.EXCEL);
        }
    }

    @Override
    public void validate(DynamicExecutionExampleRequest dynamicExecutionExampleRequest, SecurityContext securityContext) {
        String id = dynamicExecutionExampleRequest.getId();
        DynamicExecution dynamicExecution = id != null ? getByIdOrNull(id, DynamicExecution.class, null, securityContext) : null;
        if (dynamicExecution == null) {
            throw new BadRequestException("No DynamicExectuion with id " + id);
        }
        String methodName = dynamicExecution.getMethodName();
        if (methodName == null || methodName.isEmpty()) {
            throw new BadRequestException("No method name for dynamic execution");
        }
        List<ServiceCanonicalName> serviceCanonicalNames = dynamicInvokersRepository.getAllServiceCanonicalNames(dynamicExecution);
        if (serviceCanonicalNames.isEmpty()) {
            throw new BadRequestException("No Invoker Canonical Names with dynamic execution " + id);
        }

        Set<String> invokerNames = serviceCanonicalNames.parallelStream().map(f -> f.getServiceCanonicalName()).collect(Collectors.toSet());
        List<InvokerInfo> list = getAllInvokersInfo(new InvokersFilter().setInvokerTypes(invokerNames), null).getList();
        Set<String> returnTypes = new HashSet<>();
        for (InvokerInfo invokerInfo : list) {
            for (InvokerMethodInfo method : invokerInfo.getMethods()) {
                if (methodName.equals(method.getName())) {
                    String returnType;
                    if(method.getListType()!=null){
                        returnType=method.getListType().getCanonicalName();
                    }
                    else{
                        if(PaginationResponse.class.getCanonicalName().equals(method.getReturnType())){
                            returnType=invokerInfo.getHandlingType().getCanonicalName();
                        }
                        else{
                            returnType=method.getReturnType();
                        }
                    }
                    returnTypes.add(returnType);
                }
            }
        }
        if (returnTypes.isEmpty()) {
            throw new BadRequestException("No method " + methodName + " for invokers " + invokerNames);
        }
        String returnType = null;
        long inheritingClasses = 0;
        for (String inspectedReturnType : returnTypes) {

            long inheriting = InheritanceUtils.listInheritingClassesWithFilter(new GetClassInfo().setClassName(inspectedReturnType)).getTotalRecords();
            if (returnType == null || inheritingClasses < inheriting) {
                returnType = inspectedReturnType;
                inheritingClasses = inheriting;
            }
        }
        dynamicExecutionExampleRequest.setClassName(returnType);

    }
}

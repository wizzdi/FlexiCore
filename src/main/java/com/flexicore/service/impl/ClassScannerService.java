package com.flexicore.service.impl;

import com.flexicore.annotations.AnnotatedClazz;
import com.flexicore.annotations.AnnotatedClazzWithName;
import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.OperationsInside;
import com.flexicore.annotations.rest.*;
import com.flexicore.data.BaselinkRepository;
import com.flexicore.data.ClazzRegistration;
import com.flexicore.data.ClazzRepository;
import com.flexicore.interfaces.dynamic.InvokerInfo;
import com.flexicore.interfaces.dynamic.InvokerMethodInfo;
import com.flexicore.model.*;
import com.flexicore.model.dynamic.DynamicInvoker;
import com.flexicore.provider.EntitiesHolder;
import com.flexicore.request.*;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.PasswordGenerator;
import com.flexicore.utils.InheritanceUtils;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.reflections.Reflections;
import org.reflections.serializers.JsonSerializer;
import org.reflections.util.FilterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.ManyToOne;
import java.util.stream.Collectors;

@Primary
@Component
public class ClassScannerService {

    private static final String DEFAULT_TENANT_ID = "jgV8M9d0Qd6owkPPFrbWIQ";
    private static final String TENANT_TO_USER_ID = "Xk5siBx+TyWv+G6V+XuSdw";
    private static final String SUPER_ADMIN_ROLE_ID = "HzFnw-nVR0Olq6WBvwKcQg";
    private static final String SUPER_ADMIN_TO_ADMIN_ID = "EbVFgr+YS3ezYUblzceVGA";
    @Autowired
    OperationService operationService;
    @Autowired
    ClazzRegistration clazzRegistration;
    @Autowired
    ClazzRepository clazzrepository;

    @Autowired
    BaselinkRepository baselinkrepository;
    private static final Logger logger = LoggerFactory.getLogger(ClassScannerService.class);

    @Autowired
    private SecurityService securityService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserService userService;
    @Autowired
    private DynamicInvokersService dynamicInvokersService;
    @Autowired
    private EntitiesHolder entitiesHolder;

    private Reflections reflections;
    @Value("${flexicore.users.firstRunPath:/home/flexicore/firstRun.txt}")
    private String firstRunFilePath;
    @Value("${flexicore.users.adminEmail:admin@flexicore.com}")
    private String adminEmail;
    @Autowired
    @Qualifier("systemAdminId")
    private String systemAdminId;


    public void initializeInvokers() {
        SecurityContext securityContext = securityService.getAdminUserSecurityContext();
        Map<String, DynamicInvoker> invokersMap = dynamicInvokersService.getAllInvokers(new InvokersFilter(), securityContext).getList()
                .parallelStream().collect(Collectors.toMap(f -> f.getCanonicalName(), f -> f));
        InvokersOperationFilter invokersOperationFilter = new InvokersOperationFilter()
                .setInvokers(new ArrayList<>(invokersMap.values()));
        List<Operation> invokerOperations = invokersMap.isEmpty() ? new ArrayList<>() : dynamicInvokersService.getInvokerOperations(invokersOperationFilter, securityContext);
        Map<String, Map<String, Operation>> operationsForInvokers = invokerOperations
                .parallelStream().filter(f -> f.getDynamicInvoker() != null).collect(Collectors.groupingBy(f -> f.getDynamicInvoker().getId(), Collectors.toMap(f -> f.getId(), f -> f)));
        Set<String> operationIds = invokerOperations.parallelStream().map(f -> f.getId()).collect(Collectors.toSet());
        Map<String, OperationToClazz> relatedClazzMap = operationService.getRelatedClasses(operationIds).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));

        initReflections();
        Set<Class<?>> invokers = reflections.getTypesAnnotatedWith(InvokerInfo.class, true);
        List<Object> toMergeInvokers = new ArrayList<>();
        List<Object> toMergeOperations = new ArrayList<>();

        for (Class<?> invoker : invokers) {
            registerInvoker(invoker, invokersMap, operationsForInvokers, relatedClazzMap, toMergeInvokers, toMergeOperations, securityContext);
        }
        dynamicInvokersService.massMerge(toMergeInvokers);
        operationService.massMerge(toMergeOperations);

    }

    public void registerInvoker(Class<?> invoker, SecurityContext securityContext) {
        List<Object> toMergeInvokers = new ArrayList<>();
        List<Object> toMergeOperations = new ArrayList<>();
        Map<String, DynamicInvoker> invokersMap = dynamicInvokersService.getAllInvokers(new InvokersFilter(), securityContext).getList()
                .parallelStream().collect(Collectors.toMap(f -> f.getCanonicalName(), f -> f));
        InvokersOperationFilter invokersOperationFilter = new InvokersOperationFilter()
                .setInvokers(new ArrayList<>(invokersMap.values()));
        List<Operation> invokerOperations = dynamicInvokersService.getInvokerOperations(invokersOperationFilter, securityContext);
        Map<String, Map<String, Operation>> invokerOperationsMap = invokerOperations
                .parallelStream().filter(f -> f.getDynamicInvoker() != null).collect(Collectors.groupingBy(f -> f.getDynamicInvoker().getId(), Collectors.toMap(f -> f.getId(), f -> f)));
        Map<String, OperationToClazz> relatedClazzMap = operationService.getRelatedClasses(invokerOperations.parallelStream().map(f -> f.getId()).collect(Collectors.toSet()))
                .parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));
        registerInvoker(invoker, invokersMap, invokerOperationsMap, relatedClazzMap, toMergeInvokers, toMergeOperations, securityContext);
        dynamicInvokersService.massMerge(toMergeInvokers);
        operationService.massMerge(toMergeOperations);


    }


    private void registerInvoker(Class<?> invokerClass, Map<String, DynamicInvoker> invokersMap, Map<String, Map<String, Operation>> operationsForInvokers, Map<String, OperationToClazz> related, List<Object> toMergeInvokers, List<Object> toMergeOperations, SecurityContext securityContext) {
        DynamicInvoker dynamicInvoker = invokersMap.get(invokerClass.getCanonicalName());
        InvokerInfo invokerInfo = invokerClass.getDeclaredAnnotation(InvokerInfo.class);
        if (dynamicInvoker == null) {
            CreateInvokerRequest createInvokerRequest = new CreateInvokerRequest()
                    .setCanonicalName(invokerClass.getCanonicalName())
                    .setDescription(invokerInfo.description())
                    .setDisplayName(invokerInfo.displayName().isEmpty() ? invokerClass.getSimpleName() : invokerInfo.displayName());
            dynamicInvoker = dynamicInvokersService.createInvokerNoMerge(createInvokerRequest, securityContext);
            toMergeInvokers.add(dynamicInvoker);
            invokersMap.put(dynamicInvoker.getCanonicalName(), dynamicInvoker);
            logger.info("created invoker " + dynamicInvoker.getCanonicalName());
        } else {
            UpdateInvokerRequest createInvokerRequest = new UpdateInvokerRequest()
                    .setCanonicalName(invokerClass.getCanonicalName())
                    .setDescription(invokerInfo.description())
                    .setDisplayName(invokerInfo.displayName().isEmpty() ? invokerClass.getSimpleName() : invokerInfo.displayName())
                    .setInvoker(dynamicInvoker);
            if (dynamicInvokersService.updateInvokerNoMerge(createInvokerRequest, createInvokerRequest.getInvoker())) {
                toMergeInvokers.add(dynamicInvoker);
                logger.debug("updated invoker " + dynamicInvoker.getCanonicalName());
            } else {
                logger.debug("invoker " + dynamicInvoker.getCanonicalName() + " already exists");

            }

        }
        Map<String, Operation> operationMap = operationsForInvokers.getOrDefault(dynamicInvoker.getId(), new HashMap<>());
        for (Method method : invokerClass.getDeclaredMethods()) {
            if (method.isBridge()) {
                continue;
            }
            InvokerMethodInfo invokerMethodInfo = method.getAnnotation(InvokerMethodInfo.class);
            if (invokerMethodInfo != null) {
                String operationId = Baseclass.generateUUIDFromString(method.toString());


                Operation operation = operationMap.get(operationId);
                if (operation == null) {
                    OperationCreate createOperationRequest = new OperationCreate()
                            .setDefaultaccess(invokerMethodInfo.access())
                            .setDynamicInvoker(dynamicInvoker)
                            .setName(invokerMethodInfo.displayName().isEmpty() ? method.getName() : invokerMethodInfo.displayName())
                            .setDescription(invokerMethodInfo.description());

                    operation = operationService.createOperationNoMerge(createOperationRequest,securityContext);
                    operation.setId(operationId);

                    toMergeOperations.add(operation);
                    operationMap.put(operation.getId(), operation);
                    logger.debug("created operation " + operation.getName() + "(" + operationId + ") for invoker " + dynamicInvoker.getCanonicalName());

                } else {
                    UpdateOperationRequest updateOperationRequest = new UpdateOperationRequest()
                            .setName(invokerMethodInfo.displayName().isEmpty() ? method.getName() : invokerMethodInfo.displayName())
                            .setDescription(invokerMethodInfo.description())
                            .setAccess(invokerMethodInfo.access())
                            .setDynamicInvoker(dynamicInvoker)
                            .setId(operationId)
                            .setOperation(operation);
                    if (operationService.updateOperationNoMerge(updateOperationRequest, updateOperationRequest.getOperation())) {
                        toMergeOperations.add(operation);
                        logger.debug("updated operation " + operation.getName() + "(" + operationId + ") for invoker " + dynamicInvoker.getCanonicalName());
                    } else {
                        logger.debug("operation " + operation.getName() + "(" + operationId + ") for invoker " + dynamicInvoker.getCanonicalName() + " already exists");

                    }
                }

                operationService.handleOperationRelatedClassesNoMerge(operation, invokerMethodInfo.relatedClasses(), related, toMergeOperations);

            }
        }


    }


    /**
     * runs once per server start. synchronizes annotated methods with
     * (IOperation) in the database so roles can be built with proper access
     * rights
     */
    public void InitializeOperations() {
        initReflections();
        SecurityContext securityContext = securityService.getAdminUserSecurityContext();

        Set<Class<?>> operationClasses = reflections.getTypesAnnotatedWith(OperationsInside.class, true);
        for (Class<?> annotated : operationClasses) {
            registerOperationsInclass(annotated,securityContext);

        }
        List<Object> toMerge = new ArrayList<>();
        String deleteId = Baseclass.generateUUIDFromString(Delete.class.getCanonicalName());
        String readId = Baseclass.generateUUIDFromString(Read.class.getCanonicalName());
        String updateId = Baseclass.generateUUIDFromString(Update.class.getCanonicalName());
        String writeId = Baseclass.generateUUIDFromString(Write.class.getCanonicalName());
        String allId = Baseclass.generateUUIDFromString(All.class.getCanonicalName());

        Map<String, Operation> existing = baselinkrepository.findByIds(Operation.class, new HashSet<>(Arrays.asList(deleteId, readId, updateId, writeId, allId))).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));
        IOperation ioOperation = Delete.class.getDeclaredAnnotation(IOperation.class);
        addOperation(ioOperation, deleteId, toMerge, existing,securityContext);
        ioOperation = Read.class.getDeclaredAnnotation(IOperation.class);
        addOperation(ioOperation, readId, toMerge, existing,securityContext);
        ioOperation = Update.class.getDeclaredAnnotation(IOperation.class);
        addOperation(ioOperation, updateId, toMerge, existing,securityContext);
        ioOperation = Write.class.getDeclaredAnnotation(IOperation.class);
        addOperation(ioOperation, writeId, toMerge, existing,securityContext);
        ioOperation = All.class.getDeclaredAnnotation(IOperation.class);
        addOperation(ioOperation, allId, toMerge, existing,securityContext);

        baselinkrepository.massMerge(toMerge);


    }


    public void registerOperationsInclass(Class<?> clazz,SecurityContext securityContext) {
        List<Object> toMerge = new ArrayList<>();
        OperationsInside operationsInside = clazz.getAnnotation(OperationsInside.class);
        Map<String, IOperation> toHandle = new HashMap<>();
        Map<String, Class<? extends Baseclass>[]> opIdToRelated = new HashMap<>();
        Map<String, Operation> ops = new HashMap<>();
        for (Method method : clazz.getMethods()) {

            IOperation ioperation = method.getAnnotation(IOperation.class);
            if (ioperation == null) {
                io.swagger.v3.oas.annotations.Operation apiOperation = method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
                if (apiOperation != null) {
                    ioperation = operationService.getIOperationFromApiOperation(apiOperation, method);
                }
            }
            if (ioperation != null) {
                if (ioperation.relatedClazzes().length == 0 && method.getReturnType() != null && Baseclass.class.isAssignableFrom(method.getReturnType())) {
                    ioperation = addRelatedClazz(ioperation, (Class<? extends Baseclass>[]) new Class<?>[]{method.getReturnType()});
                }
                String id = Baseclass.generateUUIDFromString(method.toString());
                toHandle.put(id, ioperation);
            }
            Map<String, Operation> existing = toHandle.isEmpty() ? new HashMap<>() : clazzrepository.findByIds(Operation.class, toHandle.keySet()).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));
            for (Map.Entry<String, IOperation> stringIOperationEntry : toHandle.entrySet()) {
                IOperation op = stringIOperationEntry.getValue();
                Operation operation = addOperation(op, stringIOperationEntry.getKey(), toMerge, existing,securityContext);
                existing.put(operation.getId(), operation);
                Class<? extends Baseclass>[] related = op.relatedClazzes();
                opIdToRelated.put(operation.getId(), related);
            }
            ops.putAll(existing);


        }
        Set<String> ids = new HashSet<>();
        for (Map.Entry<String, Class<? extends Baseclass>[]> stringEntry : opIdToRelated.entrySet()) {
            for (Class<? extends Baseclass> aClass : stringEntry.getValue()) {
                ids.add(stringEntry.getKey() + aClass.getCanonicalName());
            }
        }
        Map<String, OperationToClazz> existing = ids.isEmpty() ? new HashMap<>() : baselinkrepository.findByIds(OperationToClazz.class, ids).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));
        for (Operation value : ops.values()) {
            Class<? extends Baseclass>[] related = opIdToRelated.get(value.getId());
            if (related != null && related.length > 0) {
                handleOperationRelatedClasses(value, related, toMerge, existing);

            }

        }

        baselinkrepository.massMerge(toMerge);


    }

    private IOperation addRelatedClazz(IOperation ioperation, Class<? extends Baseclass>[] classes) {
        return new IOperation() {
            @Override
            public String Name() {
                return ioperation.Name();
            }

            @Override
            public String Description() {
                return ioperation.Description();
            }

            @Override
            public String Category() {
                return ioperation.Category();
            }

            @Override
            public boolean auditable() {
                return ioperation.auditable();
            }

            @Override
            public Class<? extends Baseclass>[] relatedClazzes() {
                return classes;
            }

            @Override
            public Access access() {
                return ioperation.access();
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return IOperation.class;
            }
        };
    }





    private Operation addOperation(IOperation ioperation, String id, List<Object> toMerge, Map<String, Operation> existing,SecurityContext securityContext) {
        Operation operation = existing.get(id);
        if (operation == null) {
            OperationCreate createOperationRequest = new OperationCreate()
                    .setDefaultaccess(ioperation.access())
                    .setDescription(ioperation.Description())
                    .setName(ioperation.Name());
            operation = operationService.createOperationNoMerge(createOperationRequest,securityContext);
            operation.setId(id);
            toMerge.add(operation);

            logger.debug("Have created a new operation" + operation.toString());


        } else {
            if (!operation.isSystemObject()) {
                operation.setSystemObject(true);
                toMerge.add(operation);
            }
            logger.debug("operation already exists: " + operation);

        }



        return operation;
    }

    private void handleOperationRelatedClasses(Operation operation, Class<? extends Baseclass>[] related, List<Object> toMerge, Map<String, OperationToClazz> existing) {

        for (Class<? extends Baseclass> relatedClazz : related) {
            String linkId = Baseclass.generateUUIDFromString(operation.getId() + relatedClazz.getCanonicalName());
            OperationToClazz operationToClazz = existing.get(linkId);
            if (operationToClazz == null) {
                try {
                    operationToClazz = new OperationToClazz("OperationToClazz", null);
                    operationToClazz.setOperation(operation);
                    operationToClazz.setClazz(Baseclass.getClazzByName(relatedClazz.getCanonicalName()));
                    operationToClazz.setId(linkId);
                    operationToClazz.setSystemObject(true);
                    toMerge.add(operationToClazz);
                } catch (Exception e) {
                    logger.info("[registerClazzRelatedOperationsInclass] Error while creating operation: " + e.getMessage());

                }

            } else {
                if (!operationToClazz.isSystemObject()) {
                    operationToClazz.setSystemObject(true);
                    toMerge.add(operationToClazz);
                }
            }

        }
    }

    private void initReflections() {
        if (reflections != null) {
            return;
        }
        try {
            reflections = Reflections.collect("META-INF/reflections/", new FilterBuilder().include(".*-reflections.json"),new JsonSerializer());


        } catch (Exception e) {
            logger.error( "failed initializing reflections", e);
        }


    }

    /**
     * Make sure that all classes annotated with {@code AnnotatedClazz} are registered in the database
     * @return list of initialized classes
     */
    @Transactional
    public List<Clazz> InitializeClazzes() {

        Set<Class<?>> entities = entitiesHolder.getEntities();
        logger.debug("detected classes:  " + entities.parallelStream().map(e -> e.getCanonicalName()).collect(Collectors.joining(System.lineSeparator())));

        Set<String> ids = entities.parallelStream().map(f -> Baseclass.generateUUIDFromString(f.getCanonicalName())).collect(Collectors.toSet());
        ids.add(Baseclass.generateUUIDFromString(Clazz.class.getCanonicalName()));
        ids.add(Baseclass.generateUUIDFromString(ClazzLink.class.getCanonicalName()));
        Map<String, Clazz> existing = new HashMap<>();
        for (List<String> part : Lists.partition(new ArrayList<>(ids), 50)) {
            if (!part.isEmpty()) {
                existing.putAll(clazzrepository.findByIds(Clazz.class, ids).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f)));
            }
        }
        List<Object> toMerge = new ArrayList<>();
        List<AnnotatedClazzWithName> defaults = new ArrayList<>();
        // registering clazz before all
        handleEntityClass(Clazz.class, existing, defaults, toMerge);
        handleEntityClass(ClazzLink.class, existing, defaults, toMerge);
        // registering the rest
        for (Class<?> annotated : entities) {
            if (!annotated.getCanonicalName().equalsIgnoreCase(Clazz.class.getCanonicalName())) {
                handleEntityClass(annotated, existing, defaults, toMerge);
            }
        }
        baselinkrepository.massMerge(toMerge);
        entities.add(Clazz.class);
        entities.add(ClazzLink.class);
        //createIndexes(entities);
       return new ArrayList<>(existing.values());


    }

    private void handleEntityClass(Class<?> claz, Map<String, Clazz> existing, List<AnnotatedClazzWithName> defaults, List<Object> toMerge) {
        registerClazzes(claz, existing, defaults, toMerge);
    }






    private void registerClazzes(Class<?> claz, Map<String, Clazz> existing, List<AnnotatedClazzWithName> defaults, List<Object> toMerge) {
        String classname = claz.getCanonicalName();
        AnnotatedClazz annotatedclazz = claz.getAnnotation(AnnotatedClazz.class);

        if (annotatedclazz == null) {
            annotatedclazz = generateAnnotatedClazz(claz);
        }
        String ID = Baseclass.generateUUIDFromString(classname);


        Clazz clazz = existing.get(ID);
        if (clazz == null) {
            try {

                clazz = Baselink.class.isAssignableFrom(claz) ? createClazzLink(claz, existing, defaults, toMerge) : new Clazz(classname, null);
                clazz.setId(ID);
                clazz.setDescription(annotatedclazz.Description());
                clazz.setSystemObject(true);
                toMerge.add(clazz);
                existing.put(clazz.getId(), clazz);
                logger.debug("Have created a new class " + clazz.toString());
            } catch (Exception e) {
                logger.error( "[register classes] Error while creating operation: ", e);
            }

        } else {
            logger.debug("Clazz  allready exists: " + clazz);

        }
        if (clazz != null) {
            baselinkrepository.addtocache(clazz);
        } else {
            logger.error("clazz for " + claz.getCanonicalName() + " was not registered");
        }


    }

    private ClazzLink createClazzLink(Class<?> claz, Map<String, Clazz> existing, List<AnnotatedClazzWithName> defaults, List<Object> toMerge) {
        //handle the case where a ClazzLink is needed
        String classname = claz.getCanonicalName();
        ClazzLink clazzLink = new ClazzLink(classname, null);
        Class<?>[] params = new Class[0];
        try {
            Method l = claz.getDeclaredMethod("getLeftside", params);
            Method r = claz.getDeclaredMethod("getRightside", params);
            Clazz valueClazz = Baseclass.getClazzByName(Baseclass.class.getCanonicalName());
            if (valueClazz == null) {
                handleEntityClass(Baseclass.class, existing, defaults, toMerge);
                valueClazz = Baseclass.getClazzByName(Baseclass.class.getCanonicalName());
            }
            try {
                Method v = claz.getDeclaredMethod("getValue", params);
                ManyToOne mtO = v.getAnnotation(ManyToOne.class);
                Class<?> cv = mtO.targetEntity();
                handleEntityClass(cv, existing, defaults, toMerge);
                valueClazz = Baseclass.getClazzByName(cv.getCanonicalName());
            } catch (NoSuchMethodException e) {
                logger.info("there is not spesific decleration for value for: " + claz.getCanonicalName());

            }
            clazzLink.setValue(valueClazz);
            if (l.isAnnotationPresent(ManyToOne.class)) {
                ManyToOne mtO = l.getAnnotation(ManyToOne.class);
                Class<?> cl = mtO.targetEntity();
                handleEntityClass(cl, existing, defaults, toMerge);
                Clazz lclazz = Baseclass.getClazzByName(cl.getCanonicalName());
                clazzLink.setLeft(lclazz);

            }
            if (r.isAnnotationPresent(ManyToOne.class)) {
                ManyToOne mtO = r.getAnnotation(ManyToOne.class);
                Class<?> cr = mtO.targetEntity();
                handleEntityClass(cr, existing, defaults, toMerge);
                Clazz rclazz = Baseclass.getClazzByName(cr.getCanonicalName());
                clazzLink.setRight(rclazz);

            }
        } catch (Exception e) {
            logger.error( "failed setting clazzlink properties", e);
        }
        return clazzLink;
    }



    private AnnotatedClazz generateAnnotatedClazz(Class<?> claz) {
        return new AnnotatedClazz() {

            @Override
            public String DisplayName() {
                return claz.getSimpleName();
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return AnnotatedClazz.class;
            }


            @Override
            public String Name() {
                return claz.getCanonicalName();
            }

            @Override
            public String Description() {
                return "Auto Generated Description";
            }

            @Override
            public String Category() {
                return "Auto Generated Category";
            }
        };
    }

    protected <T> T save(T Acd, boolean en) {

        return Acd;
    }


    /**
     * creates all defaults instances, these are defined by the {@link AnnotatedClazz}
     */
    @SuppressWarnings("unused")
    public void createDefaultObjects() {
        List<Object> toMerge=new ArrayList<>();
        TenantAndUserInit tenantAndUserInit = createAdminAndDefaultTenant(toMerge);
        Tenant defaultTenant = tenantAndUserInit.getDefaultTenant();
        User admin = tenantAndUserInit.getAdmin();

        TenantToUserCreate tenantToUserCreate = new TenantToUserCreate().setDefaultTenant(true).setUser(admin).setTenant(defaultTenant);
        TenantToUser tenantToUser=baselinkrepository.findByIdOrNull(TenantToUser.class, TENANT_TO_USER_ID);
        if(tenantToUser==null){
            logger.debug("Creating Tenant To User link");
            tenantToUser=userService.createTenantToUserNoMerge(tenantToUserCreate,null);
            tenantToUser.setCreator(admin);
            tenantToUser.setId(TENANT_TO_USER_ID);
            toMerge.add(tenantToUser);
        }
        else{
            if(userService.updateTenantToUserNoMerge(tenantToUserCreate,tenantToUser)){
                toMerge.add(tenantToUser);
                logger.debug("Updated Tenant To User");
            }
        }
        RoleCreate roleCreate=new RoleCreate()
                .setName("Super Administrators")
                .setDescription("Role for Super Administrators of the system")
                .setTenant(defaultTenant);
        Role superAdminRole=baselinkrepository.findByIdOrNull(Role.class, SUPER_ADMIN_ROLE_ID);
        if(superAdminRole==null){
            logger.debug("Creating Super Admin role");
            superAdminRole=roleService.createRoleNoMerge(roleCreate,null);
            superAdminRole.setCreator(admin);
            superAdminRole.setId(SUPER_ADMIN_ROLE_ID);
            toMerge.add(superAdminRole);
        }
        RoleToUserCreate roleToUserCreate=new RoleToUserCreate().setRole(superAdminRole).setUser(admin).setTenant(defaultTenant);
        RoleToUser roleToUser=baselinkrepository.findByIdOrNull(RoleToUser.class, SUPER_ADMIN_TO_ADMIN_ID);
        if(roleToUser==null){
            logger.debug("Creating Role To User Link");
            roleToUser=userService.createRoleToUserNoMerge(roleToUserCreate,null);
            roleToUser.setCreator(admin);
            roleToUser.setId(SUPER_ADMIN_TO_ADMIN_ID);
            toMerge.add(roleToUser);
        }
        else{
            if(userService.updateRoleToUserNoMerge(roleToUserCreate,roleToUser)){
                toMerge.add(roleToUser);
                logger.debug("Updated Role To User Link");
            }
        }
        baselinkrepository.massMerge(toMerge);

    }

    private String readFromFirstRunFile() {
        File file = new File(firstRunFilePath);
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                System.out.println("cannot create first run file parent dir");
            }

        }
        List<String> lines =null;
        try {
            lines = FileUtils.readLines(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines!=null && !lines.isEmpty()?lines.get(0).trim():null;

    }

    private void writeToFirstRunFile(String pass) {
        File file = new File(firstRunFilePath);
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                System.out.println("cannot create first run file parent dir");
            }

        }
        List<String> lines = new ArrayList<>();
        lines.add(pass);
        try {
            FileUtils.writeLines(file, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void createSwaggerTags() {
        SecurityContext securityContext = securityService.getAdminUserSecurityContext();
        initReflections();
        Set<Class<?>> definitions = reflections.getTypesAnnotatedWith(OpenAPIDefinition.class);

        for (Class<?> annotated : definitions) {
            addSwaggerTags(annotated, securityContext);
        }

        Set<Class<?>> tags = reflections.getTypesAnnotatedWith(Tag.class);

        for (Class<?> annotated : tags) {
            addSwaggerTags(annotated, securityContext);
        }


    }

    public void addSwaggerTags(Class<?> annotated, SecurityContext securityContext) {
        OpenAPIDefinition def = annotated.getAnnotation(OpenAPIDefinition.class);
        if (def != null) {
            for (Tag tag : def.tags()) {
                addTag(securityContext, tag);


            }
        }
        Tag[] tags = annotated.getAnnotationsByType(Tag.class);
        for (Tag tag : tags) {
            if(tag!=null){
                addTag(securityContext,tag);
            }
        }



    }

    private void addTag(SecurityContext securityContext, Tag tag) {
        String id = Baseclass.generateUUIDFromString(tag.name());
        DocumentationTag doc = baselinkrepository.findByIdOrNull(DocumentationTag.class, id);
        if (doc == null) {

            doc = new DocumentationTag(tag.name(), securityContext);
            doc.setSystemObject(true);
            doc.setId(id);
            doc.setDescription(doc.getDescription());
            baselinkrepository.merge(doc);
            logger.debug("found new tag: " + tag.name());
        } else {
            logger.debug("tag: " + tag.name() + " already exist in the database");
        }
    }


    public void registerClasses() {
        initReflections();
        Set<Class<?>> classes = reflections.getSubTypesOf(Object.class);
        for (Class<?> c : classes) {
            try {
                InheritanceUtils.registerClass(c);
            } catch (Throwable e) {
                logger.error( "failed registering", e);
            }

        }
    }

    public TenantAndUserInit createAdminAndDefaultTenant(List<Object> toMerge) {
        boolean tenantUpdated=false;
        TenantCreate tenantCreate=new TenantCreate()
                .setName("Default Tenant")
                .setDescription("Default Tenant");
        Tenant defaultTenant = baselinkrepository.findByIdOrNull(Tenant.class, DEFAULT_TENANT_ID);
        if(defaultTenant==null){
            logger.debug("Creating Default Tenant");
            defaultTenant=tenantService.createTenantNoMerge(tenantCreate,null);
            defaultTenant.setId(DEFAULT_TENANT_ID);
            defaultTenant.setTenant(defaultTenant);
            toMerge.add(defaultTenant);
        }
        else{
            if(defaultTenant.getTenant()==null){
                defaultTenant.setTenant(defaultTenant);
                tenantUpdated=true;
            }
        }
        String pass=readFromFirstRunFile();
        if(pass==null){
            pass = PasswordGenerator.generateRandom(8);
            writeToFirstRunFile(pass);
        }
        UserCreate userCreate=new UserCreate()
                .setEmail(adminEmail)
                .setPassword(pass)
                .setLastName("Admin")
                .setTenant(defaultTenant)
                .setName("Admin");
        User admin = baselinkrepository.findByIdOrNull(User.class, systemAdminId);
        if(admin==null){
            logger.debug("Creating Admin User");
            admin=userService.createUserNoMerge(userCreate,null);
            admin.setCreator(admin);
            admin.setId(systemAdminId);
            toMerge.add(admin);
        }
        else{
            if(admin.getCreator()==null){
                admin.setCreator(admin);
                toMerge.add(admin);
            }
        }

        if(defaultTenant.getCreator()==null){
            defaultTenant.setCreator(admin);
            tenantUpdated=true;
        }
        if(tenantUpdated){
            toMerge.add(defaultTenant);
        }
        return new TenantAndUserInit(admin,defaultTenant);
    }


    private static class TenantAndUserInit {
        private final Tenant defaultTenant;
        private final User admin;


        public TenantAndUserInit(User admin,Tenant defaultTenant) {
            this.defaultTenant=defaultTenant;
            this.admin=admin;
        }

        public Tenant getDefaultTenant() {
            return defaultTenant;
        }

        public User getAdmin() {
            return admin;
        }


    }
}

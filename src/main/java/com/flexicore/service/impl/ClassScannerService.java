package com.flexicore.service.impl;

import com.flexicore.annotations.*;
import com.flexicore.annotations.IOperation.Access;
import com.flexicore.annotations.rest.*;
import com.flexicore.constants.Constants;
import com.flexicore.data.BaselinkRepository;
import com.flexicore.data.ClazzRegistration;
import com.flexicore.data.ClazzRepository;
import com.flexicore.interfaces.dynamic.InvokerInfo;
import com.flexicore.interfaces.dynamic.InvokerMethodInfo;
import com.flexicore.model.*;
import com.flexicore.model.dynamic.DynamicInvoker;
import com.flexicore.model.licensing.LicensingFeature;
import com.flexicore.model.licensing.LicensingProduct;
import com.flexicore.provider.EntitiesHolder;
import com.flexicore.request.*;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.PasswordGenerator;
import com.flexicore.utils.InheritanceUtils;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Primary
@Component
public class ClassScannerService {

    public static final String FC_FEATURE = "FlexiCore";
    public static final String FC_PRODUCT = "com.flexicore";
    private static final String API_KEY = "Default_Tenant";
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
    private Logger logger = Logger.getLogger(getClass().getCanonicalName());
    @Autowired
    LicensingFeatureService featureService;
    @Autowired
    LicensingProductService licensingProductService;
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
                logger.fine("updated invoker " + dynamicInvoker.getCanonicalName());
            } else {
                logger.fine("invoker " + dynamicInvoker.getCanonicalName() + " already exists");

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
                    CreateOperationRequest createOperationRequest = new CreateOperationRequest()
                            .setName(invokerMethodInfo.displayName().isEmpty() ? method.getName() : invokerMethodInfo.displayName())
                            .setDescription(invokerMethodInfo.description())
                            .setAccess(invokerMethodInfo.access())
                            .setDynamicInvoker(dynamicInvoker)
                            .setId(operationId);
                    operation = operationService.createOperationNoMerge(createOperationRequest);
                    toMergeOperations.add(operation);
                    operationMap.put(operation.getId(), operation);
                    logger.fine("created operation " + operation.getName() + "(" + operationId + ") for invoker " + dynamicInvoker.getCanonicalName());

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
                        logger.fine("updated operation " + operation.getName() + "(" + operationId + ") for invoker " + dynamicInvoker.getCanonicalName());
                    } else {
                        logger.fine("operation " + operation.getName() + "(" + operationId + ") for invoker " + dynamicInvoker.getCanonicalName() + " already exists");

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
        Set<Class<?>> operationClasses = reflections.getTypesAnnotatedWith(OperationsInside.class, true);
        for (Class<?> annotated : operationClasses) {
            registerOperationsInclass(annotated);

        }
        List<Object> toMerge = new ArrayList<>();
        String deleteId = Baseclass.generateUUIDFromString(Delete.class.getCanonicalName());
        String readId = Baseclass.generateUUIDFromString(Read.class.getCanonicalName());
        String updateId = Baseclass.generateUUIDFromString(Update.class.getCanonicalName());
        String writeId = Baseclass.generateUUIDFromString(Write.class.getCanonicalName());
        String allId = Baseclass.generateUUIDFromString(All.class.getCanonicalName());

        Map<String, Operation> existing = baselinkrepository.findByIds(Operation.class, new HashSet<>(Arrays.asList(deleteId, readId, updateId, writeId, allId))).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));
        IOperation ioOperation = Delete.class.getDeclaredAnnotation(IOperation.class);
        addOperation(ioOperation, deleteId, toMerge, existing);
        ioOperation = Read.class.getDeclaredAnnotation(IOperation.class);
        addOperation(ioOperation, readId, toMerge, existing);
        ioOperation = Update.class.getDeclaredAnnotation(IOperation.class);
        addOperation(ioOperation, updateId, toMerge, existing);
        ioOperation = Write.class.getDeclaredAnnotation(IOperation.class);
        addOperation(ioOperation, writeId, toMerge, existing);
        ioOperation = All.class.getDeclaredAnnotation(IOperation.class);
        addOperation(ioOperation, allId, toMerge, existing);

        baselinkrepository.massMerge(toMerge);


    }

    public void registerFlexiCoreLicense() {
        logger.info("creating FC Feature");
        HasFeature flexicoreFeature = getHasFeature(FC_FEATURE, FC_PRODUCT);
        SecurityService.setFlexiCoreFeature(flexicoreFeature);
        SecurityContext securityContext = securityService.getAdminUserSecurityContext();
        addFeature(flexicoreFeature);

    }

    private HasFeature getHasFeature(String featureName, String productName) {
        return new HasFeature() {
            @Override
            public String canonicalName() {
                return featureName;
            }

            @Override
            public String productCanonicalName() {
                return productName;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return HasFeature.class;
            }
        };
    }

    public void registerOperationsInclass(Class<?> clazz) {
        List<Object> toMerge = new ArrayList<>();
        OperationsInside operationsInside = clazz.getAnnotation(OperationsInside.class);
        for (HasFeature hasFeature : operationsInside.features()) {
            addFeature(hasFeature);
        }
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
                Operation operation = addOperation(op, stringIOperationEntry.getKey(), toMerge, existing);
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
            public HasFeature[] features() {
                return ioperation.features();
            }

            @Override
            public boolean noOtherLicenseRequired() {
                return false;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return IOperation.class;
            }
        };
    }


    private void addFeature(HasFeature hasFeature) {
        String canonicalName = hasFeature.canonicalName();
        List<LicensingFeature> licensingFeatures = featureService.listAllLicensingFeatures(new LicensingFeatureFiltering().setCanonicalNames(Collections.singleton(canonicalName)), null);

        LicensingFeature licensingFeature = licensingFeatures.isEmpty() ? null : licensingFeatures.get(0);
        if (licensingFeature == null) {
            logger.info("registering feature: " + canonicalName);
            List<LicensingProduct> licensingProducts = licensingProductService.listAllLicensingProducts(new LicensingProductFiltering().setCanonicalNames(Collections.singleton(hasFeature.productCanonicalName())), null);
            LicensingProduct licensingProduct = licensingProducts.isEmpty() ? null : licensingProducts.get(0);
            if (licensingProduct == null) {
                logger.info("registering product: " + hasFeature.productCanonicalName());

                licensingProduct = licensingProductService.createLicensingProduct(new LicensingProductCreate().setCanonicalName(hasFeature.productCanonicalName()).setName(hasFeature.productCanonicalName()), null);
            }
            licensingFeature = featureService.createLicensingFeature(new LicensingFeatureCreate().setLicensingProduct(licensingProduct).setCanonicalName(canonicalName).setName(canonicalName), null);
        }

    }


    private Operation addOperation(IOperation ioperation, String id, List<Object> toMerge, Map<String, Operation> existing) {
        Operation operation = existing.get(id);
        if (operation == null) {
            CreateOperationRequest createOperationRequest = new CreateOperationRequest()
                    .setAccess(ioperation.access())
                    .setDescription(ioperation.Description())
                    .setId(id)
                    .setName(ioperation.Name());
            operation = operationService.createOperationNoMerge(createOperationRequest);
            toMerge.add(operation);

            logger.fine("Have created a new operation" + operation.toString());


        } else {
            if (!operation.isSystemObject()) {
                operation.setSystemObject(true);
                toMerge.add(operation);
            }
            logger.fine("operation already exists: " + operation);

        }

        for (HasFeature hasFeature : ioperation.features()) {
            addFeature(hasFeature);
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
                    operationToClazz.setClazz(Baseclass.getClazzbyname(relatedClazz.getCanonicalName()));
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
            reflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage("com.flexicore"))
                    .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(false), new MethodAnnotationsScanner()).useParallelExecutor());


        } catch (Exception e) {
            logger.log(Level.SEVERE, "failed initializing reflections", e);
        }


    }

    /**
     * Make sure that all classes annotated with {@code AnnotatedClazz} are registered in the database
     * @return list of initialized classes
     */
    @Transactional
    public List<Clazz> InitializeClazzes() {

        Set<Class<?>> entities = entitiesHolder.getEntities();
        logger.fine("detected classes:  " + entities.parallelStream().map(e -> e.getCanonicalName()).collect(Collectors.joining(System.lineSeparator())));

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
        HasFeature hasFeature = claz.getAnnotation(HasFeature.class);
        if (hasFeature != null) {
            String canonicalName = hasFeature.canonicalName();
            if (canonicalName.isEmpty()) {
                hasFeature = getHasFeature(classname, hasFeature.productCanonicalName());
            }
            addFeature(hasFeature);
        }
        String ID = Baseclass.generateUUIDFromString(classname);


        Clazz clazz = existing.get(ID);
        if (clazz == null) {
            try {

                clazz = new Clazz(classname, null);


                clazz.setId(ID);
                clazz.setDescription(annotatedclazz.Description());
                clazz.setSystemObject(true);
                toMerge.add(clazz);
                existing.put(clazz.getId(), clazz);
                logger.fine("Have created a new class " + clazz.toString());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[register classes] Error while creating operation: ", e);
            }

        } else {
            logger.fine("Clazz  allready exists: " + clazz);

        }
        if (clazz != null) {
            baselinkrepository.addtocache(clazz);
        } else {
            logger.severe("clazz for " + claz.getCanonicalName() + " was not registered");
        }


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
            logger.fine("Creating Tenant To User link");
            tenantToUser=userService.createTenantToUserNoMerge(tenantToUserCreate,null);
            tenantToUser.setCreator(admin);
            tenantToUser.setId(TENANT_TO_USER_ID);
            toMerge.add(tenantToUser);
        }
        else{
            if(userService.updateTenantToUserNoMerge(tenantToUserCreate,tenantToUser)){
                toMerge.add(tenantToUser);
                logger.fine("Updated Tenant To User");
            }
        }
        RoleCreate roleCreate=new RoleCreate()
                .setName("Super Administrators")
                .setDescription("Role for Super Administrators of the system")
                .setTenant(defaultTenant);
        Role superAdminRole=baselinkrepository.findByIdOrNull(Role.class, SUPER_ADMIN_ROLE_ID);
        if(superAdminRole==null){
            logger.fine("Creating Super Admin role");
            superAdminRole=roleService.createRoleNoMerge(roleCreate,null);
            superAdminRole.setCreator(admin);
            superAdminRole.setId(SUPER_ADMIN_ROLE_ID);
            toMerge.add(superAdminRole);
        }
        RoleToUserCreate roleToUserCreate=new RoleToUserCreate().setRole(superAdminRole).setUser(admin).setTenant(defaultTenant);
        RoleToUser roleToUser=baselinkrepository.findByIdOrNull(RoleToUser.class, SUPER_ADMIN_TO_ADMIN_ID);
        if(roleToUser==null){
            logger.fine("Creating Role To User Link");
            roleToUser=userService.createRoleToUserNoMerge(roleToUserCreate,null);
            roleToUser.setCreator(admin);
            roleToUser.setId(SUPER_ADMIN_TO_ADMIN_ID);
            toMerge.add(roleToUser);
        }
        else{
            if(userService.updateRoleToUserNoMerge(roleToUserCreate,roleToUser)){
                toMerge.add(roleToUser);
                logger.fine("Updated Role To User Link");
            }
        }
        baselinkrepository.massMerge(toMerge);

    }

    private static String readFromFirstRunFile() {
        File file = new File(Constants.FIRST_RUN_FILE);
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

    private static void writeToFirstRunFile(String pass) {
        File file = new File(Constants.FIRST_RUN_FILE);
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
            logger.fine("found new tag: " + tag.name());
        } else {
            logger.fine("tag: " + tag.name() + " already exist in the database");
        }
    }


    public void registerClasses() {
        initReflections();
        Set<Class<?>> classes = reflections.getSubTypesOf(Object.class);
        for (Class<?> c : classes) {
            try {
                InheritanceUtils.registerClass(c);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "failed registering", e);
            }

        }
    }

    public TenantAndUserInit createAdminAndDefaultTenant(List<Object> toMerge) {
        boolean tenantUpdated=false;
        TenantCreate tenantCreate=new TenantCreate()
                .setApiKey(API_KEY)
                .setName("Default Tenant")
                .setDescription("Default Tenant");
        Tenant defaultTenant = baselinkrepository.findByIdOrNull(Tenant.class, DEFAULT_TENANT_ID);
        if(defaultTenant==null){
            logger.fine("Creating Default Tenant");
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
                .setEmail(Constants.adminEmail)
                .setPassword(pass)
                .setLastName("Admin")
                .setTenant(defaultTenant)
                .setName("Admin");
        User admin = baselinkrepository.findByIdOrNull(User.class, Constants.systemAdminId);
        if(admin==null){
            logger.fine("Creating Admin User");
            admin=userService.createUserNoMerge(userCreate,null);
            admin.setCreator(admin);
            admin.setId(Constants.systemAdminId);
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
        private Tenant defaultTenant;
        private User admin;


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

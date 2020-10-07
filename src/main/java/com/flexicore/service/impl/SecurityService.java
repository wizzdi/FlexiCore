package com.flexicore.service.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.BadRequestException;

import com.flexicore.annotations.Baseclassroot;
import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.IOperation.Access;
import com.flexicore.annotations.rest.Delete;
import com.flexicore.annotations.rest.Read;
import com.flexicore.annotations.rest.Update;
import com.flexicore.annotations.rest.Write;
import com.flexicore.data.BaseclassRepository;
import com.flexicore.data.jsoncontainers.OperationInfo;
import com.flexicore.interfaces.FlexiCoreService;
import com.flexicore.model.*;
import com.flexicore.request.*;
import com.flexicore.response.PermissionSummaryEntry;
import com.flexicore.response.PermissionSummaryResponse;
import com.flexicore.security.RunningUser;
import com.flexicore.security.SecurityContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class SecurityService implements com.flexicore.service.SecurityService {
    /**
     *
     */
    private static final long serialVersionUID = 460083370640407824L;
    @Autowired
    @Baseclassroot
    BaseclassRepository baseclassrpository;
    private Logger logger = Logger.getLogger(getClass().getCanonicalName());
    @Autowired
    UserService userservice;
    @Autowired
    OperationService operationrepository;

    @Autowired
    private BaselinkService baselinkService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private TenantService tenantService;


    @Override
    public boolean checkIfAllowed(SecurityContext securityContext) {
        Access defaultaccess = securityContext.getOperation() != null && securityContext.getOperation().getDefaultaccess() != null ? securityContext.getOperation().getDefaultaccess() : Access.allow;
        return checkIfAllowed(securityContext.getUser(), securityContext.getTenants(), securityContext.getOperation(), defaultaccess);
    }


    @Override
    public boolean checkIfAllowed(User user, List<Tenant> tenants, Operation operation, Access access) {

        if (operationrepository.userAllowed(operation, user)) {
            return true; // if the operation is allowed to this user.
        } else {
            if (operationrepository.userDenied(operation, user)) { // if this
                // user
                // cannot
                // perform
                // this
                // operation
                // (force
                // deny on
                // user or
                // Role)
                return false;
            }
            if (operationrepository.roleAllowed(operation, user)) {
                return true; // if the operation is allowed to everyone.
            } else {
                if (operationrepository.roleDenied(operation, user)) { // if
                    // this
                    // user
                    // cannot
                    // perform
                    // this
                    // operation
                    // (force
                    // deny
                    // on
                    // user
                    // or
                    // Role)
                    return false;
                } else {
                    for (Tenant tenant : tenants) {
                        if (operationrepository.tennantAllowed(operation, tenant)) { // if
                            // by
                            // default
                            // all
                            // users/roles
                            // of
                            // this
                            // tenant
                            // is
                            // explicitly
                            // allowed.
                            return true;
                        }
                    }
                    boolean allDenied = true;
                    for (Tenant tenant : tenants) {
                        allDenied = operationrepository.tennantDenied(operation, tenant) && allDenied;

                    }
                    if (allDenied) {
                        return false;
                    } else {
                        // if this operation
// is allowed by
// default.
                        return access == Access.allow;
                    }


                }
            }

        }
    }

    @Override
    public SecurityContext getSecurityContext(String authenticationkey, String tenantApi, String operationId) {

        RunningUser runningUser = userservice.getRunningUser(authenticationkey);
        if (runningUser == null) {
            return null;
        }
        Tenant tenantToCreateIn = runningUser.getDefaultTenant();
        User user = runningUser.getUser();
        List<Tenant> tenants = runningUser.getTenants();
        boolean impersonated = runningUser.isImpersonated();
        if (tenantApi != null) {
            Optional<Tenant> impersonateTenantOptional = tenants.parallelStream().filter(f -> tenantApi.equals(f.getApiKey())).findFirst();
            if (impersonateTenantOptional.isPresent()) {
                Tenant impersonateTenant = impersonateTenantOptional.get();
                logger.info("Will impersonate tenant " + impersonateTenant.getId() + "(" + impersonateTenant.getApiKey() + ")");
                tenants = Collections.singletonList(impersonateTenant);
                tenantToCreateIn = impersonateTenant;
                impersonated = true;
            } else {
                logger.warning("Could not find target impersonate tenant with api key " + tenantApi);
            }

        }

        Operation operation = operationrepository.findById(operationId);
        return new SecurityContext(tenants, user, operation, tenantToCreateIn).setImpersonated(impersonated);


    }

    @Override
    public OperationInfo getIOperation(Method method) {
        OperationInfo operationInfo = new OperationInfo();
        IOperation ioOperation = method.getAnnotation(IOperation.class);
        if (ioOperation == null) {
            io.swagger.v3.oas.annotations.Operation apiOperation = method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
            if (apiOperation != null) {
                ioOperation = operationrepository.getIOperationFromApiOperation(apiOperation, method);
            }
            if (ioOperation == null) {
                Annotation annotaion = method.getAnnotation(Read.class);
                if (annotaion == null) {
                    annotaion = method.getAnnotation(Write.class);
                    if (annotaion == null) {
                        annotaion = method.getAnnotation(Update.class);
                        if (annotaion == null) {
                            annotaion = method.getAnnotation(Delete.class);
                        }
                    }
                }

                if (annotaion != null) {
                    ioOperation = annotaion.annotationType().getAnnotation(IOperation.class);
                    String name = annotaion.annotationType().getCanonicalName();
                    operationInfo.setOperationId(Baseclass.generateUUIDFromString(name));
                }
            } else {
                operationInfo.setOperationId(Baseclass.generateUUIDFromString(method.toString()));

            }


        } else {
            operationInfo.setOperationId(Baseclass.generateUUIDFromString(method.toString()));

        }
        operationInfo.setiOperation(ioOperation);
        return operationInfo;
    }

    @Override
    public SecurityContext getAdminUserSecurityContext() {
        User user = userservice.getAdminUser();
        String token = userservice.registerUserIntoSystem(user).getAuthenticationkey().getKey();
        String opId = Baseclass.generateUUIDFromString(Read.class.getCanonicalName());
        return getSecurityContext(token, null, opId);
    }

    @Override
    public SecurityContext getUserSecurityContextByEmail(String email) {
        List<User> users = userservice.listAllUsers(new UserFiltering().setEmails(Collections.singleton(email)), null);
        User user = users.isEmpty() ? null : users.get(0);
        if (user != null) {
            String token = userservice.registerUserIntoSystem(user).getAuthenticationkey().getKey();
            String opId = Baseclass.generateUUIDFromString(Read.class.getCanonicalName());
            return getSecurityContext(token, null, opId);
        }
        return null;

    }



    @Override
    public SecurityContext getUserSecurityContext(User user) {

        String token = userservice.registerUserIntoSystem(user).getAuthenticationkey().getKey();
        String opId = Baseclass.generateUUIDFromString(Read.class.getCanonicalName());
        return getSecurityContext(token, null, opId);

    }

    @Override
    public void refrehEntityManager() {
        baseclassrpository.refrehEntityManager();
        userservice.refrehEntityManager();
        operationrepository.refrehEntityManager();
    }


    @Override
    public void validate(PermissionSummaryRequest permissionSummaryRequest, SecurityContext securityContext) {
        Set<String> usersIds = permissionSummaryRequest.getUserIds();
        if (usersIds.isEmpty()) {
            throw new BadRequestException("Permission Summary must have at least one user id as input");
        }
        Map<String, User> users = baselinkService.listByIds(User.class,usersIds,securityContext).parallelStream().collect(Collectors.toMap(f->f.getId(),f->f));
        usersIds.removeAll(users.keySet());
        if(!usersIds.isEmpty()){
            throw new BadRequestException("No Users with ids "+usersIds);
        }
        permissionSummaryRequest.setUsers(new ArrayList<>(users.values()));

        Set<String> baseclassIds = permissionSummaryRequest.getBaseclassIds();
        if (baseclassIds.isEmpty()) {
            throw new BadRequestException("Permission Summary must have at least one Baseclass id as input");
        }
        Map<String, Baseclass> baseclassMap = baselinkService.listByIds(Baseclass.class,baseclassIds,securityContext).parallelStream().collect(Collectors.toMap(f->f.getId(),f->f));
        baseclassIds.removeAll(baseclassMap.keySet());
        if(!baseclassIds.isEmpty()){
            throw new BadRequestException("No baseclass with ids "+baseclassIds);
        }
        permissionSummaryRequest.setBaseclasses(new ArrayList<>(baseclassMap.values()));
    }

    @Override
    public PermissionSummaryResponse getPermissionsSummary(PermissionSummaryRequest permissionSummaryRequest, SecurityContext securityContext) {
        List<RoleToUser> roleToUsers = baselinkService.listAllBaselinks(new BaselinkFilter().setLinkClass(RoleToUser.class).setRightside(permissionSummaryRequest.getUsers().parallelStream().collect(Collectors.toList())), securityContext);
        Map<String,List<RoleToUser>> roleMap= roleToUsers.parallelStream().collect(Collectors.groupingBy(f->f.getRightside().getId()));

        List<TenantToUser> tenantToUsers = baselinkService.listAllBaselinks(new BaselinkFilter().setLinkClass(TenantToUser.class).setRightside(permissionSummaryRequest.getUsers().parallelStream().collect(Collectors.toList())), securityContext);
        Map<String,List<TenantToUser>> tenantMap= tenantToUsers.parallelStream().collect(Collectors.groupingBy(f->f.getRightside().getId()));
        List<Baseclass> baseclasses = permissionSummaryRequest.getBaseclasses();
        Clazz securityWildcard = Baseclass.getClazzbyname(SecurityWildcard.class.getCanonicalName());

        Map<String,Map<String,Baseclass>> relatedBaseclass=new HashMap<>();
        for (Baseclass baseclass : baseclasses) {
            relatedBaseclass.computeIfAbsent(baseclass.getId(),f->new HashMap<>()).put(baseclass.getId(),baseclass);
            relatedBaseclass.computeIfAbsent(baseclass.getClazz().getId(),f->new HashMap<>()).put(baseclass.getId(),baseclass);
            relatedBaseclass.computeIfAbsent(securityWildcard.getId(),f->new HashMap<>()).put(baseclass.getId(),baseclass);


        }
        List<PermissionGroupToBaseclass> permissionGroupToBaseclassesUndeleted = baselinkService.listAllBaselinks(new BaselinkFilter().setLinkClass(PermissionGroupToBaseclass.class).setRightside(baseclasses), null);
        List<PermissionGroupToBaseclass> permissionGroupToBaseclasses= permissionGroupToBaseclassesUndeleted.stream().filter(f->!f.getLeftside().isSoftDelete()).collect(Collectors.toList());;
        for (PermissionGroupToBaseclass permissionGroupToBaseclass : permissionGroupToBaseclasses) {
            relatedBaseclass.computeIfAbsent(permissionGroupToBaseclass.getLeftside().getId(),f->new HashMap<>()).put(permissionGroupToBaseclass.getRightside().getId(),permissionGroupToBaseclass.getRightside());
        }
        Map<String,List<PermissionGroupToBaseclass>> permissionGroupToBaseclassMap=permissionGroupToBaseclasses.parallelStream().collect(Collectors.groupingBy(f->f.getRightside().getId()));

        List<Baseclass> leftsides=new ArrayList<>(permissionSummaryRequest.getUsers());
        leftsides.addAll(roleToUsers.parallelStream().filter(f->f.getLeftside()!=null).map(f->f.getLeftside()).collect(Collectors.toList()));
        leftsides.addAll(tenantToUsers.parallelStream().filter(f->f.getLeftside()!=null).map(f->f.getLeftside()).collect(Collectors.toList()));
        List<Baseclass> rightSide=new ArrayList<>(baseclasses);
        rightSide.addAll(permissionGroupToBaseclasses.parallelStream().filter(f->f.getLeftside()!=null).map(f->f.getLeftside()).collect(Collectors.toList()));
        Map<String,Clazz> clazzMap=rightSide.parallelStream().filter(f->f.getClazz()!=null).map(f->f.getClazz()).collect(Collectors.toMap(f->f.getId(),f->f,(a,b)->a));
        rightSide.addAll(new ArrayList<>(clazzMap.values()));
        rightSide.add(securityWildcard);
        List<SecurityLink> securityLinks=baselinkService.listAllBaselinks(new BaselinkFilter().setLinkClass(SecurityLink.class).setLeftside(leftsides).setRightside(rightSide),null);
        Map<String,Map<String,List<UserToBaseClass>>> userToBaseclassMap=securityLinks.parallelStream().filter(f->f instanceof UserToBaseClass && (!(f.getRightside() instanceof Clazz) && !(f.getRightside() instanceof PermissionGroup))).map(f->(UserToBaseClass)f).collect(Collectors.groupingBy(f->f.getLeftside().getId(),Collectors.groupingBy(f->f.getRightside().getId())));
        Map<String,Map<String,List<UserToBaseClass>>> userToClazzMap=securityLinks.parallelStream().filter(f->f instanceof UserToBaseClass && f.getRightside() instanceof Clazz).map(f->(UserToBaseClass)f).collect(Collectors.groupingBy(f->f.getLeftside().getId(),Collectors.groupingBy(f->f.getRightside().getId())));
        Map<String,Map<String,List<UserToBaseClass>>> userToPermissionGroupMap=securityLinks.parallelStream().filter(f->f instanceof UserToBaseClass && f.getRightside() instanceof PermissionGroup).map(f->(UserToBaseClass)f).collect(Collectors.groupingBy(f->f.getLeftside().getId(),Collectors.groupingBy(f->f.getRightside().getId())));

        Map<String,Map<String,List<RoleToBaseclass>>> roleToBaseclassMap=securityLinks.parallelStream().filter(f->f instanceof RoleToBaseclass).map(f->(RoleToBaseclass)f).collect(Collectors.groupingBy(f->f.getLeftside().getId(),Collectors.groupingBy(f->f.getRightside().getId())));
        Map<String,Map<String,List<TenantToBaseClassPremission>>> tenantToBaseclassMap=securityLinks.parallelStream().filter(f->f instanceof TenantToBaseClassPremission).map(f->(TenantToBaseClassPremission)f).collect(Collectors.groupingBy(f->f.getLeftside().getId(),Collectors.groupingBy(f->f.getRightside().getId())));

        PermissionSummaryResponse permissionSummaryResponse=new PermissionSummaryResponse()
                .setPermissionGroupToBaseclasses(permissionGroupToBaseclassMap);
        List<PermissionSummaryEntry> entries=new ArrayList<>();
        for (User user : permissionSummaryRequest.getUsers()) {
            List<RoleToUser> roles = roleMap.getOrDefault(user.getId(), new ArrayList<>());
            List<TenantToUser> tenants = tenantMap.getOrDefault(user.getId(), new ArrayList<>());
            Map<String,List<UserToBaseClass>> userToBaseclassSpecificMap=userToBaseclassMap.getOrDefault(user.getId(),new HashMap<>());
            Map<String, List<UserToBaseClass>> userToClazz = userToClazzMap.getOrDefault(user.getId(),new HashMap<>());
            Map<String, List<UserToBaseClass>> userToPermissionGroup = userToPermissionGroupMap.getOrDefault(user.getId(),new HashMap<>());
            PermissionSummaryEntry permissionSummaryEntry=new PermissionSummaryEntry()
                    .setUser(user)
                    .setRoles(roles)
                    .setTenants(tenants)
                    .setCreator(baseclasses.parallelStream().filter(f->f.getCreator()!=null&&f.getCreator().getId().equals(user.getId())).collect(Collectors.toMap(f->f.getId(),f->f)))
                    .setUserToClazz(userToClazz)
                    .setUserToPermissionGroup(userToPermissionGroup)
                    .setUserToBaseClasses(userToBaseclassSpecificMap);



            for (Baseclass baseclass :rightSide) {
                Map<String,List<RoleToBaseclass>> outputMapRoles;
                Map<String,List<TenantToBaseClassPremission>> outputMapTenant;

                boolean isClazz = baseclass instanceof Clazz;
                if(isClazz){
                    outputMapRoles=permissionSummaryEntry.getRoleToClazz();
                    outputMapTenant=permissionSummaryEntry.getTenantToClazz();

                }
                else{
                    if(baseclass instanceof PermissionGroup){
                        outputMapRoles=permissionSummaryEntry.getRoleToPermissionGroup();
                        outputMapTenant=permissionSummaryEntry.getTenantToPermissionGroup();
                    }
                    else{
                        outputMapRoles= permissionSummaryEntry.getRoleToBaseclasses();
                        outputMapTenant=permissionSummaryEntry.getTenantToBaseClassPremissions();
                    }
                }

                String baseId = baseclass.getId();
                Map<String,Baseclass> relatedBaseclasses=relatedBaseclass.getOrDefault(baseId,new HashMap<>());
                for (Baseclass related : relatedBaseclasses.values()) {
                    String id=related.getId();
                    for (RoleToUser role : roles) {
                        if(!isClazz||(role.getLeftside().getTenant()!=null &&related.getTenant()!=null&& role.getLeftside().getTenant().getId().equals(related.getTenant().getId()))){
                            Map<String,List<RoleToBaseclass>> map=roleToBaseclassMap.get(role.getLeftside().getId());
                            if(map!=null){
                                List<RoleToBaseclass> list=map.get(baseId);
                                if(list!=null){
                                    outputMapRoles.put(id,list);
                                }
                            }
                        }


                    }

                    for (TenantToUser tenantToUser : tenants) {
                        if(tenantToUser.getLeftside()==null){
                            continue;
                        }
                        if(!isClazz || (related.getTenant()!=null&&tenantToUser.getLeftside().getId().equals(related.getTenant().getId()))){
                            Map<String,List<TenantToBaseClassPremission>> map=tenantToBaseclassMap.get(tenantToUser.getLeftside().getId());
                            if(map!=null){
                                List<TenantToBaseClassPremission> list=map.get(baseId);
                                if(list!=null){
                                    outputMapTenant.put(id,list);
                                }
                            }
                        }


                    }
                }



            }
            for (Baseclass baseclass : baseclasses) {
                permissionSummaryEntry.updateAllowed(baseclass.getId());
                permissionSummaryEntry.updateExplanation(baseclass.getId());


            }
            entries.add(permissionSummaryEntry);

        }
        return permissionSummaryResponse.setPermissionSummaryEntries(entries);

    }
}

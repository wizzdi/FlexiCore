/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import com.flexicore.data.RoleToUserRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.model.*;
import com.flexicore.request.RoleToUserCreate;
import com.flexicore.request.RoleToUserFilter;
import com.flexicore.request.RoleToUserUpdate;
import com.flexicore.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.*;
import java.util.stream.Collectors;

@Primary
@Component
public class RoleToUserService implements com.flexicore.service.RoleToUserService {

    private static final Logger log= LoggerFactory.getLogger(RoleToUserService.class);

    @Autowired
    private RoleToUserRepository roleToUserRepository;

    @Autowired
    private BaseclassNewService baseclassService;



    @Override
    public RoleToUser createRoleToUser(RoleToUserCreate roleToUserCreate, SecurityContext securityContext){
        RoleToUser roleToUser=createRoleToUserNoMerge(roleToUserCreate,securityContext);
        roleToUserRepository.merge(roleToUser);
        return roleToUser;
    }


    @Override
    public RoleToUser createRoleToUserNoMerge(RoleToUserCreate roleToUserCreate, SecurityContext securityContext){
        RoleToUser roleToUser=new RoleToUser(roleToUserCreate.getName(),securityContext);
        updateRoleToUserNoMerge(roleToUser,roleToUserCreate);
        return roleToUser;
    }

    private boolean updateRoleToUserNoMerge(RoleToUser roleToUser, RoleToUserCreate roleToUserCreate) {
        boolean update = baseclassService.updateBaseclassNoMerge(roleToUserCreate, roleToUser);
        if(roleToUserCreate.getRole()!=null && (roleToUser.getLeftside()==null || !roleToUserCreate.getRole().getId().equals(roleToUser.getLeftside().getId()))){
            roleToUser.setRole(roleToUserCreate.getRole());
            update=true;
        }
        if(roleToUserCreate.getUser()!=null && (roleToUser.getRightside()==null || !roleToUserCreate.getUser().getId().equals(roleToUser.getRightside().getId()))){
            roleToUser.setUser(roleToUserCreate.getUser());
            update=true;
        }
        return update;
    }



    @Override
    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString, SecurityContext securityContext) {
        return roleToUserRepository.getByIdOrNull(id, c, batchString, securityContext);
    }

    @Override
    public void massMerge(List<?> toMerge) {
        roleToUserRepository.massMerge(toMerge);
    }

    @Override
    public void merge(Object base) {
        roleToUserRepository.merge(base);
    }


    @Override
    public void validate(RoleToUserFilter roleToUserFilter, SecurityContext securityContext) {
        Set<String> userIds=roleToUserFilter.getUsersIds();
        Map<String,User> userMap=userIds.isEmpty()?new HashMap<>(): roleToUserRepository.listByIds(User.class,userIds,securityContext).stream().collect(Collectors.toMap(f->f.getId(), f->f));
        userIds.removeAll(userMap.keySet());
        if(!userIds.isEmpty()){
            throw new BadRequestException("No Users with ids " +userIds);
        }
        roleToUserFilter.setUsers(new ArrayList<>(userMap.values()));

        Set<String> rolesIds=roleToUserFilter.getRolesIds();
        Map<String,Role> roleMap=rolesIds.isEmpty()?new HashMap<>(): roleToUserRepository.listByIds(Role.class,rolesIds,securityContext).stream().collect(Collectors.toMap(f->f.getId(), f->f));
        rolesIds.removeAll(roleMap.keySet());
        if(!rolesIds.isEmpty()){
            throw new BadRequestException("No Roles with ids " +rolesIds);
        }
        roleToUserFilter.setRoles(new ArrayList<>(roleMap.values()));
    }

    @Override
    public void validate(RoleToUserCreate roleToUserCreate, SecurityContext securityContext) {
        baseclassService.validate(roleToUserCreate,securityContext);
        String userId = roleToUserCreate.getUserId();
        User user=  userId!=null?getByIdOrNull(userId,User.class,null,securityContext):null;
        if(user==null){
            throw new BadRequestException("No User with id "+userId);
        }
        roleToUserCreate.setUser(user);

        String roleId = roleToUserCreate.getRoleId();
        Role role=  roleId!=null?getByIdOrNull(roleId,Role.class,null,securityContext):null;
        if(role==null){
            throw new BadRequestException("No Role with id "+roleId);
        }
        roleToUserCreate.setRole(role);
    }

    @Override
    public PaginationResponse<RoleToUser> getAllRoleToUsers(RoleToUserFilter roleToUserFilter, SecurityContext securityContext) {

        List<RoleToUser> list= listAllRoleToUsers(roleToUserFilter, securityContext);
        long count= roleToUserRepository.countAllRoleToUsers(roleToUserFilter,securityContext);
        return new PaginationResponse<>(list,roleToUserFilter,count);

    }

    @Override
    public List<RoleToUser> listAllRoleToUsers(RoleToUserFilter roleToUserFilter, SecurityContext securityContext) {
        return roleToUserRepository.getAllRoleToUsers(roleToUserFilter,securityContext);
    }

    @Override
    public RoleToUser updateRoleToUser(RoleToUserUpdate roleToUserCreate, SecurityContext securityContext) {
        RoleToUser roleToUser=roleToUserCreate.getRoleToUser();
        if(updateRoleToUserNoMerge(roleToUser,roleToUserCreate)){
            roleToUserRepository.merge(roleToUser);
        }
        return roleToUser;
    }
}

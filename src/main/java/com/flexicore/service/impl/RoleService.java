/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.flexicore.data.RoleRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.model.*;
import com.flexicore.request.RoleCreate;
import com.flexicore.request.RoleFilter;
import com.flexicore.request.RoleToBaseclassCreate;
import com.flexicore.request.RoleUpdate;
import com.flexicore.security.SecurityContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.ws.rs.BadRequestException;

@Primary
@Component
public class RoleService implements com.flexicore.service.RoleService {
	private Logger log = Logger.getLogger(getClass().getCanonicalName());

	@Autowired
	private RoleRepository rolerepository;

	@Autowired
	private BaseclassNewService baseclassService;


	@Override
	public Role createRole(String roleName, SecurityContext securityContext){
		Role role = new Role(roleName,securityContext);
		rolerepository.merge(role);
		return role;
	}
	@Override
	public Role createRole(RoleCreate roleCreate, SecurityContext securityContext){
		Role role=createRoleNoMerge(roleCreate,securityContext);
		rolerepository.merge(role);
		return role;
	}


	@Override
	public Role createRoleNoMerge(RoleCreate roleCreate, SecurityContext securityContext){
		Role role=new Role(roleCreate.getName(),securityContext);
		updateRoleNoMerge(role,roleCreate);
		return role;
	}

	private boolean updateRoleNoMerge(Role role, RoleCreate roleCreate) {
		return baseclassService.updateBaseclassNoMerge(roleCreate,role);
	}


	@Override
	public Role findById(String id, SecurityContext securityContext) {
		return rolerepository.getById(id,Role.class,null,securityContext);
	}

	@Override
	public List<Role> getAllFiltered(QueryInformationHolder<Role> queryInformationHolder) {
		return rolerepository.getAllFiltered(queryInformationHolder);
	}

    @Override
	public List<Role> getAllUserRoles(QueryInformationHolder<Role> queryInformationHolder, User user){
		return rolerepository.getAllUserRoles(queryInformationHolder,user);
	}

	@Override
	public <T extends Baseclass>  T getById(String id, Class<T> c, List<String> batchString, SecurityContext securityContext){
		return rolerepository.getById(id,c,batchString,securityContext);
	}

	@Override
	public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString, SecurityContext securityContext) {
		return rolerepository.getByIdOrNull(id, c, batchString, securityContext);
	}

	@Override
	public void massMerge(List<?> toMerge) {
		rolerepository.massMerge(toMerge);
	}

	@Override
	public void merge(Object base) {
		rolerepository.merge(base);
	}

	@Override
	public void refrehEntityManager() {
		rolerepository.refrehEntityManager();
	}

	@Override
	public RoleToBaseclass createRoleToBaseclassNoMerge(RoleToBaseclassCreate roleToBaseclassCreate, SecurityContext securityContext) {

		RoleToBaseclass roleToBaseclass=new RoleToBaseclass("RoleToBaseclass",securityContext);
		roleToBaseclass.setLeftside(roleToBaseclassCreate.getRole());
		roleToBaseclass.setBaseclass(roleToBaseclassCreate.getBaseclass());
		roleToBaseclass.setValue(roleToBaseclassCreate.getOperation());
		return roleToBaseclass;
	}

	@Override
	public void validate(RoleFilter roleFilter, SecurityContext securityContext) {
		Set<String> userIds=roleFilter.getUsersIds();
		Map<String,User> userMap=userIds.isEmpty()?new HashMap<>():rolerepository.listByIds(User.class,userIds,securityContext).stream().collect(Collectors.toMap(f->f.getId(), f->f));
		userIds.removeAll(userMap.keySet());
		if(!userIds.isEmpty()){
			throw new BadRequestException("No Users with ids " +userIds);
		}
		roleFilter.setUsers(new ArrayList<>(userMap.values()));
	}

	@Override
	public void validate(RoleCreate roleCreate, SecurityContext securityContext) {
		baseclassService.validate(roleCreate,securityContext);
	}

	@Override
	public PaginationResponse<Role> getAllRoles(RoleFilter roleFilter, SecurityContext securityContext) {

		List<Role> list= listAllRoles(roleFilter, securityContext);
		long count=rolerepository.countAllRoles(roleFilter,securityContext);
		return new PaginationResponse<>(list,roleFilter,count);

	}

	@Override
	public List<Role> listAllRoles(RoleFilter roleFilter, SecurityContext securityContext) {
		return rolerepository.getAllRoles(roleFilter,securityContext);
	}

	@Override
	public Role updateRole(RoleUpdate roleCreate, SecurityContext securityContext) {
		Role role=roleCreate.getRole();
		if(updateRoleNoMerge(role,roleCreate)){
			rolerepository.merge(role);
		}
		return role;
	}
}

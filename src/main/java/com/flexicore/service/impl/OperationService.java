/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import com.flexicore.annotations.IOperation;
import com.flexicore.data.OperationRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.model.*;
import com.flexicore.model.dynamic.DynamicInvoker;
import com.flexicore.request.CreateOperationRequest;
import com.flexicore.request.OperationCreate;
import com.flexicore.request.OperationFiltering;
import com.flexicore.request.OperationUpdate;
import com.flexicore.security.SecurityContext;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.BadRequestException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Primary
@Component
public class OperationService implements com.flexicore.service.OperationService {

	@Autowired
	private OperationRepository operationRepository;

	@Autowired
	private BaselinkService baselinkService;

	@Autowired
	private BaseclassNewService baseclassNewService;


	private static Cache<String,Boolean> accessControlCache=CacheBuilder.newBuilder().maximumSize(100).expireAfterWrite(15, TimeUnit.MINUTES).build();



	private Logger logger = Logger.getLogger(getClass().getCanonicalName());



	public OperationService() {
		// TODO Auto-generated constructor stub
	}


	@Override
	public IOperation getIOperationFromApiOperation(io.swagger.v3.oas.annotations.Operation apiOperation, Method method) {
		return operationRepository.getIOperationFromApiOperation(apiOperation,method);
	}


	@Override
	public Operation findById(String id){
		return operationRepository.findById(id);
	}

	@Override
	public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString, SecurityContext securityContext) {
		return operationRepository.getByIdOrNull(id, c, batchString, securityContext);
	}

	@Override
	@Transactional
	public void merge(Object o) {
		operationRepository.merge(o);
	}



	/**
	 * check f
	 *
	 * @param operation
	 * @param tenant
	 * @return
	 */
	@Override
	public boolean tennantAllowed(Operation operation, Tenant tenant) {
		IOperation.Access access=IOperation.Access.allow;
		String cacheKey= com.flexicore.service.OperationService.getAccessControlKey(TENANT_TYPE,operation.getId(),tenant.getId(),access);
		Boolean val=accessControlCache.getIfPresent(cacheKey);
		if(val!=null){
			return val;
		}
		Baselink link = baselinkService.findBySidesAndValue(tenant, operation,access.name() );
		val= link != null;
		accessControlCache.put(cacheKey,val);
		return val;
	}

	@Override
	public boolean tennantDenied(Operation operation, Tenant tenant) {
		IOperation.Access access=IOperation.Access.deny;
		String cacheKey= com.flexicore.service.OperationService.getAccessControlKey(TENANT_TYPE,operation.getId(),tenant.getId(),access);
		Boolean val=accessControlCache.getIfPresent(cacheKey);
		if(val!=null){
			return val;
		}
		Baselink link = baselinkService.findBySidesAndValue(tenant, operation, access.name());
		val= link != null;
		accessControlCache.put(cacheKey,val);
		return val;
	}


	@Override
	public List<Operation> findAllOrderedByName(QueryInformationHolder<Operation> queryInformationHolder) {

		return operationRepository.getAllFiltered(queryInformationHolder);
	}

	@Override
	public boolean userAllowed(Operation operation, User user) {
		return checkUser(operation, user, IOperation.Access.allow);

	}

	@Override
	public boolean userDenied(Operation operation, User user) {
		return checkUser(operation, user, IOperation.Access.deny);
	}

	@Override
	public boolean roleAllowed(Operation operation, User user) {
		IOperation.Access access = IOperation.Access.allow;
		String cacheKey= com.flexicore.service.OperationService.getAccessControlKey(ROLE_TYPE,operation.getId(),user.getId(),access);
		Boolean val=accessControlCache.getIfPresent(cacheKey);
		if(val!=null){
			return val;
		}
		val = operationRepository.checkRole(operation, user, access);
		accessControlCache.put(cacheKey,val);

		return val;

	}

	@Override
	public boolean roleDenied(Operation operation, User user) {
		IOperation.Access access = IOperation.Access.deny;

		String cacheKey= com.flexicore.service.OperationService.getAccessControlKey(ROLE_TYPE,operation.getId(),user.getId(),access);
		Boolean val=accessControlCache.getIfPresent(cacheKey);
		if(val!=null){
			return val;
		}
		val = operationRepository.checkRole(operation, user, access);
		accessControlCache.put(cacheKey,val);
		return val;
	}


	@Override
	public boolean checkUser(Operation operation, User user, IOperation.Access access) {
		String cacheKey= com.flexicore.service.OperationService.getAccessControlKey(USER_TYPE,operation.getId(),user.getId(),access);
		Boolean val=accessControlCache.getIfPresent(cacheKey);
		if(val!=null){
			return val;
		}
		 val = operationRepository.checkUser(operation, user, access);
		accessControlCache.put(cacheKey,val);
		return val;
	}

	@Override
	public <T extends Baseclass> List<T> getAllFiltered(QueryInformationHolder<T> queryInformationHolder) {
		return operationRepository.getAllFiltered(queryInformationHolder);
	}

	@Override
	public void updateCahce(Operation operation) {
		operationRepository.updateCahce(operation);
	}

	@Override
	public void refrehEntityManager() {
		operationRepository.refrehEntityManager();
		baselinkService.refrehEntityManager();
	}

	@Override
	@Transactional
	public void massMerge(List<?> toMerge) {
		operationRepository.massMerge(toMerge);
	}

	@Override
	@Transactional
	public Operation createOperation(CreateOperationRequest createOperationRequest) {
		Operation operation = createOperationNoMerge(createOperationRequest);
		merge(operation);
		return operation;
	}

	@Override
	public Operation createOperationNoMerge(CreateOperationRequest createOperationRequest) {
		Operation operation = new Operation(createOperationRequest.getName(), null);
		if(createOperationRequest.getId()!=null){
			operation.setId(createOperationRequest.getId());

		}
		updateOperationNoMerge(createOperationRequest,operation);
		return operation;
	}

	@Override
	public boolean updateOperationNoMerge(CreateOperationRequest updateOperationRequest, Operation operation) {
		boolean update=false;
		if(updateOperationRequest.isAuditable()!=null && operation.isAuditable()!=updateOperationRequest.isAuditable()){
			operation.setAuditable(updateOperationRequest.isAuditable());
			update=true;
		}
		if(updateOperationRequest.getDescription()!=null && !updateOperationRequest.getDescription().equals(operation.getDescription())){
			operation.setDescription(updateOperationRequest.getDescription());
			update=true;
		}
		if(updateOperationRequest.getName()!=null && !updateOperationRequest.getName().equals(operation.getName())){
			operation.setName(updateOperationRequest.getName());
			update=true;
		}
		if(updateOperationRequest.getAccess()!=null && !updateOperationRequest.getAccess().equals(operation.getDefaultaccess())){
			operation.setDefaultaccess(updateOperationRequest.getAccess());
			update=true;
		}
		if(updateOperationRequest.getDynamicInvoker()!=null && (operation.getDynamicInvoker()==null||!updateOperationRequest.getDynamicInvoker().getId().equals(operation.getDynamicInvoker().getId()))){
			operation.setDynamicInvoker(updateOperationRequest.getDynamicInvoker());
			update=true;
		}
		if(!operation.isSystemObject()){
			operation.setSystemObject(true);
			update=true;
		}

		return update;
	}


	@Override
	public void handleOperationRelatedClassesNoMerge(Operation operation, Class<? extends Baseclass>[] related, Map<String, OperationToClazz> existingMap, List<Object> toMerge) {
		for (Class<? extends Baseclass> relatedClazz : related) {
			String linkId = Baseclass.generateUUIDFromString(operation.getId() + relatedClazz.getCanonicalName());
			OperationToClazz operationToClazz = existingMap.get(linkId);
			if (operationToClazz == null) {
					operationToClazz = new OperationToClazz("OperationToClazz", null);
					operationToClazz.setClazz(Baseclass.getClazzbyname(relatedClazz.getCanonicalName()));
					operationToClazz.setOperation(operation);
					operationToClazz.setId(linkId);
					operationToClazz.setSystemObject(true);
					toMerge.add(operationToClazz);
					existingMap.put(linkId,operationToClazz);

			}
			if(!operationToClazz.isSystemObject()){
				operationToClazz.setSystemObject(true);
				toMerge.add(operationToClazz);
			}

		}
	}

	@Override
	public List<OperationToClazz> getRelatedClasses(Set<String> operationIds) {
		return operationIds.isEmpty()?new ArrayList<>():operationRepository.getRelatedClasses(operationIds);
	}
	@Override
	public PaginationResponse<Operation> getAllOperations(OperationFiltering operationFiltering, SecurityContext securityContext) {
		QueryInformationHolder<Operation> cats = new QueryInformationHolder<>(operationFiltering, Operation.class, securityContext);

		List<Operation> list = operationRepository.getAllFiltered(cats);
		long count = operationRepository.countAllFiltered(cats);
		return new PaginationResponse<>(list, operationFiltering, count);
	}

	@Override
	public List<Operation> listAllOperations(OperationFiltering operationFiltering, SecurityContext securityContext) {
		return operationRepository.listAllOperations(operationFiltering, securityContext);
	}

	@Override
	public Operation createOperationNoMerge(OperationCreate operationCreate, SecurityContext securityContext) {
		Operation operation = new Operation(operationCreate.getName(), securityContext);
		updateOperationNoMerge(operationCreate, operation);
		return operation;
	}

	@Override
	public Operation createOperation(OperationCreate operationCreate, SecurityContext securityContext) {
		Operation operation = createOperationNoMerge(operationCreate, securityContext);
		operationRepository.merge(operation);
		return operation;
	}

	@Override
	public Operation updateOperation(OperationUpdate operationUpdate, SecurityContext securityContext) {
		Operation operation = operationUpdate.getOperation();
		if (updateOperationNoMerge(operationUpdate, operation)) {
			operationRepository.merge(operation);
		}
		return operation;
	}

	@Override
	public boolean updateOperationNoMerge(OperationCreate operationCreate, Operation operation) {
		boolean update = baseclassNewService.updateBaseclassNoMerge(operationCreate, operation);
		if (operationCreate.getAuditable() != null && operationCreate.getAuditable() != operation.isAuditable()) {
			operation.setAuditable(operationCreate.getAuditable());
			update = true;
		}
		if (operationCreate.getDefaultaccess() != null && operationCreate.getDefaultaccess() != operation.getDefaultaccess()) {
			operation.setDefaultaccess(operationCreate.getDefaultaccess());
			update = true;
		}
		if (operationCreate.getDynamicInvoker() != null && (operation.getDynamicInvoker() == null || !operationCreate.getDynamicInvoker().getId().equals(operation.getDynamicInvoker().getId()))) {
			operation.setDynamicInvoker(operationCreate.getDynamicInvoker());
			update = true;
		}
		return update;
	}

	public void validate(OperationCreate operationCreate, SecurityContext securityContext) {
		baseclassNewService.validateCreate(operationCreate,securityContext);
		String dynamicInvokerId=operationCreate.getDynamicInvokerId();
		DynamicInvoker dynamicInvoker=dynamicInvokerId!=null?getByIdOrNull(dynamicInvokerId,DynamicInvoker.class,null,securityContext):null;
		if (dynamicInvoker == null && dynamicInvokerId != null) {
			throw new BadRequestException("No Dynamic Invoker with id"+dynamicInvokerId);
		}
		operationCreate.setDynamicInvoker(dynamicInvoker);
	}


	public void validate(OperationFiltering filter, SecurityContext securityContext) {
		baseclassNewService.validateFilter(filter, securityContext);
		Set<String> dynamicInvokerIds = filter.getDynamicInvokerIds().stream().map(f -> f.getId()).collect(Collectors.toSet());
		Map<String, DynamicInvoker> dynamicInvokerMap = dynamicInvokerIds.isEmpty() ? new HashMap<>() : operationRepository.listByIds(DynamicInvoker.class, dynamicInvokerIds, securityContext).stream().collect(Collectors.toMap(f -> f.getId(), f -> f));
		dynamicInvokerIds.removeAll(dynamicInvokerMap.keySet());
		if (!dynamicInvokerIds.isEmpty()) {
			throw new BadRequestException("No Invokers with ids" + dynamicInvokerIds);
		}
		filter.setDynamicInvokers(new ArrayList<>(dynamicInvokerMap.values()));
	}
}

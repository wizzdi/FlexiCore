/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.rest;

import com.flexicore.annotations.OperationsInside;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.data.jsoncontainers.TreeCreationContainer;
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.FileResource;
import com.flexicore.model.Presenter;
import com.flexicore.model.dynamic.DynamicExecution;
import com.flexicore.model.tree.Tree;
import com.flexicore.model.tree.TreeNode;
import com.flexicore.request.*;
import com.flexicore.response.SaveTreeNodeStatusResponse;
import com.flexicore.response.TreeNodeStatusResponse;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.TreeService;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/tree")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "Tree")
@Tag(name = "Core")
@ExternalDocumentation(description = "The tree service provides client side with persistence for trees and any hierarchical type of widget," +
		" multiple tree objects can be stored in the system. Trees define the hierarchy and allow nodes to be either static or dynamic",url="https://docs.google.com/document/d/1RtXPUqgeICUU-4OS1oBjq8_N9ZL5JDuuFlKN3gJZj3Q/edit?usp=sharing")
public class TreeRESTService implements RESTService {
	@Autowired
	private TreeService service;


	@POST
	@Produces("application/json")
	@Operation(summary = "getAllTrees", description = "lists all trees")
	@Path("getAllTrees")
	public PaginationResponse<Tree> getAllTrees(
			@HeaderParam("authenticationKey") String authenticationKey,
			TreeFilter treeFilter,
			@Context SecurityContext securityContext) {

		return service.getAllTrees(treeFilter, securityContext);
	}

	@POST
	@Produces("application/json")
	@Operation(summary = "getAllTreeNodes", description = "lists all children nodes, parent is specified in the TreeNodeFiler")
	@Path("getAllTreeNodes")
	public PaginationResponse<TreeNode> getAllTreeNodes(
			@HeaderParam("authenticationKey") String authenticationKey,
			TreeNodeFilter treeFilter,
			@Context SecurityContext securityContext) {
		validateTreeNodFiltering(treeFilter,securityContext);

		return service.getAllTreeNodes(treeFilter, securityContext);
	}


	@POST
	@Produces("application/json")
	@Operation(summary = "saveTreeNodeStatus", description = "save Tree node Status, the TreeNode Status saves the status for the current user and allows a client to show the tree in the same expansion collapsing saved")
	@Path("saveTreeNodeStatus")
	public SaveTreeNodeStatusResponse saveTreeNodeStatus(
			@HeaderParam("authenticationKey") String authenticationKey,
			@RequestBody(description = "Stores  a list of NodeID,boolean pairs",required = true)  SaveTreeNodeStatusRequest saveTreeNodeStatusRequest,
			@Context SecurityContext securityContext) {
		validateSaveTreeNodeStatusRequest(saveTreeNodeStatusRequest,securityContext);

		return service.saveTreeNodeStatus(saveTreeNodeStatusRequest, securityContext);
	}

	private void validateSaveTreeNodeStatusRequest(SaveTreeNodeStatusRequest saveTreeNodeStatusRequest, SecurityContext securityContext) {
		if(saveTreeNodeStatusRequest.getNodeStatus().isEmpty()){
			throw new BadRequestException("No status was provided");
		}
		Set<String> ids=saveTreeNodeStatusRequest.getNodeIdtoTree().keySet();
		List<TreeNode> treeNodes=service.listByIds(TreeNode.class,ids,securityContext);
		ids.removeAll(treeNodes.parallelStream().map(f->f.getId()).collect(Collectors.toSet()));
		if(!ids.isEmpty()){
			throw new BadRequestException("No Tree nodes with ids " +ids);
		}
		Map<String, TreeNode> treeNodeMap = treeNodes.parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));
		saveTreeNodeStatusRequest.setNodeIdtoTree(treeNodeMap);
	}

	@POST
	@Produces("application/json")
	@Operation(summary = "getTreeNodeStatus", description = "get Tree nodes Status, get the stored values for the list of nodes stored in saveTreeNodeStatus")
	@Path("getTreeNodeStatus")
	public TreeNodeStatusResponse getTreeNodeStatus(
			@HeaderParam("authenticationKey") String authenticationKey,
			TreeNodeStatusRequest treeNodeStatusRequest,
			@Context SecurityContext securityContext) {
		validateTreeNodeStatusRequest(treeNodeStatusRequest,securityContext);

		return service.getTreeNodeStatus(treeNodeStatusRequest, securityContext);
	}

	private void validateTreeNodeStatusRequest(TreeNodeStatusRequest treeNodeStatusRequest, SecurityContext securityContext) {
		if(treeNodeStatusRequest.getNodeIds().isEmpty()){
			throw new BadRequestException("Tree Nodes must be provided");
		}
		List<TreeNode> treeNodes=service.listByIds(TreeNode.class,treeNodeStatusRequest.getNodeIds(),securityContext);
		treeNodeStatusRequest.getNodeIds().removeAll(treeNodes.parallelStream().map(f->f.getId()).collect(Collectors.toSet()));
		if(!treeNodeStatusRequest.getNodeIds().isEmpty()){
			throw new BadRequestException("No Tree nodes with ids " + treeNodeStatusRequest.getNodeIds());
		}
		treeNodeStatusRequest.setTreeNodes(treeNodes);
	}


	@PUT
	@Produces("application/json")
	@Operation(summary = "updateTree", description = "update tree by tree ID")
	@Path("updateTree")
	public Tree updateTree(
			@HeaderParam("authenticationKey") String authenticationKey,
			@RequestBody(description = "Provide treeId, root node ID , tree name , tree description") TreeUpdateContainer updateTree,
			@Context SecurityContext securityContext) {
		Tree tree=updateTree.getTreeId()!=null?service.getByIdOrNull(updateTree.getTreeId(),Tree.class,null,securityContext):null;
		if(tree==null){
			throw new BadRequestException("No Tree with id "+updateTree.getTreeId());
		}
		updateTree.setTree(tree);
		validateTreeCreationContainer(updateTree,securityContext);

		return service.updateTree(updateTree, securityContext);
	}


	@POST
	@Produces("application/json")
	@Operation(summary = "createTree", description = "create tree, provide tree name , description and root node")
	@Path("createTree")
	public Tree createTree(
			@HeaderParam("authenticationKey") String authenticationKey,
			@RequestBody(description = "Tree name, description, root node ID , root node should be created before the tree is created") TreeCreationContainer treeCreationContainer,
			@Context SecurityContext securityContext) {
		validateTreeCreationContainer(treeCreationContainer,securityContext);

		return service.createTree(treeCreationContainer, securityContext);
	}

	private void validateTreeCreationContainer(TreeCreationContainer treeCreationContainer, SecurityContext securityContext) {
		TreeNode root=treeCreationContainer.getRootId()!=null?service.getByIdOrNull(treeCreationContainer.getRootId(),TreeNode.class,null,securityContext):null;
		if(root==null && treeCreationContainer.getRootId()!=null){
			throw new BadRequestException("No Tree Node With id "+treeCreationContainer.getRootId());
		}
		treeCreationContainer.setRoot(root);

	}


	@POST
	@Produces("application/json")
	@Operation(summary = "createTreeNode", description = "create tree node")
	@Path("createTreeNode")
	public TreeNode createTreeNode(
			@HeaderParam("authenticationKey") String authenticationKey,
			TreeNodeCreate treeNodeCreationContainer,
			@Context SecurityContext securityContext) {
		validateTreeNodeCreationContainer(treeNodeCreationContainer,securityContext);

		return service.createTreeNode(treeNodeCreationContainer, securityContext);
	}

	@PUT
	@Produces("application/json")
	@Operation(summary = "updateTreeNode", description = "update tree node")
	@Path("updateTreeNode")
	public TreeNode updateTreeNode(
			@HeaderParam("authenticationKey") String authenticationKey,
			TreeNodeUpdateContainer treeNodeCreationContainer,
			@Context SecurityContext securityContext) {
		TreeNode treeNode=treeNodeCreationContainer.getNodeId()!=null?service.getByIdOrNull(treeNodeCreationContainer.getNodeId(),TreeNode.class,null,securityContext):null;
		if(treeNode==null){
			throw new BadRequestException("No Tree node with id "+treeNodeCreationContainer.getNodeId());
		}
		treeNodeCreationContainer.setTreeNode(treeNode);
		validateTreeNodeCreationContainer(treeNodeCreationContainer,securityContext);

		return service.updateTreeNode(treeNodeCreationContainer, securityContext);
	}

	private void validateTreeNodeCreationContainer(TreeNodeCreate treeNodeCreationContainer, SecurityContext securityContext) {
		String parentId = treeNodeCreationContainer.getParentId();
		TreeNode parent= parentId !=null?service.getByIdOrNull(parentId,TreeNode.class,null,securityContext):null;
		if(parent==null && parentId !=null){
			throw new BadRequestException("No Tree Node With id "+ parentId);
		}
		treeNodeCreationContainer.setParent(parent);

		String dynamicExecutionId = treeNodeCreationContainer.getDynamicExecutionId();
		DynamicExecution dynamicExecution= dynamicExecutionId !=null?service.getByIdOrNull(dynamicExecutionId,DynamicExecution.class,null,securityContext):null;
		if(dynamicExecution==null&& dynamicExecutionId !=null){
			throw new BadRequestException("No Dynamic Execution with id "+ dynamicExecutionId);
		}
		treeNodeCreationContainer.setDynamicExecution(dynamicExecution);

		String presenterId = treeNodeCreationContainer.getPresenterId();
		Presenter presenter= presenterId !=null?service.getByIdOrNull(presenterId,Presenter.class,null,securityContext):null;
		if(presenter==null && presenterId !=null){
			throw new BadRequestException("No Presenter With id "+ presenterId);
		}
		treeNodeCreationContainer.setPresenter(presenter);

		String iconId = treeNodeCreationContainer.getIconId();
		FileResource icon= iconId !=null?service.getByIdOrNull(iconId, FileResource.class,null,securityContext):null;
		if(icon==null && iconId !=null){
			throw new BadRequestException("No FileResource With id "+ iconId);
		}
		treeNodeCreationContainer.setIcon(icon);


	}

	private void validateTreeNodFiltering(TreeNodeFilter treeNodeFilter, SecurityContext securityContext) {
		TreeNode parent=treeNodeFilter.getParentId()!=null?service.getByIdOrNull(treeNodeFilter.getParentId(),TreeNode.class,null,securityContext):null;
		if(parent==null && treeNodeFilter.getParentId()!=null){
			throw new BadRequestException("No Tree Node With id "+treeNodeFilter.getParentId());
		}
		treeNodeFilter.setParent(parent);
	}


}

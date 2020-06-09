package com.flexicore.service.impl;

import com.flexicore.data.TreeRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.data.jsoncontainers.TreeCreationContainer;
import com.flexicore.model.Baseclass;
import com.flexicore.model.User;
import com.flexicore.model.tree.Tree;
import com.flexicore.model.tree.TreeNode;
import com.flexicore.model.tree.TreeNodeToUser;
import com.flexicore.request.*;
import com.flexicore.response.SaveTreeNodeStatusResponse;
import com.flexicore.response.TreeNodeStatusResponse;
import com.flexicore.security.SecurityContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Primary
@Component
public class TreeService implements com.flexicore.service.TreeService {

    @Autowired
    private TreeRepository treeRepository;

    @Override
    public TreeNode createTreeNode(TreeNodeCreate treeNodeCreationContainer, SecurityContext securityContext) {
        TreeNode treeNode=new TreeNode(treeNodeCreationContainer.getName(),securityContext);
        updateTreeNodeNoMerge(treeNodeCreationContainer,treeNode);
        treeRepository.merge(treeNode);
        return treeNode;
    }

    @Override
    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString, SecurityContext securityContext) {
        return treeRepository.getByIdOrNull(id, c, batchString, securityContext);
    }

    @Override
    public <T extends Baseclass> List<T> listByIds(Class<T> c, Set<String> ids, SecurityContext securityContext) {
        return treeRepository.listByIds(c, ids, securityContext);
    }

    private boolean updateTreeNoMerge(TreeCreationContainer treeNodeCreationContainer, Tree treeNode) {
        boolean update=false;


        if(treeNodeCreationContainer.getName()!=null && !treeNodeCreationContainer.getName().equals(treeNode.getName())){
            treeNode.setName(treeNodeCreationContainer.getName());
            update=true;
        }

        if(treeNodeCreationContainer.getDescription()!=null && !treeNodeCreationContainer.getDescription().equals(treeNode.getDescription())){
            treeNode.setDescription(treeNodeCreationContainer.getDescription());
            update=true;
        }

        if(treeNodeCreationContainer.getRoot()!=null &&( treeNode.getRoot()==null ||!treeNodeCreationContainer.getRoot().getId().equals(treeNode.getRoot().getId()))){
            treeNode.setRoot(treeNodeCreationContainer.getRoot());
            update=true;
        }
        return update;

    }

    private boolean updateTreeNodeNoMerge(TreeNodeCreate treeNodeCreationContainer, TreeNode treeNode) {
        boolean update=false;
        if(treeNodeCreationContainer.getEager()!=null && treeNodeCreationContainer.getEager()!=treeNode.isEager()){
            treeNode.setEager(treeNodeCreationContainer.getEager());
            update=true;
        }
        if(treeNodeCreationContainer.getInvisible()!=null && treeNodeCreationContainer.getInvisible()!=treeNode.isInvisible()){
            treeNode.setInvisible(treeNodeCreationContainer.getInvisible());
            update=true;
        }

        if(treeNodeCreationContainer.getAllowFilteringEditing()!=null && treeNodeCreationContainer.getAllowFilteringEditing()!=treeNode.isAllowFilteringEditing()){
            treeNode.setAllowFilteringEditing(treeNodeCreationContainer.getAllowFilteringEditing());
            update=true;
        }
        if(treeNodeCreationContainer.getInMap()!=null && treeNodeCreationContainer.getInMap()!=treeNode.isInMap()){
            treeNode.setInMap(treeNodeCreationContainer.getInMap());
            update=true;
        }
        if(treeNodeCreationContainer.getStaticChildren()!=null && treeNodeCreationContainer.getStaticChildren()!=treeNode.isStaticChildren()){
            treeNode.setStaticChildren(treeNodeCreationContainer.getStaticChildren());
            update=true;
        }

        if(treeNodeCreationContainer.getName()!=null && !treeNodeCreationContainer.getName().equals(treeNode.getName())){
            treeNode.setName(treeNodeCreationContainer.getName());
            update=true;
        }

        if(treeNodeCreationContainer.getDescription()!=null && !treeNodeCreationContainer.getDescription().equals(treeNode.getDescription())){
            treeNode.setDescription(treeNodeCreationContainer.getDescription());
            update=true;
        }

        if(treeNodeCreationContainer.getContextString()!=null && !treeNodeCreationContainer.getContextString().equals(treeNode.getContextString())){
            treeNode.setContextString(treeNodeCreationContainer.getContextString());
            update=true;
        }

        if(treeNodeCreationContainer.getParent()!=null &&( treeNode.getParent()==null ||!treeNodeCreationContainer.getParent().getId().equals(treeNode.getParent().getId()))){
            treeNode.setParent(treeNodeCreationContainer.getParent());
            update=true;
        }

        if(treeNodeCreationContainer.getDynamicExecution()!=null &&( treeNode.getDynamicExecution()==null ||!treeNodeCreationContainer.getDynamicExecution().getId().equals(treeNode.getDynamicExecution().getId()))){
            treeNode.setDynamicExecution(treeNodeCreationContainer.getDynamicExecution());
            update=true;
        }

        if(treeNodeCreationContainer.getPresenter()!=null &&( treeNode.getPresenter()==null ||!treeNodeCreationContainer.getPresenter().getId().equals(treeNode.getPresenter().getId()))){
            treeNode.setPresenter(treeNodeCreationContainer.getPresenter());
            update=true;
        }

        if(treeNodeCreationContainer.getIcon()!=null &&( treeNode.getIcon()==null ||!treeNodeCreationContainer.getIcon().getId().equals(treeNode.getIcon().getId()))){
            treeNode.setIcon(treeNodeCreationContainer.getIcon());
            update=true;
        }
        return update;

    }

    @Override
    public Tree createTree(TreeCreationContainer treeCreationContainer, SecurityContext securityContext) {

        Tree tree=new Tree(treeCreationContainer.getName(),securityContext);
        updateTreeNoMerge(treeCreationContainer,tree);
        treeRepository.merge(tree);
        return tree;
    }

    @Override
    public PaginationResponse<Tree> getAllTrees(TreeFilter treeFilter, SecurityContext securityContext) {
        List<Tree> list=treeRepository.getAllTrees(treeFilter,securityContext);
        long count=treeRepository.countAllTrees(treeFilter,securityContext);
        return new PaginationResponse<>(list,treeFilter,count);
    }

    @Override
    public PaginationResponse<TreeNode> getAllTreeNodes(TreeNodeFilter treeFilter, SecurityContext securityContext) {
        List<TreeNode> list=treeRepository.getAllTreeNodes(treeFilter,securityContext);
        long count=treeRepository.countAllTreeNodes(treeFilter,securityContext);
        return new PaginationResponse<>(list,treeFilter,count);

    }

    @Override
    public SaveTreeNodeStatusResponse saveTreeNodeStatus(SaveTreeNodeStatusRequest saveTreeNodeStatusRequest, SecurityContext securityContext) {
        Map<String,TreeNodeToUser> links=treeRepository.getAllTreeNodeToUserLinks(saveTreeNodeStatusRequest.getNodeIdtoTree().keySet(),securityContext).parallelStream().collect(Collectors.toMap(f->f.getLeftside().getId(),f->f));
        List<Object> toMerge=new ArrayList<>();
        SaveTreeNodeStatusResponse saveTreeNodeStatusResponse=new SaveTreeNodeStatusResponse();
        for (Map.Entry<String, Boolean> entry : saveTreeNodeStatusRequest.getNodeStatus().entrySet()) {
            TreeNodeToUser link=links.get(entry.getKey());
            TreeNode treeNode=saveTreeNodeStatusRequest.getNodeIdtoTree().get(entry.getKey());
            if(link==null){
                link=createTreeNodeToTenantLinkNoMerge(treeNode,securityContext.getUser(),entry.getValue(),securityContext);
                toMerge.add(link);
                saveTreeNodeStatusResponse.setCreated(saveTreeNodeStatusResponse.getCreated()+1);

            }
            else{
                if(link.isNodeOpen()!=entry.getValue()){
                    link.setNodeOpen(entry.getValue());
                    toMerge.add(link);
                    saveTreeNodeStatusResponse.setUpdated(saveTreeNodeStatusResponse.getUpdated()+1);

                }
            }
        }
        treeRepository.massMerge(toMerge);
        return saveTreeNodeStatusResponse;

    }

    private TreeNodeToUser createTreeNodeToTenantLinkNoMerge(TreeNode treeNode, User user, Boolean value,SecurityContext securityContext) {
        TreeNodeToUser link=new TreeNodeToUser("link",securityContext);
        link.setTreeNode(treeNode);
        link.setUser(user);
        return link;
    }

    @Override
    public TreeNodeStatusResponse getTreeNodeStatus(TreeNodeStatusRequest treeNodeStatusRequest, SecurityContext securityContext) {
        Map<String,Boolean> links=treeRepository.getAllTreeNodeToUserLinks(treeNodeStatusRequest.getNodeIds(),securityContext).parallelStream().collect(Collectors.toMap(f->f.getLeftside().getId(),f->f.isNodeOpen()));
        Map<String,Boolean> res=treeNodeStatusRequest.getNodeIds().parallelStream().collect(Collectors.toMap(f->f,f->links.getOrDefault(f,false)));
        return new TreeNodeStatusResponse(res);

    }

    @Override
    public TreeNode updateTreeNode(TreeNodeUpdateContainer treeNodeCreationContainer, SecurityContext securityContext) {
        TreeNode treeNode = treeNodeCreationContainer.getTreeNode();
        if(updateTreeNodeNoMerge(treeNodeCreationContainer, treeNode)){
            treeRepository.merge(treeNode);
        }
        return treeNode;
    }

    @Override
    public Tree updateTree(TreeUpdateContainer updateTree, SecurityContext securityContext) {

        Tree tree = updateTree.getTree();
        if(updateTreeNoMerge(updateTree, tree)){
            treeRepository.merge(tree);
        }
        return tree;
    }
}

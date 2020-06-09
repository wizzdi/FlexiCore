package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.Baselink_;
import com.flexicore.model.FilteringInformationHolder;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.tree.*;
import com.flexicore.request.TreeFilter;
import com.flexicore.request.TreeNodeFilter;
import com.flexicore.security.SecurityContext;

import javax.ejb.Stateless;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@InheritedComponent
public class TreeRepository extends BaseclassRepository {


    public List<Tree> getAllTrees(TreeFilter treeFilter, SecurityContext securityContext) {
        CriteriaBuilder cb=em.getCriteriaBuilder();
        CriteriaQuery<Tree> q=cb.createQuery(Tree.class);
        Root<Tree> r=q.from(Tree.class);
        List<Predicate> preds=new ArrayList<>();
        QueryInformationHolder<Tree> queryInformationHolder=new QueryInformationHolder<>(treeFilter,Tree.class,securityContext);
        return getAllFiltered(queryInformationHolder,preds,cb,q,r);

    }

    public long countAllTrees(TreeFilter treeFilter, SecurityContext securityContext) {
        CriteriaBuilder cb=em.getCriteriaBuilder();
        CriteriaQuery<Long> q=cb.createQuery(Long.class);
        Root<Tree> r=q.from(Tree.class);
        List<Predicate> preds=new ArrayList<>();
        QueryInformationHolder<Tree> queryInformationHolder=new QueryInformationHolder<>(treeFilter,Tree.class,securityContext);
        return countAllFiltered(queryInformationHolder,preds,cb,q,r);

    }


    public List<TreeNode> getAllTreeNodes(TreeNodeFilter treeFilter, SecurityContext securityContext) {
        CriteriaBuilder cb=em.getCriteriaBuilder();
        CriteriaQuery<TreeNode> q=cb.createQuery(TreeNode.class);
        Root<TreeNode> r=q.from(TreeNode.class);
        List<Predicate> preds=new ArrayList<>();
        addTreeNodeFiltering(r,cb,preds,treeFilter);
        QueryInformationHolder<TreeNode> queryInformationHolder=new QueryInformationHolder<>(treeFilter,TreeNode.class,securityContext);
        return getAllFiltered(queryInformationHolder,preds,cb,q,r);

    }

    public long countAllTreeNodes(TreeNodeFilter treeFilter, SecurityContext securityContext) {
        CriteriaBuilder cb=em.getCriteriaBuilder();
        CriteriaQuery<Long> q=cb.createQuery(Long.class);
        Root<TreeNode> r=q.from(TreeNode.class);
        List<Predicate> preds=new ArrayList<>();
        addTreeNodeFiltering(r,cb,preds,treeFilter);
        QueryInformationHolder<TreeNode> queryInformationHolder=new QueryInformationHolder<>(treeFilter,TreeNode.class,securityContext);
        return countAllFiltered(queryInformationHolder,preds,cb,q,r);

    }

    private void addTreeNodeFiltering(Root<TreeNode> r, CriteriaBuilder cb, List<Predicate> preds,TreeNodeFilter treeNodeFilter) {
        if(treeNodeFilter.getParent()!=null){
            preds.add(cb.equal(r.get(TreeNode_.parent),treeNodeFilter.getParent()));
        }
    }

    public List<TreeNodeToUser> getAllTreeNodeToUserLinks(Set<String> ids, SecurityContext securityContext) {
        CriteriaBuilder cb=em.getCriteriaBuilder();
        CriteriaQuery<TreeNodeToUser> q=cb.createQuery(TreeNodeToUser.class);
        Root<TreeNodeToUser> r=q.from(TreeNodeToUser.class);
        Join<TreeNodeToUser,TreeNode> join=r.join(TreeNodeToUser_.treeNode);
        List<Predicate> preds=new ArrayList<>();
        preds.add(cb.and(
                cb.not(cb.isTrue(r.get(Baselink_.softDelete))),
                join.get(TreeNode_.id).in(ids),
                cb.equal(r.get(TreeNodeToUser_.user),securityContext.getUser())
        ));
        QueryInformationHolder<TreeNodeToUser> queryInformationHolder=new QueryInformationHolder<>(new FilteringInformationHolder(),TreeNodeToUser.class,securityContext);
        return getAllFiltered(queryInformationHolder,preds,cb,q,r);
    }
}

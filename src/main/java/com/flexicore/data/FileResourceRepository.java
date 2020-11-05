/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.data;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;



import com.flexicore.annotations.InheritedComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;

import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.*;
import com.flexicore.request.FileResourceFilter;
import com.flexicore.request.ZipFileFilter;
import com.flexicore.request.ZipFileToFileResourceFilter;
import com.flexicore.security.SecurityContext;
import org.apache.commons.io.FilenameUtils;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Primary
@InheritedComponent

@Transactional
public class FileResourceRepository extends BaseclassRepository {


   private static final Logger logger = LoggerFactory.getLogger(FileResourceRepository.class);

    public void persist(Object o) {
        em.persist(o);
    }

    public List<FileResource> getFileResourceScheduledForDelete(OffsetDateTime date) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<FileResource> q = cb.createQuery(FileResource.class);
        Root<FileResource> r = q.from(FileResource.class);
        Predicate predicate = cb.and(
                cb.lessThanOrEqualTo(r.get(FileResource_.keepUntil), date)
                , cb.or(
                        cb.isFalse(r.get(FileResource_.softDelete))
                        , cb.isNull(r.get(FileResource_.softDelete))));
        q.select(r).where(predicate).distinct(true);
        TypedQuery<FileResource> query = em.createQuery(q);
        return query.getResultList();
    }

    public List<FileResource> listAllFileResources(FileResourceFilter fileResourceFilter, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<FileResource> q = cb.createQuery(FileResource.class);
        Root<FileResource> r = q.from(FileResource.class);
        List<Predicate> preds = new ArrayList<>();
        addFileResourcePredicates(fileResourceFilter, r, q, cb, preds);
        QueryInformationHolder<FileResource> queryInformationHolder = new QueryInformationHolder<>(fileResourceFilter, FileResource.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    private void addFileResourcePredicates(FileResourceFilter fileResourceFilter, Root<FileResource> r, CriteriaQuery<?> q, CriteriaBuilder cb, List<Predicate> preds) {
        if (fileResourceFilter.getMd5s() != null && !fileResourceFilter.getMd5s().isEmpty()) {
            preds.add(r.get(FileResource_.md5).in(fileResourceFilter.getMd5s()));
        }
    }

    public List<ZipFileToFileResource> listZipFileToFileResource(ZipFileToFileResourceFilter zipFileToFileResourceFilter, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ZipFileToFileResource> q = cb.createQuery(ZipFileToFileResource.class);
        Root<ZipFileToFileResource> r = q.from(ZipFileToFileResource.class);
        List<Predicate> preds = new ArrayList<>();
        addZipFileToFileResourcePredicates(zipFileToFileResourceFilter, r, q, cb, preds);
        QueryInformationHolder<ZipFileToFileResource> queryInformationHolder = new QueryInformationHolder<>(zipFileToFileResourceFilter, ZipFileToFileResource.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    private void addZipFileToFileResourcePredicates(ZipFileToFileResourceFilter zipFileToFileResourceFilter, Root<ZipFileToFileResource> r, CriteriaQuery<ZipFileToFileResource> q, CriteriaBuilder cb, List<Predicate> preds) {

        Join<ZipFileToFileResource, ZipFile> zipFileJoin = r.join(ZipFileToFileResource_.zipFile);
        if (zipFileToFileResourceFilter.getZipFiles() != null && !zipFileToFileResourceFilter.getZipFiles().isEmpty()) {
            Set<String> ids = zipFileToFileResourceFilter.getZipFiles().parallelStream().map(f -> f.getId()).collect(Collectors.toSet());

            preds.add(zipFileJoin.get(ZipFile_.id).in(ids));
        }

        if (zipFileToFileResourceFilter.getFileResources() != null && !zipFileToFileResourceFilter.getFileResources().isEmpty()) {
            Set<String> ids = zipFileToFileResourceFilter.getFileResources().parallelStream().map(f -> f.getId()).collect(Collectors.toSet());
            Join<ZipFileToFileResource, FileResource> join = r.join(ZipFileToFileResource_.zippedFile);
            preds.add(join.get(FileResource_.id).in(ids));
        }
        preds.add(cb.isFalse(zipFileJoin.get(ZipFile_.softDelete)));
    }

    public List<ZipFile> listAllZipFiles(ZipFileFilter zipFileFilter, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ZipFile> q = cb.createQuery(ZipFile.class);
        Root<ZipFile> r = q.from(ZipFile.class);
        List<Predicate> preds = new ArrayList<>();
        addZipFilePredicates(zipFileFilter, r, q, cb, preds);
        QueryInformationHolder<ZipFile> queryInformationHolder = new QueryInformationHolder<>(zipFileFilter, ZipFile.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    private void addZipFilePredicates(ZipFileFilter zipFileFilter, Root<ZipFile> r, CriteriaQuery<ZipFile> q, CriteriaBuilder cb, List<Predicate> preds) {

        if (zipFileFilter.getFileResources() != null && !zipFileFilter.getFileResources().isEmpty()) {
            Set<String> ids = zipFileFilter.getFileResources().parallelStream().map(f -> f.getId()).collect(Collectors.toSet());
            Join<ZipFile, ZipFileToFileResource> join = r.join(ZipFile_.zippedFilesToFileResourceList);
            Join<ZipFileToFileResource, FileResource> join2 = join.join(ZipFileToFileResource_.zippedFile);
            preds.add(join2.get(FileResource_.id).in(ids));
        }
    }


    public long countAllFileResources(FileResourceFilter fileResourceFilter, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<FileResource> r = q.from(FileResource.class);
        List<Predicate> preds = new ArrayList<>();
        addFileResourcePredicates(fileResourceFilter, r, q, cb, preds);
        QueryInformationHolder<FileResource> queryInformationHolder = new QueryInformationHolder<>(fileResourceFilter, FileResource.class, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }
}

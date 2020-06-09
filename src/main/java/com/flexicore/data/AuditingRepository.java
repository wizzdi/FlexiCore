package com.flexicore.data;

import com.flexicore.model.auditing.AuditingEvent;
import com.flexicore.model.auditing.OperationHolder;
import com.flexicore.model.auditing.UserHolder;
import com.flexicore.request.AuditingFilter;
import com.flexicore.request.GetClassInfo;
import com.flexicore.utils.InheritanceUtils;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.inject.Named;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.flexicore.service.MongoConnectionService.MONGO_DB;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Sorts.orderBy;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Created by Asaf on 01/12/2016.
 */
@Component
@Primary
public class AuditingRepository extends NoSqlRepository{
    public static final String AUDITING_COLLECION_NAME = "Auditing";

    private static final String DATE_OCCURRED = "dateOccurred";
    private static final String AUDITING_TYPE = "auditingType";
    private static final String OPERATION_ID = "operationHolder.operationId";
    private static final String USER_ID = "userHolder.userId";


   private Logger logger = Logger.getLogger(getClass().getCanonicalName());
    private static CodecRegistry pojoCodecRegistry;
    private static final AtomicBoolean init=new AtomicBoolean(false);

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    @Qualifier(MONGO_DB)
    private String mongoDBName;

    @PostConstruct
    private void postConstruct() {
        if (init.compareAndSet(false, true)) {
            pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().register(
                            AuditingEvent.class,OperationHolder.class,UserHolder.class
                    ).build()));
        }
    }

    @Override
    public void merge(Object o) {
        if (o instanceof AuditingEvent) {
            MongoDatabase db =mongoClient.getDatabase(mongoDBName);
            MongoCollection<AuditingEvent> collection = db.getCollection(AUDITING_COLLECION_NAME, AuditingEvent.class).withCodecRegistry(pojoCodecRegistry);
            collection.insertOne((AuditingEvent) o);
        }

    }



    static Bson getAuditingPredicates(AuditingFilter eventFiltering) {
        Bson pred = null;
        if (eventFiltering.getFromDate() != null) {

            Date start = Date.from(eventFiltering.getFromDate().toInstant());
            Bson gte = gte(DATE_OCCURRED, start);
            pred = pred == null ? gte : and(pred, gte);
        }

        if (eventFiltering.getToDate() != null) {
            Date end = Date.from(eventFiltering.getToDate().toInstant());
            Bson lte = lte(DATE_OCCURRED, end);
            pred = pred == null ? lte : and(pred, lte);
        }


        String eventType = eventFiltering.getTypeLike();
        if (eventType != null) {
            Set<String> names= InheritanceUtils.listInheritingClassesWithFilter(new GetClassInfo().setClassName(eventType)).getList().parallelStream().map(f->f.getClazz().getCanonicalName()).collect(Collectors.toSet());
            names.add(eventType);
            Bson eq = in(AUDITING_TYPE, names);
            pred = pred == null ? eq : and(pred, eq);
        }
        if (eventFiltering.getOperations()!=null && !eventFiltering.getOperations().isEmpty()) {
            Set<String> operationsIds=eventFiltering.getOperations().stream().map(f->f.getId()).collect(Collectors.toSet());
            Bson eq = in(OPERATION_ID, operationsIds);
            pred = pred == null ? eq : and(pred, eq);
        }
        if (eventFiltering.getUsers()!=null && !eventFiltering.getUsers().isEmpty()) {
            Set<String> userIds=eventFiltering.getUsers().stream().map(f->f.getId()).collect(Collectors.toSet());
            Bson eq = in(USER_ID, userIds);
            pred = pred == null ? eq : and(pred, eq);
        }


        return pred;
    }

    public  List<AuditingEvent> listAllAuditingEvents(AuditingFilter auditingFilter) {
        MongoDatabase db = mongoClient.getDatabase(mongoDBName).withCodecRegistry(pojoCodecRegistry);
        MongoCollection<AuditingEvent> collection = db.getCollection(AUDITING_COLLECION_NAME, AuditingEvent.class).withCodecRegistry(pojoCodecRegistry);

        Bson pred = getAuditingPredicates(auditingFilter);

        FindIterable<AuditingEvent> base = pred == null ? collection.find(AuditingEvent.class) : collection.find(pred, AuditingEvent.class);
        FindIterable<AuditingEvent> iter = base.sort(orderBy(descending(DATE_OCCURRED)));
        if (auditingFilter.getCurrentPage() != null && auditingFilter.getPageSize() != null && auditingFilter.getCurrentPage() > -1 && auditingFilter.getPageSize() > 0) {
            iter.limit(auditingFilter.getPageSize()).skip(auditingFilter.getPageSize() * auditingFilter.getCurrentPage());
        }
        List<AuditingEvent> alerts = new ArrayList<>();
        for (AuditingEvent alert : iter) {
            alerts.add(alert);
        }
        return alerts;
    }

    public long countAllAuditingEvents(AuditingFilter auditingFilter) {
        MongoDatabase db =mongoClient.getDatabase(mongoDBName).withCodecRegistry(pojoCodecRegistry);
        MongoCollection<AuditingEvent> collection = db.getCollection(AUDITING_COLLECION_NAME, AuditingEvent.class).withCodecRegistry(pojoCodecRegistry);

        Bson pred = getAuditingPredicates(auditingFilter);

        return pred == null ? collection.countDocuments() : collection.countDocuments(pred);

    }
}

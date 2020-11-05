package com.flexicore.data.impl;

import com.flexicore.data.NoSqlRepository;
import com.flexicore.model.nosql.BaseclassNoSQL;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.flexicore.service.MongoConnectionService.MONGO_DB;


@Component
@Primary
public class BaseclassNoSQLRepository extends NoSqlRepository implements com.flexicore.data.BaseclassNoSQLRepository {

   private static final Logger logger = LoggerFactory.getLogger(BaseclassNoSQLRepository.class);
    private static AtomicBoolean init = new AtomicBoolean(false);


    @Autowired
    private MongoClient mongoClient;

    @Autowired
    @Qualifier(MONGO_DB)
    private String mongoDBName;


    static {
        clazzToRegister.add(BaseclassNoSQL.class);
    }

    @Override
    public <T extends BaseclassNoSQL> T getByIdOrNull(Class<T> c, String id) {
        return getByIdOrNull(c,BASECLASS_NO_SQL,id);
    }

    @Override
    public <T extends BaseclassNoSQL> T getByIdOrNull(Class<T> c,String collectionName, String id) {
        MongoDatabase db = mongoClient.getDatabase(mongoDBName).withCodecRegistry(pojoCodecRegistry.get());
        MongoCollection<T> collection = db.getCollection(collectionName, c).withCodecRegistry(pojoCodecRegistry.get());

        Bson pred = Filters.eq(ID, id);
        FindIterable<T> iter = collection.find(pred, c);

        MongoCursor<T> iterator = iter.iterator();
        try {
            return iterator.hasNext() ? iterator.next() : null;
        } finally {
            iterator.close();
        }
    }

    @Override
    public <T extends BaseclassNoSQL> List<T> listByIds(Class<T> c, Set<String> ids) {
        return listByIds(c,BASECLASS_NO_SQL,ids);
    }

    @Override
    public <T extends BaseclassNoSQL> List<T> listByIds(Class<T> c,String collectionName, Set<String> ids) {
        MongoDatabase db = mongoClient.getDatabase(mongoDBName).withCodecRegistry(pojoCodecRegistry.get());
        MongoCollection<T> collection = db.getCollection(collectionName, c).withCodecRegistry(pojoCodecRegistry.get());

        Bson pred = Filters.in(ID, ids);
        FindIterable<T> iter = collection.find(pred, c);

        List<T> alerts = new ArrayList<>();
        for (T alert : iter) {
            alerts.add(alert);
        }
        return alerts;
    }

    @Override
    public void mergeBaseclassNoSQL(BaseclassNoSQL o) {
        mergeBaseclassNoSQL(o,BASECLASS_NO_SQL);
    }

    @Override
    public void mergeBaseclassNoSQL(BaseclassNoSQL o,String collectionName) {
        MongoDatabase db = mongoClient.getDatabase(mongoDBName).withCodecRegistry(pojoCodecRegistry.get());
        MongoCollection<BaseclassNoSQL> collection = db.getCollection(collectionName, BaseclassNoSQL.class).withCodecRegistry(pojoCodecRegistry.get());
        collection.insertOne(o);

    }

    @Override
    public void massMergeBaseclassNoSQL(List<? extends BaseclassNoSQL> o) {
        massMergeBaseclassNoSQL(o,BASECLASS_NO_SQL);
    }

    @Override
    public void massMergeBaseclassNoSQL(List<? extends BaseclassNoSQL> o,String collectionName) {
        MongoDatabase db = mongoClient.getDatabase(mongoDBName).withCodecRegistry(pojoCodecRegistry.get());
        MongoCollection<BaseclassNoSQL> collection = db.getCollection(collectionName, BaseclassNoSQL.class).withCodecRegistry(pojoCodecRegistry.get());
        collection.insertMany(o);

    }



}

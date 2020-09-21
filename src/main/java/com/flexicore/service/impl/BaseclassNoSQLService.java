package com.flexicore.service.impl;

import com.flexicore.data.impl.BaseclassNoSQLRepository;
import com.flexicore.model.nosql.BaseclassNoSQL;
import com.flexicore.request.BaseclassNoSQLCreate;
import com.flexicore.request.BaseclassNoSQLUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Primary
@Component
public class BaseclassNoSQLService implements com.flexicore.service.BaseclassNoSQLService {


    @Autowired
    private BaseclassNoSQLRepository baseclassNoSQLRepository;


    @Override
    public <T extends BaseclassNoSQL> T getByIdOrNull(Class<T> c, String id) {
        return baseclassNoSQLRepository.getByIdOrNull(c, id);
    }


    @Override
    public <T extends BaseclassNoSQL> List<T> listByIds(Class<T> c, Set<String> ids) {
        if(ids.isEmpty()){
            return new ArrayList<>();
        }
        return baseclassNoSQLRepository.listByIds(c, ids);
    }

    @Override
    public <T extends BaseclassNoSQL> List<T> listByIds(Class<T> c, String collection, Set<String> ids) {
        if(ids.isEmpty()){
            return new ArrayList<>();
        }
        return baseclassNoSQLRepository.listByIds(c,collection, ids);
    }

    @Override
    public <T extends BaseclassNoSQL> T getByIdOrNull(Class<T> c, String collectionName, String id) {
        return baseclassNoSQLRepository.getByIdOrNull(c, collectionName, id);
    }

    @Override
    public void massMergeBaseclassNoSQL(List<? extends BaseclassNoSQL> o, String collectionName) {
        if(o.isEmpty()){
            return;
        }
        baseclassNoSQLRepository.massMergeBaseclassNoSQL(o, collectionName);
    }

    @Override
    public void mergeBaseclassNoSQLByCollection(Map<String, String> noSQLNodesCollections, List<? extends BaseclassNoSQL> o) {
        if(noSQLNodesCollections==null){
            massMergeBaseclassNoSQL(o);
            return;
        }
        List<? extends BaseclassNoSQL> defaultCollection = o.stream().filter(f -> noSQLNodesCollections.get(f.getId()) == null).collect(Collectors.toList());
        if(!defaultCollection.isEmpty()){
            baseclassNoSQLRepository.massMergeBaseclassNoSQL(defaultCollection);
        }

        Map<String,List<BaseclassNoSQL>> idsPerCollection= o.stream().filter(f -> noSQLNodesCollections.get(f.getId()) != null).collect(Collectors.groupingBy(f-> noSQLNodesCollections.get(f.getId())));
        for (Map.Entry<String, List<BaseclassNoSQL>> idsPerCollectionEntry : idsPerCollection.entrySet()) {
            List<BaseclassNoSQL> ids=idsPerCollectionEntry.getValue();
            if(!ids.isEmpty()){
                String collectionName=idsPerCollectionEntry.getKey();
                massMergeBaseclassNoSQL(ids,collectionName);

            }
        }

    }

    @Override
    public <T extends BaseclassNoSQL> List<T> getBaseclassNoSQLByCollection(Map<String, String> noSQLNodesCollections,Class<T> c,Set<String> noSQLIds) {
        if(noSQLNodesCollections==null){
            return listByIds(c,noSQLIds);
        }
        List<T> baseclassNosqls =new ArrayList<>();
        Set<String> defaultCollection = noSQLIds.stream().filter(f -> noSQLNodesCollections.get(f) == null).collect(Collectors.toSet());
        if(!defaultCollection.isEmpty()){
            baseclassNosqls.addAll(listByIds(c, defaultCollection));
        }
        Map<String,Set<String>> idsPerCollection= noSQLIds.stream().filter(f -> noSQLNodesCollections.get(f) != null).collect(Collectors.groupingBy(f-> noSQLNodesCollections.get(f),Collectors.toSet()));
        for (Map.Entry<String, Set<String>> idsPerCollectionEntry : idsPerCollection.entrySet()) {
            Set<String> ids=idsPerCollectionEntry.getValue();
            if(!ids.isEmpty()){
                String collectionName=idsPerCollectionEntry.getKey();
                baseclassNosqls.addAll(listByIds(c,collectionName, ids));

            }
        }
        return baseclassNosqls;
    }

    @Override
    public BaseclassNoSQL createBaseclassNoSQL(BaseclassNoSQLCreate baseclassNoSQLCreate) {
        BaseclassNoSQL baseclassNoSQL = createBaseclassNoSQLNoMerge(baseclassNoSQLCreate);
        baseclassNoSQLRepository.mergeBaseclassNoSQL(baseclassNoSQL);
        return baseclassNoSQL;
    }

    @Override
    public BaseclassNoSQL createBaseclassNoSQLNoMerge(BaseclassNoSQLCreate baseclassNoSQLCreate) {
        BaseclassNoSQL baseclassNoSQL = new BaseclassNoSQL();
        updateBaseclassNoSQLNoMerge(baseclassNoSQL, baseclassNoSQLCreate);
        return baseclassNoSQL;
    }

    @Override
    public BaseclassNoSQL updateBaseclassNoSQL(BaseclassNoSQLUpdate baseclassNoSQLCreate) {
        BaseclassNoSQL baseclassNoSQL = baseclassNoSQLCreate.getBaseclassNoSQL();
        if (updateBaseclassNoSQLNoMerge(baseclassNoSQL, baseclassNoSQLCreate)) {
            baseclassNoSQLRepository.mergeBaseclassNoSQL(baseclassNoSQL);
        }
        return baseclassNoSQL;
    }

    @Override
    public boolean updateBaseclassNoSQLNoMerge(BaseclassNoSQL baseclassNoSQL, BaseclassNoSQLCreate create) {
        boolean update = false;
        if (create.getName() != null && create.getName().equals(baseclassNoSQL.getName())) {
            baseclassNoSQL.setName(create.getName());
            update = true;
        }
        return update;
    }

    @Override
    public void mergeBaseclassNoSQL(BaseclassNoSQL o) {
        baseclassNoSQLRepository.mergeBaseclassNoSQL(o);
    }

    @Override
    public void massMergeBaseclassNoSQL(List<? extends BaseclassNoSQL> o) {
        if (o.isEmpty()) {
            return;
        }
        baseclassNoSQLRepository.massMergeBaseclassNoSQL(o);
    }

    @Override
    public void mergeBaseclassNoSQL(BaseclassNoSQL o, String collectionName) {
        baseclassNoSQLRepository.mergeBaseclassNoSQL(o, collectionName);
    }
}

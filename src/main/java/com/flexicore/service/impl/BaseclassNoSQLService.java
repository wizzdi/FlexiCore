package com.flexicore.service.impl;

import com.flexicore.data.impl.BaseclassNoSQLRepository;
import com.flexicore.model.nosql.BaseclassNoSQL;
import com.flexicore.request.BaseclassNoSQLCreate;
import com.flexicore.request.BaseclassNoSQLUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

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
        return baseclassNoSQLRepository.listByIds(c, ids);
    }

    @Override
    public BaseclassNoSQL createBaseclassNoSQL(BaseclassNoSQLCreate baseclassNoSQLCreate) {
        BaseclassNoSQL baseclassNoSQL= createBaseclassNoSQLNoMerge(baseclassNoSQLCreate);
        baseclassNoSQLRepository.mergeBaseclassNoSQL(baseclassNoSQL);
        return baseclassNoSQL;
    }

    @Override
    public BaseclassNoSQL createBaseclassNoSQLNoMerge(BaseclassNoSQLCreate baseclassNoSQLCreate) {
        BaseclassNoSQL baseclassNoSQL= new BaseclassNoSQL();
        updateBaseclassNoSQLNoMerge(baseclassNoSQL,baseclassNoSQLCreate);
        return baseclassNoSQL;
    }

    @Override
    public BaseclassNoSQL updateBaseclassNoSQL(BaseclassNoSQLUpdate baseclassNoSQLCreate) {
        BaseclassNoSQL baseclassNoSQL=baseclassNoSQLCreate.getBaseclassNoSQL();
       if(updateBaseclassNoSQLNoMerge(baseclassNoSQL, baseclassNoSQLCreate)){
           baseclassNoSQLRepository.mergeBaseclassNoSQL(baseclassNoSQL);
       }
       return baseclassNoSQL;
    }

    private boolean updateBaseclassNoSQLNoMerge(BaseclassNoSQL baseclassNoSQL, BaseclassNoSQLCreate create) {
        boolean update=false;
        if(create.getName()!=null && create.getName().equals(baseclassNoSQL.getName())){
            baseclassNoSQL.setName(create.getName());
            update=true;
        }
        return update;
    }

    @Override
    public void mergeBaseclassNoSQL(BaseclassNoSQL o) {
        baseclassNoSQLRepository.mergeBaseclassNoSQL(o);
    }

    @Override
    public void massMergeBaseclassNoSQL(List<? extends BaseclassNoSQL> o) {
        if(o.isEmpty()){
            return;
        }
        baseclassNoSQLRepository.massMergeBaseclassNoSQL(o);
    }
}

package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Asaf on 01/12/2016.
 */
@InheritedComponent
public class NoSqlRepository{



    public void refreshEntityManager(){

    }



    public void persist(Object o){


    }

    public void merge(Object o){


    }


    public void batchMerge(List<Object> o){

    }

    public void batchPersist(List<?> o){

    }



}

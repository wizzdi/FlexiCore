/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flexicore.runningentities;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author Asaf
 */
public class GenericFilter implements FilenameFilter{
    private String[] ext;
    
   public GenericFilter(String[] ext){
        this.ext=ext;
    }

    @Override
    public boolean accept(File dir, String name) {
        for (String s : ext) {
            if (name.endsWith(s)) {
                return true;
            }
        }
        return false;
   }
    
   
    
}

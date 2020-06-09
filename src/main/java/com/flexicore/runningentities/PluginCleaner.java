/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.runningentities;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import com.flexicore.service.impl.PluginService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class PluginCleaner implements Runnable {
	@Autowired
	private PluginService pluginService;
	private Logger logger = Logger.getLogger(getClass().getCanonicalName());
	private boolean stop=false;
	private static final long CLEAN_GAP=10000;

	public PluginCleaner() {
		
	}

	@Override
	public void run() {
		while(!stop){
			//pluginService.cleanPlugins();
			try {
				Thread.sleep(CLEAN_GAP);
			} catch (InterruptedException e) {
				logger.log(Level.SEVERE, "unable to sleep", e);
				return;
			}
		}

	}

	public boolean isRunning() {
		return stop;
	}

	public void stop() {
		this.stop =true;
	}

}

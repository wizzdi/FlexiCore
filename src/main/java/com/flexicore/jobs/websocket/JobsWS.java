package com.flexicore.jobs.websocket;

import com.flexicore.annotations.plugins.PluginInfo;
import com.flexicore.annotations.rest.Update;
import com.flexicore.annotations.rest.Write;
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.interfaces.WebSocketPlugin;
import com.flexicore.jobs.encoders.JobMessageEncoder;
import com.flexicore.jobs.messages.JobWSHelloMessage;
import com.flexicore.model.Baseclass;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.JobService;

import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Asaf on 31/08/2016.
 */
@ServerEndpoint(value = "/jobsWS/{authenticationKey}",
        encoders = {JobMessageEncoder.class})
@Protected
@PluginInfo(version = 1)
public class JobsWS implements WebSocketPlugin {

   private static final Logger logger = LoggerFactory.getLogger(JobsWS.class);



    @OnOpen
    @Write

    public void open(@PathParam("authenticationKey") String authenticationKey, Session session) {
        SecurityContext securityContext = (SecurityContext) session.getUserProperties().get("securityContext");

        logger.info("Opening:" + session.getId());

        com.flexicore.service.JobService.registerSession(session);
        try {
            session.getBasicRemote().sendObject(new JobWSHelloMessage().setId(Baseclass.getBase64ID()).setSessionId(session.getId()));
        } catch (IOException |EncodeException e) {
            logger.error("failed sending hello message",e);
        }
    }

    @OnClose
    @Update
    public void close(@PathParam("authenticationKey") String authenticationKey, Session session, CloseReason c) {
        SecurityContext securityContext = (SecurityContext) session.getUserProperties().get("securityContext");

        logger.info("Closing:" + session.getId());
        JobService.unregisterSession(session);


    }


}

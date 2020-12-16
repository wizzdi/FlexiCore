package com.flexicore.data.jsoncontainers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.flexicore.exceptions.ExceptionHolder;
import com.flexicore.interfaces.ErrorCodeException;
import org.jboss.resteasy.spi.DefaultOptionsMethodException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Provider
@Service
public class BadRequestExceptionMapper implements ExceptionMapper<Exception>  {

    private static final Logger logger= LoggerFactory.getLogger(BadRequestExceptionMapper.class);

    private Response.ResponseBuilder badBuilder =Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON);
    private Response.ResponseBuilder internalBuilder =Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON);

    @Context
    private HttpServletRequest request;
    @Value("${flexicore.http.showExceptions:false}")
    private boolean showExceptionsInHttpResponse;
    @Value("${flexicore.http.exceptionPlaceHolder:Contact Your System Administrator}")
    private String exceptionPlaceHolder;

    @Override
    public Response toResponse(Exception exception) {
        if(exception instanceof DefaultOptionsMethodException){
            return ((DefaultOptionsMethodException) exception).getResponse();
        }

        logger.error("error",exception);
        Response response = exception instanceof WebApplicationException? ((WebApplicationException) exception).getResponse():null;

        int errorCode=exception instanceof ErrorCodeException ?((ErrorCodeException) exception).getErrorCode():-1;
        boolean bad = exception instanceof ClientErrorException || exception instanceof JsonProcessingException;
        int statusCode = getStatusCode(response, bad);

        return getResponse(exception, response, errorCode, bad, statusCode);
    }

    private Response getResponse(Exception exception, Response response, int errorCode, boolean bad, int statusCode) {
        if(response!=null&&response.hasEntity()){
            return response;
        }
        if(!isJsonMediaType()){
            return response;
        }


        Response.ResponseBuilder builder=response!=null?Response.fromResponse(response):(bad?badBuilder:internalBuilder);
        String message = showExceptionsInHttpResponse || bad ? exception.getMessage() : exceptionPlaceHolder;
        return  builder.entity(new ExceptionHolder(statusCode,errorCode, message)).build();
    }

    private boolean isJsonMediaType(){
        String header=enumerationAsStream(request.getHeaderNames()).parallel().filter(f->f.toLowerCase().equals("accept")).findAny().orElse(null);
        if(header!=null){
           return enumerationAsStream(request.getHeaders(header)).parallel().anyMatch(f->f.toLowerCase().contains(MediaType.APPLICATION_JSON));
        }
        return false;
    }

    public static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<T>() {
                            public T next() {
                                return e.nextElement();
                            }
                            public boolean hasNext() {
                                return e.hasMoreElements();
                            }
                        },
                        Spliterator.ORDERED), false);
    }

    private int getStatusCode(Response response, boolean bad) {
        if(response!=null){
            return response.getStatus();
        }

        return bad ?Response.Status.BAD_REQUEST.getStatusCode():Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

}

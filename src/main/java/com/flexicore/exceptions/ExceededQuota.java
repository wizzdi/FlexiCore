package com.flexicore.exceptions;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;

public class ExceededQuota extends ForbiddenException {

    public ExceededQuota() {
    }

    public ExceededQuota(String message) {
        super(message);
    }

    public ExceededQuota(Response response) {
        super(response);
    }

    public ExceededQuota(String message, Response response) {
        super(message, response);
    }

    public ExceededQuota(Throwable cause) {
        super(cause);
    }

    public ExceededQuota(String message, Throwable cause) {
        super(message, cause);
    }

    public ExceededQuota(Response response, Throwable cause) {
        super(response, cause);
    }

    public ExceededQuota(String message, Response response, Throwable cause) {
        super(message, response, cause);
    }
}

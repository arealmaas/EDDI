package ai.labs.rest.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * @author ginccc
 */
@Api(value = "Infrastructure Endpoints")
@Path("health")
public interface IRestHealthCheck {
    @GET
    @ApiOperation(value = "Server health check.")
    Response checkHealth();
}
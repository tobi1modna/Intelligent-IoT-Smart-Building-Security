package BuildingSecurityController.api.resources;


import BuildingSecurityController.api.auth.User;
import BuildingSecurityController.api.data_transfer_object.PolicyCreationRequest;
import BuildingSecurityController.api.data_transfer_object.PolicyUpdateRequest;
import BuildingSecurityController.api.data_transfer_object.UserCreationRequest;
import BuildingSecurityController.api.data_transfer_object.UserUpdateRequest;
import BuildingSecurityController.api.exception.IInventoryDataManagerConflict;
import BuildingSecurityController.api.model.PolicyDescriptor;
import BuildingSecurityController.api.model.UserDescriptor;
import BuildingSecurityController.api.services.OperatorAppConfig;
import com.codahale.metrics.annotation.Timed;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Path("/building/user")
@Api("IoT User Inventory Endpoint")
public class UserResource {

    final protected Logger logger = LoggerFactory.getLogger(UserResource.class);


    @SuppressWarnings("serial")
    public static class MissingKeyException extends Exception{}
    final OperatorAppConfig conf;

    public UserResource(OperatorAppConfig conf){
        this.conf = conf;
    }

    @RolesAllowed("ADMIN")
    @GET
    @Path("/")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all registered Users' usernames")
    public Response GetUserList(@Context ContainerRequestContext requestContext){

        try{
            logger.info("Loading all stored IoT Inventory Policies.");

            List<String> serviceList = null;

            serviceList = this.conf.getInventoryDataManager().getUsernameList();

            if(serviceList == null)
                return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.NOT_FOUND.getStatusCode(), "Users not found")).build();

            return Response.ok(serviceList).build();

        } catch(Exception e){
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Internal Server Error!")).build();
        }
    }

    @RolesAllowed("ADMIN")
    @GET
    @Path("/{username}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value="Get a Single User")
    public Response getUser(@Context ContainerRequestContext requestContext,
                                @PathParam("username") String username) {

        try {

            logger.info("Loading User Info for id: {}", username);

            //Check the request
            if(username == null)
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),"Invalid Username Provided !")).build();

            Optional<UserDescriptor> userDescriptor = this.conf.getInventoryDataManager().getUser(username);

            if(!userDescriptor.isPresent())
                return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.NOT_FOUND.getStatusCode(),"User Not Found !")).build();

            return Response.ok(userDescriptor.get()).build();

        } catch (Exception e){
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),"Internal Server Error !")).build();
        }
    }

    @RolesAllowed("ADMIN")
    @POST
    @Path("/")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value="Create a new User")
    public Response createLocation(@Context ContainerRequestContext req,
                                   @Context UriInfo uriInfo,
                                   UserCreationRequest userCreationRequest) {

        try {

            logger.info("Incoming User Creation Request: {}", userCreationRequest);

            //Check the request
            if(userCreationRequest == null)
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),"Invalid request payload")).build();

            UserDescriptor newUserDescriptor = (UserDescriptor) userCreationRequest;

            newUserDescriptor = this.conf.getInventoryDataManager().createNewUser(newUserDescriptor);

            return Response.created(new URI(String.format("%s/%s",uriInfo.getAbsolutePath(),newUserDescriptor.getUsername()))).build();

        } catch (IInventoryDataManagerConflict e){
            return Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.CONFLICT.getStatusCode(),"User already exists !")).build();
        } catch (Exception e){
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),"Internal Server Error !")).build();
        }
    }

    @RolesAllowed("ADMIN")
    @PUT
    @Path("/{username}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value="Update an existing User")
    public Response updateUser(@Context ContainerRequestContext req,
                                   @Context UriInfo uriInfo,
                                   @PathParam("username") String username,
                                   UserUpdateRequest userUpdateRequest) {

        try {

            logger.info("Incoming User ({}) Update Request: {}", username, userUpdateRequest);

            //Check if the request is valid, the id must be the same in the path and in the json request payload
            if(userUpdateRequest == null || !userUpdateRequest.getUsername().equals(username))
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),"Invalid request ! Check Username")).build();

            //Check if the device is available and correctly registered otherwise a 404 response will be sent to the client
            if(!this.conf.getInventoryDataManager().getUser(username).isPresent())
                return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.NOT_FOUND.getStatusCode(),"User not found !")).build();

            UserDescriptor userDescriptor = (UserDescriptor) userUpdateRequest;
            this.conf.getInventoryDataManager().updateUser(userUpdateRequest);

            return Response.noContent().build();

        } catch (Exception e){
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),"Internal Server Error !")).build();
        }
    }

    @RolesAllowed("ADMIN")
    @DELETE
    @Path("/{username}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value="Delete a Single User")
    public Response deleteDevice(@Context ContainerRequestContext req,
                                 @PathParam("username") String username) {

        try {

            logger.info("Deleting User with id: {}", username);

            //Check the request
            if(username == null)
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),"Invalid Username Provided !")).build();

            //Check if the device is available or not
            if(!this.conf.getInventoryDataManager().getUser(username).isPresent())
                return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.NOT_FOUND.getStatusCode(),"User Not Found !")).build();

            //Delete the location
            this.conf.getInventoryDataManager().deleteUser(username);

            return Response.noContent().build();

        } catch (Exception e){
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),"Internal Server Error !")).build();
        }
    }



}

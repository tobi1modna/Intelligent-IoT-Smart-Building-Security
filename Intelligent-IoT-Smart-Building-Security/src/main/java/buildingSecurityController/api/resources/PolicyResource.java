package buildingSecurityController.api.resources;


import buildingSecurityController.api.client.CoapResourceClient;
import buildingSecurityController.api.data_transfer_object.PolicyCreationRequest;
import buildingSecurityController.api.data_transfer_object.PolicyUpdateRequest;
import buildingSecurityController.api.exception.IInventoryDataManagerConflict;
import buildingSecurityController.api.model.GenericDeviceDescriptor;
import buildingSecurityController.api.model.PolicyDescriptor;
import buildingSecurityController.api.services.OperatorAppConfig;
import com.codahale.metrics.annotation.Timed;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.eclipse.californium.core.CoapResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Path("/building/policy")
@Api("IoT Policy Inventory Endpoint")
public class PolicyResource {

    final protected Logger logger = LoggerFactory.getLogger(PolicyResource.class);

    @SuppressWarnings("serial")
    public static class MissingKeyException extends Exception{}
    final OperatorAppConfig conf;

    public PolicyResource(OperatorAppConfig conf){
        this.conf = conf;
    }

    @RolesAllowed("USER")
    @GET
    @Path("/")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all the Policies")
    public Response GetPolicies(@Context ContainerRequestContext requestContext,
                                @QueryParam("floor_id") String floorId){

        try{

            List<PolicyDescriptor> serviceList = null;

            if(floorId == null) {
                logger.info("Loading all stored IoT Inventory Policies");
                serviceList = this.conf.getInventoryDataManager().getPolicyList();
            }

            else if(floorId != null) {
                logger.info("Loading all stored IoT Inventory Policies filtered by Floor: {}", floorId);
                serviceList = this.conf.getInventoryDataManager().getPolicyListByFloor(floorId);
            }
            else
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(), "only policy_id is required for filtering!")).build();

            if(serviceList == null)
                return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.NOT_FOUND.getStatusCode(), "Policies not found")).build();

            return Response.ok(serviceList).build();

        } catch(Exception e){
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Internal Server Error!")).build();
        }
    }

    @RolesAllowed("USER")
    @GET
    @Path("/{policy_id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value="Get a Single Policy")
    public Response getLocation(@Context ContainerRequestContext requestContext,
                                @PathParam("policy_id") String policy_id) {

        try {

            logger.info("Loading Policy Info for id: {}", policy_id);

            //Check the request
            if(policy_id == null)
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),"Invalid Policy Id Provided !")).build();

            Optional<PolicyDescriptor> policyDescriptor = this.conf.getInventoryDataManager().getPolicy(policy_id);

            if(!policyDescriptor.isPresent())
                return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.NOT_FOUND.getStatusCode(),"Policy Not Found !")).build();

            return Response.ok(policyDescriptor.get()).build();

        } catch (Exception e){
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),"Internal Server Error !")).build();
        }
    }

    @RolesAllowed("USER")
    @POST
    @Path("/")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value="Create a new Policy")
    public Response createLocation(@Context ContainerRequestContext req,
                                   @Context UriInfo uriInfo,
                                   PolicyCreationRequest policyCreationRequest) {

        try {

            logger.info("Incoming Policy Creation Request: {}", policyCreationRequest);

            //Check the request
            if(policyCreationRequest == null)
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),"Invalid request payload")).build();

            PolicyDescriptor newPolicyDescriptor = (PolicyDescriptor) policyCreationRequest;

            newPolicyDescriptor.setPolicy_id(null);

            newPolicyDescriptor = this.conf.getInventoryDataManager().createNewPolicy(newPolicyDescriptor);

            return Response.created(new URI(String.format("%s/%s",uriInfo.getAbsolutePath(),newPolicyDescriptor.getPolicy_id()))).build();

        } catch (IInventoryDataManagerConflict e){
            return Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.CONFLICT.getStatusCode(),"Policy already available !")).build();
        } catch (Exception e){
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),"Internal Server Error !")).build();
        }
    }

    @RolesAllowed("USER")
    @PUT
    @Path("/{policy_id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value="Update an existing Policy")
    public Response updateLocation(@Context ContainerRequestContext req,
                                   @Context UriInfo uriInfo,
                                   @PathParam("policy_id") String policy_id,
                                   PolicyUpdateRequest policyUpdateRequest) {

        try {

            logger.info("Incoming Policy ({}) Update Request: {}", policy_id, policyUpdateRequest);

            //Check if the request is valid, the id must be the same in the path and in the json request payload
            if(policyUpdateRequest == null || !policyUpdateRequest.getPolicy_id().equals(policy_id))
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),"Invalid request ! Check Policy Id")).build();

            //Check if the device is available and correctly registered otherwise a 404 response will be sent to the client
            if(!this.conf.getInventoryDataManager().getPolicy(policy_id).isPresent())
                return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.NOT_FOUND.getStatusCode(),"Policy not found !")).build();

            PolicyDescriptor policyDescriptor = (PolicyDescriptor) policyUpdateRequest;
            this.conf.getInventoryDataManager().updatePolicy(policyDescriptor);


            if (!policyUpdateRequest.getIs_enabled()){

                List<GenericDeviceDescriptor> devList = this.conf.getInventoryDataManager().getDeviceList();
                List<GenericDeviceDescriptor> newList = new ArrayList<>();

                devList.forEach(dev ->{
                    if(dev.getAreaId().equals(policyUpdateRequest.getArea_id())){
                        newList.add(dev);
                    }
                });

                if(!newList.isEmpty()){
                    newList.forEach(dev ->{
                        if(dev.getDeviceId().contains("alarm") || dev.getDeviceId().contains("light")){

                            CoapResourceClient coapResourceClient = new CoapResourceClient();
                            CoapResponse response = coapResourceClient.putRequest(String.format("coap://192.168.1.107:5683/%s", dev.getDeviceId()), "false");
                            logger.info("Deactivated: {}", dev.getDeviceId());
                        }

                    });
                }


            }



            return Response.noContent().build();

        } catch (Exception e){
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),"Internal Server Error !")).build();
        }
    }

    @RolesAllowed("USER")
    @DELETE
    @Path("/{policy_id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value="Delete a Single Policy")
    public Response deleteDevice(@Context ContainerRequestContext req,
                                 @PathParam("policy_id") String policy_id) {

        try {

            logger.info("Deleting Policy with id: {}", policy_id);

            //Check the request
            if(policy_id == null)
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),"Invalid Policy Id Provided !")).build();

            //Check if the device is available or not
            if(!this.conf.getInventoryDataManager().getPolicy(policy_id).isPresent())
                return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.NOT_FOUND.getStatusCode(),"Policy Not Found !")).build();

            //Delete the policy
            this.conf.getInventoryDataManager().deletePolicy(policy_id);

            return Response.noContent().build();

        } catch (Exception e){
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),"Internal Server Error !")).build();
        }
    }

}

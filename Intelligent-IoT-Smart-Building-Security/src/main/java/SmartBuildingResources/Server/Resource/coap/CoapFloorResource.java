package SmartBuildingResources.Server.Resource.coap;

import SmartBuildingResources.Server.Resource.SmartBuildingCoapSmartObjectProcess;
import SmartBuildingResources.Server.Resource.raw.AreaResource;
import SmartBuildingResources.Server.Resource.raw.FloorResource;
import SmartBuildingResources.Server.Resource.raw.ResourceDataListener;
import SmartBuildingResources.Server.Resource.raw.SmartObjectResource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.observe.ObserveRelationFilter;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.EndpointContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CoreInterfaces;
import utils.SenMLPack;
import utils.SenMLRecord;

import java.util.Optional;

public class CoapFloorResource extends CoapResource {

    private final static Logger logger = LoggerFactory.getLogger(CoapFloorResource.class);

    private final static String OBJECT_TITLE = "Floor";

    private static final String TARGET_LISTENING_IP = "192.168.1.107";

    private static final int TARGET_PORT = 5683;

    private FloorResource floorResource;

    private ObjectMapper objectMapper;


    public CoapFloorResource(String name, FloorResource floorResource) throws InterruptedException {
        super(name);

        if (floorResource != null)
        {
            this.floorResource = floorResource;
            this.objectMapper=new ObjectMapper();
            this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            setObservable(true);
            getAttributes().setObservable();
            setObserveType(CoAP.Type.CON);

            getAttributes().setTitle(OBJECT_TITLE);
            getAttributes().setObservable();
            getAttributes().addAttribute("rt", floorResource.getType());
            getAttributes().addAttribute("if", CoreInterfaces.CORE_B.getValue());
            getAttributes().addAttribute("ct", Integer.toString(MediaTypeRegistry.APPLICATION_LINK_FORMAT));
            getAttributes().addAttribute("ct", Integer.toString(MediaTypeRegistry.TEXT_PLAIN));


        }
        else {
            logger.error( "ERROR -->NULL Raw References" );
        }

    }

    private CoapResource createAreaResource(String areaId) throws InterruptedException {

        AreaResource areaResource = new AreaResource();
        CoapAreaResource coapAreaResource = new CoapAreaResource(String.format("area%s", areaId), areaResource);
        return coapAreaResource;
    };

    private Optional<String> getJsonSenmlResponse() {

        try {

            SenMLPack senMLPack = new SenMLPack();

            SenMLRecord senMLRecord = new SenMLRecord();
            senMLRecord.setBn(String.format("%s", this.getName()));
            senMLPack.add(senMLRecord);

            logger.info("{}", senMLPack);

            return Optional.of(this.objectMapper.writeValueAsString(senMLPack));

        } catch (Exception e) {
            return Optional.empty();
        }
    }
    @Override
    public void handleGET(CoapExchange exchange){


        if(exchange.getRequestOptions().getAccept() == MediaTypeRegistry.APPLICATION_SENML_JSON ||
                exchange.getRequestOptions().getAccept() == MediaTypeRegistry.APPLICATION_JSON){


            Optional<String> senmlPayload = getJsonSenmlResponse();

            if(senmlPayload.isPresent()){
                exchange.respond(CoAP.ResponseCode.CONTENT, senmlPayload.get(), exchange.getRequestOptions().getAccept());
            }
            else
                exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }
    @Override
    public void handlePOST(CoapExchange exchange) {
        try {
                if (exchange.getRequestPayload() != null) {
                    String areaId = new String(exchange.getRequestPayload());

                    this.add(createAreaResource(areaId));


                    logger.info("Resource Status Updated: Area {} Created", areaId);

                    exchange.respond(CoAP.ResponseCode.CREATED);

                    SmartBuildingCoapSmartObjectProcess.registerToCoapResourceDirectory(this.getParent().getParent(), "CoapEndpointSmartObject", TARGET_LISTENING_IP, TARGET_PORT);


                } else
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST);

            } catch (Exception e) {
                logger.error("Error Handling POST -> {}", e.getLocalizedMessage());
                exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
            }
    }
    @Override
    public void handlePUT(CoapExchange exchange){
        try{

            //If the request body is available
            if(exchange.getRequestPayload() != null){

                String submittedValue = new String(exchange.getRequestPayload());

                //Update internal status
                this.setName(String.format("floor%s", submittedValue));

                logger.info("Resource Status Updated: {}", submittedValue);

                exchange.respond(CoAP.ResponseCode.CHANGED);

                SmartBuildingCoapSmartObjectProcess.registerToCoapResourceDirectory(this.getParent().getParent(), "CoapEndpointSmartObject", TARGET_LISTENING_IP, TARGET_PORT);

                //this.notify();

                ObserveRelationFilter filter = new ObserveRelationFilter() {
                    @Override
                    public boolean accept(ObserveRelation relation) {
                        return true;
                    }
                };

                this.notifyObserverRelations(filter);

            }
            else
                exchange.respond(CoAP.ResponseCode.BAD_REQUEST);

        }catch (Exception e){
            logger.error("Error Handling PUT -> {}", e.getLocalizedMessage());
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }

    }
    @Override
    public void handleDELETE(CoapExchange exchange){
        try{

            //If the request body is available
            if(exchange.getRequestPayload() == null){

                //Update internal status
                this.delete();

                logger.info("Resource Status Updated: Floor {} deleted", this.getName());

                exchange.respond(CoAP.ResponseCode.CHANGED);
            }
            else
                exchange.respond(CoAP.ResponseCode.BAD_REQUEST);

        }catch (Exception e){
            logger.error("Error Handling DELETE -> {}", e.getLocalizedMessage());
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }

    }
}

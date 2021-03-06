package smartBuilding.server.resource.coap;

import smartBuilding.server.resource.raw.LightActuator;
import smartBuilding.server.resource.raw.ResourceDataListener;
import smartBuilding.server.resource.raw.SmartObjectResource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CoreInterfaces;
import utils.SenMLPack;
import utils.SenMLRecord;

import java.util.Optional;

public class CoapLightResource extends CoapResource {

    private final static Logger logger = LoggerFactory.getLogger(CoapLightResource.class);

    private final static String OBJECT_TITLE = "Light Actuator";

    private Double ACTUATOR_VERSION = 0.5;

    private LightActuator lightActuator;

    private Boolean Is_Active=false;

    private String deviceId;
    private ObjectMapper objectMapper;


    public CoapLightResource(String name, String deviceId, LightActuator lightActuator) {
        super(String.format("%s:%s", deviceId, name));



        if (lightActuator != null && deviceId != null)
        {
            this.deviceId = deviceId;
            this.lightActuator = lightActuator;
            this.objectMapper=new ObjectMapper();
            this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            setObservable(true);
            setObserveType(CoAP.Type.CON);

            getAttributes().setTitle(OBJECT_TITLE);
            getAttributes().setObservable();
            getAttributes().addAttribute("rt", lightActuator.getType());
            getAttributes().addAttribute("if", CoreInterfaces.CORE_A.getValue());
            getAttributes().addAttribute("ct", Integer.toString(MediaTypeRegistry.APPLICATION_SENML_JSON));
            getAttributes().addAttribute("ct", Integer.toString(MediaTypeRegistry.TEXT_PLAIN));

            lightActuator.addDataListener(new ResourceDataListener<Boolean>() {
                @Override
                public void onDataChanged(SmartObjectResource<Boolean> resource, Boolean updatedValue) {
                    logger.info("Raw Resource Notification. New Value: {}", updatedValue);

                    Is_Active=updatedValue;
                    changed();
                }
            });

        }
        else {
            logger.error(" ERROR -->NULL Raw References");
        }

    }
    private Optional<String> getJsonSenmlResponse() {

        try {

            SenMLPack senMLPack = new SenMLPack();

            SenMLRecord senMLRecord = new SenMLRecord();
            senMLRecord.setBn(String.format("%s:%s", this.deviceId, "light"));
            senMLRecord.setBver(ACTUATOR_VERSION);
            senMLRecord.setVb(Is_Active);
            senMLRecord.setT(System.currentTimeMillis());

            senMLPack.add(senMLRecord);

            logger.info("{}", senMLPack);

            return Optional.of(this.objectMapper.writeValueAsString(senMLPack));

        } catch (Exception e) {
            return Optional.empty();
        }
    }


    @Override
    public void handleGET(CoapExchange exchange) {


        logger.info("Pretty Print: \n{}", Utils.prettyPrint(exchange.advanced().getRequest()));
        logger.info("Pretty Print: \n{}", exchange.getRequestOptions());

        Optional<String> senmlPayload = getJsonSenmlResponse();

        if (senmlPayload.isPresent()){
            exchange.respond(CoAP.ResponseCode.CONTENT, senmlPayload.get(), exchange.getRequestOptions().getAccept());
            logger.info("Pretty Print: \n{}", exchange.getRequestOptions());
        }
        else
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);

    }
    @Override
    public void handlePOST(CoapExchange exchange){
        try{
            if(exchange.getRequestPayload() == null){

                this.Is_Active = !Is_Active;
                this.lightActuator.setActive(Is_Active);

                logger.info("Resource Status Updated: {}", this.Is_Active);

                changed();

                exchange.respond(CoAP.ResponseCode.CHANGED);
            }
            else
                exchange.respond(CoAP.ResponseCode.BAD_REQUEST);

        }catch (Exception e){
            logger.error("Error Handling POST -> {}", e.getLocalizedMessage());
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }
    @Override
    public void handlePUT(CoapExchange exchange){
        try{

            if(exchange.getRequestPayload() != null){

                boolean submittedValue = Boolean.parseBoolean(new String(exchange.getRequestPayload()));

                logger.info("Submitted value: {}", submittedValue);

                this.Is_Active = submittedValue;
                this.lightActuator.setActive(this.Is_Active);

                logger.info("Resource Status Updated: {}", this.Is_Active);

                changed();

                exchange.respond(CoAP.ResponseCode.CHANGED);
            }
            else
                exchange.respond(CoAP.ResponseCode.BAD_REQUEST);

        }catch (Exception e){
            logger.error("Error Handling POST -> {}", e.getLocalizedMessage());
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }


    }
}

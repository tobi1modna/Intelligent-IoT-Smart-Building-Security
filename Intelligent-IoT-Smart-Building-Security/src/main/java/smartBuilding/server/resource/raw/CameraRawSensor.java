package smartBuilding.server.resource.raw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class CameraRawSensor extends SmartObjectResource<Integer>{

    private static Logger logger = LoggerFactory.getLogger(CameraRawSensor.class);

    private static final double MIN_VALUE = 0;

    private static final double MAX_VALUE = 30;

    private static final double MIN_VARIATION = 0;

    private static final double MAX_VARIATION = 5;

    private static final String LOG_DISPLAY_NAME = "CameraSensor";

    private static final String type ="People";

    public static final long UPDATE_PERIOD = 5000;

    private static final long TASK_DELAY_TIME = 10000;

    private static final String RESOURCE_TYPE = "iot.sensor.camera";

    private int updatedValue;



    private Random random;

    private Timer updateTimer = null;

    public CameraRawSensor() {
        super(UUID.randomUUID().toString(), RESOURCE_TYPE);
        init();
    }

    private void init(){

        try{

            this.random = new Random(System.currentTimeMillis());
            this.updatedValue = ((int) (MIN_VALUE + this.random.nextDouble()*(MAX_VALUE - MIN_VALUE)));

            startPeriodicEventValueUpdateTask();

        }catch (Exception e){
            logger.error("Error initializing the IoT Resource ! Msg: {}", e.getLocalizedMessage());
        }

    }

    private void startPeriodicEventValueUpdateTask(){

        try{

            logger.info("Starting periodic Update Task with Period: {} ms", UPDATE_PERIOD);

            this.updateTimer = new Timer();
            this.updateTimer.schedule(new TimerTask() {
                @Override
                public void run() {

                    int people = (int) (MIN_VARIATION + MAX_VARIATION*random.nextDouble());
                    if (random.nextDouble()>0.5){
                        updatedValue = updatedValue + people;
                    }
                    else {
                        if (updatedValue - people >= 0)
                        {
                            updatedValue = updatedValue - people;
                        }

                    }

                    notifyUpdate(updatedValue);

                }
            }, TASK_DELAY_TIME, UPDATE_PERIOD);

        }catch (Exception e){
            logger.error("Error executing periodic resource value ! Msg: {}", e.getLocalizedMessage());
        }

    }

    @Override
    public Integer loadUpdatedValue() {
        return this.updatedValue;
    }


}

package lk.leadco.ecogrid.utils;

import android.content.Context;
import android.os.Looper;

import java.util.Random;
import android.os.Handler;

import lk.leadco.ecogrid.model.Vehicle;

public class EcoGridVirtualOBDManager {

    public interface OBDDataListener{
        void onDataUpdate(int batteryLevel, double voltage, double currentAmp, int temperatureCelsius,
                          int estimatedRangeKm, String totalChargeTime, int monthlyEnergyKwh, int co2SavedKg);
    }

    private  OBDDataListener listener;
    private Handler handler;
    private Runnable obdRunnable;
    private Random random;

    private int currentBatteryLevel;
    private boolean isCharging = false;
    private int temperature = 35;

    private String monthlyChargeTime = "0h 0m";
    private int monthlyEnergyUsed = 0;
    private int monthlyCo2Saved = 0;

    private Vehicle connectedVehicle;
    private static EcoGridVirtualOBDManager instance;

    private EcoGridVirtualOBDManager(){
        handler = new Handler(Looper.getMainLooper());
        random = new Random();
    }

    public static EcoGridVirtualOBDManager getInstance(){
        if(instance == null){
            instance = new EcoGridVirtualOBDManager();
        }
        return instance;
    }

    public void setListener(OBDDataListener listener){
        this.listener = listener;
    }

    public void connectToVehicle(Context context, Vehicle vehicle){
        this.connectedVehicle = vehicle;

        this.currentBatteryLevel = SharedPrefsManager.getCurrentBatteryLevel(context);

        this.monthlyEnergyUsed = 50 + random.nextInt(150);
        this.monthlyCo2Saved = (int) (this.monthlyEnergyUsed * 0.85);

        int hours = 10 + random.nextInt(20);
        int min = random.nextInt(60);
        this.monthlyChargeTime = hours + "h "+min +"m";

    }
    public int getCurrentBatteryLevel(){
        return this.currentBatteryLevel;
    }
    public void startSimulation(Context context){

        stopSimulation();

        obdRunnable = new Runnable(){
          @Override
          public void run(){
              generateMockData(context);
              if(listener != null){
                  listener.onDataUpdate(
                          currentBatteryLevel,
                          getSimulatedVoltage(),
                          getSimulatedCurrent(),
                          temperature,
                          calculateEstimatedRange(),
                          monthlyChargeTime,
                          monthlyEnergyUsed,
                          monthlyCo2Saved
                  );
              }
              handler.postDelayed(this,2000);
          }
        };
        handler.post(obdRunnable);
    }

    public void stopSimulation(){
        if(handler != null && obdRunnable != null){
            handler.removeCallbacks(obdRunnable);
        }
    }

    public void setChargingStatus(boolean isCharging){
        this.isCharging = isCharging;
    }

    private int calculateEstimatedRange(){
        if(connectedVehicle == null) return 0;
        double efficiencyPerKwh = 6.5;
        double maxRange = connectedVehicle.getBattery_capacity_kwh() * efficiencyPerKwh;
        return (int) ((maxRange * currentBatteryLevel) / 100);
    }

    private void generateMockData(Context context){
        if(isCharging){
            if(currentBatteryLevel < 100) currentBatteryLevel += 1;
            SharedPrefsManager.saveCurrentBatteryLevel(context , currentBatteryLevel);
            if(temperature < 45) temperature += random.nextInt(2);
        }else{
            if(currentBatteryLevel > 0 && random.nextBoolean()) currentBatteryLevel -= 1;
            SharedPrefsManager.saveCurrentBatteryLevel(context , currentBatteryLevel);
            if(temperature > 30) temperature -= random.nextInt(2);
        }
    }

    private double getSimulatedVoltage(){
        return isCharging ? 400.0 + random.nextDouble() * 5 : 350.0 + random.nextDouble() * 30 ;
    }

    private double getSimulatedCurrent(){
        return isCharging ? 32.0 + random.nextDouble() : -(10.0 + random.nextDouble() * 40);
    }
}

package dev.shustoff.dikt.sample;

public class SampleMain {
    public static void main(String[] args) {
        CarModule module = new CarModule(new EngineModule(new EngineNameModule<>("test engine")));
        Garage garage = GarageKt.getGarage(module);
        System.out.println(garage.toString());
    }
}

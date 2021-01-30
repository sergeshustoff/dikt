package dev.shustoff.dikt.sample;

public class SampleMain {
    public static void main(String[] args) {
        CarModule module = new CarModule(new EngineModule("test engine"));
        Car car = module.getCar();
        System.out.println(car.toString());
    }
}

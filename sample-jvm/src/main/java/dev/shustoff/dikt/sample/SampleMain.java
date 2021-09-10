package dev.shustoff.dikt.sample;

import dev.shustoff.dikt.test.Test;

public class SampleMain {
    public static void main(String[] args) {
        Test.INSTANCE.verify();
        CarModule module = new CarModule(new EngineModule(new EngineNameModuleImpl<>("test engine")));
        Garage garage = module.getGarage();
        System.out.println(garage.toString());
    }
}

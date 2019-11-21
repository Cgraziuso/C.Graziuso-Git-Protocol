package it.unisa.gitProtocol.implementation;

import java.util.Random;

public class SupporterMethodImpl implements SupporterMethod {


    @Override
    public int randomNumber() {


        Random rand = new Random(System.currentTimeMillis());
        int number = rand.nextInt();


        return number;
    }
}


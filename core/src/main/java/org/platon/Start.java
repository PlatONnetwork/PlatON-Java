package org.platon;

import org.platon.core.facade.PlatonFactory;
import org.platon.core.facade.PlatonImpl;

import java.io.IOException;
import java.net.URISyntaxException;

public class Start {

    public static void main(String args[]) throws IOException, URISyntaxException {

        PlatonImpl platon = (PlatonImpl) PlatonFactory.createPlaton();

    }

}
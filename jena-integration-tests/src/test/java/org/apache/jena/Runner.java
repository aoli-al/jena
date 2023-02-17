package org.apache.jena;

import org.apache.jena.base.Sys;
import org.apache.jena.geosparql.TS_GeoSPARQL;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.http.TS_JenaHttp;
import org.apache.jena.test.TC_Integration;
import org.junit.runner.JUnitCore;

import javax.net.ssl.SSLContext;
import java.lang.reflect.Constructor;
import java.security.NoSuchAlgorithmException;

public class Runner {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        JUnitCore core = new JUnitCore();
        core.run(TS_GeoSPARQL.class);
        core.run(TS_JenaHttp.class);
        core.run(TS_GeoSPARQL.class);
        core.run(TC_Integration.class);
        System.exit(0);
    }
}

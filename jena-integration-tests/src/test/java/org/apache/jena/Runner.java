package org.apache.jena;

import org.apache.jena.base.Sys;
import org.apache.jena.geosparql.TS_GeoSPARQL;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.http.TS_JenaHttp;
import org.apache.jena.test.TC_Integration;
import org.junit.runner.JUnitCore;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.NoSuchAlgorithmException;

public class Runner {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        String forDate = System.getenv("PERF_OUT_FILE");
        if (forDate == null) {
            forDate = "/tmp/perf_out.txt";
        }
        File file = new File(forDate);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
        }
        for (int i = 0; i < 5; i++) {
            if (i < 4) {
                System.setProperty("bench_started", "false");
            } else {
                System.setProperty("bench_started", "true");
            }
            JUnitCore core = new JUnitCore();
            core.run(TS_JenaHttp.class);

        }
        System.exit(0);
    }
}

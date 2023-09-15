/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* ******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 * *****************************************************************************/

// Submitted JENA-256 

package com.hp.hpl.jena.tdb.extra ;

import java.util.ArrayList ;
import java.util.List ;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openjena.atlas.lib.FileOps ;
import org.openjena.atlas.logging.Log ;

import com.hp.hpl.jena.query.Dataset ;
import com.hp.hpl.jena.query.ReadWrite ;
import com.hp.hpl.jena.rdf.model.Model ;
import com.hp.hpl.jena.rdf.model.Property ;
import com.hp.hpl.jena.rdf.model.Resource ;
import com.hp.hpl.jena.tdb.TDBFactory ;
import com.hp.hpl.jena.tdb.base.block.FileMode ;
import com.hp.hpl.jena.tdb.base.file.Location ;
import com.hp.hpl.jena.tdb.sys.SystemTDB ;
import com.hp.hpl.jena.tdb.transaction.Journal ;
import com.hp.hpl.jena.tdb.transaction.JournalControl ;
import com.hp.hpl.jena.tdb.transaction.TransactionManager ;

public class T_TDBWriteTransaction {

	private final static int TOTAL = 100 ;
    static boolean bracketWithReader = true ;

	final static String INDEX_INFO_SUBJECT =   "http://test.net/xmlns/test/1.0/Triple-Indexer";
	final static String TIMESTAMP_PREDICATE =  "http://test.net/xmlns/test/1.0/lastProcessedTimestamp";
	final static String URI_PREDICATE =        "http://test.net/xmlns/test/1.0/lastProcessedUri";
	final static String VERSION_PREDICATE =    "http://test.net/xmlns/test/1.0/indexVersion";
	final static String INDEX_SIZE_PREDICATE = "http://test.net/xmlns/test/1.0/indexSize";

    public static void main(String[] args) throws Exception {
        if ( true )
            SystemTDB.setFileMode(FileMode.direct) ;

        Log.setLog4j("jena-tdb/log4j.properties") ;
	    TransactionManager.QueueBatchSize = 10;
	    
//		if (args.length == 0) {
//			System.out.println("Provide index location");
//			return;
//		}

	    String location = "DBX" ;
	    FileOps.ensureDir(location) ;
	    //FileOps.clearDirectory(location) ;
	    bracketWithReader = false ;
	    
	    run(location) ;
//	    StoreConnection.make(location).forceRecoverFromJournal() ;
//	    run(location) ;
    }

    static public void run(final String location) throws Exception {
        if ( false )
        {
            Journal journal = Journal.create(new Location(location)) ;
            JournalControl.print(journal) ;
            journal.close() ;
        }
        //String location = args[0]; // + "/" + UUID.randomUUID().toString();

		//String baseGraphName = "com.ibm.test.graphNamePrefix.";   

		final Dataset dataset = TDBFactory.createDataset(location);
		long totalExecTime = 0L;
		long size = 0;

		boolean crashHappened = false;
		for (int i = 0; i < TOTAL; i++) {
			List<String> lastProcessedUris = new ArrayList<String>();
			for (int j = 0; j < 10*i; j++) {
				String lastProcessedUri = "http://test.net/xmlns/test/1.0/someUri" + j;
				lastProcessedUris.add(lastProcessedUri);
			}
			//Dataset dataset = TDBFactory.createDataset(location);
			//String graphName = baseGraphName + i;
			long t = System.currentTimeMillis();

			try {
				dataset.begin(ReadWrite.WRITE);
				Model m = dataset.getDefaultModel();

				m.removeAll();
				Resource subject = m.createResource(INDEX_INFO_SUBJECT);
				Property predicate = m.createProperty(TIMESTAMP_PREDICATE);
				m.addLiteral(subject, predicate, System.currentTimeMillis());
				predicate = m.createProperty(URI_PREDICATE);
				for (String uri : lastProcessedUris) {
					m.add(subject, predicate, m.createResource(uri));
				}
				predicate = m.createProperty(VERSION_PREDICATE);
				m.addLiteral(subject, predicate, 1.0);

				size += m.size() + 1;

				predicate = m.createProperty(INDEX_SIZE_PREDICATE);
				m.addLiteral(subject, predicate, size);


				try {
					dataset.commit();
				} catch (Exception e) {
					e.printStackTrace();
					crashHappened = true;
				}

				String location1 = "DBX1" ;
				FileOps.ensureDir(location1) ;
				Dataset dataset1 = TDBFactory.createDataset(location1);


				dataset1.begin(ReadWrite.READ);
				Model m1 = dataset.getDefaultModel();

				m1.removeAll();
				Resource subject1 = m1.createResource(INDEX_INFO_SUBJECT);
				Property predicate1 = m1.createProperty(TIMESTAMP_PREDICATE);
				m1.addLiteral(subject1, predicate1, System.currentTimeMillis());
				predicate1 = m1.createProperty(URI_PREDICATE);
				for (String uri : lastProcessedUris) {
					m1.add(subject1, predicate1, m1.createResource(uri));
				}
				predicate1 = m.createProperty(VERSION_PREDICATE);
				m1.addLiteral(subject1, predicate1, 1.0);

				size += m1.size() + 1;

				predicate1 = m1.createProperty(INDEX_SIZE_PREDICATE);
				m1.addLiteral(subject1, predicate1, size);
				dataset1.commit();


				T_TransSystemMultiDatasets.main();

				if (crashHappened) {
					dataset.abort();
				}
			} finally {
				dataset.end();
				long writeOperationDuration = System.currentTimeMillis() - t;
				totalExecTime += writeOperationDuration;
				System.out.println("Write operation " + i + " took " + writeOperationDuration + "ms");
			}
		}

		System.out.println("All " + TOTAL + " write operations wrote " + size + " triples and took " + totalExecTime + "ms");
		Thread.sleep(2000);
	}

//		ExecutorService executor = Executors.newSingleThreadExecutor();
//		Future<Void> future = executor.submit(callable);
//		try {
//			future.get(5, TimeUnit.SECONDS);
//		} catch (TimeoutException ex) {
//			executor.shutdown();
//			Future<Void> result = (Future<Void>) executor.submit(new Runnable() {
//				@Override
//				public void run() {
//					dataset.abort();
//				}
//			});
//			result.get();
//			// handle the timeout
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//			// handle the interrupts
//		} catch (ExecutionException e) {
//			// handle other exceptions
//		} finally {
////			future.cancel(true); // may or may not desire this
//		}


}

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008-12, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.overlord.bam.performance.jee.tester;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class PerformanceJEETest {
	
	private static final Logger LOG=Logger.getLogger(PerformanceJEETest.class.getName());

    @Deployment(name="overlord-bam", order=1)
    public static WebArchive createDeployment1() {
        String version=System.getProperty("bam.version");

        java.io.File[] archiveFiles=DependencyResolvers.use(MavenDependencyResolver.class)
                .artifacts("org.overlord.bam.release.jbossas:overlord-bam:war:"+version)
                .resolveAsFiles();
        
        return ShrinkWrap.createFromZipFile(WebArchive.class,
                copyToTmpFile(archiveFiles[0],"overlord-bam.war"));
    }
    
    @Deployment(name="app", order=2)
    public static WebArchive createDeployment2() {
        String version=System.getProperty("bam.version");

        java.io.File[] archiveFiles=DependencyResolvers.use(MavenDependencyResolver.class)
                .artifacts("org.overlord.bam.release.jbossas.performance:performance-jbossas-app:war:"+version)
                .resolveAsFiles();
        
        return ShrinkWrap.createFromZipFile(WebArchive.class,
                			copyToTmpFile(archiveFiles[0],"jeeapp.war"));
    }
    
    @Deployment(name="epn", order=3)
    public static WebArchive createDeployment3() {
        String version=System.getProperty("bam.version");

        java.io.File[] archiveFiles=DependencyResolvers.use(MavenDependencyResolver.class)
                .artifacts("org.overlord.bam.release.jbossas.performance:performance-jbossas-epn:war:"+version)
                .resolveAsFiles();
        
        return ShrinkWrap.createFromZipFile(WebArchive.class, archiveFiles[0]);
    }
    
    @Deployment(name="acs", order=4)
    public static WebArchive createDeployment4() {
        String version=System.getProperty("bam.version");

        java.io.File[] archiveFiles=DependencyResolvers.use(MavenDependencyResolver.class)
                .artifacts("org.overlord.bam.release.jbossas.performance:performance-jbossas-acs:war:"+version)
                .resolveAsFiles();
        
        return ShrinkWrap.createFromZipFile(WebArchive.class, archiveFiles[0]);
    }
    
    @Deployment(name="monitor", order=5)
    public static WebArchive createDeployment5() {
        String version=System.getProperty("bam.version");

        java.io.File[] archiveFiles=DependencyResolvers.use(MavenDependencyResolver.class)
                .artifacts("org.overlord.bam.release.jbossas.performance:performance-jbossas-monitor:war:"+version)
                .resolveAsFiles();
        
        return ShrinkWrap.createFromZipFile(WebArchive.class,
                        copyToTmpFile(archiveFiles[0],"jeemonitor.war"));
    }
    
    private static java.io.File copyToTmpFile(java.io.File source, String filename) {
        String tmpdir=System.getProperty("java.io.tmpdir");
        java.io.File dir=new java.io.File(tmpdir+java.io.File.separator+"bamtests"+System.currentTimeMillis());
        
        dir.mkdir();
        
        dir.deleteOnExit();
        
        java.io.File ret=new java.io.File(dir, filename);
        ret.deleteOnExit();
        
        // Copy contents to the tmp file
        try {
            java.io.FileInputStream fis=new java.io.FileInputStream(source);
            java.io.FileOutputStream fos=new java.io.FileOutputStream(ret);
            
            byte[] b=new byte[10240];
            int len=0;
            
            while ((len=fis.read(b)) > 0) {
                fos.write(b, 0, len);
            }
            
            fis.close();
            
            fos.flush();
            fos.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to copy file '"+filename+"': "+e);
        }
        
        return(ret);
    }

    @Test @OperateOnDeployment("overlord-bam")
    public void createTransactions() {
        
        try {
        	// Start the monitor
            URL getUrl = new URL("http://localhost:8080/jeemonitor/monitor/start");
            HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
            connection.setRequestMethod("GET");

            java.io.InputStream is=connection.getInputStream();
            
            byte[] b=new byte[is.available()];
            is.read(b);
           
            is.close();
            connection.disconnect();

            int max_txns=500; // BAM-107 Need to reduce the test size for now, to avoid 1 min timeout. Was 100000;
            
            ExecutorService executor=Executors.newFixedThreadPool(100);
            
            for (int i=0; i < max_txns; i++) {
            	final int txn=i;
            	
            	java.lang.Runnable task=new java.lang.Runnable() {
            		public void run() {
            			try {
	        	            // Create transactions
	            			URL getUrl = new URL("http://localhost:8080/jeeapp/app/create?id="+txn);
	            			HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
	        	            connection.setRequestMethod("GET");
	        	
	        	            java.io.InputStream is=connection.getInputStream();
	        	            
	        	            byte[] b=new byte[is.available()];
	        	            is.read(b);
	        	           
	        	            is.close();
	        	            connection.disconnect(); 
            			} catch (Exception e) {
            			    LOG.log(Level.SEVERE, "Failed to run task", e);
            			}
            		}
            	};
            	
            	executor.execute(task);
            }
            
            executor.shutdown();
            
            LOG.info("PerformanceTest: Tester shutdown performance test executor");
            
            try {
                executor.awaitTermination(10, TimeUnit.MINUTES);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Failed to await termination of executor", t);
            }
            
            LOG.info("PerformanceTest: Tester finished awaiting termination in performance test");

            // Wait for latency to be returned, indicating completion of processing
            long latency=0;
            int count=0;
            
            do {
	            synchronized (this) {
	            	wait(2000);
	            }
	
	            // Check for latency - if 0, then means has not yet completed
	            getUrl = new URL("http://localhost:8080/jeemonitor/monitor/latency");
	            connection = (HttpURLConnection) getUrl.openConnection();
	            connection.setRequestMethod("GET");
	
	            is=connection.getInputStream();
	            
	            b=new byte[is.available()];
	            is.read(b);
	           
	            is.close();
	            connection.disconnect();
	            
	            latency = Long.parseLong(new String(b));
	            
	            if (count++ > 50) {
	            	fail("Check for latency taking too long!");
	            }

            } while (latency == 0);

            LOG.info("PerformanceTest: Monitor latency="+latency);

            // Get monitor duration
            getUrl = new URL("http://localhost:8080/jeemonitor/monitor/duration");
            connection = (HttpURLConnection) getUrl.openConnection();
            connection.setRequestMethod("GET");

            is=connection.getInputStream();
            
            b=new byte[is.available()];
            is.read(b);
           
            is.close();
            connection.disconnect();
            
            long monitorDuration=Long.parseLong(new String(b));
            
            LOG.info("PerformanceTest: Tester received monitor duration="+monitorDuration);

            // Get app duration
            getUrl = new URL("http://localhost:8080/jeeapp/app/duration");
            connection = (HttpURLConnection) getUrl.openConnection();
            connection.setRequestMethod("GET");

            is=connection.getInputStream();
            
            b=new byte[is.available()];
            is.read(b);
           
            is.close();
            
            long appDuration=Long.parseLong(new String(b));
            
            LOG.info("PerformanceTest: Tester received application duration="+appDuration);
            
            if (monitorDuration > appDuration) {
            	fail("Processing of activities took too long!");
            }

        } catch (Throwable t) {
            fail("Failed to invoke app: "+t);
        }
    }

}
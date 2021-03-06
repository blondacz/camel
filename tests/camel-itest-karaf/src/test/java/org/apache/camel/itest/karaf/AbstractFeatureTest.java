/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.itest.karaf;

import java.io.File;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultRouteContext;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.osgi.CamelContextFactory;
import org.apache.karaf.tooling.exam.options.KarafDistributionOption;
import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.UrlReference;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.replaceConfigurationFile;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;

public abstract class AbstractFeatureTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFeatureTest.class);

    @Inject
    protected BundleContext bundleContext;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    protected void testComponent(String component) throws Exception {
        long max = System.currentTimeMillis() + 10000;
        while (true) {
            try {
                assertNotNull("Cannot get component with name: " + component, createCamelContext().getComponent(component));
                return;
            } catch (Exception t) {
                if (System.currentTimeMillis() < max) {
                    Thread.sleep(1000);
                } else {
                    throw t;
                }
            }
        }
    }

    protected void testDataFormat(String format) throws Exception {
        long max = System.currentTimeMillis() + 10000;
        while (true) {
            try {
                DataFormatDefinition dataFormatDefinition = createDataformatDefinition(format);                
                assertNotNull(dataFormatDefinition);
                assertNotNull(dataFormatDefinition.getDataFormat(new DefaultRouteContext(createCamelContext())));
                return;
            } catch (Exception t) {
                if (System.currentTimeMillis() < max) {
                    Thread.sleep(1000);
                } else {
                    throw t;
                }
            }
        }
    }

    protected DataFormatDefinition createDataformatDefinition(String format) {
        return null;
    }

    protected void testLanguage(String lang) throws Exception {
        long max = System.currentTimeMillis() + 10000;
        while (true) {
            try {
                assertNotNull(createCamelContext().resolveLanguage(lang));
                return;
            } catch (Exception t) {
                if (System.currentTimeMillis() < max) {
                    Thread.sleep(1000);
                } else {
                    throw t;
                }
            }
        }
    }

    protected CamelContext createCamelContext() throws Exception {
        CamelContextFactory factory = new CamelContextFactory();
        factory.setBundleContext(bundleContext);
        LOG.info("Get the bundleContext is " + bundleContext);
        return factory.createContext();
    }

    public static String extractName(Class<?> clazz) {
        String name = clazz.getName();
        int id0 = name.indexOf("Camel") + "Camel".length();
        int id1 = name.indexOf("Test");
        StringBuilder sb = new StringBuilder();
        for (int i = id0; i < id1; i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && sb.length() > 0) {
                sb.append("-");
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    public static UrlReference getCamelKarafFeatureUrl() {
        return mavenBundle().
                groupId("org.apache.camel.karaf").
                artifactId("apache-camel").
                classifier("features").type("xml").versionAsInProject();
    }
    
    public static UrlReference getKarafFeatureUrl() {
        String karafVersion = System.getProperty("karafVersion");
        LOG.info("*** The karaf version is " + karafVersion + " ***");

        String type = "xml/features";
        return mavenBundle().groupId("org.apache.karaf.assemblies.features").
            artifactId("standard").version(karafVersion).type(type);
    }

    public static Option[] configure(String feature) {
        Option[] options = 
            new Option[]{
                karafDistributionConfiguration().frameworkUrl(
                    maven().groupId("org.apache.karaf").artifactId("apache-karaf").type("tar.gz").versionAsInProject())
                    .karafVersion("2.3.3").name("Apache Karaf")
                    .unpackDirectory(new File("target/paxexam/unpack/")),
                
                KarafDistributionOption.keepRuntimeFolder(),
                // override the config.properties (to fix pax-exam bug)
                replaceConfigurationFile("etc/config.properties", new File("src/test/resources/org/apache/camel/itest/karaf/config.properties")),
                replaceConfigurationFile("etc/custom.properties", new File("src/test/resources/org/apache/camel/itest/karaf/custom.properties")),
                replaceConfigurationFile("etc/jre.properties", new File("../../platforms/karaf/features/src/main/resources/config.properties")),
                // Add apache-snapshots repository
                editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.repositories",
                    "http://repo1.maven.org/maven2@id=central, "
                        + "http://svn.apache.org/repos/asf/servicemix/m2-repo@id=servicemix, "
                        + "http://repository.springsource.com/maven/bundles/release@id=springsource.release, "
                        + "http://repository.springsource.com/maven/bundles/external@id=springsource.external, "
                        + "http://oss.sonatype.org/content/repositories/releases/@id=sonatype, "
                        + "http://repository.apache.org/content/groups/snapshots-group@snapshots@noreleases@id=apache"),

                    // we need INFO logging otherwise we cannot see what happens
                logLevel(LogLevelOption.LogLevel.INFO),
                 // install the cxf jaxb spec as the karaf doesn't provide it by default
                scanFeatures(getCamelKarafFeatureUrl(), "cxf-jaxb", "camel-core", "camel-spring", "camel-" + feature)};

        return options;
    }

}

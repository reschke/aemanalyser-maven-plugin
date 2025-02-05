/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/
package com.adobe.aem.analyser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.sling.feature.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates user features based on the output of the content-package-to-feature-model converter
 *
 * <p>This implementation looks for the {@code runmode.mapping} file and reads the aggregates to
 * generate from it.</p>
 *
 */
public class RunmodeMappingUserFeatureAggregator implements UserFeatureAggregator {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final File featureInputDirectory;

    public RunmodeMappingUserFeatureAggregator(File featureInputDirectory) {
        this.featureInputDirectory = featureInputDirectory;
    }

    @Override
    public Map<String, List<Feature>> getUserAggregates(Map<String, Feature> projectFeatures, EnumSet<ServiceType> serviceTypes)
            throws IOException {
        // get run modes from converter output
        final Properties runmodeProps = getRunmodeMappings();

        final Map<String, List<Feature>> aggregates = new HashMap<>();

        Map<String, Set<String>> toCreate = getUserAggregatesToCreate(runmodeProps, serviceTypes);
        for (final Map.Entry<String, Set<String>> entry : toCreate.entrySet()) {
            final String name = "user-aggregated-".concat(entry.getKey());

            logger.info("For aggregate {} got entries {}", name, entry.getValue());

            final List<Feature> list = aggregates.computeIfAbsent(name, n -> new ArrayList<>());
            entry.getValue().forEach(n -> list.add(projectFeatures.get(n)));
        }

        return aggregates;
    }

    private Properties getRunmodeMappings() throws IOException {
        File mappingFile = new File(featureInputDirectory, "runmode.mapping");
        if (!mappingFile.isFile())
            throw new IOException("File generated by content package to feature model converter not found: " + mappingFile);

        Properties p = new Properties();
        try (InputStream is = new FileInputStream(mappingFile)) {
            p.load(is);
        }
        return p;
    }

    private Map<String, Set<String>> getUserAggregatesToCreate(final Properties runmodeProps, final EnumSet<ServiceType> serviceTypes)
            throws IOException {
        try {
            return AemAnalyserUtil.getAggregates(runmodeProps, serviceTypes);
        } catch ( final IllegalArgumentException iae) {
            throw new IOException(iae.getMessage());
        }
    }
}
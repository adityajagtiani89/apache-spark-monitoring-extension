/**
 * Copyright 2017 AppDynamics, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.appdynamics.extensions.spark;

/**
 * Created by aditya.jagtiani on 5/3/17.
 */

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SparkMonitor  extends AManagedMonitor {
    private static final Logger logger = LoggerFactory.getLogger(SparkMonitor.class);
    private MonitorConfiguration configuration;

    public SparkMonitor() {
        logger.info("Using Spark Monitor Version [" + getImplementationVersion() + "]");
    }

    protected void initialize(Map<String, String> argsMap) {
        if (configuration == null) {
            MetricWriteHelper metricWriter = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration("Custom Metrics|Spark|", new TaskRunner(), metricWriter);
            final String configFilePath = argsMap.get("config-file");
            conf.setConfigYml(configFilePath);
            conf.checkIfInitialized(MonitorConfiguration.ConfItem.METRIC_PREFIX, MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.HTTP_CLIENT
                    , MonitorConfiguration.ConfItem.EXECUTOR_SERVICE);
            this.configuration = conf;
        }
    }

    public TaskOutput execute(Map<String, String> map, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        logger.debug("The raw arguments are {}", map);
        try {
            initialize(map);
            configuration.executeTask();
        } catch (Exception ex) {
            if (configuration != null && configuration.getMetricWriter() != null) {
                configuration.getMetricWriter().registerError(ex.getMessage(), ex);
            }
        }
        return null;
    }

    private class TaskRunner implements Runnable {

        public void run() {
            Map<String, ?> config = configuration.getConfigYml();
            List<Map> servers = (List) config.get("servers");
            if (servers != null && !servers.isEmpty()) {
                for (Map server : servers) {
                    SparkMonitorTask task = new SparkMonitorTask(configuration, server);
                    configuration.getExecutorService().execute(task);
                }
            } else {
                logger.error("Error encountered while running the Spark Monitoring task");
            }
        }
    }

    private static String getImplementationVersion() {
        return SparkMonitor.class.getPackage().getImplementationVersion();
    }

    public static void main(String[] args) throws TaskExecutionException {
        SparkMonitor sparkMonitor = new SparkMonitor();
        Map<String, String> argsMap = new HashMap<String, String>();
        argsMap.put("config-file", "/Users/aditya.jagtiani/repos/appdynamics/extensions/apache-spark-monitoring-extension/src/main/resources/conf/config.yml");
        sparkMonitor.execute(argsMap, null);
    }
}

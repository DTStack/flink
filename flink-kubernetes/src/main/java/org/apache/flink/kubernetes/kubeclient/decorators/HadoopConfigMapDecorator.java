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

package org.apache.flink.kubernetes.kubeclient.decorators;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;
import org.apache.flink.kubernetes.kubeclient.resources.KubernetesConfigMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Decorate config map for hadoop configuration file.
 */
public class HadoopConfigMapDecorator extends Decorator<ConfigMap, KubernetesConfigMap> {

	private static final Logger LOG = LoggerFactory.getLogger(HadoopConfigMapDecorator.class);

	public HadoopConfigMapDecorator() {
	}

	@Override
	protected ConfigMap decorateInternalResource(ConfigMap resource, Configuration configuration) {
		Map<String, String> configMap = resource.getData();
		try {
			String hadoopConfString = configuration.getString(KubernetesConfigOptions.HADOOP_CONF_STRING);
			ObjectMapper objectMapper = new ObjectMapper();
			Map hadoopConfMap = objectMapper.readValue(hadoopConfString, Map.class);
			String coreSiteContent = getCoreSiteContent(hadoopConfMap);
			configMap.put("core-site.xml", coreSiteContent);
		} catch (IOException e) {
			LOG.error("", e);
			throw new RuntimeException(e.getMessage());
		}

		resource.setData(configMap);
		return resource;
	}

	protected String getCoreSiteContent(Map hadoopConfMap) {
		StringBuilder hadoopConfContent = new StringBuilder();
		hadoopConfContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(System.lineSeparator());
		hadoopConfContent.append("<?xml-stylesheet href=\"configuration.xsl\" type=\"text/xsl\"?>").append(System.lineSeparator());
		hadoopConfContent.append("<configuration>").append(System.lineSeparator());
		Iterator<Map.Entry<String, Object>> it = hadoopConfMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> e = it.next();
			String name = e.getKey();
			String value = e.getValue().toString();
			hadoopConfContent.append("    <property>").append(System.lineSeparator());
			hadoopConfContent.append("        <name>").append(name).append("</name>").append(System.lineSeparator());
			hadoopConfContent.append("        <value>").append(value).append("</value>").append(System.lineSeparator());
			hadoopConfContent.append("    </property>").append(System.lineSeparator());
		}
		hadoopConfContent.append("</configuration>").append(System.lineSeparator());

		return hadoopConfContent.toString();
	}

}

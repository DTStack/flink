<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

#### 支持访问hdfs
1. org.apache.flink.configuration.ConfigConstants：（ConfigConstants.java: 2021） 增加属性ENV_HADOOP_CONF_DIR，用于获取HADOOP_CONF_DIR环境变量名
2. org.apache.flink.kubernetes.cli.KubernetesSessionCli:（KubernetesSessionCli.java：91～123） 增加addHadoopConf方法，读取hadoop的配置添加到flink的configuration上
3. org.apache.flink.kubernetes.cli.KubernetesSessionCli:（KubernetesSessionCli.java：128～132） getEffectiveConfiguration方法更改，添加配置项hadoop.conf.string，用于保存hadoop的相关配置
4. org.apache.flink.kubernetes.configuration.KubernetesConfigOptions: （KubernetesConfigOptions.java：126～139） 增加HADOOP_CONF_DIR、HADOOP_CONF_STRING两个参数
5. org.apache.flink.kubernetes.kubeclient.Fabric8FlinkKubeClient:（Fabric8FlinkKubeClient.java:100~102） 初始化方法initialize（） configMapDecorators增加HadoopConfigMapDecorator，将hadoop配置写入configMap
6. org.apache.flink.kubernetes.kubeclient.decorators.FlinkMasterDeploymentDecorator: （FlinkMasterDeploymentDecorator.java：98～104） decorateInternalResource方法增加hadoopConfigMapVolume，将hadoopConfigMapVolume加入volume
7. org.apache.flink.kubernetes.kubeclient.decorators.FlinkMasterDeploymentDecorator: （FlinkMasterDeploymentDecorator.java:156） createJobManagerContainer方法getConfigMapVolumeMount第一个参数改为flinkConfig
8. 增加类org.apache.flink.kubernetes.kubeclient.decorators.HadoopConfigMapDecorator，将hadoop配置加入configMapDecorator
9. org.apache.flink.kubernetes.kubeclient.decorators.TaskManagerPodDecorator：（TaskManagerPodDecorator.java:83~88）增加hadoopConfigMapVolume
10. org.apache.flink.kubernetes.kubeclient.decorators.TaskManagerPodDecorator：（TaskManagerPodDecorator.java:117）getConfigMapVolumeMount第一个参数改为flinkConfig
11. org.apache.flink.kubernetes.utils.KubernetesUtils:（KubernetesUtils.java:238~254）增加getHadoopConfigMapVolume方法
12. org.apache.flink.kubernetes.utils.KubernetesUtils：（KubernetesUtils.java:289～333）重写getConfigMapVolumeMount方法
13. flink/flink-kubernetes/pom.xml （73～78） 增加依赖dom4j：1.6.1，用于解析xml文档

附：本次功能github committed：https://github.com/DTStack/flink/commit/190eac9660ebc94eca57d931e90bab037c00e773

#### 增加kubernetes.container.image.pull-secrets配置参数，支持拉取私有仓库镜像
1. org.apache.flink.kubernetes.configuration.KubernetesConfigOptions: (KubernetesConfigOptions.java: 66~71) 增加kubernetes.container.image.pull-secret参数
2. org.apache.flink.kubernetes.kubeclient.decorators.FlinkMasterDeploymentDecorator: (FlinkMasterDeploymentDecorator.java: 104) 创建podSpec的时候增加withImagePullSecrets
3. org.apache.flink.kubernetes.kubeclient.decorators.TaskManagerPodDecorator: (TaskManagerPodDecorator.java: 86) 创建podSpec的时候增加withImagePullSecrets
4. org.apache.flink.kubernetes.utils.KubernetesUtils: (KubernetesUtils.java: 324~340) 实现getImagePullSecrets方法，用来获取imagePullSecrets

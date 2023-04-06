/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.client.dsl.internal.core.v1;

import io.fabric8.kubernetes.client.dsl.internal.apps.v1.RollingUpdater;
import io.fabric8.kubernetes.client.dsl.internal.core.v1.ReplicationControllerOperationsImpl;
import okhttp3.OkHttpClient;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Operation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;

class ReplicationControllerRollingUpdater extends RollingUpdater<ReplicationController, ReplicationControllerList> {

  ReplicationControllerRollingUpdater(OkHttpClient client, Config config, String namespace) {
    super(client, config, namespace);
  }

  ReplicationControllerRollingUpdater(OkHttpClient client, Config config, String namespace, long rollingTimeoutMillis, long loggingIntervalMillis) {
    super(client, config, namespace, rollingTimeoutMillis, loggingIntervalMillis);
  }

  @Override
  protected ReplicationController createClone(ReplicationController obj, String newName, String newDeploymentHash) {
    return new ReplicationControllerBuilder(obj)
      .editMetadata()
      .withResourceVersion(null)
      .withName(newName)
      .endMetadata()
      .editSpec()
      .withReplicas(0).addToSelector(DEPLOYMENT_KEY, newDeploymentHash)
      .editTemplate().editMetadata().addToLabels(DEPLOYMENT_KEY, newDeploymentHash).endMetadata().endTemplate()
      .endSpec()
      .build();
  }

  @Override
  protected PodList listSelectedPods(ReplicationController obj) {
    return pods().inNamespace(namespace).withLabels(obj.getSpec().getSelector()).list();
  }


  @Override
  protected ReplicationController updateDeploymentKey(String name, String hash) {
     ReplicationController old = resources().inNamespace(namespace).withName(name).get();
     ReplicationController updated = new ReplicationControllerBuilder(old).editSpec()
       .addToSelector(DEPLOYMENT_KEY, hash)
       .editTemplate().editMetadata().addToLabels(DEPLOYMENT_KEY, hash).endMetadata().endTemplate()
       .endSpec()
       .build();
     return resources().inNamespace(namespace).withName(name).replace(updated);
  }

  @Override
  protected ReplicationController removeDeploymentKey(String name) {
     ReplicationController old = resources().inNamespace(namespace).withName(name).get();
     ReplicationController updated = new ReplicationControllerBuilder(old).editSpec()
       .removeFromSelector(DEPLOYMENT_KEY)
       .editTemplate().editMetadata().removeFromLabels(DEPLOYMENT_KEY).endMetadata().endTemplate()
       .endSpec()
       .build();
     return resources().inNamespace(namespace).withName(name).replace(updated);
  }


  @Override
  protected int getReplicas(ReplicationController obj) {
    return obj.getSpec().getReplicas();
  }

  @Override
  protected ReplicationController setReplicas(ReplicationController obj, int replicas) {
    return new ReplicationControllerBuilder(obj).editSpec().withReplicas(replicas).endSpec().build();
  }

  @Override
  protected Operation<ReplicationController, ReplicationControllerList, RollableScalableResource<ReplicationController>> resources() {
    return new ReplicationControllerOperationsImpl(client, config);
  }
}
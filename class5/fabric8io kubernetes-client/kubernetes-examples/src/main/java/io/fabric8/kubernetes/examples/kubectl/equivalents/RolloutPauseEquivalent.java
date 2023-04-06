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
package io.fabric8.kubernetes.examples.kubectl.equivalents;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Java equivalent for `kubectl rollout pause deploy/nginx-deployment`
 */
public class RolloutPauseEquivalent {
  public static void main(String[] args) {
    try (KubernetesClient k8s = new DefaultKubernetesClient()) {
      k8s.apps().deployments().inNamespace("default").withName("nginx-deployment")
        .rolling()
        .pause();
    }
  }
}

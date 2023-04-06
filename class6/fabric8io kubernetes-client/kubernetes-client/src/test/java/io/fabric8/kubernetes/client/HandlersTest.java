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
package io.fabric8.kubernetes.client;

import io.fabric8.kubernetes.client.handlers.apps.v1.DeploymentHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class HandlersTest {
  @Test
  public void testUnregister() {
    DeploymentHandler h = new DeploymentHandler();

    Handlers.unregister(h);

    // Pass class loader because we dont need load handler by ServiceLoader
    assertNull(Handlers.get(h.getKind(), h.getApiVersion(), ClassLoader.getSystemClassLoader().getParent()));
  }

  @Test
  public void testRegister() {
    DeploymentHandler h = new DeploymentHandler();

    Handlers.unregister(h);
    Handlers.register(h);

    assertSame(h, Handlers.get(h.getKind(), h.getApiVersion()));
  }
}
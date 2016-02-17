/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.UUID;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class RedisApiKeyRepositoryTest extends AbstractRedisTest {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Test
    public void shouldCreateApiKey() throws Exception {
        ApiKey apiKey = apiKey();

        apiKeyRepository.create(null, null, apiKey);
    }

    @Test
    public void shouldDeleteApiKey() throws Exception {
        ApiKey apiKey = apiKey();

        apiKeyRepository.create(null, null, apiKey);

        apiKeyRepository.delete(apiKey.getKey());
    }

    private ApiKey apiKey() {
        ApiKey apiKey = new ApiKey();
        apiKey.setKey(UUID.randomUUID().toString());
        apiKey.setApi("my-api");
        apiKey.setApplication("my-app");
        apiKey.setCreatedAt(new Date());
        return apiKey;
    }
}
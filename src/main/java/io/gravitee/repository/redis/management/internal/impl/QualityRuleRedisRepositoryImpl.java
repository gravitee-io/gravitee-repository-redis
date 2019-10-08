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
package io.gravitee.repository.redis.management.internal.impl;

import io.gravitee.repository.redis.management.internal.QualityRuleRedisRepository;
import io.gravitee.repository.redis.management.model.RedisQualityRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class QualityRuleRedisRepositoryImpl extends AbstractRedisRepository implements QualityRuleRedisRepository {

    private final static String REDIS_KEY = "qualityrule";

    @Override
    public List<RedisQualityRule> findAll() {
        final Map<Object, Object> qualityRules = redisTemplate.opsForHash().entries(REDIS_KEY);

        return qualityRules.values()
                .stream()
                .map(object -> convert(object, RedisQualityRule.class))
                .collect(Collectors.toList());
    }

    @Override
    public RedisQualityRule findById(final String qualityRuleId) {
        Object qualityRule = redisTemplate.opsForHash().get(REDIS_KEY, qualityRuleId);
        return convert(qualityRule, RedisQualityRule.class);
    }

    @Override
    public RedisQualityRule saveOrUpdate(final RedisQualityRule qualityRule) {
        redisTemplate.opsForHash().put(REDIS_KEY, qualityRule.getId(), qualityRule);
        return qualityRule;
    }

    @Override
    public void delete(final String qualityRule) {
        redisTemplate.opsForHash().delete(REDIS_KEY, qualityRule);
    }
}

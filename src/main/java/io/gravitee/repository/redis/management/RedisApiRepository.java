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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.redis.management.internal.ApiRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisApiRepository implements ApiRepository {

    @Autowired
    private ApiRedisRepository apiRedisRepository;

    @Override
    public Page<Api> search(ApiCriteria apiCriteria, Pageable pageable) {
        final Page<RedisApi> pagedApis = apiRedisRepository.search(apiCriteria, pageable, null);
        return new Page<>(
                pagedApis.getContent().stream().map(this::convert).collect(Collectors.toList()),
                pagedApis.getPageNumber(), (int) pagedApis.getPageElements(),
                pagedApis.getTotalElements());
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria) {
        return findByCriteria(apiCriteria, null);
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria, ApiFieldExclusionFilter apiFieldExclusionFilter) {
        return findByCriteria(apiCriteria, apiFieldExclusionFilter);
    }

    private List<Api> findByCriteria(ApiCriteria apiCriteria, ApiFieldExclusionFilter apiFieldExclusionFilter) {
        Page<RedisApi> pagedApis = apiRedisRepository.search(apiCriteria, null, apiFieldExclusionFilter);
        return pagedApis.getContent()
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Api> findById(String apiId) throws TechnicalException {
        RedisApi redisApi = this.apiRedisRepository.find(apiId);
        return Optional.ofNullable(convert(redisApi));
    }

    @Override
    public Api create(Api api) throws TechnicalException {
        RedisApi redisApi = apiRedisRepository.saveOrUpdate(convert(api));
        return convert(redisApi);
    }

    @Override
    public Api update(Api api) throws TechnicalException {
        if (api == null || api.getId() == null) {
            throw new IllegalStateException("Api to update must have an id");
        }

        final RedisApi redisApi = apiRedisRepository.find(api.getId());
        if (redisApi == null) {
            throw new IllegalStateException(String.format("No api found with id [%s]", api.getId()));
        }

        RedisApi redisApiUpdated = apiRedisRepository.saveOrUpdate(convert(api));
        return convert(redisApiUpdated);
    }

    @Override
    public void delete(String apiId) throws TechnicalException {
        apiRedisRepository.delete(apiId);
    }

    private Api convert(RedisApi redisApi) {
        if (redisApi == null) {
            return null;
        }

        Api api = new Api();

        api.setId(redisApi.getId());
        api.setName(redisApi.getName());
        api.setCreatedAt(new Date(redisApi.getCreatedAt()));
        api.setUpdatedAt(new Date(redisApi.getUpdatedAt()));
        if (redisApi.getDeployedAt() != 0) {
            api.setDeployedAt(new Date(redisApi.getDeployedAt()));
        }
        api.setDefinition(redisApi.getDefinition());
        api.setDescription(redisApi.getDescription());
        api.setVersion(redisApi.getVersion());
        api.setVisibility(Visibility.valueOf(redisApi.getVisibility()));
        if (redisApi.getLifecycleState() != null) {
            api.setLifecycleState(LifecycleState.valueOf(redisApi.getLifecycleState()));
        }
        api.setPicture(redisApi.getPicture());
        api.setGroups(redisApi.getGroups());
        api.setViews(redisApi.getViews());
        api.setLabels(redisApi.getLabels());
        if (redisApi.getApiLifecycleState() != null) {
            api.setApiLifecycleState(ApiLifecycleState.valueOf(redisApi.getApiLifecycleState()));
        }

        return api;
    }

    private RedisApi convert(Api api) {
        RedisApi redisApi = new RedisApi();

        redisApi.setId(api.getId());
        redisApi.setName(api.getName());
        redisApi.setCreatedAt(api.getCreatedAt().getTime());
        redisApi.setUpdatedAt(api.getUpdatedAt().getTime());

        if (api.getDeployedAt() != null) {
            redisApi.setDeployedAt(api.getDeployedAt().getTime());
        }

        redisApi.setDefinition(api.getDefinition());
        redisApi.setDescription(api.getDescription());
        redisApi.setVersion(api.getVersion());
        redisApi.setVisibility(api.getVisibility().name());
        if (api.getLifecycleState() != null) {
            redisApi.setLifecycleState(api.getLifecycleState().name());
        }
        redisApi.setPicture(api.getPicture());
        redisApi.setGroups(api.getGroups());
        redisApi.setViews(api.getViews());
        redisApi.setLabels(api.getLabels());
        if (api.getApiLifecycleState() != null) {
            redisApi.setApiLifecycleState(api.getApiLifecycleState().name());
        }

        return redisApi;
    }
}

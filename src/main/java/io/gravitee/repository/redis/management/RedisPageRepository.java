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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageSource;
import io.gravitee.repository.management.model.PageType;
import io.gravitee.repository.redis.management.internal.PageRedisRepository;
import io.gravitee.repository.redis.management.model.RedisPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisPageRepository implements PageRepository {

    @Autowired
    private PageRedisRepository pageRedisRepository;

    @Override
    public List<Page> search(PageCriteria criteria) throws TechnicalException {
        Collection<RedisPage> pages;
        if (criteria != null && criteria.getApi() != null && !criteria.getApi().isEmpty()) {
            pages = pageRedisRepository.findByApi(criteria.getApi());
        } else {
            pages = pageRedisRepository.findPortalPages();
        }

        Stream<RedisPage> stream = pages.stream();
        if (criteria != null) {
            if (criteria.getHomepage() != null) {
                stream = stream.filter(p -> criteria.getHomepage().equals(p.isHomepage()));
            }
            if (criteria.getPublished() != null) {
                stream = stream.filter(p -> criteria.getPublished().equals(p.isPublished()));
            }
            if (criteria.getName() != null) {
                stream = stream.filter(p -> criteria.getName().equals(p.getName()));
            }
            if (criteria.getParent() != null) {
                stream = stream.filter(p -> criteria.getParent().equals(p.getParentId()));
            }
            if (criteria.getRootParent() != null && criteria.getRootParent().equals(Boolean.TRUE)) {
                stream = stream.filter(p -> p.getParentId() == null || p.getParentId().isEmpty());
            }
            if (criteria.getType() != null) {
                stream = stream.filter(p -> criteria.getType().equals(p.getType()));
            }
        }


        return stream.map(this::convert).collect(Collectors.toList());
    }

    @Override
    public Integer findMaxApiPageOrderByApiId(String apiId) throws TechnicalException {
        return pageRedisRepository.findByApi(apiId).stream().mapToInt(RedisPage::getOrder).max().orElse(0);
    }

    @Override
    public Optional<Page> findById(String pageId) throws TechnicalException {
        RedisPage redisPage = pageRedisRepository.find(pageId);
        return Optional.ofNullable(convert(redisPage));
    }

    @Override
    public Page create(Page page) throws TechnicalException {
        RedisPage redisPage = pageRedisRepository.saveOrUpdate(convert(page));
        return convert(redisPage);
    }

    @Override
    public Page update(Page page) throws TechnicalException {
        if (page == null) {
            throw new IllegalStateException("Page must not be null");
        }

        RedisPage pageMongo = pageRedisRepository.find(page.getId());
        if (pageMongo == null) {
            throw new IllegalStateException(String.format("No page found with id [%s]", page.getId()));
        }

        RedisPage redisPageUpdated = pageRedisRepository.saveOrUpdate(convert(page));
        return convert(redisPageUpdated);
    }

    @Override
    public void delete(String pageId) throws TechnicalException {
        pageRedisRepository.delete(pageId);
    }

    @Override
    public Integer findMaxPortalPageOrder() throws TechnicalException {
        return pageRedisRepository.findPortalPages().stream().mapToInt(RedisPage::getOrder).max().orElse(0);
    }

    private Page convert(RedisPage redisPage) {
        if (redisPage == null) {
            return null;
        }

        Page page = new Page();
        page.setId(redisPage.getId());
        page.setApi(redisPage.getApi());
        page.setContent(redisPage.getContent());
        page.setCreatedAt(new Date(redisPage.getCreatedAt()));
        page.setUpdatedAt(new Date(redisPage.getUpdatedAt()));
        page.setLastContributor(redisPage.getLastContributor());
        page.setName(redisPage.getName());
        page.setOrder(redisPage.getOrder());
        page.setPublished(redisPage.isPublished());
        page.setType(PageType.valueOf(redisPage.getType()));
        page.setHomepage(redisPage.isHomepage());
        page.setExcludedGroups(redisPage.getExcludedGroups());
        page.setParentId(redisPage.getParentId());

        if (redisPage.getSourceType() != null) {
            PageSource pageSource = new PageSource();
            pageSource.setType(redisPage.getSourceType());
            pageSource.setConfiguration(redisPage.getSourceConfiguration());
            page.setSource(pageSource);
        }

        page.setConfiguration(redisPage.getConfiguration());
        page.setMetadata(redisPage.getMetadata());
        return page;
    }

    private RedisPage convert(Page page) {
        RedisPage redisPage = new RedisPage();
        redisPage.setId(page.getId());
        redisPage.setApi(page.getApi());
        redisPage.setContent(page.getContent());
        redisPage.setCreatedAt(page.getCreatedAt().getTime());
        redisPage.setUpdatedAt(page.getUpdatedAt().getTime());
        redisPage.setLastContributor(page.getLastContributor());
        redisPage.setName(page.getName());
        redisPage.setOrder(page.getOrder());
        redisPage.setPublished(page.isPublished());
        redisPage.setType(page.getType().name());
        redisPage.setHomepage(page.isHomepage());
        redisPage.setExcludedGroups(page.getExcludedGroups());
        redisPage.setParentId(page.getParentId());

        if (page.getSource() != null) {
            redisPage.setSourceType(page.getSource().getType());
            redisPage.setSourceConfiguration(page.getSource().getConfiguration());
        }

        redisPage.setConfiguration(page.getConfiguration());
        redisPage.setMetadata(page.getMetadata());
        return redisPage;
    }
}

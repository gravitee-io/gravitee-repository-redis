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
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.repository.redis.management.internal.MembershipRedisRepository;
import io.gravitee.repository.redis.management.model.RedisMembership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisMembershipRepository implements MembershipRepository {

    @Autowired
    private MembershipRedisRepository membershipRedisRepository;

    @Override
    public Membership create(Membership membership) throws TechnicalException {
        return convert(membershipRedisRepository.saveOrUpdate(convert(membership)));
    }

    @Override
    public Membership update(Membership membership) throws TechnicalException {
        if (membership == null || membership.getUserId() == null || membership.getReferenceId() == null || membership.getReferenceType() == null) {
            throw new IllegalStateException("Membership to update must have an user id, a reference id and type");
        }

        RedisMembership redisMembership = membershipRedisRepository.findById(membership.getUserId(), membership.getReferenceType().name(), membership.getReferenceId());

        if (redisMembership == null) {
            throw new IllegalStateException(String.format("No membership found with user id [%s], reference type [%s] and id [%s]",
                    membership.getUserId(), membership.getReferenceType().name(), membership.getReferenceId()));
        }

        return convert(membershipRedisRepository.saveOrUpdate(convert(membership)));
    }

    @Override
    public void delete(Membership membership) throws TechnicalException {
        membershipRedisRepository.delete(convert(membership));
    }

    @Override
    public void deleteMembers(MembershipReferenceType referenceType, String referenceId) throws TechnicalException {
        membershipRedisRepository.findByReferences(referenceType.name(), Collections.singletonList(referenceId))
                .forEach(membershipRedisRepository::delete);
    }

    @Override
    public Optional<Membership> findById(String userId, MembershipReferenceType referenceType, String referenceId) throws TechnicalException {
        return Optional.ofNullable(convert(membershipRedisRepository.findById(userId, referenceType.name(), referenceId)));
    }

    @Override
    public Set<Membership> findByUser(String userId) throws TechnicalException {
        return membershipRedisRepository.findByUser(userId).stream().map(this::convert).collect(Collectors.toSet());
    }

    @Override
    public Set<Membership> findByIds(String userId, MembershipReferenceType referenceType, Set<String> referenceIds) throws TechnicalException {
        return membershipRedisRepository.findByIds(userId, referenceType.name(), referenceIds).stream().map(this::convert).collect(Collectors.toSet());
    }

    @Override
    public Set<Membership> findByReferenceAndRole(MembershipReferenceType referenceType, String referenceId, RoleScope roleScope, String roleName) throws TechnicalException {
        return findByReferencesAndRole(referenceType, Collections.singletonList(referenceId), roleScope, roleName);
    }

    @Override
    public Set<Membership> findByReferencesAndRole(MembershipReferenceType referenceType, List<String> referenceIds, RoleScope roleScope, String roleName) throws TechnicalException {
        String membershipType = convertRoleToType(roleScope, roleName);
        Set<RedisMembership> memberships = membershipRedisRepository.findByReferences(referenceType.name(), referenceIds);
        if(membershipType == null){
            return memberships.stream()
                    .map(this::convert)
                    .collect(Collectors.toSet());
        } else {
            return memberships.stream()
                    .filter(membership -> membership.getRoles().contains(membershipType))
                    .map(this::convert)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public Set<Membership> findByUserAndReferenceType(String userId, MembershipReferenceType referenceType) throws TechnicalException {
        return findByUserAndReferenceTypeAndRole(userId, referenceType, null, null);
    }

    @Override
    public Set<Membership> findByUserAndReferenceTypeAndRole(String userId, MembershipReferenceType referenceType, RoleScope roleScope, String roleName) throws TechnicalException {
        String membershipType = convertRoleToType(roleScope, roleName);
        Set<RedisMembership> memberships = membershipRedisRepository.findByUserAndReferenceType(userId, referenceType.name());
        if(membershipType == null) {
            return memberships.stream()
                    .map(this::convert)
                    .collect(Collectors.toSet());
        } else {
            return memberships.stream()
                    .filter(membership -> membership.getRoles().contains(membershipType))
                    .map(this::convert)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public Set<Membership> findByRole(RoleScope roleScope, String roleName) throws TechnicalException {
        String membershipType = convertRoleToType(roleScope, roleName);
        return membershipRedisRepository.findAll().stream()
                .filter(membership -> membership.getRoles().contains(membershipType))
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    private RedisMembership convert(Membership membership) {
        if (membership == null) {
            return null;
        }
        RedisMembership redisMembership = new RedisMembership();
        redisMembership.setUserId(membership.getUserId());
        redisMembership.setReferenceId(membership.getReferenceId());
        redisMembership.setReferenceType(membership.getReferenceType().name());
        if (membership.getRoles() != null) {
            List<String> roles = new ArrayList<>(membership.getRoles().size());
            for (Map.Entry<Integer, String> roleEntry : membership.getRoles().entrySet()) {
                roles.add(convertRoleToType(roleEntry.getKey(), roleEntry.getValue()));
            }
            redisMembership.setRoles(roles);
        }
        redisMembership.setCreatedAt(membership.getCreatedAt() != null ? membership.getCreatedAt().getTime() : new Date().getTime());
        redisMembership.setUpdatedAt(membership.getUpdatedAt() != null ? membership.getUpdatedAt().getTime() : redisMembership.getCreatedAt());
        return redisMembership;
    }

    private Membership convert(RedisMembership redisMembership) {
        if (redisMembership == null) {
            return null;
        }
        Membership membership = new Membership();
        membership.setUserId(redisMembership.getUserId());
        membership.setReferenceId(redisMembership.getReferenceId());
        membership.setReferenceType(MembershipReferenceType.valueOf(redisMembership.getReferenceType()));
        if (redisMembership.getRoles() != null) {
            Map<Integer, String> roles = new HashMap<>(redisMembership.getRoles().size());
            for (String roleAsString : redisMembership.getRoles()) {
                String[] role = convertTypeToRole(roleAsString);
                roles.put(Integer.valueOf(role[0]), role[1]);
            }
            membership.setRoles(roles);
        }

        membership.setCreatedAt(new Date(redisMembership.getCreatedAt()));
        membership.setUpdatedAt(new Date(redisMembership.getUpdatedAt()));
        return membership;
    }

    private String convertRoleToType(RoleScope roleScope, String roleName) {
        if (roleName == null) {
            return null;
        }
        return convertRoleToType(roleScope.getId(), roleName);
    }

    private String convertRoleToType(int roleScope, String roleName) {
        return roleScope + ":" + roleName;
    }

    private String[] convertTypeToRole(String type) {
        if(type == null) {
            return null;
        }
        String[] role = type.split(":");
        if (role .length != 2) {
            return null;
        }
        return role;
    }
}

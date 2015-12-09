/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.misc.utils.RealmUtils;
import org.apache.syncope.core.misc.search.SearchCondConverter;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.misc.security.DelegatedAdministrationException;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.group.JPATypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAGroupDAO extends AbstractAnyDAO<Group> implements GroupDAO {

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private UserDAO userDAO;

    @Override
    protected AnyUtils init() {
        return new JPAAnyUtilsFactory().getInstance(AnyTypeKind.GROUP);
    }

    @Override
    protected void securityChecks(final Group group) {
        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(StandardEntitlement.GROUP_READ);
        boolean authorized = IterableUtils.matchesAny(authRealms, new Predicate<String>() {

            @Override
            public boolean evaluate(final String realm) {
                return group.getRealm().getFullPath().startsWith(realm)
                        || realm.equals(RealmUtils.getGroupOwnerRealm(group.getRealm().getFullPath(), group.getKey()));
            }
        });
        if (authRealms == null || authRealms.isEmpty() || !authorized) {
            throw new DelegatedAdministrationException(AnyTypeKind.GROUP, group.getKey());
        }
    }

    @Override
    public Group find(final String name) {
        TypedQuery<Group> query = entityManager().createQuery(
                "SELECT e FROM " + JPAGroup.class.getSimpleName() + " e WHERE e.name = :name", Group.class);
        query.setParameter("name", name);

        Group result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No group found with name {}", name, e);
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Group> findOwnedByUser(final Long userKey) {
        User owner = userDAO.find(userKey);
        if (owner == null) {
            return Collections.<Group>emptyList();
        }

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(JPAGroup.class.getSimpleName()).
                append(" e WHERE e.userOwner=:owner ");
        for (Long groupKey : userDAO.findAllGroupKeys(owner)) {
            queryString.append("OR e.groupOwner.id=").append(groupKey).append(' ');
        }

        TypedQuery<Group> query = entityManager().createQuery(queryString.toString(), Group.class);
        query.setParameter("owner", owner);

        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Group> findOwnedByGroup(final Long groupId) {
        Group owner = find(groupId);
        if (owner == null) {
            return Collections.<Group>emptyList();
        }

        TypedQuery<Group> query = entityManager().createQuery(
                "SELECT e FROM " + JPAGroup.class.getSimpleName() + " e WHERE e.groupOwner=:owner", Group.class);
        query.setParameter("owner", owner);

        return query.getResultList();
    }

    @Override
    public List<AMembership> findAMemberships(final Group group) {
        TypedQuery<AMembership> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAMembership.class.getSimpleName()
                + " e WHERE e.rightEnd=:group", AMembership.class);
        query.setParameter("group", group);

        return query.getResultList();
    }

    @Override
    public List<UMembership> findUMemberships(final Group group) {
        TypedQuery<UMembership> query = entityManager().createQuery(
                "SELECT e FROM " + JPAUMembership.class.getSimpleName()
                + " e WHERE e.rightEnd=:group", UMembership.class);
        query.setParameter("group", group);

        return query.getResultList();
    }

    private SearchCond buildDynMembershipCond(final String baseCondFIQL, final Realm groupRealm) {
        AssignableCond cond = new AssignableCond();
        cond.setRealmFullPath(groupRealm.getFullPath());
        cond.setFromGroup(false);

        return SearchCond.getAndCond(SearchCond.getLeafCond(cond), SearchCondConverter.convert(baseCondFIQL));
    }

    @Override
    public Group save(final Group group) {
        // refresh dynaminc memberships
        if (group.getUDynMembership() != null) {
            List<User> matching = searchDAO.search(
                    buildDynMembershipCond(group.getUDynMembership().getFIQLCond(), group.getRealm()),
                    AnyTypeKind.USER);

            group.getUDynMembership().getMembers().clear();
            for (User user : matching) {
                group.getUDynMembership().add(user);
            }
        }
        for (ADynGroupMembership memb : group.getADynMemberships()) {
            List<AnyObject> matching = searchDAO.search(
                    buildDynMembershipCond(memb.getFIQLCond(), group.getRealm()),
                    AnyTypeKind.ANY_OBJECT);

            memb.getMembers().clear();
            for (AnyObject anyObject : matching) {
                memb.add(anyObject);
            }
        }

        return super.save(group);
    }

    @Override
    public void delete(final Group group) {
        for (AMembership membership : findAMemberships(group)) {
            membership.getLeftEnd().remove(membership);
            anyObjectDAO.save(membership.getLeftEnd());

            entityManager().remove(membership);
        }
        for (UMembership membership : findUMemberships(group)) {
            membership.getLeftEnd().remove(membership);
            userDAO.save(membership.getLeftEnd());

            entityManager().remove(membership);
        }

        entityManager().remove(group);
    }

    private void populateTransitiveResources(
            final Group group, final Any<?> any, final Map<Long, PropagationByResource> result) {

        PropagationByResource propByRes = new PropagationByResource();
        for (ExternalResource resource : group.getResources()) {
            if (!any.getResources().contains(resource)) {
                propByRes.add(ResourceOperation.DELETE, resource.getKey());
            }

            if (!propByRes.isEmpty()) {
                result.put(any.getKey(), propByRes);
            }
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<Long, PropagationByResource> findAnyObjectsWithTransitiveResources(final Long groupKey) {
        Group group = authFind(groupKey);

        Map<Long, PropagationByResource> result = new HashMap<>();

        for (AMembership membership : findAMemberships(group)) {
            populateTransitiveResources(group, membership.getLeftEnd(), result);
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public Map<Long, PropagationByResource> findUsersWithTransitiveResources(final Long groupKey) {
        Group group = authFind(groupKey);

        Map<Long, PropagationByResource> result = new HashMap<>();

        for (UMembership membership : findUMemberships(group)) {
            populateTransitiveResources(group, membership.getLeftEnd(), result);
        }

        return result;
    }

    @Override
    public List<TypeExtension> findTypeExtensionByAnyTypeClass(final AnyTypeClass anyTypeClass) {
        TypedQuery<TypeExtension> query = entityManager().createQuery(
                "SELECT e FROM " + JPATypeExtension.class.getSimpleName()
                + " e WHERE :anyTypeClass MEMBER OF e.auxClasses", TypeExtension.class);
        query.setParameter("anyTypeClass", anyTypeClass);

        return query.getResultList();
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final AnyObject anyObject) {
        for (Group group : findAll()) {
            for (ADynGroupMembership memb : group.getADynMemberships()) {
                if (searchDAO.matches(
                        anyObject,
                        buildDynMembershipCond(memb.getFIQLCond(), group.getRealm()),
                        AnyTypeKind.ANY_OBJECT)) {

                    memb.add(anyObject);
                } else {
                    memb.remove(anyObject);
                }
            }
        }
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final User user) {
        for (Group group : findAll()) {
            if (group.getUDynMembership() != null) {
                if (searchDAO.matches(
                        user,
                        buildDynMembershipCond(group.getUDynMembership().getFIQLCond(), group.getRealm()),
                        AnyTypeKind.USER)) {

                    group.getUDynMembership().add(user);
                } else {
                    group.getUDynMembership().remove(user);
                }
            }
        }
    }
}

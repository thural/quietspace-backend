package dev.thural.quietspace.user;

import dev.thural.quietspace.shared.util.PageUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserQuery {

    private final EntityManager entityManager;

    public Page<User> findAllByQuery(
            String username,
            String firstname,
            String lastname,
            Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> criteriaQuery = cb.createQuery(User.class);
        List<Predicate> predicates = new ArrayList<>();
        Root<User> userRoot = criteriaQuery.from(User.class);

        if (username != null) predicates.add(
                cb.like(userRoot.get("username"), "%" + username + "%")
        );

        if (firstname != null) predicates.add(
                cb.like(userRoot.get("firstname"), "%" + firstname + "%")
        );

        if (lastname != null) predicates.add(
                cb.like(userRoot.get("lastname"), "%" + lastname + "%")
        );

        Predicate orPredicate = cb.or(predicates.toArray(new Predicate[0]));

        criteriaQuery.where(orPredicate);
        TypedQuery<User> query = entityManager.createQuery(criteriaQuery);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<User> resultList = query.getResultList();

        return PageUtils.pageFromList(resultList, pageable);
    }


}

package jpabook.jpashop.queryDSL.service;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jpabook.jpashop.queryDSL.entity.QQueryDSLMemberEntity;
import jpabook.jpashop.queryDSL.entity.QueryDSLMemberEntity;
import jpabook.jpashop.queryDSL.entity.QueryDSLTeamEntity;
import jpabook.jpashop.springbasic1.domain.Member;
import jpabook.jpashop.springbasic1.domain.QMember;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static jpabook.jpashop.queryDSL.entity.QQueryDSLMemberEntity.queryDSLMemberEntity;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QueryDSLMemberServiceTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        QueryDSLTeamEntity teamA = new QueryDSLTeamEntity("teamA");
        QueryDSLTeamEntity teamB = new QueryDSLTeamEntity("teamB");

        em.persist(teamA);
        em.persist(teamB);

        QueryDSLMemberEntity member1 = new QueryDSLMemberEntity("member1", 10, teamA);
        QueryDSLMemberEntity member2 = new QueryDSLMemberEntity("member2", 20, teamA);

        QueryDSLMemberEntity member3 = new QueryDSLMemberEntity("member3", 30, teamB);
        QueryDSLMemberEntity member4 = new QueryDSLMemberEntity("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    /**
     * @ JPQL로 오류가있는경우 실행한 후 런타임 에러가 발생한다.
     */
    @Test
    public void startJPQL() {
        // member1
        QueryDSLMemberEntity findByJPQL = em.createQuery("select m from QueryDSLMemberEntity m where m.username = :username", QueryDSLMemberEntity.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findByJPQL.getUsername()).isEqualTo("member1");
    }

    /**
     * - 장점
     * 1. QueryDSL 로 실행할경우 컴파일시점에 오류를 발견할수있다.
     * 2. parameter-binding을 자동으로 해준다.
     */
    @Test
    public void StartQuerydsl() {
        // querydsl 객체 사용시 gralde -> other -> compileQueryDSL

        // 1. 별칭 직접사용   - 자체 테이블 조인할 경우 밑처럼 선언해서 사용하면된다.
        // QQueryDSLMemberEntity m = new QQueryDSLMemberEntity("m");
        // 2. 인스턴스 이용
        // QQueryDSLMemberEntity member = QQueryDSLMemberEntity.queryDSLMemberEntity;

        QueryDSLMemberEntity finedMember = queryFactory
                .select(queryDSLMemberEntity)
                .from(queryDSLMemberEntity)
                .where(queryDSLMemberEntity.username.eq("member1")) // parameter binding을 자바식으로 처리
                .fetchOne();

        assertThat(finedMember.getUsername()).isEqualTo("member1");
    }

    /**
     * 검색조건 queryDSL
     */
    @Test
    public void searchAndParam() {
        QueryDSLMemberEntity findMember = queryFactory
                .selectFrom(queryDSLMemberEntity)
                .where(queryDSLMemberEntity.username.eq("member1")
                        , (queryDSLMemberEntity.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        /*
            // 리스트 조회
            List<QueryDSLMemberEntity> fetch = queryFactory
                    .selectFrom(queryDSLMemberEntity)
                    .fetch();

            // 단건 조회
            QueryDSLMemberEntity fetchOne = queryFactory
                    .selectFrom(queryDSLMemberEntity)
                    .fetchOne();

            // 리스트 첫번쨰 데이터 조회
            QueryDSLMemberEntity fetchFirst = queryFactory
                    .selectFrom(QQueryDSLMemberEntity.queryDSLMemberEntity)
                    .fetchFirst();
        */

      /*
        QueryResults<QueryDSLMemberEntity> results = queryFactory
                .selectFrom(queryDSLMemberEntity)
                .fetchResults();

        results.getTotal();
        List<QueryDSLMemberEntity> content = results.getResults();
      */

        // count query만 조회 fetchCount()는 @depreceated 이므로
        //          fetch().size() 사용을 권장 한다.
        long total = queryFactory
                .selectFrom(queryDSLMemberEntity)
                .fetch()
                .size();

        System.out.println(total);
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new QueryDSLMemberEntity(null, 100));
        em.persist(new QueryDSLMemberEntity("member5", 100));
        em.persist(new QueryDSLMemberEntity("member6", 100));

        List<QueryDSLMemberEntity> memberList = queryFactory
                .selectFrom(queryDSLMemberEntity)
                .where(queryDSLMemberEntity.age.eq(100))
                .orderBy(queryDSLMemberEntity.age.desc(),
                        queryDSLMemberEntity.username.asc().nullsLast()).fetch();

        QueryDSLMemberEntity member5 = memberList.get(0);
        QueryDSLMemberEntity member6 = memberList.get(1);
        QueryDSLMemberEntity memberNull = memberList.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test
    public void paging1(){
        List<QueryDSLMemberEntity> result = queryFactory
                .selectFrom(queryDSLMemberEntity)
                .orderBy(queryDSLMemberEntity.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        System.out.println(result);
    }
}
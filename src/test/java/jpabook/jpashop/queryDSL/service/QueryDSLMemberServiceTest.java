package jpabook.jpashop.queryDSL.service;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jpabook.jpashop.queryDSL.entity.QQueryDSLMemberEntity;
import jpabook.jpashop.queryDSL.entity.QQueryDSLTeamEntity;
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
import static jpabook.jpashop.queryDSL.entity.QQueryDSLTeamEntity.*;
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
     * @ JPQL??? ????????????????????? ????????? ??? ????????? ????????? ????????????.
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
     * - ??????
     * 1. QueryDSL ??? ??????????????? ?????????????????? ????????? ??????????????????.
     * 2. parameter-binding??? ???????????? ?????????.
     */
    @Test
    public void StartQuerydsl() {
        // querydsl ?????? ????????? gralde -> other -> compileQueryDSL

        // 1. ?????? ????????????   - ?????? ????????? ????????? ?????? ????????? ???????????? ??????????????????.
        // QQueryDSLMemberEntity m = new QQueryDSLMemberEntity("m");
        // 2. ???????????? ??????
        // QQueryDSLMemberEntity member = QQueryDSLMemberEntity.queryDSLMemberEntity;

        QueryDSLMemberEntity finedMember = queryFactory
                .select(queryDSLMemberEntity)
                .from(queryDSLMemberEntity)
                .where(queryDSLMemberEntity.username.eq("member1")) // parameter binding??? ??????????????? ??????
                .fetchOne();

        assertThat(finedMember.getUsername()).isEqualTo("member1");
    }

    /**
     * ???????????? queryDSL
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
            // ????????? ??????
            List<QueryDSLMemberEntity> fetch = queryFactory
                    .selectFrom(queryDSLMemberEntity)
                    .fetch();
            // ?????? ??????
            QueryDSLMemberEntity fetchOne = queryFactory
                    .selectFrom(queryDSLMemberEntity)
                    .fetchOne();
            // ????????? ????????? ????????? ??????
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

        // count query??? ?????? fetchCount()??? @depreceated ?????????
        //          fetch().size() ????????? ?????? ??????.
        long total = queryFactory
                .selectFrom(queryDSLMemberEntity)
                .fetch()
                .size();

        System.out.println(total);
    }

    /**
     * ?????? ?????? ??????
     * 1. ?????? ?????? ????????????(desc)
     * 2. ?????? ?????? ????????????(asc)
     * ??? 2?????? ?????? ????????? ????????? ???????????? ??????(nulls last)
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
    public void paging1() {
        List<QueryDSLMemberEntity> result = queryFactory
                .selectFrom(queryDSLMemberEntity)
                .orderBy(queryDSLMemberEntity.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        System.out.println(result);
    }


    /**
     * ?????? ??????
     */
    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        queryDSLMemberEntity.count(),
                        queryDSLMemberEntity.age.sum(),
                        queryDSLMemberEntity.age.avg(),
                        queryDSLMemberEntity.age.max(),
                        queryDSLMemberEntity.age.min()
                )
                .from(queryDSLMemberEntity)
                .fetch();
        Tuple tuple = result.get(0);

        assertThat(tuple.get(queryDSLMemberEntity.count())).isEqualTo(4);
        assertThat(tuple.get(queryDSLMemberEntity.age.sum())).isEqualTo(100);
        assertThat(tuple.get(queryDSLMemberEntity.age.avg())).isEqualTo(25);
        assertThat(tuple.get(queryDSLMemberEntity.age.max())).isEqualTo(40);
        assertThat(tuple.get(queryDSLMemberEntity.age.min())).isEqualTo(10);
    }

    /**
     * ?????? ????????? ????????? ?????? ????????? ?????????
     */
    @Test
    public void getTeamAverageAge() {
        List<Tuple> result = queryFactory
                .select(queryDSLTeamEntity.name,
                        queryDSLMemberEntity.age.avg())
                .from(queryDSLMemberEntity)
                .innerJoin(queryDSLMemberEntity.team, queryDSLTeamEntity)
                .groupBy(queryDSLTeamEntity.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(queryDSLTeamEntity.name)).isEqualTo("teamA");
        assertThat(teamA.get(queryDSLMemberEntity.age.avg())).isEqualTo(30 / 2);

        assertThat(teamB.get(queryDSLTeamEntity.name)).isEqualTo("teamB");
        assertThat(teamB.get(queryDSLMemberEntity.age.avg())).isEqualTo(70 / 2);
    }

    /**
     * ??? A??? ????????? ?????? ??????
     */
    @Test
    public void join() {
        List<QueryDSLMemberEntity> result = queryFactory
                .selectFrom(queryDSLMemberEntity)
                .join(queryDSLMemberEntity.team, queryDSLTeamEntity)
                .where(queryDSLTeamEntity.name.eq("teamA")).fetch();

        // ?????? ????????? ????????? containsExactly???????????? ????????? ?????????????????? ???????????? ??????
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * ?????? ?????? ( ???????????? ?????? ????????? ?????? ??????)
     * ??????(outer) ?????? ????????? -> ?????? ???????????? on??? ????????? ??????(outer) ?????? ??????
     * ???) ????????? ????????? ??? ????????? ?????? ?????? ??????
     */
    @Test
    public void theta_join() {
        em.persist(new QueryDSLMemberEntity("teamA"));
        em.persist(new QueryDSLMemberEntity("teamB"));

        List<QueryDSLMemberEntity> result = queryFactory
                .select(queryDSLMemberEntity)
                .from(queryDSLMemberEntity, queryDSLTeamEntity)
                .where(queryDSLMemberEntity.username.eq(queryDSLTeamEntity.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }

    /**
     * ?????? ?????? ?????? ????????? ???????????? ???????????? ?????? ??????
     * ???) ????????? ?????? ?????? ?????????, ??? ????????? teamA??? ?????? ??????, ????????? ?????? ??????
     * JPQL: select m, t from qMember m left join m.team t on t.name = 'teamA'
     *
     */
    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(queryDSLMemberEntity,queryDSLTeamEntity)
                .from(queryDSLMemberEntity)
                .join(queryDSLMemberEntity.team, queryDSLTeamEntity)
                .on(queryDSLTeamEntity.name.eq("teamA"))
                .fetch();
        for(Tuple member:result){
            System.out.println(member);
        }
    }

    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        QueryDSLMemberEntity member = queryFactory
                .selectFrom(queryDSLMemberEntity)
                .where(queryDSLMemberEntity.username.eq("member1"))
                .fetchOne();
    }

    /**
     * queryDSL subquery ?????????
     * com.querydsl.jpa.JPAExpressions ??????
     *
     * ????????? ?????? ?????? ?????? ??????
     */

    @Test
    public void subQuery(){

        QQueryDSLMemberEntity subQueryMember = new QQueryDSLMemberEntity("subQueryMember");
        List<QueryDSLMemberEntity> result = queryFactory
                .selectFrom(queryDSLMemberEntity)
                .where(queryDSLMemberEntity.age.eq(
                        JPAExpressions
                                .select(subQueryMember.age.max())
                                .from(subQueryMember)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }


    /**
     * ????????? case???
     */

    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(queryDSLMemberEntity.age
                        .when(10).then("??????")
                        .when(20).then("?????????")
                        .otherwise("??????"))
                .from(queryDSLMemberEntity)
                .fetch();

        for(String s : result){
            System.out.println("s = " + s);
        }
    }

    /**
     *  ????????? case???
     */

    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(queryDSLMemberEntity.age.between(0, 20)).then("0~20???")
                        .when(queryDSLMemberEntity.age.between(21, 40)).then("20~40???")
                        .otherwise("??????"))
                .from(queryDSLMemberEntity)
                .fetch();

        for(String s: result){
            System.out.println("s= " + s);
        }
    }
}

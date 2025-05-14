package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @Autowired
  EntityManager em;

  // 멀티쓰레드 safe
  // 트랜잭션에 따라서 알아서 잘 돌아간다.
  JPAQueryFactory queryFactory;

  @BeforeEach
  public void before() {
    queryFactory = new JPAQueryFactory(em);

    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");
    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);

    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);


  }

  @Test
  public void startJPQL() {
    // find member1
    Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                          .setParameter("username", "member1").getSingleResult();

    Assertions.assertEquals("member1", findMember.getUsername());

  }

  @Test
  public void startQuerydsl() {
    Member findMember = queryFactory.select(member).from(member).where(member.username.eq("member1")).fetchOne();
    Assertions.assertNotNull(findMember);
    Assertions.assertEquals("member1", findMember.getUsername());
  }

  @Test
  public void search() {
    Member findMember = queryFactory.selectFrom(member).where(member.username.eq("member1").and(member.age.eq(10)))
                                    .fetchOne();
    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  // where 안에 여러 조건을 넣을 수 있다. ==> .and()로 체이닝하지 않아도 됨.
  @Test
  public void searchAndParam() {
    Member findMember = queryFactory.selectFrom(member).where(member.username.eq("member1"), (member.age.eq(10)))
                                    .fetchOne();
    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void result() {
    // List
    List<Member> fetch = queryFactory.selectFrom(member).fetch();

    // 단 건
    Member findMember1 = queryFactory.selectFrom(member).where(member.username.eq("member2")).fetchOne();

    // 처음 한 건 조회
    Member findMember2 = queryFactory.selectFrom(member).fetchFirst();

    /*
    deprecated.....
    페이징 쿼리를 따로 날려야 한다.

    // 페이징에서 사용
    QueryResults<Member> results = queryFactory.selectFrom(member).fetchResults();

    // count 쿼리로 변경
    long count = queryFactory.selectFrom(member).fetchCount();
    */

    // 1. springdatajpa를 이용하는 방법.
    // Spring Data JPA Repository에서 Pageable 사용

    // 2. count() 사용하기.
    Long totalCount1 = queryFactory.select(member.count()).from(member).fetchOne();
    System.out.println("totalCount = " + totalCount1);
    Long totalCount2 = queryFactory.select(Wildcard.count).from(member).fetchOne();
    System.out.println("totalCount2 = " + totalCount2);

  }


  /*
  회원 정렬 순서
  1. 회원 나이 내림차순
  2. 최원 이름 올림차순
  * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
   */

  @Test
  public void sort() {
    em.persist(new Member(null, 100));
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));

    List<Member> result = queryFactory.selectFrom(member).where(member.age.eq(100))
                                      .orderBy(member.age.desc(), member.username.asc().nullsLast()).fetch();

    Member member5 = result.get(0);
    Member member6 = result.get(1);
    Member memberNull = result.get(2);
    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();

  }

  @Test
  public void paging1() {
    List<Member> result = queryFactory.selectFrom(member).orderBy(member.username.desc()).offset(1).limit(2).fetch();
    assertThat(result).hasSize(2);
  }

  @Test
  public void aggregation() {
    Tuple result = queryFactory.select(
        member.count(), member.age.sum(), member.age.avg(), member.age.max(), member.age.min()).from(member).fetchOne();

    assertThat(result.get(member.count())).isEqualTo(4L);
    assertThat(result.get(member.age.sum())).isEqualTo(100);
    assertThat(result.get(member.age.avg())).isEqualTo(25);
    assertThat(result.get(member.age.max())).isEqualTo(40);
    assertThat(result.get(member.age.min())).isEqualTo(10);

  }

  /*
   *  팀의 이르뫄 각 팀의 평균 연령을 구해라.
   * */
  @Test
  public void group() {
    List<Tuple> result = queryFactory.select(team.name, member.age.avg()).from(member).join(member.team, team)
                                     .groupBy(team.name).fetch();
    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("teamA");
    assertThat(teamA.get(member.age.avg())).isEqualTo(15);

    assertThat(teamB.get(team.name)).isEqualTo("teamB");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35);

  }

  // teamA에 소소된 모든 회원
  @Test
  public void join() {
    List<Member> result1 = queryFactory.selectFrom(member).join(member.team, team).fetchJoin()
                                       .where(team.name.eq("teamA")).fetch();
    List<Member> result2 = queryFactory.selectFrom(member).join(member.team, team).where(team.name.eq("teamA")).fetch();
    System.out.println("result1 = " + result1);
    System.out.println("result2 = " + result2);

    assertThat(result2).hasSize(2);
    assertThat(result2).extracting("username").containsExactly("member1", "member2");
  }

  // 회원의 이름이 팀 이름과 같은 회원 조회
  @Test
  public void theta_join() {
    em.persist(new Member("teamA", 100));
    em.persist(new Member("teamB", 100));

    List<Member> result = queryFactory.select(member).from(member, team).where(member.username.eq(team.name)).fetch();
    assertThat(result).extracting("username").containsExactly("teamA", "teamB");
  }


  // On
  // 1. 조인 대상 필터링
  @Test
  public void join_on_filtering() {
    // leftJoin + on
    List<Tuple> result = queryFactory.select(member, team).from(member).leftJoin(member.team, team)
                                     .on(team.name.eq("teamA")).fetch();
    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }

    // innerJoin + where
    List<Tuple> result1 = queryFactory.select(member, team).from(member).join(member.team, team)
                                      .where(team.name.eq("teamA")).fetch();
    for (Tuple fetch1 : result1) {
      System.out.println("fetch1 = " + fetch1);
    }
  }

  // On
  // 2. 연관관계 없는 엔티티 외부 조인
  // leftJoin에 있는 team이 타겟 엔티티, alias 로 사용된다.
  // 이럴 때는 연관관계(보통 id 또는 외래 키로 조인)를 사용하지 않는다.
  // 대신 on절에 명시된 임의의 조건을 사용해서 조인을 수행한다.
  @Test
  public void join_on_no_relation() throws Exception {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    List<Tuple> result = queryFactory.select(member, team).from(member).leftJoin(team).on(member.username.eq(team.name))
                                     .fetch();
    for (Tuple tuple : result) {
      System.out.println("t=" + tuple);
    }
  }
}

package study.querydsl;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @Autowired
  EntityManager em;

  // 멀티쓰레드 safe
  // 트랜잭션에 따라서 알아서 잘 돌아간다.
  JPAQueryFactory queryFactory;
  @PersistenceUnit
  EntityManagerFactory emf;

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


  /*
  회원 정렬 순서
  1. 회원 나이 내림차순
  2. 최원 이름 올림차순
  * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
   */

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

  @Test
  public void fetchJoinNo() {
    em.flush();
    em.clear();

    Member findMember = queryFactory.selectFrom(member).join(member.team, team).where(member.username.eq("member1"))
                                    .fetchOne();
    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).isFalse();

  }

  @Test
  public void fetchJoinUse() {
    em.flush();
    em.clear();

    Member findMember = queryFactory.selectFrom(member).join(member.team, team).fetchJoin()
                                    .where(member.username.eq("member1")).fetchOne();
    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).isTrue();

  }

  // 나이가 가장 많은 회원 조회
  /*
  * JPQL/SQL의 별칭(Alias): JPQL이나 SQL에서는 쿼리 내에서 테이블이나 엔티티를 참조할 때 별칭을 사용합니다 (예: SELECT m FROM Member m). 이 별칭은 쿼리 전체 또는 특정 스코프 내에서 해당 테이블/엔티티의 특정 인스턴스를 지칭하는 데 사용됩니다.
서브쿼리의 독립적인 스코프: 서브쿼리는 메인 쿼리와는 별개의 독립적인 쿼리 스코프(범위)를 가집니다. 서브쿼리 내에서 참조하는 엔티티는 서브쿼리 자체의 스코프 내에서 유효한 별칭을 가집니다.
동일 엔티티를 다른 스코프에서 참조: 이 예시처럼 메인 쿼리도 Member 엔티티를 조회하고, 서브쿼리도 Member 엔티티의 최대 나이를 찾기 위해 Member 엔티티를 다시 참조합니다. 만약 서브쿼리 내에서도 메인 쿼리와 동일한 별칭(member)을 사용한다면, JPQL/SQL 파서가 서브쿼리 내의 member가 메인 쿼리의 member를 의미하는지, 아니면 서브쿼리 내의 Member 엔티티 자체를 의미하는지 혼동할 수 있습니다.
Querydsl Q-타입은 별칭을 나타냄: Querydsl에서 QMember member = QMember.member; 또는 QMember memberSub = new QMember("memberSub"); 와 같이 생성하는 Q-타입 인스턴스는 JPQL/SQL 쿼리에서 사용될 해당 엔티티의 별칭을 표현합니다.
따라서 서브쿼리 내에서 Member 엔티티를 참조할 때는, 메인 쿼리에서 사용된 별칭(member)과 구분하기 위해 새로운 Q-타입 인스턴스(memberSub)를 생성하고 서브쿼리 내에서 사용될 별도의 별칭("memberSub")을 지정해 주어야 합니다. 이는 서브쿼리의 스코프 내에서 Member 엔티티의 해당 사용을 명확하게 식별할 수 있도록 해줍니다.
  * */
  @Test
  public void subQuery() {
    QMember memberSub = new QMember("memberSub");
    List<Member> result = queryFactory.selectFrom(member)
                                      .where(member.age.eq(select(memberSub.age.max()).from(memberSub))).fetch();
    assertThat(result).hasSize(1);
    assertThat(result).extracting("age").containsExactly(40);
  }

  // 나이가 평균 이상인 회원
  @Test
  public void subQueryGoe() {
    QMember memberSub = new QMember("memberSub");
    List<Member> result = queryFactory.selectFrom(member)
                                      .where(member.age.goe(select(memberSub.age.avg()).from(memberSub))).fetch();
    assertThat(result).hasSize(2);
    assertThat(result).extracting("age").containsExactly(30, 40);
  }

  // where 절에 in
  @Test
  public void subQueryIn() {
    QMember memberSub = new QMember("memberSub");
    List<Member> result = queryFactory.selectFrom(member).where(
        member.age.in(select(memberSub.age).from(memberSub).where(memberSub.age.gt(10)))).fetch();
    assertThat(result).hasSize(3);
    assertThat(result).extracting("age").containsExactly(20, 30, 40);
  }

  // select 절 안에 select
  @Test
  public void selectSubQuery() {
    QMember memberSub = new QMember("memberSub");
    List<Tuple> result = queryFactory.select(member.username, select(memberSub.age.avg()).from(memberSub)).from(member)
                                     .fetch();
    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  /**
   * JPQL does not support subqueries in the FROM clause.
   * This test demonstrates that limitation and is expected to fail.
   *
   * Querydsl follows JPQL specification, so it also doesn't support subqueries in FROM.
   * Alternative approaches:
   * 1. Use join operations
   * 2. Execute the subquery first, then use the results in a second query
   * 3. Use native SQL if subqueries in FROM are absolutely necessary
   */
  @Test
  public void fromSubQuery() {
    // This code will not compile because JPQL doesn't support subqueries in FROM clause
    QMember memberSub = new QMember("memberSub");
    JPQLQuery<Member> sub = JPAExpressions.selectFrom(memberSub).where(memberSub.age.gt(20));
    // The following line causes a compilation error:
    // queryFactory.select(memberSub).from(sub).fetch();

    // Instead, we could use a regular query with a where clause:
    List<Member> result = queryFactory.selectFrom(member).where(member.age.gt(20)).fetch();
    assertThat(result).extracting("age").containsExactly(30, 40);
  }

  // blaze persistence 가 javax의 entityManavaer를 요규한다.;;;;
  // @Test
  // public void fromSubQueryBlaze() {
  //   // This code will not compile because JPQL doesn't support subqueries in FROM clause
  //   QMember memberSub = new QMember("memberSub");
  //   JPQLQuery<Member> sub = JPAExpressions.selectFrom(memberSub).where(memberSub.age.gt(20));
  //   // The following line causes a compilation error:
  //   // queryFactory.select(memberSub).from(sub).fetch();
  //
  //   // Instead, we could use a regular query with a where clause:
  //   List<Member> result = queryFactory.select(member).from(sub).fetch();
  //   assertThat(result).extracting("age").containsExactly(30, 40);
  // }


  @Test
  public void basicCase() {
    List<String> result = queryFactory.select(member.age.when(10).then("age10").when(20).then("age20").otherwise("ect"))
                                      .from(member).fetch();
    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  public void complexCase() {
    List<String> result = queryFactory.select(
        new CaseBuilder().when(member.age.between(0, 20)).then("0~20").when(member.age.between(21, 40)).then("20~40")
                         .otherwise("ect")).from(member).fetch();
    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  public void constant() {
    List<Tuple> result = queryFactory.select(member.username, Expressions.constant("A")).from(member).fetch();
    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @Test
  public void concat() {
    // username_age
    List<String> result = queryFactory.select(member.username.concat("_").concat(member.age.stringValue())).from(member)
                                      .where(member.username.eq("member1")).fetch();
    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  public void simpleProjection() {
    List<String> result = queryFactory.select(member.username).from(member).fetch();
    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  // tuple의 패키지 --> package com.querydsl.core;
  // 튜플을 리포지토리 계층 안에서 사용하는건 괜찮지만, 서비스나 컨트롤러 계층으로 넘어가는건 좋은 설계가 아니다.
  // 어떤 구현기술을 사용하는지, 서비스나 컨트롤러에서 알 필요 없고 알면 안 된다.
  @Test
  public void tupleProjection() {
    List<Tuple> result = queryFactory.select(member.username, member.age).from(member).fetch();
    for (Tuple s : result) {
      System.out.println(s.get(member.username));
      System.out.println(s.get(member.age));
    }
  }

  @Test
  public void findDtoByJPQL() {
    List<MemberDto> result = em.createQuery(
        "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class).getResultList();
    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  // Projections.bean --> setter를 사용한다.
  // 따라서 setter를 허용해줘야 한다.
  @Test
  public void findDtoBySetter() {
    List<MemberDto> result = queryFactory.select(Projections.bean(MemberDto.class, member.username, member.age))
                                         .from(member).fetch();
  }

  @Test
  public void findDtoByField() {
    List<MemberDto> result = queryFactory.select(Projections.fields(MemberDto.class, member.username, member.age))
                                         .from(member).fetch();
  }

  // 생성자 타입에서는 타입 순서를 맞춰줘야 한다.
  @Test
  public void findDtoByConstructor() {
    List<MemberDto> result = queryFactory.select(Projections.constructor(MemberDto.class, member.username, member.age))
                                         .from(member).fetch();
  }

  // 별칭이 다를 때는, .as() 를 사용해줘야 한다. 아니면 null 로 뜸.
  @Test
  public void findUserDtoAlias() {
    List<UserDto> result = queryFactory.select(
        Projections.fields(UserDto.class, member.username.as("name"), member.age)).from(member).fetch();
    for (UserDto userDto : result) {
      System.out.println("userDto = " + userDto);
    }

    List<UserDto> resultNoAs = queryFactory.select(Projections.fields(UserDto.class, member.username, member.age))
                                           .from(member).fetch();
    for (UserDto userDto : resultNoAs) {
      System.out.println("userDto = " + userDto);
    }

    QMember memberSub = new QMember("memberSub");

    // Projections.fields()를 사용할 때, 서브쿼리 결과를 DTO에 매핑하려면 별칭(alias)을 지정해야 한다.
    // ExpressionUtils.as(subquery, "alias")를 사용해 DTO의 필드명과 일치하는 별칭을 부여해야 한다.
    List<UserDto> age = queryFactory.select(Projections.fields(
        UserDto.class, member.username, ExpressionUtils.as(
            select(memberSub.age.max()).from(memberSub), "age"))).from(member).fetch();
    for (UserDto userDto : resultNoAs) {
      System.out.println("userDto = " + userDto);
    }
  }

  // findDtoByConstructor 와의 차이점
  // 생성자에서 정해놓은 타입이 아니거나, 파라미터 개수가 다르면 컴파일 에러가 발생.
  @Test
  public void findDtoByQueryProjection() {
    List<MemberDto> result = queryFactory.select(new QMemberDto(member.username, member.age)).from(member).fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  public void dynamicQuery_BooleanBuilder() {
    String usernameParam = "member1";
    Integer ageParam = null;

    List<Member> result = searchMember1(usernameParam, ageParam);
  }

  private List<Member> searchMember1(String usernameCond, Integer ageCond) {
    BooleanBuilder builder = new BooleanBuilder();
    if (usernameCond != null) {
      builder.and(member.username.eq(usernameCond));
    }
    if (ageCond != null) {
      builder.and(member.age.eq(ageCond));
    }

    return queryFactory.selectFrom(member).where(builder).fetch();
  }
}

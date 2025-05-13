package study.querydsl;

import static study.querydsl.entity.QMember.member;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
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
    org.assertj.core.api.Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  // where 안에 여러 조건을 넣을 수 있다. ==> .and()로 체이닝하지 않아도 됨.
  @Test
  public void searchAndParam() {
    Member findMember = queryFactory.selectFrom(member).where(member.username.eq("member1"), (member.age.eq(10)))
                                    .fetchOne();
    org.assertj.core.api.Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
  }
}

package study.querydsl.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@Transactional
@SpringBootTest
class MemberRepositoryTest {

  @Autowired
  EntityManager em;
  @Autowired
  MemberRepository memberRepository;

  @Test
  public void basicTest() {
    Member member = new Member("member1", 10);
    memberRepository.save(member);

    Member findMember = memberRepository.findById(member.getId()).get();
    assertThat(findMember).isEqualTo(member);

    List<Member> result1 = memberRepository.findAll();
    assertThat(result1).containsExactly(member);

    List<Member> result2 = memberRepository.findByUsername("member1");
    assertThat(result2).containsExactly(member);
  }


  @Test
  public void searchTest() {
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

    MemberSearchCondition condition = new MemberSearchCondition();
    condition.setAgeGoe(35);
    condition.setAgeLoe(40);
    condition.setTeamName("teamB");

    List<MemberTeamDto> result = memberRepository.search(condition);
    for (MemberTeamDto memberTeamDto : result) {
      System.out.println("memberTeamDto = " + memberTeamDto);
    }
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getUsername()).isEqualTo("member4");
    assertThat(result).extracting("username").containsExactly("member4");
  }

  @Test
  public void searchPageSimple() {
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

    MemberSearchCondition condition = new MemberSearchCondition();
    PageRequest pageRequest = PageRequest.of(0, 3);

    Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);
    for (MemberTeamDto memberTeamDto : result) {
      System.out.println("memberTeamDto = " + memberTeamDto);
    }
    assertThat(result).hasSize(3);
    assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
  }

  // querydslPredicateExecutor 한계점
  // 조인 x (명시적 조인 (left join)이 안된다. 묵시적 조인은 가능)
  // 클라이언트가 Querydsl을 의존
  @Test
  public void querydslPredicateExecutorTest() {
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

    QMember member = QMember.member;
    Iterable<Member> result = memberRepository.findAll(member.age.between(20, 40));
    for (Member m : result) {
      System.out.println("m = " + m);
    }
  }
}
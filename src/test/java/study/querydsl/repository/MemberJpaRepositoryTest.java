package study.querydsl.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

  @Autowired
  EntityManager em;
  @Autowired
  MemberJpaRepository memberJpaRepository;

  @Test
  public void basicTest() {
    Member member = new Member("member1", 10);
    memberJpaRepository.save(member);

    Member findMember = memberJpaRepository.findById(member.getId()).get();
    assertThat(findMember).isEqualTo(member);

    List<Member> result1 = memberJpaRepository.findAll();
    assertThat(result1).containsExactly(member);

    List<Member> result2 = memberJpaRepository.findByUsername("member1");
    assertThat(result2).containsExactly(member);
  }

  @Test
  public void basicQuerydslTest() {
    Member member = new Member("member1", 10);
    memberJpaRepository.save(member);

    Member findMember = memberJpaRepository.findById(member.getId()).get();
    assertThat(findMember).isEqualTo(member);

    List<Member> result1 = memberJpaRepository.findAll_Querydsl();
    assertThat(result1).containsExactly(member);

    List<Member> result2 = memberJpaRepository.findByUsername_Querydsl("member1");
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

    List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
    for (MemberTeamDto memberTeamDto : result) {
      System.out.println("memberTeamDto = " + memberTeamDto);
    }
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getUsername()).isEqualTo("member4");
    assertThat(result).extracting("username").containsExactly("member4");
  }


  /*제공해주신 코드에서 MemberSearchCondition의 조건들이 모두 주석 처리되어 있기 때문에, searchByBuilder 메서드는 아무런 필터링 조건 없이 쿼리를 실행하게 됩니다. 이러한 경우 동적 쿼리에서 조건이 없을 때 발생할 수 있는 주요 문제점들은 다음과 같습니다.

성능 저하 및 풀 테이블 스캔 (Performance Degradation / Full Table Scan):

WHERE 절에 아무런 조건이 붙지 않으므로, 데이터베이스는 Member 테이블과 Team 테이블의 **모든 레코드를 스캔(Full Table Scan)**하게 됩니다.
테이블에 데이터가 적을 때는 문제가 없지만, 실제 운영 환경에서 수십만, 수백만 건 이상의 데이터가 쌓이게 되면 쿼리 실행 시간이 기하급수적으로 늘어나고, 데이터베이스 서버에 엄청난 부하를 주게 됩니다.
애플리케이션 서버 메모리 부족 (Application Server Memory Exhaustion):

데이터베이스에서 조회된 모든 결과를 애플리케이션 서버의 메모리로 로드하게 됩니다.
조회되는 데이터의 양이 많아지면 애플리케이션 서버의 힙 메모리가 부족해져 OutOfMemoryError가 발생하고, 이는 서버 다운으로 이어질 수 있습니다. 특히 MemberTeamDto와 같은 DTO가 아닌 실제 엔티티를 반환하는 경우, JPA 영속성 컨텍스트가 모든 엔티티를 관리하려 하면서 더 큰 메모리 문제가 발생할 수 있습니다.
네트워크 대역폭 소모 (Network Bandwidth Consumption):

데이터베이스 서버에서 애플리케이션 서버로 대량의 데이터를 전송해야 하므로, 네트워크 대역폭을 과도하게 소모하게 됩니다. 이는 다른 서비스의 통신에도 영향을 미칠 수 있습니다.
데이터베이스 커넥션 풀 고갈 (Database Connection Pool Exhaustion):

쿼리 실행 시간이 길어지면 데이터베이스 커넥션을 오랫동안 점유하게 됩니다.
만약 동시 요청이 많아진다면, 사용 가능한 커넥션이 부족해져 다른 요청들이 데이터베이스에 접근하지 못하고 대기하거나 실패하는 현상이 발생할 수 있습니다.
의도하지 않은 데이터 노출 및 보안 문제 (Unintended Data Exposure / Security Risks):

만약 이 쿼리가 웹 API 등을 통해 사용자에게 노출된다면, 아무런 필터링 없이 전체 데이터를 조회할 수 있게 되어 민감한 정보가 대량으로 유출될 수 있는 보안 취약점이 됩니다.
불필요한 데이터 처리 (Unnecessary Data Processing):

클라이언트(또는 사용자)가 실제로 필요로 하는 데이터는 소량인데, 불필요하게 모든 데이터를 가져와 애플리케이션 레벨에서 필터링하거나 가공해야 하는 비효율이 발생합니다.
이러한 문제들을 방지하기 위해 동적 쿼리 설계 시에는 기본(default) 조건을 설정하거나, **최대 조회 건수 제한(limit)**을 두거나, **페이징(Paging)**을 강제하는 등의 안전 장치를 마련하는 것이 중요합니다.
  * */
  @Test
  public void searchTestNoCond() {
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
    // condition.setAgeGoe(35);
    // condition.setAgeLoe(40);
    // condition.setTeamName("teamB");

    List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
    for (MemberTeamDto memberTeamDto : result) {
      System.out.println("memberTeamDto = " + memberTeamDto);
    }
    assertThat(result).hasSize(4);

    // 순서도 맞아야 됨.
    assertThat(result).extracting("username").containsExactly("member1", "member2", "member3", "member4");
  }
}
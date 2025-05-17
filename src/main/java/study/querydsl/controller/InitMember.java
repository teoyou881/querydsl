package study.querydsl.controller;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Team;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

  private final InitMemberService initMemberService;

  @PostConstruct
  public void init() {
    initMemberService.init();
  }


  @Component
  static class InitMemberService {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void init() {
      Team teamA = new Team("teamA");
      Team teamB = new Team("teamB");
      em.persist(teamA);
      em.persist(teamB);
      for (int i = 1; i <= 100; i++) {
        String username = "member" + i;
        int age = i * 10;
        Team selectedTeam = i % 2 == 0 ? teamA : teamB;
        em.persist(new study.querydsl.entity.Member(username, age, selectedTeam));
        em.persist(new study.querydsl.entity.Member(username, age, selectedTeam));
      }
    }
  }
}

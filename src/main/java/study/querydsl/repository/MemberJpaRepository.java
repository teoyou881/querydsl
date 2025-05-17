package study.querydsl.repository;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

@Repository
@RequiredArgsConstructor
@Transactional
public class MemberJpaRepository {

  private final EntityManager em;
  private final JPAQueryFactory queryFactory;

  public void save(Member member) {
    em.persist(member);
  }

  public Optional<Member> findById(Long id) {
    return Optional.ofNullable(em.find(Member.class, id));
  }

  public List<Member> findAll() {
    return em.createQuery("select m from Member m", Member.class).getResultList();
  }

  public List<Member> findByUsername(String username) {
    return em.createQuery("select m from Member m where m.username =:username", Member.class)
             .setParameter(
                 "username",
                 username)
             .getResultList();
  }

  public List<Member> findAll_Querydsl() {
    return queryFactory.selectFrom(member).fetch();
  }

  public List<Member> findByUsername_Querydsl(String username) {
    return queryFactory.selectFrom(member).where(member.username.eq(username)).fetch();
  }

  public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

    BooleanBuilder builder = new BooleanBuilder();
    if (hasText(condition.getUsername())) {
      builder.and(member.username.eq(condition.getUsername()));
    }
    if (hasText(condition.getTeamName())) {
      builder.and(team.name.eq(condition.getTeamName()));
    }
    if (condition.getAgeGoe() != null) {
      builder.and(member.age.goe(condition.getAgeGoe()));
    }
    if (condition.getAgeLoe() != null) {
      builder.and(member.age.loe(condition.getAgeLoe()));
    }

    return queryFactory.select(new QMemberTeamDto(
                           member.id.as("memberId"),
                           member.username,
                           member.age,
                           team.id.as("teamId"),
                           team.name.as("teamName")))
                       .from(member)
                       .leftJoin(member.team, team)
                       .where(builder)
                       .fetch();
  }

  public List<MemberTeamDto> searchWhere(MemberSearchCondition condition) {

    return queryFactory.select(new QMemberTeamDto(
                           member.id.as("memberId"),
                           member.username,
                           member.age,
                           team.id.as("teamId"),
                           team.name.as("teamName"))).from(member).leftJoin(member.team, team)
                       // .where(
                       //     usernameEq(condition.getUsername()),
                       //     teamNameEq(condition.getTeamName()),
                       //     ageGoe(condition.getAgeGoe()),
                       //     ageLoe(condition.getAgeLoe()))
                       .where(checkMemberDtoAll(condition)).fetch();
  }

  // StringUtils.hasText() -> 문자열이 null 이 아니고, 빈 문자열도 아니고, 공백으로만 이뤄지지 않는 경우 true
  // 따라서 바로 null 체크 하지 말고, hasText 사용.
  //

  private BooleanExpression usernameEq(String username) {
    return StringUtils.hasText(username) ? member.username.eq(username) : null;
  }

  private BooleanExpression teamNameEq(String teamName) {
    return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
  }

  private BooleanExpression ageGoe(Integer age) {
    return age == null ? null : member.age.goe(age);
  }

  private BooleanExpression ageLoe(Integer age) {
    return age == null ? null : member.age.loe(age);
  }

  private BooleanExpression checkMemberDtoAll(MemberSearchCondition condition) {
    BooleanExpression username = usernameEq(condition.getUsername());
    BooleanExpression teamName = teamNameEq(condition.getTeamName());
    BooleanExpression ageGoe = ageGoe(condition.getAgeGoe());
    BooleanExpression ageLoe = ageLoe(condition.getAgeLoe());

    return Expressions.allOf(username, teamName, ageGoe, ageLoe);
  }


}

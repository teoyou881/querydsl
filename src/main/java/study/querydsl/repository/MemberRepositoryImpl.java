package study.querydsl.repository;

import static com.querydsl.core.types.ExpressionUtils.count;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;


@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  // QuerydslRepositorySupport 사용 코드
  // public class MemberRepositoryImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom {
  // public MemberRepositoryImpl() {
  //   super(member.getClass());
  // }


  @Override
  public List<MemberTeamDto> search(MemberSearchCondition condition) {

    // QuerydslRepositorySupport 사용 코드
    // List<MemberTeamDto> result = from(member).leftJoin(member.team, team)
    //                                          .where(checkMemberDtoAll(condition))
    //                                          .select(new QMemberTeamDto(
    //                                              member.id.as("memberId"),
    //                                              member.username,
    //                                              member.age,
    //                                              team.id.as("teamId"),
    //                                              team.name.as("teamName")))
    //                                          .fetch();

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


  private BooleanExpression checkMemberDtoAll(MemberSearchCondition condition) {
    BooleanExpression username = usernameEq(condition.getUsername());
    BooleanExpression teamName = teamNameEq(condition.getTeamName());
    BooleanExpression ageGoe = ageGoe(condition.getAgeGoe());
    BooleanExpression ageLoe = ageLoe(condition.getAgeLoe());

    return Expressions.allOf(username, teamName, ageGoe, ageLoe);
  }

  @Override
  public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
    List<MemberTeamDto> result = queryFactory.select(new QMemberTeamDto(
                                                 member.id.as("memberId"),
                                                 member.username,
                                                 member.age,
                                                 team.id.as("teamId"),
                                                 team.name.as("teamName")))
                                             .from(member)
                                             .leftJoin(member.team, team)
                                             // .where(
                                             //     usernameEq(condition.getUsername()),
                                             //     teamNameEq(condition.getTeamName()),
                                             //     ageGoe(condition.getAgeGoe()),
                                             //     ageLoe(condition.getAgeLoe()))
                                             .where(checkMemberDtoAll(condition))
                                             .offset(pageable.getOffset())
                                             .limit(pageable.getPageSize())
                                             .fetch();
    Long count = queryFactory.select(count(member)).from(member).where(checkMemberDtoAll(condition)).fetchOne();

    return new PageImpl<>(result, pageable, count);
  }

  @Override
  public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
    List<MemberTeamDto> result = queryFactory.select(new QMemberTeamDto(
                                                 member.id.as("memberId"),
                                                 member.username,
                                                 member.age,
                                                 team.id.as("teamId"),
                                                 team.name.as("teamName")))
                                             .from(member)
                                             .leftJoin(member.team, team)
                                             // .where(
                                             //     usernameEq(condition.getUsername()),
                                             //     teamNameEq(condition.getTeamName()),
                                             //     ageGoe(condition.getAgeGoe()),
                                             //     ageLoe(condition.getAgeLoe()))
                                             .where(checkMemberDtoAll(condition))
                                             .offset(pageable.getOffset())
                                             .limit(pageable.getPageSize())
                                             .fetch();
    // Long count = queryFactory.select(count(member)).from(member).where(checkMemberDtoAll(condition)).fetchOne();
    JPAQuery<Long> countQuery = queryFactory.select(count(member)).from(member).where(checkMemberDtoAll(condition));

    // 실행 방식
    // result.size() < pageable.getPageSize() → 즉 현재 페이지의 결과 수가 페이지 크기보다 작으면, 마지막 페이지이므로 카운트 쿼리를 실행하지 않고 페이지를 바로 만듭니다.
    // 그렇지 않으면 → 즉, 아직 뒤에 더 데이터가 있을 수 있다면 → countQuery::fetchOne 이 실행돼서 전체 개수를 계산합니다.
    return PageableExecutionUtils.getPage(result, pageable, countQuery::fetchOne);

    // return new PageImpl<>(result, pageable, count);
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
}

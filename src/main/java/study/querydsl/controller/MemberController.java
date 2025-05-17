package study.querydsl.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;

@RestController
@AllArgsConstructor
public class MemberController {

  private final MemberJpaRepository memberJpaRepository;

  @GetMapping("/v1/members")
  public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
    return memberJpaRepository.searchWhere(condition);

  }

}

package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

  private String username;
  private int age;

  // querydsl
  // dto도 Q파일을 만들어준다.

  // package study.querydsl.dto;
  //
  // import com.querydsl.core.types.dsl.*;
  //
  // import com.querydsl.core.types.ConstructorExpression;
  // import javax.annotation.processing.Generated;
  //
  // /**
  //  * study.querydsl.dto.QMemberDto is a Querydsl Projection type for MemberDto
  //  */
  // @Generated("com.querydsl.codegen.DefaultProjectionSerializer")
  // public class QMemberDto extends ConstructorExpression<MemberDto> {
  //
  //     private static final long serialVersionUID = 1356709634L;
  //
  //     public QMemberDto(com.querydsl.core.types.Expression<String> username, com.querydsl.core.types.Expression<Integer> age) {
  //         super(MemberDto.class, new Class<?>[]{String.class, int.class}, username, age);
  //     }
  //
  // }
  @QueryProjection
  public MemberDto(String username, int age) {
    this.username = username;
    this.age = age;
  }
}

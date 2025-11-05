package com.AISA.AISA.member.adapter.in;

import com.AISA.AISA.global.response.SuccessResponse;
import com.AISA.AISA.member.adapter.in.dto.MemberSignupRequest;
import com.AISA.AISA.member.adapter.out.dto.MemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
@Tag(name = "멤버 API", description = "멤버 관련 API")
public class MemberController {

    private final MemberService memberService;


    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "입력한 유저 정보를 바탕으로 회원가입을 진행합니다.")
    public ResponseEntity<SuccessResponse<Member>> signup(
            @RequestBody MemberSignupRequest request) throws Exception {

        Member signUpMember = memberService.signup(request);
        return ResponseEntity.ok(new SuccessResponse<>(true, "회원가입 성공", signUpMember));
    }

    @GetMapping("/members")
    @Operation(summary = "전체 회원 조회", description = "모든 회원 정보를 조회합니다.")
    public ResponseEntity<SuccessResponse<List<MemberResponse>>> getAllMembers() {
        List<MemberResponse> members = memberService.findAllMember();
        return ResponseEntity.ok(new SuccessResponse<>(true, "전체 회원 조회 성공", members));
    }

    @GetMapping("/members/{memberId}")
    @Operation(summary = "특정 회원 조회", description = "특정 회원 정보를 조회합니다.")
    public ResponseEntity<SuccessResponse<MemberResponse>> getMemberById(
        @PathVariable UUID memberId
    ){
        MemberResponse member = memberService.findMemberById(memberId);
        return ResponseEntity.ok(new SuccessResponse<>(true, "회원 조회 성공", member));
    }
}

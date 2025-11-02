package com.AISA.AISA.member.adapter.in;

import com.AISA.AISA.global.response.SuccessResponse;
import com.AISA.AISA.member.adapter.in.dto.MemberSignupRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
@Tag(name = "멤버 API", description = "멤버 관련 API")
public class MemberController {

    private final MemberService memberService;


    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "입력한 유저 정보를 바탕으로 회원가입을 진행")
    public ResponseEntity<SuccessResponse<Member>> signup(
            @RequestBody MemberSignupRequest request) throws Exception {

        Member signUpMember = memberService.signup(request);
        return ResponseEntity.ok(new SuccessResponse<>(true, "회원가입 성공", signUpMember));
    }
}

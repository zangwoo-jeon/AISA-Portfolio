package com.AISA.AISA.member.adapter.in;

import com.AISA.AISA.global.exception.BusinessException;
import com.AISA.AISA.member.adapter.in.dto.MemberSignupRequest;
import com.AISA.AISA.member.adapter.in.exception.MemberErrorCode;
import com.AISA.AISA.member.adapter.out.dto.MemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member signup(MemberSignupRequest request) {
        // ID와 비밀번호는 8자 이상이여야 한다.
        if (request.getUserName().length() < 8 || request.getPassword().length() < 8) {
            throw new BusinessException(MemberErrorCode.INVALID_CREDENTIALS_LENGTH);
        }

        // 비밀번호는 문자와 숫자를 모두 포함해야 한다.
        String password = request.getPassword();
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            throw new BusinessException(MemberErrorCode.INVALID_PASSWORD_POLICY);
        }

        Member newMember = Member.builder()
                .userName(request.getUserName())
                .displayName(request.getDisplayName())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        return memberRepository.save(newMember);
    }

    public MemberResponse findMemberById(UUID memberId) {

        return memberRepository.findById(memberId)
                .map(MemberResponse::new)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    public List<MemberResponse> findAllMember() {
        return memberRepository.findAll().stream()
                .map(MemberResponse::new)
                .collect(Collectors.toList());
    }
}

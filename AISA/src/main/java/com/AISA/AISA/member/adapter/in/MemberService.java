package com.AISA.AISA.member.adapter.in;

import com.AISA.AISA.global.exception.BusinessException;
import com.AISA.AISA.member.adapter.in.dto.MemberSignupRequest;
import com.AISA.AISA.member.adapter.in.exception.MemberErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member signup(MemberSignupRequest request) {
        if (request.getUserName().length() < 8 || request.getPassword().length() < 8) {
            throw new BusinessException(MemberErrorCode.INVALID_CREDENTIALS_LENGTH);
        }

        Member newMember = Member.builder()
                .userName(request.getUserName())
                .displayName(request.getDisplayName())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        return memberRepository.save(newMember);
    }
}

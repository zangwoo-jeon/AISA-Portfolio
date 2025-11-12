package com.AISA.AISA.kisStock.exception;

import com.AISA.AISA.global.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum KisApiErrorCode implements ErrorCode {

    TOKEN_ISSUANCE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, false, "K500-1", "KIS API 접근 토큰 발급에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final boolean isSuccess;
    private final String code;
    private final String message;
}
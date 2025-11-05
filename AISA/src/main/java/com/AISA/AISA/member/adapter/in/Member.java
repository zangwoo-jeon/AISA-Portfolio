package com.AISA.AISA.member.adapter.in;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_user_name", columnNames = "user_name"),
                @UniqueConstraint(name = "uk_member_display_name", columnNames = "display_name")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID memberId;

    @Column(name = "display_name", nullable = false)
    private String displayName;
    @Column(name = "user_name", nullable = false)
    private String userName;
    @Column(nullable = false)
    private String password;

    @Builder
    public Member(String userName, String displayName, String password) {
        this.userName = userName;
        this.displayName = displayName;
        this.password = password;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }
}

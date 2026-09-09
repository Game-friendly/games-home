package com.club.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String openid;

    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CLIENT;

    /** 打手等级，A/B/C；客户默认 C（无意义）。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade = Grade.C;

    /** 信用分。退单会扣，但当前不做任何限制（P2 再启用门槛）。 */
    @Column(nullable = false)
    private int creditScore = 100;

    /** 陪玩胜率（0-100），演示用。 */
    private Integer winRate;

    /** 起步价，单位分，演示用。 */
    private Integer minPrice;

    /** 头像表情或图片地址，演示用。 */
    private String avatar;
}

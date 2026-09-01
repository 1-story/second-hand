package com.hdu.secondhand.vo;

import lombok.Data;

/**
 * 卖家信息（对齐《接口约定规范 v1.1》7.1 / 7.2 seller 对象）
 */
@Data
public class SellerVO {

    private Long id;

    /** 真实姓名（t_user.name） */
    private String name;

    private String avatar;

    private Integer creditScore;

    /** 是否已完成学号实名认证 */
    private Boolean realNameVerified;
}

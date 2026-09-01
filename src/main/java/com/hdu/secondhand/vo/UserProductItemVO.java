package com.hdu.secondhand.vo;

import lombok.Data;

import java.util.List;

/**
 * 我的收藏/浏览记录分页项
 */
@Data
public class UserProductItemVO {

    private Long productId;
    private String title;
    /** 价格（分） */
    private Long price;
    private String coverImage;
    private String location;
    private Integer status;
    private java.time.LocalDateTime actionTime;
}

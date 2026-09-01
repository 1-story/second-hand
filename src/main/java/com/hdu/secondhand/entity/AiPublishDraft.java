package com.hdu.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 自动填表草稿表（一键发布链路）
 */
@Data
@TableName("ai_publish_draft")
public class AiPublishDraft {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 草稿数据（识别出的标题/描述/分类/成色/图片/估价等，JSON 字符串） */
    private String draftJson;

    /** 状态 0待确认 1已发布 2已过期 3已放弃 */
    private Integer status;

    /** 发布后商品 ID */
    private Long productId;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

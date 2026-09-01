package com.hdu.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表（基础字段；登录/权限由登录模块扩展）
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学号（实名认证） */
    private String studentNo;

    /** 登录名 */
    private String username;

    /** 真实姓名（注册/实名认证字段，规范 5.1） */
    private String name;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 信用分（100 起，冗余汇总） */
    private Integer creditScore;

    /** 学号实名认证 0未认证 1已认证 */
    private Integer realNameVerified;

    /** 是否在校学生 0否 1是 */
    private Integer isStudent;

    /** 状态 0禁用 1正常 */
    private Integer status;

    /** 逻辑删除 0否 1是 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

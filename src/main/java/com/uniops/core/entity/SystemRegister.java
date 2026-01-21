package com.uniops.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * SystemRegister 类的简要描述
 *
 * @author liyang
 * @since 2026/1/21
 */
@Data
@TableName("uniops_system_register")
public class SystemRegister {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 系统id，spring.application.name
     */
    @TableField("system_id")
    private String systemId;
    /**
     * 系统名称,中文名称
     */
    @TableField("system_name")
    private String systemName;
    /**
     * 系统描述
     */
    @TableField("system_desc")
    private String systemDesc;
    /**
     * 系统ip
     *
     */
    @TableField("ip")
    private String ip;
    /**
     * 系统端口
     */
    @TableField("port")
    private String port;
    /**
     * 系统servlet路径
     */
    @TableField("servlet_path")
    private String servletPath;
    /**
     * 密钥
     */
    @TableField("secret_key")
    private String secretKey;
    /**
     * 授权信息
     */
    @TableField("authorization_mes")
    private String authorizationMes;
    /**
     * 有效期
     */
    @TableField("validity_period")
    private String validityPeriod;
    /**
     * 最后一次在线时间
     */
    @TableField("last_online_time")
    private Date lastOnlineTime;
    /**
     * 状态,1正常在线，2正常离线，3异常,4已超过授权期
     */
    @TableField("status")
    private String status;


    @TableField(exist = false)
    private String managerPath;
}

package com.uniops.core.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniops.core.condition.SystemCondition;
import com.uniops.core.condition.SystemRequestCondition;
import com.uniops.core.entity.SystemRegister;
import com.uniops.core.mapper.SystemRegisterMapper;
import com.uniops.starter.autoconfigure.UniOpsProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SystemManagerServiceImpl 类的简要描述
 *
 * @author liyang
 * @since 2026/1/21
 */
@Service
@Slf4j
public class SystemManagerServiceImpl extends ServiceImpl<SystemRegisterMapper, SystemRegister>
        implements ISystemManagerService {
    @Resource
    SystemCondition systemCondition;
    @Resource
    UniOpsProperties uniOpsProperties;

    @Override
    public List<SystemRegister> searchList(SystemRequestCondition condition) {
        List<SystemRegister> list = this.list(new QueryWrapper<SystemRegister>()
                .like(StringUtils.isNotEmpty(condition.getSystemName()), "system_name", condition.getSystemName())
                .like(StringUtils.isNotEmpty(condition.getSystemId()), "system_id", condition.getSystemId()));
        list.forEach(systemRegister -> systemRegister.setManagerPath(packagePath(systemRegister)));
        return list;
    }

    @Override
    public SystemRegister getSystem(Long id) {
        return this.getById(id);
    }

    private String packagePath(SystemRegister systemRegister) {
        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(systemRegister.getOtherWebPath())) {
            return systemRegister.getOtherWebPath()+"/#/system";
        }
        if ("192.168.224.77".equals(systemRegister.getIp())) {
            //本机，就用前端开发环境
            sb.append("http://")
                    .append(systemRegister.getIp())
                    .append(":")
                    .append(3000)
                    .append("/#/system");
        } else {
            sb.append("http://")
                    .append(systemRegister.getIp())
                    .append(":")
                    .append(systemRegister.getPort())
                    .append(systemRegister.getServletPath())
                    .append("/#/system");
        }

        return sb.toString();
    }
}

package com.uniops.core.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniops.core.condition.SystemRequestCondition;
import com.uniops.core.entity.SystemRegister;
import com.uniops.core.mapper.SystemRegisterMapper;
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
    @Override
    public List<SystemRegister> searchList(SystemRequestCondition condition) {
        List<SystemRegister> list = this.list(new QueryWrapper<SystemRegister>()
                .like(StringUtils.isNotEmpty(condition.getSystemName()), "system_name", condition.getSystemName())
                .like(StringUtils.isNotEmpty(condition.getSystemId()), "system_id", condition.getSystemId()));
        list.forEach(systemRegister -> systemRegister.setManagerPath(packagePath(systemRegister)));
        return list;
    }

    private String packagePath(SystemRegister systemRegister) {
        StringBuilder sb = new StringBuilder();
//        sb.append("http://")
//                .append(systemRegister.getIp())
//                .append(":")
//                .append(systemRegister.getPort())
//                .append("/")
//                .append(systemRegister.getServletPath());
        sb.append("http://")
                .append(systemRegister.getIp())
                .append(":")
                .append(3000)
                .append("/system");
        return sb.toString();
    }
}

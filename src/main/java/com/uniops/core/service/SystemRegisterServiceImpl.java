package com.uniops.core.service;

import com.alibaba.fastjson2.util.DateUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniops.core.condition.SystemCondition;
import com.uniops.core.condition.SystemRequestCondition;
import com.uniops.core.entity.SystemRegister;
import com.uniops.core.mapper.SystemRegisterMapper;
import com.uniops.core.util.LicenseCache;
import com.uniops.core.util.LicenseManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class SystemRegisterServiceImpl extends ServiceImpl<SystemRegisterMapper, SystemRegister>
        implements ISystemRegisterService {
    @Resource
    SystemCondition systemCondition;

    @PostConstruct
    public void init() {
        register();
    }

    @PreDestroy
    public void destroy() {
        offline();
    }

    @Override
    public void register() {
        boolean exist = this.exists(new QueryWrapper<SystemRegister>()
                .eq("system_id", systemCondition.getApplicationName())
                .eq("ip", systemCondition.getIp())
                .eq("port", systemCondition.getPort()));
        if (exist) {
            log.info("系统已注册");
            //走更新
            online();
        } else {
            SystemRegister systemRegister = new SystemRegister();
            systemRegister.setSystemId(systemCondition.getApplicationName());
            systemRegister.setSystemName(systemCondition.getApplicationChineseName());
            systemRegister.setIp(systemCondition.getIp());
            systemRegister.setPort(systemCondition.getPort());
            systemRegister.setServletPath(systemCondition.getServletPath());
            systemRegister.setStatus("4");
            systemRegister.setSecretKey(LicenseManager.buildEncryptionString(systemCondition.getApplicationName()));
            this.save(systemRegister);
        }
    }

    @Override
    public SystemRegister localSystem() {
        if (LicenseCache.containsKey(getKey())) {
            return (SystemRegister) LicenseCache.get(getKey());
        } else {
            return this.getOne(new QueryWrapper<SystemRegister>()
                    .eq("system_id", systemCondition.getApplicationName())
                    .eq("ip", systemCondition.getIp())
                    .eq("port", systemCondition.getPort()));
        }
    }

    @Override
    public boolean checkLocalValidity() {
        //先到本地缓存获取判断是否有，如果有就用缓存，如果没有就去数据库查
        String key = getKey();
        SystemRegister systemRegister;
        if (LicenseCache.containsKey(key)) {
            //本地有缓存，那就不走数据库
            systemRegister = (SystemRegister) LicenseCache.get(key);
        } else {
            systemRegister = this.getOne(new QueryWrapper<SystemRegister>()
                    .eq("system_id", systemCondition.getApplicationName())
                    .eq("ip", systemCondition.getIp())
                    .eq("port", systemCondition.getPort()));
            if (systemRegister == null) {
                log.info("系统未注册");
                register();
                systemRegister = this.getOne(new QueryWrapper<SystemRegister>()
                        .eq("system_id", systemCondition.getApplicationName())
                        .eq("ip", systemCondition.getIp())
                        .eq("port", systemCondition.getPort()));
            }
            if (StringUtils.isEmpty(systemRegister.getAuthorizationMes())) {
                log.info("系统未授权,请联系公司进行授权");
                return false;
            }
            String currentSecretKey = LicenseManager.buildEncryptionString(systemCondition.getApplicationName());
            if (systemRegister.getSecretKey().equals(currentSecretKey)) {
                throw new RuntimeException("数据库加密串遭了到篡改，无法验证授权");
            }
            LicenseCache.put(key, systemRegister);
        }
        if ("4".equals(systemRegister.getStatus())) {
            log.info("系统已超过授权期");
            return false;
        }
        LicenseManager.AuthorizationResult checkAuthorization = LicenseManager.checkAuthorization(systemRegister.getAuthorizationMes(), DateUtils.format(new Date(), "yyyy-MM-dd"));
        if (checkAuthorization.isValid()) {
            log.info("系统授权有效");
            return true;
        }
        log.info("系统未授权,请联系公司进行授权");

        return false;
    }

    @Override
    public boolean checkSystemValidity(SystemRequestCondition condition) {
        SystemRegister systemRegister = getById(condition.getId());
        if (systemRegister == null) {
            return false;
        }
        if (StringUtils.isEmpty(systemRegister.getAuthorizationMes())) {
            return false;
        }
        LicenseManager.AuthorizationResult checkAuthorization = LicenseManager.checkAuthorization(systemRegister.getAuthorizationMes(), DateUtils.format(new Date(), "yyyy-MM-dd"));
        if (checkAuthorization.isValid()) {
            log.info("系统授权有效");
            return true;
        }
        log.info("系统未授权,请联系公司进行授权");
        return false;
    }

    @Override
    public void online() {
        SystemRegister systemRegister = new SystemRegister();
        systemRegister.setStatus("1");
        systemRegister.setLastOnlineTime(new Date());
        systemRegister.setSystemId(systemCondition.getApplicationName());
        systemRegister.setIp(systemCondition.getIp());
        systemRegister.setPort(systemCondition.getPort());
        updated(systemRegister);
        log.info("系统已上线");
    }

    @Override
    public void offline() {
        SystemRegister systemRegister = new SystemRegister();
        systemRegister.setStatus("2");
        systemRegister.setLastOnlineTime(new Date());
        systemRegister.setSystemId(systemCondition.getApplicationName());
        systemRegister.setIp(systemCondition.getIp());
        systemRegister.setPort(systemCondition.getPort());
        updated(systemRegister);
        log.info("系统已下线");
    }

    @Override
    public void auth(SystemRequestCondition condition) {
        //这里是验证授权
        SystemRegister systemRegister = this.getById(condition.getId());
        if (systemRegister == null) {
            throw new RuntimeException("系统未注册");
        }
        String authorization = condition.getAuthorizationMes();
        //验证这个授权码是否有效
        LicenseManager.AuthorizationResult checkAuthorization = LicenseManager.checkAuthorization(authorization, DateUtils.format(new Date(), "yyyy-MM-dd"));
        if (checkAuthorization.isValid()) {
            systemRegister.setAuthorizationMes(authorization);
            systemRegister.setValidityPeriod(checkAuthorization.getAuthTime());
            updated(systemRegister);
            log.info("系统授权成功");
            online();
            LicenseCache.put(getKey(), systemRegister);
        } else {
            log.info("系统授权失败");
            expired();
            throw new RuntimeException("系统授权失败,授权时间小于当前时间");
        }

    }

    @Override
    public void expired() {
        SystemRegister systemRegister = new SystemRegister();
        systemRegister.setStatus("4");
        systemRegister.setLastOnlineTime(new Date());
        systemRegister.setSystemId(systemCondition.getApplicationName());
        systemRegister.setIp(systemCondition.getIp());
        systemRegister.setPort(systemCondition.getPort());
        updated(systemRegister);
        log.info("系统已过期");
        LicenseCache.remove(getKey());
    }

    private String getKey() {
        return systemCondition.getApplicationName() + ":" + systemCondition.getIp() + ":" + systemCondition.getPort();
    }

    private void updated(SystemRegister systemRegister) {
        this.update(systemRegister, new QueryWrapper<SystemRegister>()
                .eq("system_id", systemRegister.getSystemId())
                .eq("ip", systemRegister.getIp())
                .eq("port", systemRegister.getPort()));
    }


    @Scheduled(cron = "0 * * * * *")
    public void heartbeat() {
        online();
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void checkAllSystemOnline() {
        //扫描所有的在线注册系统，判断最后在线时间是否有更新，根据上面那个心跳可以判断，最后在线时间和当前时间的间隔应该不超过一分钟，这个任务调度5分钟执行一次
        List<SystemRegister> systemRegisters = this.list(new QueryWrapper<SystemRegister>().eq("status", 1));
        for (SystemRegister systemRegister : systemRegisters) {
            if (getIntervalMinutes(systemRegister.getLastOnlineTime(), new Date()) > 2) {
                log.info("系统{}已离线", systemRegister.getSystemName());
                //更新为3异常，2是正常离线
                systemRegister.setStatus("3");
                updated(systemRegister);
            }
        }
    }

    /**
     * 计算两个日期之间的分钟数差值
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @return 两个日期之间的分钟数差值
     */
    private long getIntervalMinutes(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        long intervalMillis = endDate.getTime() - startDate.getTime();
        return intervalMillis / (60 * 1000);
    }
}

package com.uniops.core.service;

import com.uniops.core.condition.SystemRequestCondition;
import com.uniops.core.entity.SystemRegister;

import java.util.List;

public interface ISystemRegisterService {
    void register();

    SystemRegister localSystem();

    boolean checkLocalValidity();
     boolean checkSystemValidity(SystemRequestCondition condition);
    void online();

    void offline();
    void auth(SystemRequestCondition condition);
    void expired();




}

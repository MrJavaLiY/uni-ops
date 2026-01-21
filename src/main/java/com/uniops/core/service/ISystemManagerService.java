package com.uniops.core.service;

import com.uniops.core.condition.SystemRequestCondition;
import com.uniops.core.entity.SystemRegister;

import java.util.List;

public interface ISystemManagerService {

    List<SystemRegister> searchList(SystemRequestCondition condition);
}

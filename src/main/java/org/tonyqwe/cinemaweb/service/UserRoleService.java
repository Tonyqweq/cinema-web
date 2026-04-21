package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.tonyqwe.cinemaweb.domain.entity.SysUserRole;

import java.util.List;
import java.util.Map;

public interface UserRoleService extends IService<SysUserRole> {

    /**
     * 获取用户角色分布统计
     * @return 各角色用户数量
     */
    List<Map<String, Object>> getUserDistribution();
}

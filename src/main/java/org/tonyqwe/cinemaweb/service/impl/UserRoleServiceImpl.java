package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.SysUserRole;
import org.tonyqwe.cinemaweb.mapper.UserRoleMapper;
import org.tonyqwe.cinemaweb.service.UserRoleService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, SysUserRole> implements UserRoleService {

    @Override
    public List<Map<String, Object>> getUserDistribution() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 获取所有角色统计
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUserRole::getRoleId);
        List<SysUserRole> userRoles = this.list(wrapper);

        Map<Integer, Long> roleCountMap = new HashMap<>();
        for (SysUserRole ur : userRoles) {
            roleCountMap.merge(ur.getRoleId(), 1L, Long::sum);
        }

        // 角色ID到名称的映射
        Map<Integer, String> roleNameMap = new HashMap<>();
        roleNameMap.put(1, "管理员");
        roleNameMap.put(2, "普通用户");
        roleNameMap.put(3, "员工");

        for (Map.Entry<Integer, Long> entry : roleCountMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", roleNameMap.getOrDefault(entry.getKey(), "其他"));
            item.put("value", entry.getValue());
            result.add(item);
        }

        return result;
    }
}

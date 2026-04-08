package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.AdminCinemaRelation;
import org.tonyqwe.cinemaweb.mapper.AdminCinemaRelationMapper;
import org.tonyqwe.cinemaweb.service.AdminCinemaRelationService;
import org.tonyqwe.cinemaweb.service.UserService;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminCinemaRelationServiceImpl extends ServiceImpl<AdminCinemaRelationMapper, AdminCinemaRelation> implements AdminCinemaRelationService {

    @Autowired
    private UserService userService;

    @Override
    public Long getCinemaIdByAdminId(Integer adminId) {
        if (adminId == null) {
            return null;
        }
        AdminCinemaRelation relation = baseMapper.selectOne(new LambdaQueryWrapper<AdminCinemaRelation>()
                .eq(AdminCinemaRelation::getAdminId, adminId));
        return relation != null ? relation.getCinemaId() : null;
    }

    @Override
    public Long getCinemaIdByAdminUsername(String username) {
        if (username == null) {
            return null;
        }
        var user = userService.getByUsername(username);
        if (user == null) {
            return null;
        }
        return getCinemaIdByAdminId(user.getId());
    }

    @Override
    public List<Integer> getAdminIdsByCinemaId(Long cinemaId) {
        if (cinemaId == null) {
            return List.of();
        }
        List<AdminCinemaRelation> relations = baseMapper.selectList(new LambdaQueryWrapper<AdminCinemaRelation>()
                .eq(AdminCinemaRelation::getCinemaId, cinemaId));
        return relations.stream()
                .map(AdminCinemaRelation::getAdminId)
                .collect(Collectors.toList());
    }

    @Override
    public void bindAdminToCinema(Integer adminId, Long cinemaId) {
        if (adminId == null || cinemaId == null) {
            throw new IllegalArgumentException("adminId and cinemaId cannot be null");
        }
        
        // 先删除原有绑定
        baseMapper.delete(new LambdaQueryWrapper<AdminCinemaRelation>()
                .eq(AdminCinemaRelation::getAdminId, adminId));
        
        // 创建新绑定
        AdminCinemaRelation relation = new AdminCinemaRelation();
        relation.setAdminId(adminId);
        relation.setCinemaId(cinemaId);
        relation.setCreatedAt(new Date());
        relation.setUpdatedAt(new Date());
        baseMapper.insert(relation);
    }

    @Override
    public void unbindAdminFromCinema(Integer adminId) {
        if (adminId == null) {
            throw new IllegalArgumentException("adminId cannot be null");
        }
        baseMapper.delete(new LambdaQueryWrapper<AdminCinemaRelation>()
                .eq(AdminCinemaRelation::getAdminId, adminId));
    }

    /**
     * 检查用户是否有权限访问指定影院
     * @param username 用户名
     * @param cinemaId 影院ID
     * @return true if user has permission, false otherwise
     */
    public boolean checkUserCinemaPermission(String username, Long cinemaId) {
        if (username == null || cinemaId == null) {
            return false;
        }
        
        var user = userService.getByUsername(username);
        if (user == null) {
            return false;
        }
        
        // 检查用户是否为SUPER_ADMIN或ADMIN
        if (userService.isSuperAdmin(username) || userService.isAdmin(username)) {
            return true;
        }
        
        // 对于STAFF角色，检查是否绑定到指定影院
        Long boundCinemaId = getCinemaIdByAdminId(user.getId());
        return boundCinemaId != null && boundCinemaId.equals(cinemaId);
    }
}

package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.tonyqwe.cinemaweb.domain.entity.AdminCinemaRelation;

import java.util.List;

public interface AdminCinemaRelationService extends IService<AdminCinemaRelation> {

    /**
     * 根据管理员ID获取绑定的影院ID
     */
    Long getCinemaIdByAdminId(Integer adminId);

    /**
     * 根据管理员用户名获取绑定的影院ID
     */
    Long getCinemaIdByAdminUsername(String username);

    /**
     * 根据影院ID获取绑定的管理员列表
     */
    List<Integer> getAdminIdsByCinemaId(Long cinemaId);

    /**
     * 绑定管理员与影院
     */
    void bindAdminToCinema(Integer adminId, Long cinemaId);

    /**
     * 解除管理员与影院的绑定
     */
    void unbindAdminFromCinema(Integer adminId);
}

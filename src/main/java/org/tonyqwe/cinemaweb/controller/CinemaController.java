package org.tonyqwe.cinemaweb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.dto.CinemaPageResponse;
import org.tonyqwe.cinemaweb.domain.entity.Cinemas;
import org.tonyqwe.cinemaweb.domain.vo.CinemaVO;
import org.tonyqwe.cinemaweb.service.AdminCinemaRelationService;
import org.tonyqwe.cinemaweb.service.CinemaService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;
import org.tonyqwe.cinemaweb.utils.SecurityUtils;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cinemas")
public class CinemaController {
    @Resource
    private CinemaService cinemaService;

    @Resource
    private UserService userService;

    @Resource
    private AdminCinemaRelationService adminCinemaRelationService;

    @GetMapping
    public ResponseEntity<ResponseResult<CinemaPageResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district
    ) {
        try {
            // 检查权限
            if (SecurityUtils.isStaff() || SecurityUtils.isAdmin()) {
                // STAFF和ADMIN角色只能查看绑定的影院
                String username = SecurityUtils.getCurrentUsername();
                if (username != null) {
                    var user = userService.getByUsername(username);
                    if (user != null) {
                        Long cinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
                        if (cinemaId != null) {
                            // 只查询指定影院
                            var cinema = cinemaService.getCinemaById(cinemaId);
                            if (cinema != null) {
                                List<CinemaVO> cinemaVOs = List.of(convertToVO(cinema));
                                CinemaPageResponse response = new CinemaPageResponse(1L, cinemaVOs);
                                return ResponseEntity.ok(ResponseResult.success(response));
                            }
                        }
                        // 没有绑定影院，返回空结果
                        CinemaPageResponse response = new CinemaPageResponse(0L, List.of());
                        return ResponseEntity.ok(ResponseResult.success(response));
                    }
                }
            }
            
            // SUPER_ADMIN或其他角色可以查看所有影院
            IPage<Cinemas> result = cinemaService.pageCinemas(page, pageSize, name, province, city, district);
            List<CinemaVO> cinemaVOs = result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
            CinemaPageResponse response = new CinemaPageResponse(result.getTotal(), cinemaVOs);
            return ResponseEntity.ok(ResponseResult.success(response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ResponseResult.error("获取影院列表失败"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseResult<CinemaVO>> get(@PathVariable Long id) {
        // 检查权限
        if (SecurityUtils.isStaff() || SecurityUtils.isAdmin()) {
            // STAFF和ADMIN角色只能查看绑定的影院
            String username = SecurityUtils.getCurrentUsername();
            if (username != null) {
                var user = userService.getByUsername(username);
                if (user != null) {
                    Long cinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
                    if (cinemaId == null || !cinemaId.equals(id)) {
                        return ResponseEntity.ok(ResponseResult.error("无权访问该影院"));
                    }
                }
            }
        }
        
        Cinemas cinema = cinemaService.getCinemaById(id);
        if (cinema == null) {
            return ResponseEntity.ok(ResponseResult.error("影院不存在"));
        }
        return ResponseEntity.ok(ResponseResult.success(convertToVO(cinema)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseResult<Void>> update(@PathVariable Long id, @RequestBody Cinemas cinema) {
        // 检查权限
        if (SecurityUtils.isStaff() || SecurityUtils.isAdmin()) {
            // STAFF和ADMIN角色只能修改绑定的影院
            String username = SecurityUtils.getCurrentUsername();
            if (username != null) {
                var user = userService.getByUsername(username);
                if (user != null) {
                    Long boundCinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
                    if (boundCinemaId == null || !boundCinemaId.equals(id)) {
                        return ResponseEntity.ok(ResponseResult.error("无权修改该影院"));
                    }
                }
            }
        }
        
        cinema.setId(id);
        boolean success = cinemaService.saveCinema(cinema);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("更新失败"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseResult<Void>> delete(@PathVariable Long id) {
        // 检查权限
        if (SecurityUtils.isStaff() || SecurityUtils.isAdmin()) {
            // STAFF和ADMIN角色只能删除绑定的影院
            String username = SecurityUtils.getCurrentUsername();
            if (username != null) {
                var user = userService.getByUsername(username);
                if (user != null) {
                    Long boundCinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
                    if (boundCinemaId == null || !boundCinemaId.equals(id)) {
                        return ResponseEntity.ok(ResponseResult.error("无权删除该影院"));
                    }
                }
            }
        }
        
        boolean success = cinemaService.deleteCinema(id);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("删除失败"));
        }
    }

    @PostMapping
    public ResponseEntity<ResponseResult<Void>> add(@RequestBody Cinemas cinema) {
        // 检查权限
        if (SecurityUtils.isStaff() || SecurityUtils.isAdmin()) {
            // STAFF和ADMIN角色不能添加影院，只能由SUPER_ADMIN添加
            return ResponseEntity.ok(ResponseResult.error("无权添加影院"));
        }
        
        boolean success = cinemaService.saveCinema(cinema);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("添加失败"));
        }
    }

    private CinemaVO convertToVO(Cinemas cinema) {
        CinemaVO vo = new CinemaVO();
        vo.setId(cinema.getId());
        vo.setName(cinema.getName());
        vo.setPhone(cinema.getPhone());
        vo.setProvince(cinema.getProvince());
        vo.setCity(cinema.getCity());
        vo.setDistrict(cinema.getDistrict());
        vo.setAddress(cinema.getAddress());
        vo.setLatitude(cinema.getLatitude());
        vo.setLongitude(cinema.getLongitude());
        vo.setStatus(cinema.getStatus());
        vo.setCreatedAt(cinema.getCreatedAt());
        vo.setUpdatedAt(cinema.getUpdatedAt());
        return vo;
    }
}

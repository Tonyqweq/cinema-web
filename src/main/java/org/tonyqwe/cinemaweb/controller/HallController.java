package org.tonyqwe.cinemaweb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.dto.HallDTO;
import org.tonyqwe.cinemaweb.domain.dto.HallPageResponse;
import org.tonyqwe.cinemaweb.domain.entity.Cinemas;
import org.tonyqwe.cinemaweb.domain.entity.Halls;
import org.tonyqwe.cinemaweb.domain.vo.HallVO;
import org.tonyqwe.cinemaweb.service.AdminCinemaRelationService;
import org.tonyqwe.cinemaweb.service.CinemaService;
import org.tonyqwe.cinemaweb.service.HallService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;
import org.tonyqwe.cinemaweb.utils.SecurityUtils;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/halls")
public class HallController {

    @Resource
    private HallService hallService;

    @Resource
    private CinemaService cinemaService;

    @Resource
    private UserService userService;

    @Resource
    private AdminCinemaRelationService adminCinemaRelationService;

    /**
     * 分页查询影厅列表
     * GET /api/halls?cinemaId=1&page=1&pageSize=10
     */
    @GetMapping
    public ResponseEntity<ResponseResult<HallPageResponse>> pageHalls(
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 检查权限
        if (SecurityUtils.isStaff()) {
            // ADMIN角色只能查看绑定影院的影厅
            String username = SecurityUtils.getCurrentUsername();
            if (username != null) {
                var user = userService.getByUsername(username);
                if (user != null) {
                    Long boundCinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
                    if (boundCinemaId != null) {
                        // 只能查询绑定影院的影厅
                        if (cinemaId != null && !cinemaId.equals(boundCinemaId)) {
                            HallPageResponse response = new HallPageResponse(0L, List.of());
                            return ResponseEntity.ok(ResponseResult.success(response));
                        }
                        cinemaId = boundCinemaId;
                    } else {
                        // 没有绑定影院，返回空结果
                        HallPageResponse response = new HallPageResponse(0L, List.of());
                        return ResponseEntity.ok(ResponseResult.success(response));
                    }
                }
            }
        }
        
        IPage<Halls> hallsPage = hallService.pageHalls(cinemaId, page, pageSize);
        List<HallVO> hallVOs = hallsPage.getRecords().stream().map(this::toHallVO).collect(Collectors.toList());
        HallPageResponse response = new HallPageResponse(hallsPage.getTotal(), hallVOs);
        return ResponseEntity.ok(ResponseResult.success(response));
    }

    /**
     * 获取影厅详情
     * GET /api/halls/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseResult<HallVO>> getHall(@PathVariable Long id) {
        Halls hall = hallService.getById(id);
        if (hall == null) {
            return ResponseEntity.ok(ResponseResult.error("影厅不存在"));
        }
        
        // 检查权限
        if (SecurityUtils.isStaff()) {
            // ADMIN角色只能查看绑定影院的影厅
            String username = SecurityUtils.getCurrentUsername();
            if (username != null) {
                var user = userService.getByUsername(username);
                if (user != null) {
                    Long boundCinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
                    if (boundCinemaId == null || !boundCinemaId.equals(hall.getCinemaId())) {
                        return ResponseEntity.ok(ResponseResult.error("无权访问该影厅"));
                    }
                }
            }
        }
        
        return ResponseEntity.ok(ResponseResult.success(toHallVO(hall)));
    }

    /**
     * 添加影厅
     * POST /api/halls
     */
    @PostMapping
    public ResponseEntity<ResponseResult<Void>> addHall(@RequestBody HallDTO hallDTO) {
        // 检查权限
        if (SecurityUtils.isStaff()) {
            // ADMIN角色只能添加绑定影院的影厅
            String username = SecurityUtils.getCurrentUsername();
            if (username != null) {
                var user = userService.getByUsername(username);
                if (user != null) {
                    Long boundCinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
                    if (boundCinemaId == null || !boundCinemaId.equals(hallDTO.getCinemaId())) {
                        return ResponseEntity.ok(ResponseResult.error("无权添加该影院的影厅"));
                    }
                }
            }
        }
        
        boolean success = hallService.saveHall(hallDTO);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("添加失败"));
        }
    }

    /**
     * 修改影厅
     * PUT /api/halls/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseResult<Void>> updateHall(@PathVariable Long id, @RequestBody HallDTO hallDTO) {
        // 检查权限
        if (SecurityUtils.isStaff()) {
            // ADMIN角色只能修改绑定影院的影厅
            String username = SecurityUtils.getCurrentUsername();
            if (username != null) {
                var user = userService.getByUsername(username);
                if (user != null) {
                    Long boundCinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
                    if (boundCinemaId != null) {
                        // 检查影厅是否属于绑定的影院
                        Halls hall = hallService.getById(id);
                        if (hall == null || !boundCinemaId.equals(hall.getCinemaId())) {
                            return ResponseEntity.ok(ResponseResult.error("无权修改该影厅"));
                        }
                        // 检查是否修改了影院ID
                        if (!boundCinemaId.equals(hallDTO.getCinemaId())) {
                            return ResponseEntity.ok(ResponseResult.error("无权修改影厅所属影院"));
                        }
                    }
                }
            }
        }
        
        boolean success = hallService.updateHall(id, hallDTO);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("修改失败"));
        }
    }

    /**
     * 删除影厅
     * DELETE /api/halls/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseResult<Void>> deleteHall(@PathVariable Long id) {
        // 检查权限
        if (SecurityUtils.isStaff()) {
            // ADMIN角色只能删除绑定影院的影厅
            String username = SecurityUtils.getCurrentUsername();
            if (username != null) {
                var user = userService.getByUsername(username);
                if (user != null) {
                    Long boundCinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
                    if (boundCinemaId != null) {
                        // 检查影厅是否属于绑定的影院
                        Halls hall = hallService.getById(id);
                        if (hall == null || !boundCinemaId.equals(hall.getCinemaId())) {
                            return ResponseEntity.ok(ResponseResult.error("无权删除该影厅"));
                        }
                    }
                }
            }
        }
        
        boolean success = hallService.deleteHall(id);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("删除失败"));
        }
    }

    /**
     * 转换为VO
     */
    private HallVO toHallVO(Halls hall) {
        HallVO vo = new HallVO();
        vo.setId(hall.getId());
        vo.setCinemaId(hall.getCinemaId());
        // 获取影院名称
        Cinemas cinema = cinemaService.getCinemaById(hall.getCinemaId());
        if (cinema != null) {
            vo.setCinemaName(cinema.getName());
        }
        vo.setName(hall.getName());
        vo.setType(hall.getType());
        vo.setCapacity(hall.getCapacity());
        vo.setStatus(hall.getStatus());
        vo.setPriceNormal(hall.getPriceNormal());
        vo.setPriceGolden(hall.getPriceGolden());
        vo.setPriceVip(hall.getPriceVip());
        vo.setPriceOther(hall.getPriceOther());
        vo.setCreatedAt(hall.getCreatedAt());
        vo.setUpdatedAt(hall.getUpdatedAt());
        return vo;
    }
}

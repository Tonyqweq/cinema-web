package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.dto.SeatDTO;
import org.tonyqwe.cinemaweb.domain.entity.Halls;
import org.tonyqwe.cinemaweb.domain.vo.SeatVO;
import org.tonyqwe.cinemaweb.service.AdminCinemaRelationService;
import org.tonyqwe.cinemaweb.service.HallService;
import org.tonyqwe.cinemaweb.service.SeatService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;
import org.tonyqwe.cinemaweb.utils.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    @Resource
    private SeatService seatService;

    @Resource
    private HallService hallService;

    @Resource
    private UserService userService;

    @Resource
    private AdminCinemaRelationService adminCinemaRelationService;

    /**
     * 检查ADMIN角色是否有权限访问指定影厅的座位
     */
    private boolean checkAdminHallAccess(Long hallId) {
        if (!SecurityUtils.isStaff()) {
            return true; // 非ADMIN角色可以访问
        }
        
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return false;
        }
        
        var user = userService.getByUsername(username);
        if (user == null) {
            return false;
        }
        
        Long boundCinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
        if (boundCinemaId == null) {
            return false;
        }
        
        Halls hall = hallService.getById(hallId);
        return hall != null && boundCinemaId.equals(hall.getCinemaId());
    }

    /**
     * 根据影厅ID获取座位列表
     * GET /api/seats?hallId=1
     */
    @GetMapping
    public ResponseEntity<ResponseResult<List<SeatVO>>> getSeatsByHallId(@RequestParam Long hallId) {
        // 检查权限
        if (!checkAdminHallAccess(hallId)) {
            return ResponseEntity.ok(ResponseResult.error("无权访问该影厅的座位"));
        }
        
        List<SeatVO> seats = seatService.getSeatsByHallId(hallId);
        return ResponseEntity.ok(ResponseResult.success(seats));
    }

    /**
     * 根据场次ID获取座位列表（考虑场次的座位锁定状态）
     * GET /api/seats/showtime?showtimeId=1
     */
    @GetMapping("/showtime")
    public ResponseEntity<ResponseResult<List<SeatVO>>> getSeatsByShowtimeId(@RequestParam Long showtimeId) {
        List<SeatVO> seats = seatService.getSeatsByShowtimeId(showtimeId);
        return ResponseEntity.ok(ResponseResult.success(seats));
    }

    /**
     * 保存座位
     * POST /api/seats
     */
    @PostMapping
    public ResponseEntity<ResponseResult<Void>> saveSeat(@RequestBody SeatDTO seatDTO) {
        // 检查权限
        if (!checkAdminHallAccess(seatDTO.getHallId())) {
            return ResponseEntity.ok(ResponseResult.error("无权操作该影厅的座位"));
        }
        
        boolean success = seatService.saveSeat(seatDTO);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("保存失败"));
        }
    }

    /**
     * 批量保存座位
     * POST /api/seats/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<ResponseResult<Void>> batchSaveSeats(@RequestBody List<SeatDTO> seatDTOs) {
        // 检查权限
        if (!seatDTOs.isEmpty()) {
            Long hallId = seatDTOs.get(0).getHallId();
            if (!checkAdminHallAccess(hallId)) {
                return ResponseEntity.ok(ResponseResult.error("无权操作该影厅的座位"));
            }
        }
        
        boolean success = seatService.batchSaveSeats(seatDTOs);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("保存失败"));
        }
    }

    /**
     * 更新座位
     * PUT /api/seats/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseResult<Void>> updateSeat(@PathVariable Long id, @RequestBody SeatDTO seatDTO) {
        // 检查权限
        if (!checkAdminHallAccess(seatDTO.getHallId())) {
            return ResponseEntity.ok(ResponseResult.error("无权操作该影厅的座位"));
        }
        
        boolean success = seatService.updateSeat(id, seatDTO);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("更新失败"));
        }
    }

    /**
     * 批量更新座位类型/状态/价格
     * PUT /api/seats/batch?ids=1,2,3
     */
    @PutMapping("/batch")
    public ResponseEntity<ResponseResult<Void>> batchUpdateSeats(
            @RequestParam List<Long> ids,
            @RequestBody SeatDTO seatDTO) {
        if (ids.isEmpty()) {
            return ResponseEntity.ok(ResponseResult.error("未选择座位"));
        }
        
        // 检查第一个座位的权限即可（假设都在同一个影厅）
        var firstSeat = seatService.getSeatById(ids.get(0));
        if (firstSeat != null && !checkAdminHallAccess(firstSeat.getHallId())) {
            return ResponseEntity.ok(ResponseResult.error("无权操作这些座位的影厅"));
        }
        
        boolean success = seatService.batchUpdateSeats(ids, seatDTO);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("批量更新失败"));
        }
    }

    /**
     * 删除座位
     * DELETE /api/seats/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseResult<Void>> deleteSeat(@PathVariable Long id) {
        // 先获取座位所属的影厅ID
        var seat = seatService.getSeatById(id);
        if (seat == null) {
            return ResponseEntity.ok(ResponseResult.error("座位不存在"));
        }
        
        // 检查权限
        if (!checkAdminHallAccess(seat.getHallId())) {
            return ResponseEntity.ok(ResponseResult.error("无权操作该影厅的座位"));
        }
        
        boolean success = seatService.deleteSeat(id);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("删除失败"));
        }
    }

    /**
     * 生成影厅座位
     * POST /api/seats/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<ResponseResult<Void>> generateSeats(
            @RequestParam Long hallId,
            @RequestParam int rows,
            @RequestParam int columns) {
        // 检查权限
        if (!checkAdminHallAccess(hallId)) {
            return ResponseEntity.ok(ResponseResult.error("无权操作该影厅的座位"));
        }
        
        boolean success = seatService.generateSeats(hallId, rows, columns);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("生成座位失败"));
        }
    }

    /**
     * 根据影厅ID删除所有座位
     * DELETE /api/seats/hall/{hallId}
     */
    @DeleteMapping("/hall/{hallId}")
    public ResponseEntity<ResponseResult<Void>> deleteSeatsByHallId(@PathVariable Long hallId) {
        // 检查权限
        if (!checkAdminHallAccess(hallId)) {
            return ResponseEntity.ok(ResponseResult.error("无权操作该影厅的座位"));
        }
        
        boolean success = seatService.deleteSeatsByHallId(hallId);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("删除失败"));
        }
    }

    /**
     * 锁定座位
     * POST /api/seats/lock
     */
    @PostMapping("/lock")
    public ResponseEntity<ResponseResult<Void>> lockSeats(@RequestBody java.util.Map<String, Object> request) {
        Long showtimeId = ((Number) request.get("showtimeId")).longValue();
        @SuppressWarnings("unchecked")
        List<?> seatIdsRaw = (List<?>) request.get("seatIds");
        List<Long> seatIds = new java.util.ArrayList<>();
        for (Object id : seatIdsRaw) {
            if (id instanceof Number) {
                seatIds.add(((Number) id).longValue());
            } else if (id instanceof String) {
                seatIds.add(Long.parseLong((String) id));
            }
        }
        boolean success = seatService.lockSeats(showtimeId, seatIds);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("锁定座位失败"));
        }
    }

    /**
     * 解锁座位
     * POST /api/seats/unlock
     */
    @PostMapping("/unlock")
    public ResponseEntity<ResponseResult<Void>> unlockSeats(@RequestBody java.util.Map<String, Object> request) {
        Long showtimeId = ((Number) request.get("showtimeId")).longValue();
        @SuppressWarnings("unchecked")
        List<?> seatIdsRaw = (List<?>) request.get("seatIds");
        List<Long> seatIds = new java.util.ArrayList<>();
        for (Object id : seatIdsRaw) {
            if (id instanceof Number) {
                seatIds.add(((Number) id).longValue());
            } else if (id instanceof String) {
                seatIds.add(Long.parseLong((String) id));
            }
        }
        boolean success = seatService.unlockSeats(showtimeId, seatIds);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("解锁座位失败"));
        }
    }

    /**
     * 检查座位是否可锁定
     * GET /api/seats/check-lock
     */
    @GetMapping("/check-lock")
    public ResponseEntity<ResponseResult<java.util.Map<String, Object>>> checkSeatsLockable(@RequestParam Long showtimeId, @RequestParam List<Long> seatIds) {
        java.util.Map<String, Object> result = seatService.checkSeatsLockable(showtimeId, seatIds);
        return ResponseEntity.ok(ResponseResult.success(result));
    }
}

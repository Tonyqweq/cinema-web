package org.tonyqwe.cinemaweb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.dto.HallDTO;
import org.tonyqwe.cinemaweb.domain.entity.Cinemas;
import org.tonyqwe.cinemaweb.domain.entity.Halls;
import org.tonyqwe.cinemaweb.domain.vo.HallVO;
import org.tonyqwe.cinemaweb.service.CinemaService;
import org.tonyqwe.cinemaweb.service.HallService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/halls")
public class HallController {

    @Resource
    private HallService hallService;

    @Resource
    private CinemaService cinemaService;

    /**
     * 分页查询影厅列表
     * GET /api/halls?cinemaId=1&page=1&pageSize=10
     */
    @GetMapping
    public ResponseEntity<ResponseResult<List<HallVO>>> pageHalls(
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        IPage<Halls> hallsPage = hallService.pageHalls(cinemaId, page, pageSize);
        List<HallVO> hallVOs = hallsPage.getRecords().stream().map(this::toHallVO).collect(Collectors.toList());
        return ResponseEntity.ok(ResponseResult.success(hallVOs, hallsPage.getTotal()));
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
        return ResponseEntity.ok(ResponseResult.success(toHallVO(hall)));
    }

    /**
     * 添加影厅
     * POST /api/halls
     */
    @PostMapping
    public ResponseEntity<ResponseResult<Void>> addHall(@RequestBody HallDTO hallDTO) {
        boolean success = hallService.saveHall(hallDTO);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success("添加成功"));
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
        boolean success = hallService.updateHall(id, hallDTO);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success("修改成功"));
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
        boolean success = hallService.deleteHall(id);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success("删除成功"));
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
        Cinemas cinema = cinemaService.getById(hall.getCinemaId());
        if (cinema != null) {
            vo.setCinemaName(cinema.getName());
        }
        vo.setName(hall.getName());
        vo.setType(hall.getType());
        vo.setCapacity(hall.getCapacity());
        vo.setStatus(hall.getStatus());
        vo.setCreatedAt(hall.getCreatedAt());
        vo.setUpdatedAt(hall.getUpdatedAt());
        return vo;
    }
}

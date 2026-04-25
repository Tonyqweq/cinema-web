package org.tonyqwe.cinemaweb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.dto.ShowtimesDTO;
import org.tonyqwe.cinemaweb.domain.dto.ShowtimesPageResponse;
import org.tonyqwe.cinemaweb.domain.entity.Cinemas;
import org.tonyqwe.cinemaweb.domain.entity.Halls;
import org.tonyqwe.cinemaweb.domain.entity.Movies;
import org.tonyqwe.cinemaweb.domain.entity.Showtimes;
import org.tonyqwe.cinemaweb.domain.vo.ShowtimesVO;
import org.tonyqwe.cinemaweb.service.AdminCinemaRelationService;
import org.tonyqwe.cinemaweb.service.CinemaService;
import org.tonyqwe.cinemaweb.service.HallService;
import org.tonyqwe.cinemaweb.service.MovieService;
import org.tonyqwe.cinemaweb.service.ShowtimesService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;
import org.tonyqwe.cinemaweb.utils.SecurityUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 排片控制器
 */
@RestController
@RequestMapping("/api/showtimes")
public class ShowtimesController {

    @Resource
    private ShowtimesService showtimesService;

    @Resource
    private CinemaService cinemaService;

    @Resource
    private HallService hallService;

    @Resource
    private MovieService movieService;

    @Resource
    private UserService userService;

    @Resource
    private AdminCinemaRelationService adminCinemaRelationService;

    /**
     * 分页查询排片列表（用于购票，所有已认证用户可访问）
     */
    @GetMapping
    public ResponseEntity<ResponseResult<ShowtimesPageResponse>> pageShowtimes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) Long hallId,
            @RequestParam(required = false) Long movieId) {

        IPage<Showtimes> showtimesPage = showtimesService.pageShowtimes(page, pageSize, cinemaId, hallId, movieId);
        List<ShowtimesVO> showtimesVOs = showtimesPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        ShowtimesPageResponse response = new ShowtimesPageResponse(showtimesPage.getTotal(), showtimesVOs);
        return ResponseEntity.ok(ResponseResult.success(response));
    }

    /**
     * 获取排片详情（用于购票，所有已认证用户可访问）
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseResult<ShowtimesVO>> getShowtimes(@PathVariable Long id) {
        Showtimes showtimes = showtimesService.getById(id);
        if (showtimes == null) {
            return ResponseEntity.ok(ResponseResult.error("排片不存在"));
        }

        return ResponseEntity.ok(ResponseResult.success(convertToVO(showtimes)));
    }

    /**
     * 保存排片
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
    @PostMapping
    public ResponseEntity<ResponseResult<Void>> saveShowtimes(@RequestBody ShowtimesDTO showtimesDTO) {
        // 检查权限
        if (SecurityUtils.isStaff() || SecurityUtils.isAdmin()) {
            // STAFF和ADMIN角色只能添加绑定影院的排片
            String username = SecurityUtils.getCurrentUsername();
            if (username != null) {
                var user = userService.getByUsername(username);
                if (user != null) {
                    Long boundCinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
                    if (boundCinemaId == null || !boundCinemaId.equals(showtimesDTO.getCinemaId())) {
                        return ResponseEntity.ok(ResponseResult.error("无权添加该影院的排片"));
                    }
                }
            }
        }

        // 检查影厅是否属于指定影院
        Halls hall = hallService.getById(showtimesDTO.getHallId());
        if (hall == null || !hall.getCinemaId().equals(showtimesDTO.getCinemaId())) {
            return ResponseEntity.ok(ResponseResult.error("影厅不属于指定影院"));
        }

        // 检查时间冲突
        if (showtimesService.checkTimeConflict(showtimesDTO.getHallId(), showtimesDTO.getStartTime(), showtimesDTO.getEndTime(), showtimesDTO.getId())) {
            return ResponseEntity.ok(ResponseResult.error("该时间段影厅已被占用"));
        }

        // 检查排片时长是否大于电影时长
        Movies movie = movieService.getById(showtimesDTO.getMovieId());
        if (movie != null) {
            Integer movieDuration = movie.getDurationMin(); // 电影时长（分钟）
            if (movieDuration != null) {
                long showtimeDuration = (showtimesDTO.getEndTime().getTime() - showtimesDTO.getStartTime().getTime()) / (60 * 1000); // 排片时长（分钟）
                if (showtimeDuration < movieDuration) {
                    return ResponseEntity.ok(ResponseResult.error("排片时长必须大于电影时长（" + movieDuration + "分钟）"));
                }
            }
        }

        boolean success = showtimesService.saveShowtimes(showtimesDTO);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("保存失败"));
        }
    }

    /**
     * 删除排片
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseResult<Void>> deleteShowtimes(@PathVariable Long id) {
        Showtimes showtimes = showtimesService.getById(id);
        if (showtimes == null) {
            return ResponseEntity.ok(ResponseResult.error("排片不存在"));
        }

        // 检查权限
        if (SecurityUtils.isStaff() || SecurityUtils.isAdmin()) {
            // STAFF和ADMIN角色只能删除绑定影院的排片
            String username = SecurityUtils.getCurrentUsername();
            if (username != null) {
                var user = userService.getByUsername(username);
                if (user != null) {
                    Long boundCinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
                    if (boundCinemaId == null || !boundCinemaId.equals(showtimes.getCinemaId())) {
                        return ResponseEntity.ok(ResponseResult.error("无权删除该排片"));
                    }
                }
            }
        }

        boolean success = showtimesService.deleteShowtimes(id);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("删除失败"));
        }
    }

    /**
     * 转换为VO
     */
    private ShowtimesVO convertToVO(Showtimes showtimes) {
        ShowtimesVO vo = new ShowtimesVO();
        vo.setId(showtimes.getId());
        vo.setCinemaId(showtimes.getCinemaId());
        vo.setHallId(showtimes.getHallId());
        vo.setMovieId(showtimes.getMovieId());
        vo.setStartTime(showtimes.getStartTime());
        vo.setEndTime(showtimes.getEndTime());
        vo.setPrice(showtimes.getPrice());
        vo.setPriceNormal(showtimes.getPriceNormal());
        vo.setPriceGolden(showtimes.getPriceGolden());
        vo.setPriceVip(showtimes.getPriceVip());
        vo.setPriceOther(showtimes.getPriceOther());
        vo.setStatus(showtimes.getStatus());
        vo.setCreatedAt(showtimes.getCreatedAt());
        vo.setUpdatedAt(showtimes.getUpdatedAt());

        // 设置影院名称和地址
        Cinemas cinema = cinemaService.getCinemaById(showtimes.getCinemaId());
        if (cinema != null) {
            vo.setCinemaName(cinema.getName());
            vo.setCinemaAddress(cinema.getAddress());
        }

        // 设置影厅名称
        Halls hall = hallService.getById(showtimes.getHallId());
        if (hall != null) {
            vo.setHallName(hall.getName());
        }

        // 设置电影名称和海报
        Movies movie = movieService.getById(showtimes.getMovieId());
        if (movie != null) {
            vo.setMovieName(movie.getTitle());
            vo.setMoviePoster(movie.getPosterUrl());
        }

        // 设置状态文本
        vo.setStatusText(showtimes.getStatus() == 1 ? "正常" : "取消");

        return vo;
    }
}

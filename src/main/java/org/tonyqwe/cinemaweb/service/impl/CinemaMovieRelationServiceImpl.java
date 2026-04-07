package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.CinemaMovieRelation;
import org.tonyqwe.cinemaweb.mapper.CinemaMovieRelationMapper;
import org.tonyqwe.cinemaweb.service.CinemaMovieRelationService;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CinemaMovieRelationServiceImpl extends ServiceImpl<CinemaMovieRelationMapper, CinemaMovieRelation> implements CinemaMovieRelationService {

    @Override
    public List<Long> getMovieIdsByCinemaId(Long cinemaId) {
        List<CinemaMovieRelation> relations = baseMapper.selectList(new LambdaQueryWrapper<CinemaMovieRelation>()
                .eq(CinemaMovieRelation::getCinemaId, cinemaId));
        return relations.stream()
                .map(CinemaMovieRelation::getMovieId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getCinemaIdsByMovieId(Long movieId) {
        List<CinemaMovieRelation> relations = baseMapper.selectList(new LambdaQueryWrapper<CinemaMovieRelation>()
                .eq(CinemaMovieRelation::getMovieId, movieId));
        return relations.stream()
                .map(CinemaMovieRelation::getCinemaId)
                .collect(Collectors.toList());
    }

    @Override
    public void bindCinemaToMovie(Long cinemaId, Long movieId) {
        if (cinemaId == null || movieId == null) {
            throw new IllegalArgumentException("cinemaId and movieId cannot be null");
        }
        
        // 检查是否已存在绑定关系
        CinemaMovieRelation existingRelation = baseMapper.selectOne(new LambdaQueryWrapper<CinemaMovieRelation>()
                .eq(CinemaMovieRelation::getCinemaId, cinemaId)
                .eq(CinemaMovieRelation::getMovieId, movieId));
        
        if (existingRelation == null) {
            // 创建新绑定
            CinemaMovieRelation relation = new CinemaMovieRelation();
            relation.setCinemaId(cinemaId);
            relation.setMovieId(movieId);
            relation.setCreatedAt(new Date());
            relation.setUpdatedAt(new Date());
            baseMapper.insert(relation);
        }
    }

    @Override
    public void unbindCinemaFromMovie(Long cinemaId, Long movieId) {
        if (cinemaId == null || movieId == null) {
            throw new IllegalArgumentException("cinemaId and movieId cannot be null");
        }
        
        baseMapper.delete(new LambdaQueryWrapper<CinemaMovieRelation>()
                .eq(CinemaMovieRelation::getCinemaId, cinemaId)
                .eq(CinemaMovieRelation::getMovieId, movieId));
    }

    @Override
    public void unbindAllMoviesFromCinema(Long cinemaId) {
        if (cinemaId == null) {
            throw new IllegalArgumentException("cinemaId cannot be null");
        }
        
        baseMapper.delete(new LambdaQueryWrapper<CinemaMovieRelation>()
                .eq(CinemaMovieRelation::getCinemaId, cinemaId));
    }

    @Override
    public void unbindAllCinemasFromMovie(Long movieId) {
        if (movieId == null) {
            throw new IllegalArgumentException("movieId cannot be null");
        }
        
        baseMapper.delete(new LambdaQueryWrapper<CinemaMovieRelation>()
                .eq(CinemaMovieRelation::getMovieId, movieId));
    }
}

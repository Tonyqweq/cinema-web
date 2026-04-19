package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonyqwe.cinemaweb.domain.entity.MovieTags;
import org.tonyqwe.cinemaweb.domain.entity.Tags;
import org.tonyqwe.cinemaweb.mapper.MovieTagMapper;
import org.tonyqwe.cinemaweb.mapper.TagMapper;
import org.tonyqwe.cinemaweb.service.TagService;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TagServiceImpl implements TagService {

    @Resource
    private TagMapper tagMapper;

    @Resource
    private MovieTagMapper movieTagMapper;

    @Override
    public List<Tags> getAllTags() {
        return tagMapper.selectList(null);
    }

    @Override
    public List<Tags> getTagsByMovieId(Long movieId) {
        LambdaQueryWrapper<MovieTags> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MovieTags::getMovieId, movieId);
        List<MovieTags> movieTags = movieTagMapper.selectList(wrapper);
        
        List<Long> tagIds = movieTags.stream()
                .map(MovieTags::getTagId)
                .collect(java.util.stream.Collectors.toList());
        
        if (tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return tagMapper.selectBatchIds(tagIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTagToMovie(Long movieId, Long tagId) {
        LambdaQueryWrapper<MovieTags> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MovieTags::getMovieId, movieId)
               .eq(MovieTags::getTagId, tagId);
        MovieTags existing = movieTagMapper.selectOne(wrapper);
        
        if (existing != null) {
            return true;
        }
        
        MovieTags movieTag = new MovieTags();
        movieTag.setMovieId(movieId);
        movieTag.setTagId(tagId);
        return movieTagMapper.insert(movieTag) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTagFromMovie(Long movieId, Long tagId) {
        LambdaQueryWrapper<MovieTags> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MovieTags::getMovieId, movieId)
               .eq(MovieTags::getTagId, tagId);
        return movieTagMapper.delete(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTagsToMovie(Long movieId, List<Long> tagIds) {
        for (Long tagId : tagIds) {
            addTagToMovie(movieId, tagId);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeAllTagsFromMovie(Long movieId) {
        LambdaQueryWrapper<MovieTags> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MovieTags::getMovieId, movieId);
        return movieTagMapper.delete(wrapper) >= 0;
    }
}
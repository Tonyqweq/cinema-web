package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.UserCollection;
import org.tonyqwe.cinemaweb.mapper.UserCollectionMapper;
import org.tonyqwe.cinemaweb.service.UserCollectionService;

import java.time.LocalDateTime;

@Service
public class UserCollectionServiceImpl implements UserCollectionService {

    @Resource
    private UserCollectionMapper userCollectionMapper;

    @Override
    public IPage<UserCollection> getUserCollections(Long userId, long page, long pageSize) {
        Page<UserCollection> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<UserCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCollection::getUserId, userId)
                    .orderByDesc(UserCollection::getCreatedAt);
        return userCollectionMapper.selectPage(pageInfo, queryWrapper);
    }

    @Override
    public boolean addCollection(Long userId, Long movieId) {
        // 检查是否已经收藏
        LambdaQueryWrapper<UserCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCollection::getUserId, userId)
                    .eq(UserCollection::getMovieId, movieId);
        UserCollection existing = userCollectionMapper.selectOne(queryWrapper);
        
        if (existing != null) {
            return true; // 已经收藏，直接返回成功
        }

        UserCollection collection = new UserCollection();
        collection.setUserId(userId);
        collection.setMovieId(movieId);
        collection.setCreatedAt(LocalDateTime.now());

        return userCollectionMapper.insert(collection) > 0;
    }

    @Override
    public boolean removeCollection(Long userId, Long movieId) {
        LambdaQueryWrapper<UserCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCollection::getUserId, userId)
                    .eq(UserCollection::getMovieId, movieId);
        return userCollectionMapper.delete(queryWrapper) > 0;
    }

    @Override
    public boolean isCollected(Long userId, Long movieId) {
        LambdaQueryWrapper<UserCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCollection::getUserId, userId)
                    .eq(UserCollection::getMovieId, movieId);
        return userCollectionMapper.selectCount(queryWrapper) > 0;
    }
}

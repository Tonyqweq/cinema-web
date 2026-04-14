package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonyqwe.cinemaweb.domain.entity.UserCollection;
import org.tonyqwe.cinemaweb.mapper.UserCollectionMapper;
import org.tonyqwe.cinemaweb.service.UserCollectionService;

import java.util.List;

@Service
public class UserCollectionServiceImpl extends ServiceImpl<UserCollectionMapper, UserCollection> implements UserCollectionService {

    @Resource
    private UserCollectionMapper userCollectionMapper;

    @Override
    @Transactional
    public boolean addCollection(Integer userId, Long movieId) {
        LambdaQueryWrapper<UserCollection> qw = new LambdaQueryWrapper<>();
        qw.eq(UserCollection::getUserId, userId);
        qw.eq(UserCollection::getMovieId, movieId);
        
        UserCollection existing = userCollectionMapper.selectOne(qw);
        if (existing != null) {
            return true;
        }
        
        UserCollection collection = new UserCollection();
        collection.setUserId(userId);
        collection.setMovieId(movieId);
        return userCollectionMapper.insert(collection) > 0;
    }

    @Override
    @Transactional
    public boolean removeCollection(Integer userId, Long movieId) {
        LambdaQueryWrapper<UserCollection> qw = new LambdaQueryWrapper<>();
        qw.eq(UserCollection::getUserId, userId);
        qw.eq(UserCollection::getMovieId, movieId);
        return userCollectionMapper.delete(qw) > 0;
    }

    @Override
    public boolean isCollected(Integer userId, Long movieId) {
        LambdaQueryWrapper<UserCollection> qw = new LambdaQueryWrapper<>();
        qw.eq(UserCollection::getUserId, userId);
        qw.eq(UserCollection::getMovieId, movieId);
        return userCollectionMapper.selectCount(qw) > 0;
    }

    @Override
    public IPage<UserCollection> getUserCollections(Integer userId, long page, long pageSize) {
        Page<UserCollection> mpPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<UserCollection> qw = new LambdaQueryWrapper<>();
        qw.eq(UserCollection::getUserId, userId);
        qw.orderByDesc(UserCollection::getCreatedAt);
        return userCollectionMapper.selectPage(mpPage, qw);
    }

    @Override
    public List<Long> getUserCollectedMovieIds(Integer userId) {
        LambdaQueryWrapper<UserCollection> qw = new LambdaQueryWrapper<>();
        qw.eq(UserCollection::getUserId, userId);
        qw.select(UserCollection::getMovieId);
        return userCollectionMapper.selectList(qw).stream()
                .map(UserCollection::getMovieId)
                .toList();
    }

    @Override
    public Integer getCollectionCount(Long movieId) {
        return userCollectionMapper.getCollectionCount(movieId);
    }
}
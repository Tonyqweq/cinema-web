package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.tonyqwe.cinemaweb.domain.entity.UserCollection;

import java.util.List;

public interface UserCollectionService extends IService<UserCollection> {

    boolean addCollection(Integer userId, Long movieId);

    boolean removeCollection(Integer userId, Long movieId);

    boolean isCollected(Integer userId, Long movieId);

    IPage<UserCollection> getUserCollections(Integer userId, long page, long pageSize);

    List<Long> getUserCollectedMovieIds(Integer userId);

    Integer getCollectionCount(Long movieId);
}
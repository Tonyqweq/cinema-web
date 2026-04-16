package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.tonyqwe.cinemaweb.domain.entity.UserCollection;

import java.util.List;

public interface UserCollectionService {

    IPage<UserCollection> getUserCollections(Long userId, long page, long pageSize);
    
    boolean addCollection(Long userId, Long movieId);
    
    boolean removeCollection(Long userId, Long movieId);
    
    boolean isCollected(Long userId, Long movieId);
}

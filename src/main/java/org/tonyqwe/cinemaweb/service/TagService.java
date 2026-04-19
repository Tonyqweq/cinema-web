package org.tonyqwe.cinemaweb.service;

import org.tonyqwe.cinemaweb.domain.entity.Tags;
import java.util.List;

public interface TagService {
    
    List<Tags> getAllTags();
    
    List<Tags> getTagsByMovieId(Long movieId);
    
    boolean addTagToMovie(Long movieId, Long tagId);
    
    boolean removeTagFromMovie(Long movieId, Long tagId);
    
    boolean addTagsToMovie(Long movieId, List<Long> tagIds);
    
    boolean removeAllTagsFromMovie(Long movieId);
}
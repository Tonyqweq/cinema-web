package org.tonyqwe.cinemaweb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.entity.Tags;
import org.tonyqwe.cinemaweb.service.TagService;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Resource
    private TagService tagService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTags() {
        List<Tags> tags = tagService.getAllTags();
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", tags);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<Map<String, Object>> getTagsByMovieId(@PathVariable Long movieId) {
        List<Tags> tags = tagService.getTagsByMovieId(movieId);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", tags);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/movie/{movieId}")
    public ResponseEntity<Map<String, Object>> addTagsToMovie(
            @PathVariable Long movieId,
            @RequestBody Map<String, List<Long>> request) {
        List<Long> tagIds = request.get("tagIds");
        boolean success = tagService.addTagsToMovie(movieId, tagIds);
        Map<String, Object> response = new HashMap<>();
        response.put("code", success ? 200 : 500);
        response.put("message", success ? "标签添加成功" : "标签添加失败");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/movie/{movieId}")
    public ResponseEntity<Map<String, Object>> removeAllTagsFromMovie(@PathVariable Long movieId) {
        boolean success = tagService.removeAllTagsFromMovie(movieId);
        Map<String, Object> response = new HashMap<>();
        response.put("code", success ? 200 : 500);
        response.put("message", success ? "标签移除成功" : "标签移除失败");
        return ResponseEntity.ok(response);
    }
}
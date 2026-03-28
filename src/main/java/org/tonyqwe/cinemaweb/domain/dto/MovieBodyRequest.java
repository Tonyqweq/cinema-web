package org.tonyqwe.cinemaweb.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MovieBodyRequest {

    @NotBlank(message = "title is required")
    private String title;

    @JsonProperty("original_title")
    private String originalTitle;

    private String language;

    private String country;

    @JsonProperty("duration_min")
    private Integer durationMin;

    /**
     * 上映日期，格式 yyyy-MM-dd
     */
    @JsonProperty("release_date")
    private String releaseDate;

    private String description;

    @JsonProperty("poster_url")
    private String posterUrl;

    @JsonProperty("trailer_url")
    private String trailerUrl;
}

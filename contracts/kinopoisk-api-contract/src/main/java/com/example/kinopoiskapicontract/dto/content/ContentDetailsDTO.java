package com.example.kinopoiskapicontract.dto.content;

import org.springframework.hateoas.RepresentationModel;

import java.util.List;

public class ContentDetailsDTO extends RepresentationModel<ContentDetailsDTO> {
    private final ContentResponse content;
    private final Double averageRating;
    private final Integer totalRatings;
    private final Integer userRating;
    private final Boolean inFavorites;
    private final Integer totalReviews;
    private final List<ReviewDTO> recentReviews;

    public ContentDetailsDTO(ContentResponse content, Double averageRating,
                             Integer totalRatings, Integer userRating,
                             Boolean inFavorites, Integer totalReviews,
                             List<ReviewDTO> recentReviews) {
        this.content = content;
        this.averageRating = averageRating;
        this.totalRatings = totalRatings;
        this.userRating = userRating;
        this.inFavorites = inFavorites;
        this.totalReviews = totalReviews;
        this.recentReviews = recentReviews;
    }

    public ContentResponse getContent() { return content; }
    public Double getAverageRating() { return averageRating; }
    public Integer getTotalRatings() { return totalRatings; }
    public Integer getUserRating() { return userRating; }
    public Boolean getInFavorites() { return inFavorites; }
    public Integer getTotalReviews() { return totalReviews; }
    public List<ReviewDTO> getRecentReviews() { return recentReviews; }
}

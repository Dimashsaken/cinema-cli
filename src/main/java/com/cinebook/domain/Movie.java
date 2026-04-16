package com.cinebook.domain;

/** A film in the cinema catalog. */
public class Movie {

    private String movieId;
    private String title;
    private int durationMin;
    private String rating;
    private String genre;

    public Movie() {}

    public Movie(String movieId, String title, int durationMin, String rating, String genre) {
        this.movieId = movieId;
        this.title = title;
        this.durationMin = durationMin;
        this.rating = rating;
        this.genre = genre;
    }

    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDurationMin() { return durationMin; }
    public void setDurationMin(int durationMin) { this.durationMin = durationMin; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
}

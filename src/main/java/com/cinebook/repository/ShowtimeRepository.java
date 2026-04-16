package com.cinebook.repository;

import com.cinebook.domain.Showtime;
import java.util.List;

/** Repository for showtimes. Concrete implementation in infra package. */
public interface ShowtimeRepository extends Repository<Showtime> {

    /** Find all showtimes for a given movie. */
    List<Showtime> findByMovieId(String movieId);
}

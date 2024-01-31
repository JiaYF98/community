package com.nowcoder.community.service;

import java.time.LocalDate;
import java.util.Date;

public interface DataService {
    void recordUV(String ip);

    Long calculateUV(LocalDate start, LocalDate end);

    void recordDAU(Integer userId);

    Long calculateDAU(LocalDate start, LocalDate end);
}

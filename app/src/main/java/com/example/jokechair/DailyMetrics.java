package com.example.jokechair;

public class DailyMetrics {
    int timeSpentSitting;
    String percentSpentProper;
    String mostCommonImproper;
    String percentMostCommonImproper;

    DailyMetrics(int timeSpentSitting,
                 String percentSpentProper,
                 String mostCommonImproper,
                 String percentMostCommonImproper) {
        this.timeSpentSitting = timeSpentSitting;
        this.percentSpentProper = percentSpentProper;
        this.mostCommonImproper = mostCommonImproper;
        this.percentMostCommonImproper = percentMostCommonImproper;
    }

}


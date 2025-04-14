package com.dappstp.dappstp.model;

public class Player {
    
    private String name;
    private int matchesPlayed;
    private int goals;
    private int assists;
    private double rating;
    private String teamName;

    public Player(String name, int matchesPlayed, int goals, int assists, double rating, String teamName) {
        this.name = name;
        this.matchesPlayed = matchesPlayed;
        this.goals = goals;
        this.assists = assists;
        this.rating = rating;
        this.teamName = teamName;
    }

    public String getName() { 
        return name; 
    }
    
    public int getMatchesPlayed() { 
        return matchesPlayed; 
    }
    
    public int getGoals() { 
        return goals; 
    }
    
    public int getAssists() { 
        return assists; 
    }
    
    public double getRating() { 
        return rating; 
    }

    public String getTeamName() {
        return teamName; 
    }

    public void setName(String name) { 
        this.name = name; 
    }
    
    public void setMatchesPlayed(int matchesPlayed) { 
        this.matchesPlayed = matchesPlayed; 
    }
    
    public void setGoals(int goals) { 
        this.goals = goals; 
    }
    
    public void setAssists(int assists) { 
        this.assists = assists; 
    }
    
    public void setRating(double rating) { 
        this.rating = rating; 
    }

    public void setTeamName(String teamName) { 
        this.teamName = teamName; 
    }
}

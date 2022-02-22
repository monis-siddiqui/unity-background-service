package com.kdg.toast.plugin;

import java.util.Date;

public class StepCountData {
    int steps;
    Date date;

    public StepCountData(int steps, Date date) {
        this.steps = steps;
        this.date = date;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }




}

package com.example.walkingapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

public class CustomView	extends View {

    private static final int MIN_LINES = 3;
    private static final int MAX_LINES = 8;
    private static final int[] DISTANCES = { 1, 2, 5 };
    private static final float GRAPH_SMOOTHNES = 0.15f;

    private Dynamics[] datapoints;
    private Paint paint = new Paint();

    /**
     * Custom view Method
     * @param context Context
     * @param attrs AttributeSet
     */
    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets the y data points of the line chart. The data points on the x-axis
     *  are the Locations. The line chart will be
     * scaled so that the entire height of the view is used.
     * @param newDatapoints y values of the line chart
     */
    public void setChartData(float[] newDatapoints) {
        long now = AnimationUtils.currentAnimationTimeMillis();
        if (datapoints == null || datapoints.length != newDatapoints.length) {
            datapoints = new Dynamics[newDatapoints.length];
            for (int i = 0; i < newDatapoints.length; i++) {
                datapoints[i] = new Dynamics(100f, 0.15f);
                datapoints[i].setPosition(newDatapoints[i], now);
                datapoints[i].setTargetPosition(now);
            }
            invalidate();
        } else {
            for (int i = 0; i < newDatapoints.length; i++) {
                datapoints[i].setTargetPosition(now);
            }

            invalidate();
        }
    }

    /**
     * Draw the canvass. Call the background draw and linechart draw
     * @param canvas canvass
     */
    @Override
    protected void onDraw(Canvas canvas) {
        float maxValue = getMax(datapoints);
        drawBackground(canvas, maxValue);
        drawLineChart(canvas, maxValue);
    }

    /**
     * draw the bacground of the graph
     * take the max value to calculate how many lines are drawn
     * @param canvas canvas
     * @param maxValue the max value for the line distance
     */
    private void drawBackground(Canvas canvas, float maxValue) {
        int range = getLineDistance(maxValue);
        paint.setStyle(Style.FILL);
        paint.setColor(Color.GRAY);
        paint.setTextAlign(Align.LEFT);
        paint.setTextSize(16);
        paint.setStrokeWidth(1);
        //For loop to draw background and calculate the number of lines
        for (int y = 0; y < maxValue; y += range) {
            final int yPos = (int) getYPos(y, maxValue);

            // turn off anti alias for lines, they get crisper then
            paint.setAntiAlias(false);
            canvas.drawLine(0, yPos, getWidth(), yPos, paint);

            // turn on anti alias again for the text
            paint.setAntiAlias(true);
            canvas.drawText(String.valueOf(y), getPaddingLeft(), yPos - 2, paint);
        }
    }

    /**
     * Method to return the distance value between the graph lines
     * @param maxValue
     * @return
     */
    private int getLineDistance(float maxValue) {
        long distance;
        int distanceIndex = 0;
        long distanceMultiplier = 1;
        int numberOfLines;

        do {
            distance = DISTANCES[distanceIndex] * distanceMultiplier;
            numberOfLines = (int) Math.ceil(maxValue / distance);

            distanceIndex++;
            if (distanceIndex == DISTANCES.length) {
                distanceIndex = 0;
                distanceMultiplier *= 10;

            }
        } while (numberOfLines < MIN_LINES || numberOfLines > MAX_LINES);

        return (int) distance;
    }

    /**
     * Method to draw the line graph from user data
     * @param canvas
     * @param maxValue
     */
    private void drawLineChart(Canvas canvas, float maxValue) {
        Path path = createSmoothPath(maxValue);

        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(0xFF33B5E5);
        paint.setAntiAlias(true);
        paint.setShadowLayer(4, 2, 2, 0x81000000);
        canvas.drawPath(path, paint);
        paint.setShadowLayer(0, 0, 0, 0);
    }

    /**
     * Method to return the path value after it has been smoothed out
     * @param maxValue The max value of the data set
     * @return return the path
     */
    private Path createSmoothPath(float maxValue) {

        Path path = new Path();
        path.moveTo(getXPos(0), getYPos(datapoints[0].getPosition(), maxValue));
        for (int i = 0; i < datapoints.length - 1; i++) {
            float thisPointX = getXPos(i);
            float thisPointY = getYPos(datapoints[i].getPosition(), maxValue);
            float nextPointX = getXPos(i + 1);
            float nextPointY = getYPos(datapoints[si(i + 1)].getPosition(), maxValue);

            float startdiffX = (nextPointX - getXPos(si(i - 1)));
            float startdiffY = (nextPointY - getYPos(datapoints[si(i - 1)].getPosition(), maxValue));
            float endDiffX = (getXPos(si(i + 2)) - thisPointX);
            float endDiffY = (getYPos(datapoints[si(i + 2)].getPosition(), maxValue) - thisPointY);

            float firstControlX = thisPointX + (GRAPH_SMOOTHNES * startdiffX);
            float firstControlY = thisPointY + (GRAPH_SMOOTHNES * startdiffY);
            float secondControlX = nextPointX - (GRAPH_SMOOTHNES * endDiffX);
            float secondControlY = nextPointY - (GRAPH_SMOOTHNES * endDiffY);

            path.cubicTo(firstControlX, firstControlY, secondControlX, secondControlY, nextPointX,
                    nextPointY);
        }
        return path;
    }

    /**
     * Given an index in datapoints, it will make sure the the returned index is
     * within the array
     * @param i integer
     * @return returned integer
     */
    private int si(int i) {
        if (i > datapoints.length - 1) {
            return datapoints.length - 1;
        } else if (i < 0) {
            return 0;
        }
        return i;
    }

    /**
     * Returns the maximum value in the user data
     * @param array takes in the dynamics array
     * @return returns the MAX value
     */
    private float getMax(Dynamics[] array) {
        float max = array[0].getPosition();
        for (int i = 1; i < array.length; i++) {
            if (array[i].getPosition() > max) {
                max = array[i].getPosition();
            }
        }
        return max;
    }

    /**
     * Return the Y position of the data received
     * @param value float value
     * @param maxValue Max value
     * @return returned float y value
     */
    private float getYPos(float value, float maxValue) {
        float height = getHeight() - getPaddingTop() - getPaddingBottom();

        // scale it to the view size
        value = (value / maxValue) * height;

        // invert it so that higher values have lower y
        value = height - value;

        // offset it to adjust for padding
        value += getPaddingTop();

        return value;
    }

    /**
     *  Return the Y position of the data received
     * @param value float value
     * @return returned float x value
     */
    private float getXPos(float value) {
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float maxValue = datapoints.length - 1;

        // scale it to the view size
        value = (value / maxValue) * width;

        // offset it to adjust for padding
        value += getPaddingLeft();

        return value;
    }

}
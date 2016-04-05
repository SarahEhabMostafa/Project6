/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.sarahehabm.common.Utility;
import com.sarahehabm.common.WearableConstants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = SunshineWatchFaceService.class.getSimpleName();

    private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
    private static final Typeface THIN_TYPEFACE = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private String minTemp, maxTemp;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private Calendar calendar;
        private Date date;
        private Resources resources;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();

                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        private final Handler updateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                invalidate();

                if(shouldTimerBeRunning()) {
                    long timeMillis = System.currentTimeMillis();
                    long delayMillis = INTERACTIVE_UPDATE_RATE_MS
                            - (timeMillis % INTERACTIVE_UPDATE_RATE_MS);
                    updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMillis);
                }
            }
        };

        Paint mBackgroundPaint;
        Paint mTimePaint, mDatePaint, mLowPaint, mHighPaint;

        boolean mAmbient;

        Time mTime;

        float mXOffset;
        float mYOffset;
        private float mTextSpacingHeight;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN | WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            resources = SunshineWatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mTextSpacingHeight = resources.getDimension(R.dimen.interactive_text_size);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.digital_background));

            mTimePaint = new Paint();
            mTimePaint = createTextPaint(resources.getColor(R.color.white));
            mDatePaint = new Paint();
            mDatePaint = createTextPaint(resources.getColor(R.color.color_date));
            mDatePaint.setTypeface(THIN_TYPEFACE);

            mLowPaint = new Paint();
            mLowPaint = createTextPaint(resources.getColor(R.color.white));
            mHighPaint = new Paint();
            mHighPaint = createTextPaint(resources.getColor(R.color.white));
            mHighPaint.setTypeface(THIN_TYPEFACE);

            calendar = Calendar.getInstance();
            mTime = new Time();
            date = new Date();
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float temperatureTextSize = resources.getDimension(isRound
                    ? R.dimen.temperature_text_size_round: R.dimen.temperature_text_size);

            mTimePaint.setTextSize(textSize);

            float dateTextSize = resources.getDimension(R.dimen.date_text_size);
            mDatePaint.setTextSize(dateTextSize);
            mLowPaint.setTextSize(temperatureTextSize);
            mHighPaint.setTextSize(temperatureTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTimePaint.setAntiAlias(!inAmbientMode);
                    mDatePaint.setAntiAlias(!inAmbientMode);
                    mLowPaint.setAntiAlias(!inAmbientMode);
                    mHighPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if(mAmbient)
                canvas.drawColor(Color.BLACK);
            else
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
            date = calendar.getTime();
            String text = mAmbient
                    ? String.format("%d:%02d", mTime.hour, mTime.minute)
                    : String.format("%d:%02d:%02d", mTime.hour, mTime.minute, mTime.second);

            float x = (canvas.getWidth()/2) - (mTimePaint.measureText(text)/2);
            canvas.drawText(text, x, mYOffset, mTimePaint);

            String dateText = new SimpleDateFormat("EEE, dd MMM yyyy").format(date).toUpperCase();
            x = (canvas.getWidth()/2) - (mDatePaint.measureText(dateText)/2);
            canvas.drawText(dateText, x, mYOffset + mTextSpacingHeight, mDatePaint);

            float y = mYOffset+(resources.getDimension(R.dimen.date_text_size)*2);
            canvas.drawLine(mXOffset, y, canvas.getWidth()-mXOffset, y, mDatePaint);

            minTemp = Utility.getString(SunshineWatchFaceService.this, WearableConstants.MIN_TEMP);
            maxTemp = Utility.getString(SunshineWatchFaceService.this, WearableConstants.MAX_TEMP);
            String bitmapData = Utility.getString(SunshineWatchFaceService.this, WearableConstants.RES_ID);

            if (minTemp != null && maxTemp != null && bitmapData != null) {
                Bitmap bitmap = Utility.StringToBitMap(bitmapData);

                if(bitmap!=null) {
                    int width = bounds.width() / 4, height = bounds.height() / 4;
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
                    canvas.drawBitmap(resizedBitmap, mXOffset, y + 5, mTimePaint);
                }

                x = (canvas.getWidth()/2) - (mLowPaint.measureText(minTemp)/2);
                y += (resources.getDimension(R.dimen.date_text_size)*2.5);
                canvas.drawText(minTemp, x, y, mLowPaint);
                x += mLowPaint.measureText(minTemp) + 10;
                canvas.drawText(maxTemp, x, y, mHighPaint);
            } else {
                x = (canvas.getWidth()/2) - (mDatePaint.measureText("No data found")/2);
                y += (resources.getDimension(R.dimen.date_text_size)*2);
                canvas.drawText("No data found", x, y, mDatePaint);
            }
        }

        /**
         * Starts the {@link #updateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #updateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}

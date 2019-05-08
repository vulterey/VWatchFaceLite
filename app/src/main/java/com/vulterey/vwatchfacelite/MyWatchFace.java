/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.vulterey.vwatchfacelite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand, battery meter and date.
 * In ambient mode, the second hand isn't shown.
 * On devices with low-bit ambient mode, the content of the watchface are changed for
 * their low-bit versions.
 */
public class MyWatchFace extends CanvasWatchFaceService {

    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    private boolean mRegisteredTimeZoneReceiver = false;

    private Calendar mCalendar;

    private Paint mHandPaint;
    private Paint mDatePaint;

    //Ambient Paints

    private Paint mHandPaintAmb;
    private Paint mDatePaintAmb;

    //Ambient Paints end

    public Typeface mDateTypeface;

    private Bitmap mBackgroundBitmap;
    private Bitmap mHourHand;
    private Bitmap mMinuteHand;
    private Bitmap mSecondHand;
    private Bitmap mBattHand;

    private boolean mAmbient;

    //Ambient mode graphics

    private Bitmap mBackgroundBitmapAmb;
    private Bitmap mHourHandAmb;
    private Bitmap mMinuteHandAmb;
    private Bitmap mBattHandAmb;

    //Ambient mode graphics end

    private float mCenterX;
    private float mCenterY;
    private float anchXbattHand;
    private float anchYbattHand;
    private float dateXpoint;
    private float dateYpoint;

    private float xHourHand;
    private float yHourHand;
    private float xMinHand;
    private float yMinHand;
    private float xSecHand;
    private float ySecHand;
    private float xBattHand;
    private float yBattHand;

    public String today;


    //Battery

    private int batteryLevel = 50;
    private double batteryHandAngle = 0;

    //Battery end

    // Get today date and convert it to string
    private void getDate() {
        int date = mCalendar.get(Calendar.DAY_OF_MONTH);

        // If date is less than 10 or is equal to 11
        // for aesthetic purposes format date string with leading zero
        if (date < 10 || date == 11) {
            NumberFormat f = new DecimalFormat(" 0");
            today = String.valueOf(f.format(date));
        } else {
            today = String.valueOf(date);
        }
    }

    @Override
    public Engine onCreateEngine() {

        //Battery

        getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context battContext, Intent battIntent) {
                batteryLevel = (int) (100 * battIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) /
                        ((float) (battIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1))));
                calcBatteryAngle();
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        //Battery end

        return new Engine();
    }

    //Battery

    /* Calculate an angle for rotation of the battery hand to represent actual level of the battery.
     * As the battery hand make half of the circle, the range for it is 180 degrees (-90 to +90 with 0 in the half point).
     * Battery charge range is from 0-100,
     * hence 180 / 100 = 1.8.
     * So, each 1% of the battery correspond to 1.8 degree.
     * So, to calculate battery hand angle, the value of the battery charge level need to be multiplied by 1.8,
     * but as the battery hand range is from -90 to 90, from above result it need to be deducted 90 degrees.
     * For example:
     * For 0% charge: 0*1.8 = 0, 0-90 = -90 (hand will go to bottom left on the scale)
     * For 50% charge: 50*1,8 = 90, 90-90 = 0 (hand will go to the centre of the scale)
     * For 100% charge: 100*1,8 = 180, 180-90 = 90 (hand will go to the bottom right of the scale).
     */

    private void calcBatteryAngle() {
        batteryHandAngle = (batteryLevel * 1.8f)-90f;
    }

    //Battery end

    private class Engine extends CanvasWatchFaceService.Engine {

        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (R.id.message_update == message.what) {
                    invalidate();
                    if (shouldTimerBeRunning()) {
                        long timeMs = System.currentTimeMillis();
                        long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                        mUpdateTimeHandler.sendEmptyMessageDelayed(R.id.message_update, delayMs);
                    }
                }
            }
        };

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this).build());

            final int backgroundResId = R.drawable.background;
            final int hour_handResId = R.drawable.hour_hand;
            final int minute_handResId = R.drawable.minute_hand;
            final int second_handResId = R.drawable.second_hand;
            final int battery_handResId = R.drawable.batt_hand;

            mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), backgroundResId);
            mHourHand = BitmapFactory.decodeResource(getResources(), hour_handResId);
            mMinuteHand = BitmapFactory.decodeResource(getResources(), minute_handResId);
            mSecondHand = BitmapFactory.decodeResource(getResources(), second_handResId);
            mBattHand = BitmapFactory.decodeResource(getResources(), battery_handResId);

            mHandPaint = new Paint();
            mHandPaint.setAntiAlias(false);
            mHandPaint.setFilterBitmap(true);

            mDateTypeface = Typeface.createFromAsset(getAssets(),"bahnschrift.ttf");

            mDatePaint = new Paint();
            mDatePaint.setColor(0xFFD4B682);
            mDatePaint.setAntiAlias(true);
            mDatePaint.setTextSize(29f);
            mDatePaint.setTypeface(mDateTypeface);

            mCalendar = Calendar.getInstance();

            //Ambient mode graphics

            final int backgroundResIdAmb = R.drawable.background_amb;
            final int hour_handResIdAmb = R.drawable.hour_hand_amb;
            final int minute_handResIdAmb = R.drawable.minute_hand_amb;
            final int battery_handResIdAmb = R.drawable.batt_hand_amb;


            mBackgroundBitmapAmb = BitmapFactory.decodeResource(getResources(), backgroundResIdAmb);
            mHourHandAmb = BitmapFactory.decodeResource(getResources(), hour_handResIdAmb);
            mMinuteHandAmb = BitmapFactory.decodeResource(getResources(), minute_handResIdAmb);
            mBattHandAmb = BitmapFactory.decodeResource(getResources(), battery_handResIdAmb);

            mHandPaintAmb = new Paint();
            mHandPaintAmb.setAntiAlias(false);
            mHandPaintAmb.setFilterBitmap(false);

            mDateTypeface = Typeface.createFromAsset(getAssets(),"bahnschrift.ttf");

            mDatePaintAmb = new Paint();
            mDatePaintAmb.setColor(0xFFFFFFFF);
            mDatePaintAmb.setAntiAlias(false);
            mDatePaintAmb.setTextSize(29f);
            mDatePaintAmb.setTypeface(mDateTypeface);

            //Ambient mode graphics end

            getDate();
        }


        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(R.id.message_update);
            super.onDestroy();
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
                invalidate();
            }

            /*
             * Whether the timer should be running depends on whether we're visible (as well as
             * whether we're in ambient mode), so we may need to start or stop the timer.
             */
            updateTimer();
            getDate();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen.
             * Ignore the window insets so that, on round watches
             * with a "chin", the watch face is centered on the entire screen,
             * not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            float mScale = ((float) width) / (float) mBackgroundBitmap.getWidth();


            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * mScale),
                    (int) (mBackgroundBitmap.getHeight() * mScale), true);

            mHourHand = Bitmap.createScaledBitmap(mHourHand,
                    (int) (mHourHand.getWidth() * mScale),
                    (int) (mHourHand.getHeight() * mScale), true);

            mMinuteHand = Bitmap.createScaledBitmap(mMinuteHand,
                    (int) (mMinuteHand.getWidth() * mScale),
                    (int) (mMinuteHand.getHeight() * mScale), true);

            mSecondHand = Bitmap.createScaledBitmap(mSecondHand,
                    (int) (mSecondHand.getWidth() * mScale),
                    (int) (mSecondHand.getHeight() * mScale), true);

            mBattHand = Bitmap.createScaledBitmap(mBattHand,
                    (int) (mBattHand.getWidth() * mScale),
                    (int) (mBattHand.getHeight() * mScale), true);

            //Ambient mode scaling

            mBackgroundBitmapAmb = Bitmap.createScaledBitmap(mBackgroundBitmapAmb,
                    (int) (mBackgroundBitmapAmb.getWidth() * mScale),
                    (int) (mBackgroundBitmapAmb.getHeight() * mScale), true);

            mHourHandAmb = Bitmap.createScaledBitmap(mHourHandAmb,
                    (int) (mHourHandAmb.getWidth() * mScale),
                    (int) (mHourHandAmb.getHeight() * mScale), true);

            mMinuteHandAmb = Bitmap.createScaledBitmap(mMinuteHandAmb,
                    (int) (mMinuteHandAmb.getWidth() * mScale),
                    (int) (mMinuteHandAmb.getHeight() * mScale), true);

            mBattHandAmb = Bitmap.createScaledBitmap(mBattHandAmb,
                    (int) (mBattHandAmb.getWidth() * mScale),
                    (int) (mBattHandAmb.getHeight() * mScale), true);

            //Ambient mode scaling end

            /*
             * Place the hands in the center of the screen.
             * Take calculated center of the screen and
             * calculate center of the hand base on the size of it and its pivot point.
             * For battery hand calculate pivot point base on battery scale on the watch face.
             */

            xHourHand = mCenterX - (mHourHand.getWidth() / 2f);
            yHourHand = (mCenterY - (mHourHand.getHeight() * 6 / 7f));

            xMinHand = mCenterX - (mMinuteHand.getWidth() / 2f);
            yMinHand = (mCenterY - (mMinuteHand.getHeight() * 7 / 10f));

            xSecHand = mCenterX - (mSecondHand.getWidth() / 2f);
            ySecHand = (mCenterY - (mSecondHand.getHeight() * 29 / 40f));

            xBattHand = width * (14 / 39f);
            yBattHand = height * (21 / 39f);

            // Calculate anchor point for the battery hand, based on size of the screen.

            anchXbattHand = xBattHand + (mBattHand.getWidth() / 2f);
            anchYbattHand = (yBattHand + (mBattHand.getHeight() * 7 / 10f));


            // Calculate anchor point for the date display, based on size of the screen.

            dateXpoint = width * (51 / 78f);
            dateYpoint = height * (55 / 78f);
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            // Draw the background.
            if (!mAmbient) {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
            }
            else {
                canvas.drawBitmap(mBackgroundBitmapAmb, 0, 0, null);
            }

            // Save the canvas state before we begin to rotate it
            canvas.save();

            // Display today date on the watch face.
            if (!mAmbient) {
                canvas.drawText(today, dateXpoint, dateYpoint, mDatePaint);
            }
            else {
                canvas.drawText(today, dateXpoint, dateYpoint, mDatePaintAmb);
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            // Battery hand:

            // Rotate canvas to draw on it the battery hand
            canvas.rotate((float) batteryHandAngle, anchXbattHand, anchYbattHand);

            // Draw battery hand on rotated canvas
            if (!mAmbient) {
                canvas.drawBitmap(mBattHand, xBattHand, yBattHand, mHandPaint);
            }
            else {
                canvas.drawBitmap(mBattHandAmb, xBattHand, yBattHand, mHandPaintAmb);
            }

            // Restore canvas to original orientation
            canvas.restore();

            // Save the canvas state before we begin to rotate it
            canvas.save();


            // Hour hand:

            // Rotate canvas to draw on it the hour hand
            canvas.rotate(hoursRotation, mCenterX, mCenterY);

            // Draw hour hand on rotated canvas
            if (!mAmbient) {
                canvas.drawBitmap(mHourHand, xHourHand, yHourHand, mHandPaint);
            }
            else {
                canvas.drawBitmap(mHourHandAmb, xHourHand, yHourHand, mHandPaintAmb);
            }


            // Minute hand:

            // Rotate canvas to draw on it the minute hand
            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);

            // Draw minute hand on rotated canvas
            if (!mAmbient) {
                canvas.drawBitmap(mMinuteHand, xMinHand, yMinHand, mHandPaint);
            }
            else {
                canvas.drawBitmap(mMinuteHandAmb, xMinHand, yMinHand, mHandPaintAmb);
            }


            // Second hand:

            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawBitmap(mSecondHand, xSecHand, ySecHand, mHandPaint);
            }

            // Restore the canvas original orientation.
            canvas.restore();

            getDate();
        }



        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /*
             * Whether the timer should be running depends on whether we're visible
             * (as well as whether we're in ambient mode),
             * so we may need to start or stop the timer.
             */
            updateTimer();
            getDate();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(R.id.message_update);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(R.id.message_update);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}

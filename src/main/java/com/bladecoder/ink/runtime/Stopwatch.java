package com.bladecoder.ink.runtime;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Stopwatch {
	// constants
	private static final long nsPerTick = 100;
	private static final long nsPerMs = 1000000;
	private static final long nsPerSs = 1000000000;
	private static final long nsPerMm = 60000000000L;
	private static final long nsPerHh = 3600000000000L;

	private long startTime = 0;
	private long stopTime = 0;
	private boolean running = false;

	/**
	 * Starts measuring elapsed time for an interval.
	 */
	public void start() {
		this.startTime = System.nanoTime();
		this.running = true;
	}

	/**
	 * Stops measuring elapsed time for an interval.
	 */
	public void stop() {
		this.stopTime = System.nanoTime();
		this.running = false;
	}

	/**
	 * Stops time interval measurement and resets the elapsed time to zero.
	 */
	public void reset() {
		this.startTime = 0;
		this.stopTime = 0;
		this.running = false;
	}

	/**
	 * Gets the total elapsed time measured by the current instance, in nanoseconds.
	 * 1 Tick = 100 nanoseconds
	 */
	public long getElapsedTicks() {
		long elapsed;
		if (running) {
			elapsed = (System.nanoTime() - startTime);
		} else {
			elapsed = (stopTime - startTime);
		}
		return elapsed / nsPerTick;
	}

	/**
	 * Gets the total elapsed time measured by the current instance, in
	 * milliseconds. 10000 Ticks = 1 millisecond (1000000 nanoseconds)
	 */
	public long getElapsedMilliseconds() {
		long elapsed;
		if (running) {
			elapsed = (System.nanoTime() - startTime);
		} else {
			elapsed = (stopTime - startTime);
		}
		return elapsed / nsPerMs;
	}

	/**
	 * Gets the total elapsed time measured by the current instance, in seconds.
	 * 10000000 Ticks = 1 second (1000 milliseconds)
	 */
	public long getElapsedSeconds() {
		long elapsed;
		if (running) {
			elapsed = (System.nanoTime() - startTime);
		} else {
			elapsed = (stopTime - startTime);
		}
		return elapsed / nsPerSs;
	}

	/**
	 * Gets the total elapsed time measured by the current instance, in minutes.
	 * 600000000 Ticks = 1 minute (60 seconds)
	 */
	public long getElapsedMinutes() {
		long elapsed;
		if (running) {
			elapsed = (System.nanoTime() - startTime);
		} else {
			elapsed = (stopTime - startTime);
		}
		return elapsed / nsPerMm;
	}

	/**
	 * Gets the total elapsed time measured by the current instance, in hours.
	 * 36000000000 Ticks = 1 hour (60 minutes)
	 */
	public long getElapsedHours() {
		long elapsed;
		if (running) {
			elapsed = (System.nanoTime() - startTime);
		} else {
			elapsed = (stopTime - startTime);
		}
		return elapsed / nsPerHh;
	}

	/**
	 * Gets the total elapsed time with format 00:00:00.0000000 = 00:mm:ss.SSS +
	 * 9999 Ticks
	 */
	public String getElapsed() {
		String timeFormatted = "";
		timeFormatted = this.formatTime(this.getElapsedTicks());
		return timeFormatted;
	}

	/**
	 * Gets the total elapsed time with format 00:00:00.0000000 = 00:mm:ss.SSS +
	 * #### Ticks
	 * 
	 * @param elapsedTicks
	 *            elapsed ticks between start and stop nano time
	 */
	private String formatTime(final long elapsedTicks) {
		String formattedTime = "";
		// should be hh:mm:ss.SSS, but 00 starts with 01
		SimpleDateFormat formatter = new SimpleDateFormat("00:mm:ss.SSS");
		Calendar calendar = Calendar.getInstance();

		if (elapsedTicks <= 9999) {
			calendar.setTimeInMillis(0);
			formattedTime = formatter.format(calendar.getTime()) + String.valueOf(String.format("%04d", elapsedTicks));
		} else {
			calendar.setTimeInMillis(elapsedTicks * nsPerTick / nsPerMs);
			String formattedTicks = String.format("%07d", elapsedTicks);
			formattedTicks = formattedTicks.substring(formattedTicks.length() - 4);
			formattedTime = formatter.format(calendar.getTime()) + formattedTicks;
		}
		return formattedTime;
	}

}

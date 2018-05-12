package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.bladecoder.ink.runtime.Path.Component;

/**
 * Simple ink profiler that logs every instruction in the story and counts
 * frequency and timing. To use:
 * 
 * var profiler = story.StartProfiling(),
 * 
 * (play your story for a bit)
 * 
 * var reportStr = profiler.Report();
 * 
 * story.EndProfiling();
 * 
 */
public class Profiler {
	private Stopwatch continueWatch = new Stopwatch();
	private Stopwatch stepWatch = new Stopwatch();
	private Stopwatch snapWatch = new Stopwatch();
	private Stopwatch restoreWatch = new Stopwatch();

	private double continueTotal;
	private double snapTotal;
	private double stepTotal;
	private double restoreTotal;

	private String[] currStepStack;
	private StepDetails currStepDetails;
	private ProfileNode rootNode;
	private int numContinues;

	private class StepDetails {
		public String type;
		public String detail;
		public double time;

		StepDetails(String type, String detail, double time) {
			this.type = type;
			this.detail = detail;
			this.time = time;
		}
	}

	private List<StepDetails> stepDetails = new ArrayList<StepDetails>();

	ProfileNode getRootNode() {
		return rootNode;
	}

	Profiler() {
		rootNode = new ProfileNode();
	}

	/**
	 * Generate a printable report based on the data recording during profiling.
	 */
	public String report() {
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("%d CONTINUES / LINES:\n", numContinues));
		sb.append(String.format("TOTAL TIME: %s\n", formatMillisecs(continueTotal)));
		sb.append(String.format("SNAPSHOTTING: %s\n", formatMillisecs(snapTotal)));
		sb.append(String.format("RESTORING: %s\n", formatMillisecs(restoreTotal)));
		sb.append(
				String.format("OTHER: %s\n", formatMillisecs(continueTotal - (stepTotal + snapTotal + restoreTotal))));
		sb.append(rootNode.toString());

		return sb.toString();
	}

	void preContinue() {
		continueWatch.reset();
		continueWatch.start();
	}

	void postContinue() {
		continueWatch.stop();
		continueTotal += millisecs(continueWatch);
		numContinues++;
	}

	void preStep() {
		currStepStack = null;
		stepWatch.reset();
		stepWatch.start();
	}

	void step(CallStack callstack) {
		stepWatch.stop();

		String[] stack = new String[callstack.getElements().size()];
		for (int i = 0; i < stack.length; i++) {
			Path objPath = callstack.getElements().get(i).getCurrentRTObject().getPath();
			String stackElementName = "";

			for (int c = 0; c < objPath.getComponentCount(); c++) {
				Component comp = objPath.getComponent(c);
				if (!comp.isIndex()) {
					stackElementName = comp.getName();
					break;
				}
			}

			stack[i] = stackElementName;
		}

		currStepStack = stack;

		RTObject currObj = callstack.getCurrentElement().getCurrentRTObject() != null
				? callstack.getCurrentElement().getCurrentRTObject()
				: callstack.getCurrentElement().currentContainer;

		currStepDetails = new StepDetails(currObj.getClass().getSimpleName(), currObj.toString(), 0f);

		stepWatch.start();
	}

	void postStep() {
		stepWatch.stop();

		double duration = millisecs(stepWatch);
		stepTotal += duration;

		rootNode.addSample(currStepStack, duration);

		currStepDetails.time = duration;
		stepDetails.add(currStepDetails);
	}

	/**
	 * Generate a printable report specifying the average and maximum times spent
	 * stepping over different internal ink instruction types.
	 * This report type is primarily used to profile the ink engine itself rather
	 * than your own specific ink.
	 */
	String stepLengthReport() {
		StringBuilder sb = new StringBuilder();

		HashMap<String, Double> typeToDetails = new HashMap<String, Double>();

		// average group by s.type
		for (StepDetails sd : stepDetails) {
			if (typeToDetails.containsKey(sd.type))
				continue;

			String type = sd.type;
			double avg = 0f;
			float num = 0;

			for (StepDetails sd2 : stepDetails) {
				if (type.equals(sd2.type)) {
					num++;
					avg += sd2.time;
				}
			}

			avg = avg / num;

			typeToDetails.put(sd.type, avg);
		}

		// sort by average
		List<Entry<String, Double>> averageStepTimes = new LinkedList<Entry<String, Double>>(typeToDetails.entrySet());
		
		Collections.sort(averageStepTimes, new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
				return (int) (o1.getValue() - o2.getValue());
			}
		});

		// join times
		sb.append("AVERAGE STEP TIMES: ");
		for(int i = 0; i < averageStepTimes.size(); i++) {
			sb.append(averageStepTimes.get(i).getKey());
			sb.append(": ");
			sb.append(averageStepTimes.get(i).getValue());
			sb.append("ms");
			
			if(i != averageStepTimes.size() -1)
				sb.append(',');
		}

		sb.append('\n');

		List<String> maxStepTimes = new ArrayList<String>();

		stepDetails.sort(new Comparator<StepDetails>() {

			@Override
			public int compare(StepDetails o1, StepDetails o2) {
				if (o1.time < o2.time)
					return -1;
				if (o1.time > o2.time)
					return 1;
				return 0;
			}
		});

		for (int i = 0; i < 100; i++) {
			StepDetails d = stepDetails.get(i);
			maxStepTimes.add(d.detail + ":" + d.time + "ms");
		}

		sb.append("MAX STEP TIMES: " + stringJoin("\n", maxStepTimes));
		sb.append('\n');

		return sb.toString();
	}

	void preSnapshot() {
		snapWatch.reset();
		snapWatch.start();
	}

	void postSnapshot() {
		snapWatch.stop();
		snapTotal += millisecs(snapWatch);
	}

	void preRestore() {
		restoreWatch.reset();
		restoreWatch.start();
	}

	void postRestore() {
		restoreWatch.stop();
		restoreTotal += millisecs(restoreWatch);
	}

	double millisecs(Stopwatch watch) {
		return watch.getElapsedMilliseconds();
	}

	static String formatMillisecs(double num) {
		if (num > 5000) {
			return String.format("%.1f secs", num / 1000.0);
		}
		if (num > 1000) {
			return String.format("%.2f secs", num / 1000.0);
		} else if (num > 100) {
			return String.format("%.0f ms", num);
		} else if (num > 1) {
			return String.format("%.1f ms", num);
		} else if (num > 0.01) {
			return String.format("%.3f ms", num);
		} else {
			return String.format("%.0f ms", num);
		}
	}

	private String stringJoin(String conjunction, List<String> list) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String item : list) {
			if (first)
				first = false;
			else
				sb.append(conjunction);
			sb.append(item);
		}
		return sb.toString();
	}
}
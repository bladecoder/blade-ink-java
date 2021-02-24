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

	private double continueTotal;
	private double snapTotal;
	private double stepTotal;

	private String[] currStepStack;
	private StepDetails currStepDetails;
	private ProfileNode rootNode;
	private int numContinues;

	private class StepDetails {
		public String type;
		public RTObject obj;
		public double time;

		StepDetails(String type, RTObject obj, double time) {
			this.type = type;
			this.obj = obj;
			this.time = time;
		}
	}

	private List<StepDetails> stepDetails = new ArrayList<>();

	/**
	 * The root node in the hierarchical tree of recorded ink timings.
	 */
	public ProfileNode getRootNode() {
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
		sb.append(String.format("OTHER: %s\n", formatMillisecs(continueTotal - (stepTotal + snapTotal))));
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

			String stackElementName = "";

			if (!callstack.getElements().get(i).currentPointer.isNull()) {
				Path objPath = callstack.getElements().get(i).currentPointer.getPath();

				for (int c = 0; c < objPath.getLength(); c++) {
					Component comp = objPath.getComponent(c);
					if (!comp.isIndex()) {
						stackElementName = comp.getName();
						break;
					}
				}
			}

			stack[i] = stackElementName;
		}

		currStepStack = stack;

		RTObject currObj = callstack.getCurrentElement().currentPointer.resolve();

		String stepType = null;
		ControlCommand controlCommandStep = currObj instanceof ControlCommand ? (ControlCommand) currObj : null;
		if (controlCommandStep != null)
			stepType = controlCommandStep.getCommandType().toString() + " CC";
		else
			stepType = currObj.getClass().getSimpleName();

		currStepDetails = new StepDetails(stepType, currObj, 0f);

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
	 * stepping over different internal ink instruction types. This report type is
	 * primarily used to profile the ink engine itself rather than your own specific
	 * ink.
	 */
	public String stepLengthReport() {
		StringBuilder sb = new StringBuilder();

		sb.append("TOTAL: " + rootNode.getTotalMillisecs() + "ms\n");

		// AVERAGE STEP TIMES
		HashMap<String, Double> typeToDetails = new HashMap<>();

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
		List<Entry<String, Double>> averageStepTimes = new LinkedList<>(typeToDetails.entrySet());

		Collections.sort(averageStepTimes, new Comparator<Entry<String, Double>>() {
			@Override
			public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
				return (int) (o1.getValue() - o2.getValue());
			}
		});

		// join times
		sb.append("AVERAGE STEP TIMES: ");
		for (int i = 0; i < averageStepTimes.size(); i++) {
			sb.append(averageStepTimes.get(i).getKey());
			sb.append(": ");
			sb.append(averageStepTimes.get(i).getValue());
			sb.append("ms");

			if (i != averageStepTimes.size() - 1)
				sb.append(',');
		}

		sb.append('\n');

		// ACCUMULATED STEP TIMES
		typeToDetails.clear();

		// average group by s.type
		for (StepDetails sd : stepDetails) {
			if (typeToDetails.containsKey(sd.type))
				continue;

			String type = sd.type;
			double sum = 0f;

			for (StepDetails sd2 : stepDetails) {
				if (type.equals(sd2.type)) {
					sum += sd2.time;
				}
			}

			typeToDetails.put(sd.type + " (x" + typeToDetails.size() + ")", sum);
		}

		// sort by average
		List<Entry<String, Double>> accumStepTimes = new LinkedList<>(typeToDetails.entrySet());

		Collections.sort(accumStepTimes, new Comparator<Entry<String, Double>>() {
			@Override
			public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
				return (int) (o1.getValue() - o2.getValue());
			}
		});

		// join times
		sb.append("ACCUMULATED STEP TIMES: ");
		for (int i = 0; i < accumStepTimes.size(); i++) {
			sb.append(accumStepTimes.get(i).getKey());
			sb.append(": ");
			sb.append(accumStepTimes.get(i).getValue());

			if (i != accumStepTimes.size() - 1)
				sb.append(',');
		}

		sb.append('\n');

		return sb.toString();
	}

	/**
	 * Create a large log of all the internal instructions that were evaluated while
	 * profiling was active. Log is in a tab-separated format, for easy loading into
	 * a spreadsheet application.
	 */
	public String megalog() {
		StringBuilder sb = new StringBuilder();

		sb.append("Step type\tDescription\tPath\tTime\n");

		for (StepDetails step : stepDetails) {
			sb.append(step.type);
			sb.append("\t");
			sb.append(step.obj.toString());
			sb.append("\t");
			sb.append(step.obj.getPath());
			sb.append("\t");
			sb.append(Double.toString(step.time));
			sb.append('\n');
		}

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
}
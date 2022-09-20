package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.bladecoder.ink.runtime.SimpleJson.Writer;

public class Flow {
	public String name;
	public CallStack callStack;
	public List<RTObject> outputStream;
	public List<Choice> currentChoices;

	public Flow(String name, Story story) {
		this.name = name;
		this.callStack = new CallStack(story);
		this.outputStream = new ArrayList<>();
		this.currentChoices = new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	public Flow(String name, Story story, HashMap<String, Object> jObject) throws Exception {
		this.name = name;
		this.callStack = new CallStack(story);
		this.callStack.setJsonToken((HashMap<String, Object>) jObject.get("callstack"), story);
		this.outputStream = Json.jArrayToRuntimeObjList((List<Object>) jObject.get("outputStream"));
		this.currentChoices = Json.jArrayToRuntimeObjList((List<Object>) jObject.get("currentChoices"));

		// choiceThreads is optional
		Object jChoiceThreadsObj = jObject.get("choiceThreads");

		loadFlowChoiceThreads((HashMap<String, Object>) jChoiceThreadsObj, story);
	}

	public void writeJson(SimpleJson.Writer writer) throws Exception {
		writer.writeObjectStart();

		writer.writeProperty("callstack", new SimpleJson.InnerWriter() {
			@Override
			public void write(Writer w) throws Exception {
				callStack.writeJson(w);
			}

		});

		writer.writeProperty("outputStream", new SimpleJson.InnerWriter() {
			@Override
			public void write(Writer w) throws Exception {
				Json.writeListRuntimeObjs(w, outputStream);
			}

		});

		// choiceThreads: optional
		// Has to come BEFORE the choices themselves are written out
		// since the originalThreadIndex of each choice needs to be set
		boolean hasChoiceThreads = false;
		for (Choice c : currentChoices) {
			c.originalThreadIndex = c.getThreadAtGeneration().threadIndex;

			if (callStack.getThreadWithIndex(c.originalThreadIndex) == null) {
				if (!hasChoiceThreads) {
					hasChoiceThreads = true;
					writer.writePropertyStart("choiceThreads");
					writer.writeObjectStart();
				}

				writer.writePropertyStart(c.originalThreadIndex);
				c.getThreadAtGeneration().writeJson(writer);
				writer.writePropertyEnd();
			}
		}

		if (hasChoiceThreads) {
			writer.writeObjectEnd();
			writer.writePropertyEnd();
		}

		writer.writeProperty("currentChoices", new SimpleJson.InnerWriter() {
			@Override
			public void write(Writer w) throws Exception {
				w.writeArrayStart();
				for (Choice c : currentChoices)
					Json.writeChoice(w, c);
				w.writeArrayEnd();
			}

		});

		writer.writeObjectEnd();
	}

	// Used both to load old format and current
	@SuppressWarnings("unchecked")
	public void loadFlowChoiceThreads(HashMap<String, Object> jChoiceThreads, Story story) throws Exception {
		for (Choice choice : currentChoices) {
			CallStack.Thread foundActiveThread = callStack.getThreadWithIndex(choice.originalThreadIndex);
			if (foundActiveThread != null) {
				choice.setThreadAtGeneration(foundActiveThread.copy());
			} else {
				HashMap<String, Object> jSavedChoiceThread = (HashMap<String, Object>) jChoiceThreads
						.get(Integer.toString(choice.originalThreadIndex));
				choice.setThreadAtGeneration(new CallStack.Thread(jSavedChoiceThread, story));
			}
		}
	}
}

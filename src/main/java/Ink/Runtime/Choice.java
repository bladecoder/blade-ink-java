package Ink.Runtime;

/**
 * A generated Choice from the story. A single ChoicePoint in the Story could
 * potentially generate different Choices dynamically dependent on state, so
 * they're separated.
 */
public class Choice extends RTObject{
	/**
	 * The main text to presented to the player for this Choice.
	 */
	private String __text = new String();

	public String gettext() {
		return __text;
	}

	public void settext(String value) {
		__text = value;
	}

	/**
	 * The target path that the Story should be diverted to if this Choice is
	 * chosen.
	 */
	public String getpathStringOnChoice() throws Exception {
		return getchoicePoint().getpathStringOnChoice();
	}

	/**
    * The original index into currentChoices list on the Story when
    * this Choice was generated, for convenience.
    */
    private int __index = 0;

	public int getindex() {
		return __index;
	}

	public void setindex(int value) {
		__index = value;
	}

	private ChoicePoint __choicePoint;

	public ChoicePoint getchoicePoint() {
		return __choicePoint;
	}

	public void setchoicePoint(ChoicePoint value) {
		__choicePoint = value;
	}

	private Ink.Runtime.CallStack.Thread __threadAtGeneration;

	public Ink.Runtime.CallStack.Thread getthreadAtGeneration() {
		return __threadAtGeneration;
	}

	public void setthreadAtGeneration(Ink.Runtime.CallStack.Thread value) {
		__threadAtGeneration = value;
	}

	public int originalThreadIndex = 0;
    // Only used temporarily for loading/saving from JSON
	public String originalChoicePath = new String();

	public Choice() throws Exception {
	}

	public Choice(ChoicePoint choice) throws Exception {
		this.setchoicePoint(choice);
	}

}

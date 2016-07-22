package Ink.Runtime;

/**
* The ChoicePoint represents the point within the Story where
* a Choice instance gets generated. The distinction is made
* because the text of the Choice can be dynamically generated.
*/
public class ChoicePoint extends RTObject 
{
    private Path __pathOnChoice;
    public Path getpathOnChoice() {
        return __pathOnChoice;
    }

    public void setpathOnChoice(Path value) {
        __pathOnChoice = value;
    }

    public Container getchoiceTarget() throws Exception {
        return this.ResolvePath(getpathOnChoice()) instanceof Container ? (Container)this.ResolvePath(getpathOnChoice()) : (Container)null;
    }

    public String getpathStringOnChoice() throws Exception {
        return CompactPathString(getpathOnChoice());
    }

    public void setpathStringOnChoice(String value) throws Exception {
        setpathOnChoice(new Path(value));
    }

    private boolean __hasCondition;
    public boolean gethasCondition() {
        return __hasCondition;
    }

    public void sethasCondition(boolean value) {
        __hasCondition = value;
    }

    private boolean __hasStartContent;
    public boolean gethasStartContent() {
        return __hasStartContent;
    }

    public void sethasStartContent(boolean value) {
        __hasStartContent = value;
    }

    private boolean __hasChoiceOnlyContent;
    public boolean gethasChoiceOnlyContent() {
        return __hasChoiceOnlyContent;
    }

    public void sethasChoiceOnlyContent(boolean value) {
        __hasChoiceOnlyContent = value;
    }

    private boolean __onceOnly;
    public boolean getonceOnly() {
        return __onceOnly;
    }

    public void setonceOnly(boolean value) {
        __onceOnly = value;
    }

    private boolean __isInvisibleDefault;
    public boolean getisInvisibleDefault() {
        return __isInvisibleDefault;
    }

    public void setisInvisibleDefault(boolean value) {
        __isInvisibleDefault = value;
    }

    public int getflags() throws Exception {
        int flags = 0;
        if (gethasCondition())
            flags |= 1;
         
        if (gethasStartContent())
            flags |= 2;
         
        if (gethasChoiceOnlyContent())
            flags |= 4;
         
        if (getisInvisibleDefault())
            flags |= 8;
         
        if (getonceOnly())
            flags |= 16;
         
        return flags;
    }

    public void setflags(int value) throws Exception {
        sethasCondition((value & 1) > 0);
        sethasStartContent((value & 2) > 0);
        sethasChoiceOnlyContent((value & 4) > 0);
        setisInvisibleDefault((value & 8) > 0);
        setonceOnly((value & 16) > 0);
    }

    public ChoicePoint(boolean onceOnly) throws Exception {
        this.setonceOnly(onceOnly);
    }

    public ChoicePoint() throws Exception {
        this(true);
    }

    public String toString() {
        try
        {
            int targetLineNum = DebugLineNumberOfPath(getpathOnChoice());
            
            String targetString = getpathOnChoice().toString();
            
            if (targetLineNum != null)
            {
                targetString = " line " + targetLineNum;
            }
             
            return "Choice: -> " + targetString;
        }
        catch (RuntimeException __dummyCatchVar0)
        {
            throw __dummyCatchVar0;
        }
        catch (Exception __dummyCatchVar0)
        {
            throw new RuntimeException(__dummyCatchVar0);
        }
    
    }

}



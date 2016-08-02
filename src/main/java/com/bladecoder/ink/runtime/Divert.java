package com.bladecoder.ink.runtime;

public class Divert extends RTObject {
	public Path gettargetPath() throws Exception {
		// Resolve any relative paths to global ones as we come across them
		if (_targetPath != null && _targetPath.getisRelative()) {
			RTObject targetObj = gettargetContent();

			if (targetObj != null) {
				_targetPath = targetObj.path;
			}

		}

		return _targetPath;
	}

	public void settargetPath(Path value) throws Exception {
		_targetPath = value;
		_targetContent = null;
	}

	Path _targetPath;

	public RTObject gettargetContent() throws Exception {
		if (_targetContent == null) {
			_targetContent = resolvePath(_targetPath);
		}

		return _targetContent;
	}

	RTObject _targetContent;

	public String gettargetPathString() throws Exception {
		if (gettargetPath() == null)
			return null;

		return compactPathString(gettargetPath());
	}

	public void settargetPathString(String value) throws Exception {
		if (value == null) {
			settargetPath(null);
		} else {
			settargetPath(new Path(value));
		}
	}

	private String __variableDivertName = new String();

	public String getvariableDivertName() {
		return __variableDivertName;
	}

	public void setvariableDivertName(String value) {
		__variableDivertName = value;
	}

	public boolean gethasVariableTarget() throws Exception {
		return getvariableDivertName() != null;
	}

	private boolean __pushesToStack;

	public boolean getpushesToStack() {
		return __pushesToStack;
	}

	public void setpushesToStack(boolean value) {
		__pushesToStack = value;
	}

	public PushPopType stackPushType = PushPopType.Tunnel;
	private boolean __isExternal;

	public boolean getisExternal() {
		return __isExternal;
	}

	public void setisExternal(boolean value) {
		__isExternal = value;
	}

	private int __externalArgs;

	public int getexternalArgs() {
		return __externalArgs;
	}

	public void setexternalArgs(int value) {
		__externalArgs = value;
	}

	private boolean __isConditional;

	public boolean getisConditional() {
		return __isConditional;
	}

	public void setisConditional(boolean value) {
		__isConditional = value;
	}

	public Divert() throws Exception {
		setpushesToStack(false);
	}

	public Divert(PushPopType stackPushType) throws Exception {
		setpushesToStack(true);
		this.stackPushType = stackPushType;
	}

	public boolean equals(RTObject obj) {
		try {
			Divert otherDivert = obj instanceof Divert ? (Divert) obj : (Divert) null;
			if (otherDivert != null) {
				if (this.gethasVariableTarget() == otherDivert.gethasVariableTarget()) {
					if (this.gethasVariableTarget()) {
						return this.getvariableDivertName().equals(otherDivert.getvariableDivertName());
					} else {
						return this.gettargetPath().equals(otherDivert.gettargetPath());
					}
				}

			}

			return false;
		} catch (RuntimeException __dummyCatchVar0) {
			throw __dummyCatchVar0;
		} catch (Exception __dummyCatchVar0) {
			throw new RuntimeException(__dummyCatchVar0);
		}

	}

	public int hashCode() {
		try {
			if (gethasVariableTarget()) {
				int variableTargetSalt = 12345;
				return getvariableDivertName().hashCode() + variableTargetSalt;
			} else {
				int pathTargetSalt = 54321;
				return gettargetPath().hashCode() + pathTargetSalt;
			}
		} catch (RuntimeException __dummyCatchVar1) {
			throw __dummyCatchVar1;
		} catch (Exception __dummyCatchVar1) {
			throw new RuntimeException(__dummyCatchVar1);
		}

	}

	public String toString() {
        try
        {
            if (gethasVariableTarget())
            {
                return "Divert(variable: " + getvariableDivertName() + ")";
            }
            else if (gettargetPath() == null)
            {
                return "Divert(null)";
            }
            else
            {
            	StringBuilder sb = new StringBuilder();
                String targetStr = gettargetPath().toString();
                Integer targetLineNum = debugLineNumberOfPath(gettargetPath());
                if (targetLineNum != null)
                {
                    targetStr = "line " + targetLineNum;
                }
                 
                sb.append("Divert");
                if (getpushesToStack())
                {
                    if (stackPushType == PushPopType.Function)
                    {
                        sb.append(" function");
                    }
                    else
                    {
                        sb.append(" tunnel");
                    } 
                }
                 
                sb.append(" (");
                sb.append(targetStr);
                sb.append(")");
                return sb.toString();
            }  
        }
        catch (RuntimeException __dummyCatchVar2)
        {
            throw __dummyCatchVar2;
        }
        catch (Exception __dummyCatchVar2)
        {
            throw new RuntimeException(__dummyCatchVar2);
        }
    
    }

}

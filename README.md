# blade-ink

This is a Java port of inkle's [ink](https://github.com/inkle/ink), a scripting language for writing interactive narrative.

**blade-ink** should support pretty much everything the original version does. If you find any bugs, please report them here!

## Getting started

### Loading a json file

First you need to turn your ink file into a json file [as described here](https://github.com/inkle/ink#using-inklecate-on-the-command-line).  Here is an example to load the ink JSON file as a String:

```java
InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream(filename);

BufferedReader br = new BufferedReader(new InputStreamReader(systemResourceAsStream, "UTF-8"));

try {
	StringBuilder sb = new StringBuilder();
	String line = br.readLine();

	while (line != null) {
		sb.append(line);
		sb.append("\n");
		line = br.readLine();
	}

} finally {
	br.close();
}

String json = sb.toString().replace('\uFEFF', ' ');
```

### Starting a story

Here's a taster of the code you need to get started:

```java
// 1) Load story
Story story = new Story(sourceJsonString);

// 2) Game content, line by line
while (story.canContinue()) {
	String line = story.Continue();
	System.out.print(line);
}

// 3) Display story.currentChoices list, allow player to choose one
if (story.getCurrentChoices().size() > 0) {
	for (Choice c : story.getCurrentChoices()) {
		System.out.println(c.getText());
	}

	story.chooseChoiceIndex(0);
}

// 4) Back to 2
// ...
```

From there on, you can follow [the official guide](https://github.com/inkle/ink/blob/master/Documentation/RunningYourInk.md#getting-started-with-the-runtime-api). All functions are named exactly the same.

## Integration

The **blade-ink** library is available in the Maven archives.

### Using with Gradle

Add the following line to your `build.gradle` file under the dependencies section of the core project:

```gradle
compile "com.bladecoder.ink:blade-ink:{version}"
```

Replace **{version}** with the newest version number! 

Then simply right-click the project and choose `Gradle->Refresh All`.

### Using with Maven

Right-click on your project and choose `Maven->Add Dependency` and search for **bladecoder**. Make sure to choose the most recent version if multiple appear!

### Eclipse

First, clone this project to your computer and add it to Eclipse. Then simply click on your project, and choose `Build Path->Configure Build Path`. Then go to `Projects->Add` and add the cloned project.

## Sample Projects

There are several open-source sample projects for the **blade-ink** library on different platforms:

* [blade-ink-template](https://github.com/bladecoder/blade-ink-template): LibGDX and Java (Android, Desktop, and IOS)
* [storybytes-android](https://github.com/micabytes/storybytes-android): Kotlin (Android)
* [storybytes-desktop](https://github.com/micabytes/storybytes-desktop): Kotlin and TornadoFX (Desktop)
